package com.openminis.app.importer

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * [chat-import] Pure parsing of third-party chat exports into a neutral model.
 * No Android / DB dependencies so it is unit-testable on the JVM.
 *
 * Supported (auto-detected by content, not extension):
 *  - ChatGPT export `conversations.json` (mapping tree; main branch via current_node)
 *  - Claude.ai export `conversations.json` (chat_messages)
 *  - OpenAI-style `{"messages":[{role,content}]}`, a bare array of such messages,
 *    or an array of such conversations; also `{"conversations":[...]}`
 *  - Gemini / generic `{"contents":[{role,parts:[{text}]}]}`
 *  - JSONL: one message object per line (or one conversation object per line)
 *  - Markdown / plain-text transcripts with role headers
 *  - SQLite databases (.db / .sqlite) — see [SqliteChatImport]
 *  - ZIP archives containing any of the above (e.g. the ChatGPT / Claude export zip)
 */
object ChatImportParser {

    enum class Role { USER, ASSISTANT }

    data class ImportedMessage(
        val role: Role,
        val text: String,
        /** Epoch millis, or null when the source has none. */
        val timestamp: Long? = null,
    )

    data class ImportedConversation(
        val title: String?,
        val createdAt: Long?,
        val messages: List<ImportedMessage>,
        /** Format label, for the report. */
        val source: String,
    )

    data class ParseResult(
        val conversations: List<ImportedConversation>,
        /** Human-readable notes (skipped files, unsupported entries). */
        val warnings: List<String> = emptyList(),
    )

    class UnsupportedFormatException(message: String) : Exception(message)

    /** Hard bound on what we accept, to stay well clear of OOM on phones. */
    const val MAX_INPUT_BYTES = 200L * 1024 * 1024

    private const val ATTACHMENT_NOTE = "[attachment omitted during import]"

    /**
     * [chat-import-db] Opens a SQLite file and parses it. Installed by the
     * Android importer (SQLiteDatabase); unit tests may install a JDBC one.
     * Null = SQLite unsupported in this environment.
     */
    @Volatile
    var sqliteOpener: ((ByteArray) -> ParseResult)? = null

    fun isSqlite(b: ByteArray): Boolean {
        val magic = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)
        if (b.size < magic.size) return false
        for (i in magic.indices) if (b[i] != magic[i]) return false
        return true
    }

    private fun parseSqlite(bytes: ByteArray): ParseResult =
        (sqliteOpener ?: throw UnsupportedFormatException("SQLite databases are not supported here"))(bytes)

    fun parse(bytes: ByteArray, fileName: String? = null): ParseResult {
        if (isSqlite(bytes)) return parseSqlite(bytes)
        if (isZip(bytes)) return parseZip(bytes)
        val text = decodeText(bytes)
        return parseText(text, fileName)
    }

    // ── container formats ─────────────────────────────────────────────────

    private fun isZip(b: ByteArray) =
        b.size >= 4 && b[0] == 0x50.toByte() && b[1] == 0x4B.toByte() && b[2] == 0x03.toByte() && b[3] == 0x04.toByte()

    private fun parseZip(bytes: ByteArray): ParseResult {
        val convs = ArrayList<ImportedConversation>()
        val warnings = ArrayList<String>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                if (e.isDirectory) continue
                val name = e.name
                val lower = name.lowercase()
                val isDbEntry = lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".sqlite3")
                val interesting = isDbEntry || lower.endsWith(".json") || lower.endsWith(".jsonl") ||
                    lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt")
                if (!interesting || lower.contains("__macosx/")) continue
                // Skip export side-files that are not conversations.
                val base = lower.substringAfterLast('/')
                if (base in setOf("user.json", "users.json", "projects.json", "message_feedback.json",
                        "model_comparisons.json", "shared_conversations.json", "chat.html")
                ) continue
                val data = zin.readBytes()
                total += data.size
                if (total > MAX_INPUT_BYTES) {
                    warnings.add("Archive too large; stopped at $name")
                    break
                }
                try {
                    val r = if (isSqlite(data)) parseSqlite(data) else parseText(decodeText(data), name)
                    convs.addAll(r.conversations)
                    warnings.addAll(r.warnings)
                } catch (ex: UnsupportedFormatException) {
                    warnings.add("$name: ${ex.message}")
                } catch (ex: Exception) {
                    warnings.add("$name: ${ex.javaClass.simpleName}: ${ex.message}")
                }
            }
        }
        if (convs.isEmpty()) {
            throw UnsupportedFormatException(
                "No conversations found in the archive" +
                    (if (warnings.isNotEmpty()) " (${warnings.take(3).joinToString("; ")})" else ""),
            )
        }
        return ParseResult(convs, warnings)
    }

    private fun decodeText(bytes: ByteArray): String {
        // Strip UTF-8 BOM; treat everything as UTF-8 (all supported exports are).
        val start = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) 3 else 0
        return String(bytes, start, bytes.size - start, Charsets.UTF_8)
    }

    fun parseText(raw: String, fileName: String? = null): ParseResult {
        val text = raw.trim()
        if (text.isEmpty()) throw UnsupportedFormatException("File is empty")
        if (text.startsWith("{") || text.startsWith("[")) {
            val json = runCatching { org.json.JSONTokener(text).nextValue() }.getOrNull()
            if (json != null && (json is JSONObject || json is JSONArray)) {
                // A single JSON value must span the whole file; otherwise it is JSONL.
                val consumedAll = runCatching {
                    val t = org.json.JSONTokener(text); t.nextValue(); t.nextClean() == 0.toChar()
                }.getOrDefault(false)
                if (consumedAll) return parseJson(json, fileName)
            }
            parseJsonl(text)?.let { return it }
        }
        return parseTranscript(text, fileName)
    }

    // ── JSON ──────────────────────────────────────────────────────────────

    private fun parseJson(json: Any, fileName: String?): ParseResult {
        val convs = ArrayList<ImportedConversation>()
        when (json) {
            is JSONArray -> {
                if (json.length() == 0) throw UnsupportedFormatException("JSON array is empty")
                val first = json.opt(0)
                if (first is JSONObject && looksLikeMessage(first)) {
                    // A bare array of messages = one conversation.
                    val msgs = messagesFromOpenAIArray(json)
                    if (msgs.isNotEmpty()) convs.add(ImportedConversation(titleFromFile(fileName), null, msgs, "OpenAI messages"))
                } else {
                    for (i in 0 until json.length()) {
                        val o = json.optJSONObject(i) ?: continue
                        conversationFromObject(o, fileName)?.let { convs.add(it) }
                    }
                }
            }
            is JSONObject -> {
                val list = json.optJSONArray("conversations") ?: json.optJSONArray("chats")
                    ?: json.optJSONArray("sessions") ?: json.optJSONArray("data")
                if (list != null && list.length() > 0 && list.opt(0) is JSONObject && !looksLikeMessage(list.getJSONObject(0))) {
                    for (i in 0 until list.length()) {
                        val o = list.optJSONObject(i) ?: continue
                        conversationFromObject(o, fileName)?.let { convs.add(it) }
                    }
                } else {
                    conversationFromObject(json, fileName)?.let { convs.add(it) }
                }
            }
        }
        if (convs.isEmpty()) throw UnsupportedFormatException("No recognisable conversations in JSON")
        return ParseResult(convs)
    }

    private fun looksLikeMessage(o: JSONObject): Boolean =
        (o.has("role") || o.has("sender") || o.has("author")) &&
            (o.has("content") || o.has("text") || o.has("parts") || o.has("message"))

    private fun conversationFromObject(o: JSONObject, fileName: String?): ImportedConversation? {
        // ChatGPT export
        if (o.has("mapping") && o.optJSONObject("mapping") != null) return parseChatGpt(o)
        // Claude.ai export
        if (o.has("chat_messages")) return parseClaude(o)
        val title = firstString(o, "title", "name", "topic", "subject") ?: titleFromFile(fileName)
        val created = parseTime(o.opt("create_time") ?: o.opt("created_at") ?: o.opt("createdAt")
            ?: o.opt("created") ?: o.opt("timestamp") ?: o.opt("date"))
        // OpenAI style / generic
        val arr = o.optJSONArray("messages") ?: o.optJSONArray("history") ?: o.optJSONArray("conversation")
            ?: o.optJSONArray("chat") ?: o.optJSONArray("items")
        if (arr != null) {
            val msgs = messagesFromOpenAIArray(arr)
            return if (msgs.isEmpty()) null else ImportedConversation(title, created, msgs, "JSON messages")
        }
        // Gemini API style
        o.optJSONArray("contents")?.let { contents ->
            val msgs = ArrayList<ImportedMessage>()
            for (i in 0 until contents.length()) {
                val c = contents.optJSONObject(i) ?: continue
                val role = mapRole(c.optString("role")) ?: continue
                val t = textFromParts(c.optJSONArray("parts"))
                if (t.isNotBlank()) msgs.add(ImportedMessage(role, t))
            }
            return if (msgs.isEmpty()) null else ImportedConversation(title, created, msgs, "Gemini contents")
        }
        // A single message object on its own is not a conversation.
        return null
    }

    private fun messagesFromOpenAIArray(arr: JSONArray): List<ImportedMessage> {
        val out = ArrayList<ImportedMessage>()
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i) ?: continue
            messageFromGeneric(m)?.let { out.add(it) }
        }
        return mergeAdjacent(out)
    }

    private fun messageFromGeneric(m: JSONObject): ImportedMessage? {
        val roleRaw = m.optString("role").ifEmpty {
            m.optString("sender").ifEmpty {
                m.optJSONObject("author")?.optString("role") ?: m.optString("author")
            }
        }
        val role = mapRole(roleRaw) ?: return null
        val text = when (val c = m.opt("content") ?: m.opt("text") ?: m.opt("message")) {
            is String -> c
            is JSONArray -> textFromParts(c)
            is JSONObject -> c.optJSONArray("parts")?.let { textFromParts(it) } ?: c.optString("text")
            else -> textFromParts(m.optJSONArray("parts"))
        }.trim()
        if (text.isEmpty()) return null
        val ts = parseTime(m.opt("timestamp") ?: m.opt("created_at") ?: m.opt("create_time") ?: m.opt("time"))
        return ImportedMessage(role, text, ts)
    }

    /** OpenAI content-part arrays, Claude content blocks, Gemini parts, plain strings. */
    private fun textFromParts(parts: JSONArray?): String {
        if (parts == null) return ""
        val sb = StringBuilder()
        var attachments = 0
        for (i in 0 until parts.length()) {
            when (val p = parts.opt(i)) {
                is String -> appendPara(sb, p)
                is JSONObject -> {
                    val type = p.optString("type").lowercase()
                    when {
                        p.has("text") && (type.isEmpty() || type == "text" || type == "input_text" || type == "output_text") ->
                            appendPara(sb, p.optString("text"))
                        type.contains("image") || type.contains("file") || type.contains("audio") ||
                            p.has("image_url") || p.has("inline_data") || p.has("inlineData") ||
                            p.optString("content_type").contains("asset_pointer") -> attachments++
                        type == "tool_use" || type == "tool_result" || type == "thinking" -> Unit
                        p.has("text") -> appendPara(sb, p.optString("text"))
                    }
                }
            }
        }
        if (attachments > 0) appendPara(sb, ATTACHMENT_NOTE)
        return sb.toString()
    }

    private fun appendPara(sb: StringBuilder, s: String) {
        val t = s.trim()
        if (t.isEmpty()) return
        if (sb.isNotEmpty()) sb.append("\n\n")
        sb.append(t)
    }

    // ChatGPT: follow current_node → parents to root, then reverse.
    private fun parseChatGpt(o: JSONObject): ImportedConversation? {
        val mapping = o.getJSONObject("mapping")
        val path = ArrayList<String>()
        var node: String? = o.optString("current_node").ifEmpty { null }
        if (node == null || !mapping.has(node)) {
            // No current_node: take the deepest leaf by following the last child from the root.
            val root = mapping.keys().asSequence().firstOrNull { k ->
                val n = mapping.optJSONObject(k); n != null && (n.isNull("parent") || !mapping.has(n.optString("parent")))
            }
            var cur = root
            while (cur != null) {
                path.add(cur)
                val children = mapping.optJSONObject(cur)?.optJSONArray("children")
                cur = if (children != null && children.length() > 0) children.optString(children.length() - 1) else null
                if (path.size > 100_000) break
            }
        } else {
            val seen = HashSet<String>()
            while (node != null && mapping.has(node) && seen.add(node)) {
                path.add(node)
                val p = mapping.optJSONObject(node)?.opt("parent")
                node = if (p is String && p.isNotEmpty()) p else null
            }
            path.reverse()
        }
        val msgs = ArrayList<ImportedMessage>()
        for (id in path) {
            val msg = mapping.optJSONObject(id)?.optJSONObject("message") ?: continue
            val role = mapRole(msg.optJSONObject("author")?.optString("role")) ?: continue
            val meta = msg.optJSONObject("metadata")
            if (meta?.optBoolean("is_visually_hidden_from_conversation", false) == true) continue
            val content = msg.optJSONObject("content") ?: continue
            val ctype = content.optString("content_type")
            val text = when (ctype) {
                "text", "multimodal_text", "" -> textFromParts(content.optJSONArray("parts"))
                "code" -> content.optString("text").let { if (it.isBlank()) "" else "```\n$it\n```" }
                else -> "" // thoughts, tether_browsing_display, system_error, user_editable_context …
            }.trim()
            if (text.isEmpty()) continue
            msgs.add(ImportedMessage(role, text, parseTime(msg.opt("create_time"))))
        }
        if (msgs.isEmpty()) return null
        return ImportedConversation(
            title = o.optString("title").ifBlank { null },
            createdAt = parseTime(o.opt("create_time")),
            messages = mergeAdjacent(msgs),
            source = "ChatGPT",
        )
    }

    private fun parseClaude(o: JSONObject): ImportedConversation? {
        val arr = o.optJSONArray("chat_messages") ?: return null
        val msgs = ArrayList<ImportedMessage>()
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i) ?: continue
            val role = mapRole(m.optString("sender")) ?: continue
            var text = textFromParts(m.optJSONArray("content")).trim()
            if (text.isEmpty()) text = m.optString("text").trim()
            val atts = (m.optJSONArray("attachments")?.length() ?: 0) + (m.optJSONArray("files")?.length() ?: 0)
            if (atts > 0 && !text.contains(ATTACHMENT_NOTE)) text = if (text.isEmpty()) ATTACHMENT_NOTE else "$text\n\n$ATTACHMENT_NOTE"
            if (text.isEmpty()) continue
            msgs.add(ImportedMessage(role, text, parseTime(m.opt("created_at"))))
        }
        if (msgs.isEmpty()) return null
        return ImportedConversation(
            title = o.optString("name").ifBlank { null },
            createdAt = parseTime(o.opt("created_at")),
            messages = mergeAdjacent(msgs),
            source = "Claude",
        )
    }

    // ── JSONL ─────────────────────────────────────────────────────────────

    private fun parseJsonl(text: String): ParseResult? {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return null
        val objs = ArrayList<JSONObject>(lines.size)
        for (l in lines) objs.add(runCatching { JSONObject(l) }.getOrNull() ?: return null)
        return if (objs.all { looksLikeMessage(it) }) {
            val msgs = mergeAdjacent(objs.mapNotNull { messageFromGeneric(it) })
            if (msgs.isEmpty()) null else ParseResult(listOf(ImportedConversation(null, msgs.firstOrNull()?.timestamp, msgs, "JSONL")))
        } else {
            val convs = objs.mapNotNull { conversationFromObject(it, null) }
            if (convs.isEmpty()) null else ParseResult(convs)
        }
    }

    // ── Markdown / text transcript ────────────────────────────────────────

    private val userWords = listOf("user", "human", "you", "me", "q", "question", "用户", "我", "提问", "问")
    private val assistantWords = listOf(
        "assistant", "ai", "bot", "chatgpt", "gpt", "claude", "gemini", "model", "a", "answer",
        "助手", "回答", "答", "机器人", "deepseek", "kimi", "豆包", "通义", "文心",
    )

    private fun headerRole(line: String): Pair<Role, String>? {
        // Strip markdown decoration: "## ", "### ", "**", leading "> "
        var l = line.trim()
        l = l.trimStart('#', ' ', '>').trim()
        l = l.removePrefix("**").trim()
        // Header forms: "User", "User:", "**User:**", "User：text"
        val sepIdx = l.indexOfFirst { it == ':' || it == '：' }
        val head: String
        val rest: String
        if (sepIdx in 1..24) {
            head = l.substring(0, sepIdx).trim().trim('*').trim()
            rest = l.substring(sepIdx + 1).trim().trimStart('*').trim()
        } else if (line.trim().startsWith("#") || line.trim().startsWith("**")) {
            head = l.trim('*').trim()
            rest = ""
        } else {
            return null
        }
        val key = head.lowercase().replace(Regex("\\s*\\(.*\\)$"), "").trim()
        return when {
            key in userWords -> Role.USER to rest
            key in assistantWords -> Role.ASSISTANT to rest
            else -> null
        }
    }

    private fun parseTranscript(text: String, fileName: String?): ParseResult {
        val msgs = ArrayList<ImportedMessage>()
        var curRole: Role? = null
        val buf = StringBuilder()
        var title: String? = null
        var inFence = false
        fun flush() {
            val r = curRole ?: return
            val t = buf.toString().trim().trimEnd('-').trim()
            if (t.isNotEmpty()) msgs.add(ImportedMessage(r, t))
            buf.setLength(0)
        }
        for (line in text.lines()) {
            if (line.trimStart().startsWith("```")) inFence = !inFence
            val hdr = if (inFence) null else headerRole(line)
            if (hdr != null) {
                flush()
                curRole = hdr.first
                if (hdr.second.isNotEmpty()) buf.append(hdr.second).append('\n')
                continue
            }
            if (curRole == null) {
                // Leading "# Title" before the first role header.
                val t = line.trim()
                if (title == null && t.startsWith("# ")) title = t.removePrefix("# ").trim()
                continue
            }
            buf.append(line).append('\n')
        }
        flush()
        if (msgs.size < 2 || msgs.none { it.role == Role.ASSISTANT }) {
            throw UnsupportedFormatException(
                "Unrecognised format. Supported: ChatGPT / Claude exports (.json or .zip), " +
                    "OpenAI-style JSON / JSONL, SQLite databases (.db), or a Markdown transcript with User/Assistant headers.",
            )
        }
        return ParseResult(listOf(ImportedConversation(title ?: titleFromFile(fileName), null, mergeAdjacent(msgs), "Transcript")))
    }

    // ── helpers ───────────────────────────────────────────────────────────

    internal fun mapRole(raw: String?): Role? = when (raw?.trim()?.lowercase()) {
        "user", "human", "you", "me" -> Role.USER
        "assistant", "ai", "bot", "model", "gpt", "chatgpt", "claude", "gemini" -> Role.ASSISTANT
        else -> null // system, tool, function, developer …
    }

    /** Two consecutive messages with the same role are merged (keeps strict alternation readable). */
    private fun mergeAdjacent(msgs: List<ImportedMessage>): List<ImportedMessage> {
        if (msgs.size < 2) return msgs
        val out = ArrayList<ImportedMessage>(msgs.size)
        for (m in msgs) {
            val last = out.lastOrNull()
            if (last != null && last.role == m.role) {
                out[out.lastIndex] = last.copy(text = last.text + "\n\n" + m.text)
            } else {
                out.add(m)
            }
        }
        return out
    }

    private fun firstString(o: JSONObject, vararg keys: String): String? {
        for (k in keys) {
            val v = o.optString(k, "").trim()
            if (v.isNotEmpty() && v != "null") return v
        }
        return null
    }

    private fun titleFromFile(fileName: String?): String? =
        fileName?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf {
            it.isNotBlank() && it.lowercase() !in setOf("conversations", "conversation", "chat", "messages", "export")
        }

    /** Seconds (float) / millis / ISO-8601 → epoch millis. */
    internal fun parseTime(v: Any?): Long? = when (v) {
        null, JSONObject.NULL -> null
        is Number -> {
            val d = v.toDouble()
            when {
                d <= 0 -> null
                d < 1e11 -> (d * 1000).toLong()   // seconds
                d < 1e14 -> d.toLong()            // millis
                else -> (d / 1000).toLong()       // micros
            }
        }
        is String -> v.trim().let { s ->
            s.toDoubleOrNull()?.let { parseTime(it) }
                ?: runCatching { java.time.Instant.parse(s).toEpochMilli() }.getOrNull()
                ?: runCatching { java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli() }.getOrNull()
                ?: runCatching {
                    java.time.LocalDateTime.parse(s.replace(' ', 'T'))
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrNull()
        }
        else -> null
    }
}
