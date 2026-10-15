package com.openminis.app.importer

import com.openminis.app.importer.ChatImportParser.ImportedConversation
import com.openminis.app.importer.ChatImportParser.ImportedMessage
import com.openminis.app.importer.ChatImportParser.Role
import org.json.JSONArray
import org.json.JSONObject

/**
 * [chat-import-db] Import from SQLite `.db` / `.sqlite` files.
 *
 * Kept free of android.database so it can be unit-tested on the JVM: the
 * caller supplies a tiny [Db] adapter (Android: SQLiteDatabase; tests: JDBC).
 *
 * Strategy:
 *  1. Exact adapters for apps whose schema is known (verified against their
 *     sources): RikkaHub, Open WebUI, AnythingLLM, ChatterUI, and this app's
 *     own database (OpenMinis / Minis).
 *  2. Generic fallback: any table with a role-like column + a content-like
 *     column (optionally grouped by a conversation-id column, ordered by a
 *     time / order column), titles taken from a matching conversations table
 *     when one exists. Also catches tables that store a whole conversation as
 *     a JSON column (fed through [ChatImportParser]).
 */
object SqliteChatImport {

    /** Minimal read-only SQLite access. Values: String, Long, Double, ByteArray or null. */
    interface Db {
        fun tables(): List<String>
        fun columns(table: String): List<String>
        fun query(sql: String, args: List<Any?> = emptyList()): List<Map<String, Any?>>
    }

    /** Stop reading after this many rows per table (phones have little heap). */
    private const val MAX_ROWS = 300_000

    fun parse(db: Db): ChatImportParser.ParseResult {
        val tables = db.tables().filterNot { it.startsWith("sqlite_") || it.startsWith("android_") || it == "room_master_table" }
        val cols = tables.associateWith { t -> db.columns(t).map { it.lowercase() }.toSet() }
        val warnings = ArrayList<String>()
        fun tryAdapter(name: String, block: () -> List<ImportedConversation>?): ChatImportParser.ParseResult? =
            try {
                block()?.takeIf { it.isNotEmpty() }?.let { ChatImportParser.ParseResult(it, warnings) }
            } catch (e: Exception) {
                warnings.add("$name: ${e.javaClass.simpleName}: ${e.message}"); null
            }

        // ── known schemas ─────────────────────────────────────────────────
        if (has(cols, "conversationentity", "id", "title", "nodes") || has(cols, "message_node", "conversation_id", "messages")) {
            tryAdapter("RikkaHub") { rikkaHub(db, cols) }?.let { return it }
        }
        if (has(cols, "chat", "id", "title", "chat")) {
            tryAdapter("Open WebUI") { openWebUi(db) }?.let { return it }
        }
        if (has(cols, "workspace_chats", "prompt", "response")) {
            tryAdapter("AnythingLLM") { anythingLlm(db, cols) }?.let { return it }
        }
        if (has(cols, "chat_entries", "chat_id", "is_user") && has(cols, "chat_swipes", "entry_id", "swipe")) {
            tryAdapter("ChatterUI") { chatterUi(db, cols) }?.let { return it }
        }
        if (has(cols, "sessions", "id", "title") && has(cols, "messages", "session_id", "role", "parts_json")) {
            tryAdapter("Minis") { minis(db, cols) }?.let { return it }
        }

        // ── generic ───────────────────────────────────────────────────────
        tryAdapter("generic") { generic(db, tables, cols) }?.let { return it }

        throw ChatImportParser.UnsupportedFormatException(
            "No chat tables recognised in this database (tables: ${tables.take(12).joinToString(", ")}" +
                (if (tables.size > 12) ", …" else "") + ")" +
                (if (warnings.isNotEmpty()) ". " + warnings.take(3).joinToString("; ") else ""),
        )
    }

    // ── adapters ──────────────────────────────────────────────────────────

    /** RikkaHub: ConversationEntity(id,title,nodes,create_at) [+ message_node(conversation_id,node_index,messages,select_index)]. */
    private fun rikkaHub(db: Db, cols: Map<String, Set<String>>): List<ImportedConversation> {
        val convTable = cols.keys.firstOrNull { it.equals("ConversationEntity", true) }
        val nodeTable = cols.keys.firstOrNull { it.equals("message_node", true) }
        val out = ArrayList<ImportedConversation>()
        val convRows = if (convTable != null) db.query("SELECT * FROM ${q(convTable)}") else emptyList()
        val nodesByConv: Map<String, List<Map<String, Any?>>> = if (nodeTable != null) {
            db.query("SELECT * FROM ${q(nodeTable)} ORDER BY conversation_id, node_index").groupBy { s(it["conversation_id"]) ?: "" }
        } else emptyMap()
        val ids = LinkedHashSet<String>()
        convRows.forEach { s(it["id"])?.let(ids::add) }
        nodesByConv.keys.forEach { if (it.isNotEmpty()) ids.add(it) }
        val convById = convRows.associateBy { s(it["id"]) }
        for (id in ids) {
            val row = convById[id]
            val nodes: List<Pair<JSONArray, Int>> = nodesByConv[id]?.mapNotNull { n ->
                val arr = runCatching { JSONArray(s(n["messages"]) ?: return@mapNotNull null) }.getOrNull() ?: return@mapNotNull null
                arr to (num(n["select_index"])?.toInt() ?: 0)
            } ?: run {
                // Older versions: `nodes` column = JSON array of {messages:[…], selectIndex}
                val arr = runCatching { JSONArray(s(row?.get("nodes")) ?: "[]") }.getOrDefault(JSONArray())
                (0 until arr.length()).mapNotNull { i ->
                    val node = arr.optJSONObject(i) ?: return@mapNotNull null
                    (node.optJSONArray("messages") ?: return@mapNotNull null) to node.optInt("selectIndex", 0)
                }
            }
            val msgs = ArrayList<ImportedMessage>()
            for ((arr, sel) in nodes) {
                if (arr.length() == 0) continue
                val m = arr.optJSONObject(sel.coerceIn(0, arr.length() - 1)) ?: continue
                val role = ChatImportParser.mapRole(m.optString("role")) ?: continue
                val text = rikkaParts(m.optJSONArray("parts"))
                if (text.isBlank()) continue
                msgs.add(ImportedMessage(role, text, ChatImportParser.parseTime(m.opt("createdAt"))))
            }
            if (msgs.isEmpty()) continue
            out.add(ImportedConversation(s(row?.get("title")), ChatImportParser.parseTime(row?.get("create_at")), msgs, "RikkaHub"))
        }
        return out
    }

    private fun rikkaParts(parts: JSONArray?): String {
        if (parts == null) return ""
        val sb = StringBuilder()
        var attachments = 0
        for (i in 0 until parts.length()) {
            val p = parts.optJSONObject(i) ?: continue
            val type = p.optString("type").substringAfterLast('.').lowercase()
            when {
                (type == "text" || type.isEmpty()) && p.has("text") -> para(sb, p.optString("text"))
                type in setOf("image", "video", "audio", "document") -> attachments++
            }
        }
        if (attachments > 0) para(sb, "[attachment omitted during import]")
        return sb.toString()
    }

    /** Open WebUI webui.db: chat(id,title,chat JSON{history:{messages,currentId}|messages},created_at s). */
    private fun openWebUi(db: Db): List<ImportedConversation> {
        val out = ArrayList<ImportedConversation>()
        for (row in db.query("SELECT id, title, chat, created_at FROM chat ORDER BY created_at")) {
            val chatRaw = s(row["chat"]) ?: continue
            val chat = runCatching { JSONObject(chatRaw) }.getOrNull() ?: continue
            val msgs = ArrayList<ImportedMessage>()
            val history = chat.optJSONObject("history")
            val map = history?.optJSONObject("messages")
            val current = history?.optString("currentId")?.takeIf { it.isNotEmpty() && it != "null" }
            if (map != null && current != null && map.has(current)) {
                // Walk parentId from the current leaf to the root (main branch only).
                val path = ArrayList<JSONObject>()
                var id: String? = current
                val seen = HashSet<String>()
                while (id != null && map.has(id) && seen.add(id)) {
                    val m = map.getJSONObject(id)
                    path.add(m)
                    id = m.optString("parentId").takeIf { it.isNotEmpty() && it != "null" }
                }
                path.reverse()
                path.forEach { m -> owuMsg(m)?.let(msgs::add) }
            } else {
                val arr = chat.optJSONArray("messages")
                if (arr != null) for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { owuMsg(it) }?.let(msgs::add)
            }
            if (msgs.isEmpty()) continue
            out.add(ImportedConversation(s(row["title"]) ?: chat.optString("title").ifBlank { null },
                ChatImportParser.parseTime(row["created_at"]), merge(msgs), "Open WebUI"))
        }
        return out
    }

    private fun owuMsg(m: JSONObject): ImportedMessage? {
        val role = ChatImportParser.mapRole(m.optString("role")) ?: return null
        val text = when (val c = m.opt("content")) {
            is String -> c
            is JSONArray -> (0 until c.length()).joinToString("\n\n") { c.optJSONObject(it)?.optString("text").orEmpty() }
            else -> ""
        }.trim()
        if (text.isEmpty()) return null
        return ImportedMessage(role, text, ChatImportParser.parseTime(m.opt("timestamp")))
    }

    /** AnythingLLM anythingllm.db: workspace_chats(prompt, response JSON{text}, workspaceId, thread_id, createdAt). */
    private fun anythingLlm(db: Db, cols: Map<String, Set<String>>): List<ImportedConversation> {
        val wsNames = if (cols.containsKey("workspaces")) db.query("SELECT id, name FROM workspaces").associate { s(it["id"]) to s(it["name"]) } else emptyMap()
        val thNames = if (cols.containsKey("workspace_threads")) db.query("SELECT id, name FROM workspace_threads").associate { s(it["id"]) to s(it["name"]) } else emptyMap()
        val hasThread = "thread_id" in (cols["workspace_chats"] ?: emptySet())
        val include = if ("include" in (cols["workspace_chats"] ?: emptySet())) " WHERE include = 1 OR include IS NULL" else ""
        val rows = db.query("SELECT * FROM workspace_chats$include ORDER BY id LIMIT $MAX_ROWS")
        val groups = rows.groupBy { s(it["workspaceId"] ?: it["workspaceid"]) + "#" + (if (hasThread) s(it["thread_id"]) else "") }
        return groups.mapNotNull { (_, list) ->
            val msgs = ArrayList<ImportedMessage>()
            for (r in list) {
                val t = ChatImportParser.parseTime(r["createdAt"] ?: r["createdat"])
                s(r["prompt"])?.trim()?.takeIf { it.isNotEmpty() }?.let { msgs.add(ImportedMessage(Role.USER, it, t)) }
                val resp = s(r["response"]).orEmpty()
                val text = runCatching { JSONObject(resp).optString("text") }.getOrNull()?.takeIf { it.isNotBlank() } ?: resp
                if (text.isNotBlank()) msgs.add(ImportedMessage(Role.ASSISTANT, text.trim(), t))
            }
            if (msgs.isEmpty()) return@mapNotNull null
            val first = list.first()
            val ws = wsNames[s(first["workspaceId"] ?: first["workspaceid"])]
            val th = if (hasThread) thNames[s(first["thread_id"])] else null
            ImportedConversation(listOfNotNull(ws, th).joinToString(" · ").ifBlank { null }, msgs.first().timestamp, merge(msgs), "AnythingLLM")
        }
    }

    /** ChatterUI: chats(id,name,create_date) / chat_entries(chat_id,is_user,order,swipe_id) / chat_swipes(entry_id,swipe,send_date). */
    private fun chatterUi(db: Db, cols: Map<String, Set<String>>): List<ImportedConversation> {
        val names = if (cols.containsKey("chats")) db.query("SELECT * FROM chats").associate { s(it["id"]) to it } else emptyMap()
        val entries = db.query("SELECT * FROM chat_entries ORDER BY chat_id, \"order\", id LIMIT $MAX_ROWS")
        val swipes = db.query("SELECT entry_id, swipe, send_date, id FROM chat_swipes ORDER BY entry_id, id LIMIT $MAX_ROWS")
            .groupBy { s(it["entry_id"]) }
        return entries.groupBy { s(it["chat_id"]) }.mapNotNull { (chatId, list) ->
            val msgs = list.mapNotNull { e ->
                val sw = swipes[s(e["id"])] ?: return@mapNotNull null
                val idx = (num(e["swipe_id"])?.toInt() ?: 0).coerceIn(0, sw.size - 1)
                val text = s(sw[idx]["swipe"])?.trim().orEmpty()
                if (text.isEmpty()) return@mapNotNull null
                val isUser = (num(e["is_user"]) ?: 0.0) != 0.0
                ImportedMessage(if (isUser) Role.USER else Role.ASSISTANT, text, ChatImportParser.parseTime(sw[idx]["send_date"]))
            }
            if (msgs.isEmpty()) return@mapNotNull null
            val c = names[chatId]
            ImportedConversation(s(c?.get("name")), ChatImportParser.parseTime(c?.get("create_date")), merge(msgs), "ChatterUI")
        }
    }

    /** This app's own chat.db (OpenMinis / Minis): sessions + messages(parts_json). */
    private fun minis(db: Db, cols: Map<String, Set<String>>): List<ImportedConversation> {
        val hasParent = "parent_session_id" in (cols["sessions"] ?: emptySet())
        val sessions = db.query("SELECT * FROM sessions" + if (hasParent) " WHERE parent_session_id IS NULL" else "")
        val bySession = db.query("SELECT session_id, role, parts_json, created_at FROM messages ORDER BY session_id, sort_order LIMIT $MAX_ROWS")
            .groupBy { s(it["session_id"]) }
        return sessions.mapNotNull { row ->
            val msgs = bySession[s(row["id"])].orEmpty().mapNotNull { m ->
                val role = ChatImportParser.mapRole(s(m["role"])) ?: return@mapNotNull null
                val arr = runCatching { JSONArray(s(m["parts_json"]) ?: "[]") }.getOrDefault(JSONArray())
                val sb = StringBuilder()
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    if (p.optString("type") == "text") para(sb, p.optString("value"))
                }
                if (sb.isEmpty()) null else ImportedMessage(role, sb.toString(), ChatImportParser.parseTime(m["created_at"]))
            }
            if (msgs.isEmpty()) null
            else ImportedConversation(s(row["title"]), ChatImportParser.parseTime(row["created_at"]), merge(msgs), "Minis")
        }
    }

    // ── generic heuristics ───────────────────────────────────────────────

    private val roleCols = listOf("role", "sender", "author", "speaker", "from", "type", "is_user", "isuser", "from_user", "is_bot")
    private val contentCols = listOf("content", "text", "message", "body", "msg", "value", "parts", "data")
    private val convIdCols = listOf("conversation_id", "conversationid", "chat_id", "chatid", "session_id", "sessionid",
        "thread_id", "threadid", "topic_id", "topicid", "dialog_id", "room_id", "conversation", "chat", "session", "topic")
    private val orderCols = listOf("sort_order", "order", "position", "idx", "index", "seq", "node_index",
        "created_at", "createdat", "create_time", "timestamp", "time", "date", "ts", "rowid")
    private val timeCols = listOf("created_at", "createdat", "create_time", "create_at", "timestamp", "time", "date", "ts", "send_date")
    private val titleCols = listOf("title", "name", "subject", "topic", "summary")

    private fun generic(db: Db, tables: List<String>, cols: Map<String, Set<String>>): List<ImportedConversation>? {
        // (a) per-message tables
        data class Cand(val table: String, val role: String, val content: String, val conv: String?, val score: Int)
        val cands = tables.mapNotNull { t ->
            val c = cols[t] ?: return@mapNotNull null
            val role = roleCols.firstOrNull { it in c } ?: return@mapNotNull null
            val content = contentCols.firstOrNull { it in c } ?: return@mapNotNull null
            val conv = convIdCols.firstOrNull { it in c && it != content && it != role }
            val name = t.lowercase()
            val score = (if (name.contains("message") || name.contains("msg")) 3 else 0) +
                (if (name.contains("chat") || name.contains("convers")) 1 else 0) + (if (conv != null) 2 else 0)
            Cand(t, role, content, conv, score)
        }.sortedByDescending { it.score }

        for (cand in cands) {
            val c = cols[cand.table]!!
            val order = orderCols.firstOrNull { it in c } ?: "rowid"
            val time = timeCols.firstOrNull { it in c }
            val rows = db.query("SELECT *, rowid AS __rowid FROM ${q(cand.table)} ORDER BY " +
                (if (cand.conv != null) "${q(cand.conv)}, " else "") + "${q(order)} LIMIT $MAX_ROWS")
            val titles = cand.conv?.let { conversationTitles(db, tables, cols, cand.table) } ?: emptyMap()
            val groups = if (cand.conv != null) rows.groupBy { s(it[cand.conv]) ?: "" } else mapOf("" to rows)
            val convs = groups.mapNotNull { (cid, list) ->
                val msgs = list.mapNotNull { r ->
                    val role = genericRole(cand.role, r[cand.role]) ?: return@mapNotNull null
                    val text = genericText(r[cand.content])
                    if (text.isBlank()) null else ImportedMessage(role, text, time?.let { ChatImportParser.parseTime(r[it]) })
                }
                if (msgs.size < 1 || msgs.none { it.role == Role.ASSISTANT } && msgs.none { it.role == Role.USER }) return@mapNotNull null
                val t = titles[cid]
                ImportedConversation(t?.first, t?.second ?: msgs.firstOrNull()?.timestamp, merge(msgs), "SQLite: ${cand.table}")
            }.filter { conv -> conv.messages.any { it.role == Role.ASSISTANT } }
            if (convs.isNotEmpty()) return convs
        }

        // (b) whole conversation stored as JSON in one column
        for (t in tables) {
            val c = cols[t] ?: continue
            val jsonCol = c.firstOrNull { it in setOf("chat", "conversation", "messages", "data", "content", "json", "history", "nodes") } ?: continue
            val rows = db.query("SELECT * FROM ${q(t)} LIMIT 20000")
            val convs = ArrayList<ImportedConversation>()
            for (r in rows) {
                val raw = s(r[jsonCol])?.trim() ?: continue
                if (!(raw.startsWith("{") || raw.startsWith("["))) continue
                val parsed = runCatching { ChatImportParser.parseText(raw, null) }.getOrNull() ?: continue
                val title = titleCols.firstOrNull { it in c }?.let { s(r[it]) }
                parsed.conversations.forEach { conv ->
                    convs.add(conv.copy(title = conv.title ?: title, source = "SQLite: $t"))
                }
            }
            if (convs.isNotEmpty()) return convs
        }
        return null
    }

    /** Map conversation id → (title, createdAt) from a sibling table, if one looks like a conversation list. */
    private fun conversationTitles(db: Db, tables: List<String>, cols: Map<String, Set<String>>, msgTable: String): Map<String, Pair<String?, Long?>> {
        val t = tables.filter { it != msgTable }.firstOrNull { name ->
            val c = cols[name] ?: return@firstOrNull false
            "id" in c && titleCols.any { it in c } &&
                (name.lowercase().let { it.contains("conv") || it.contains("chat") || it.contains("session") || it.contains("thread") || it.contains("topic") })
        } ?: return emptyMap()
        val c = cols[t]!!
        val titleCol = titleCols.first { it in c }
        val timeCol = timeCols.firstOrNull { it in c }
        return db.query("SELECT * FROM ${q(t)} LIMIT 100000").associate { r ->
            (s(r["id"]) ?: "") to (s(r[titleCol]) to timeCol?.let { ChatImportParser.parseTime(r[it]) })
        }
    }

    private fun genericRole(col: String, v: Any?): Role? {
        if (col in setOf("is_user", "isuser", "from_user")) {
            val n = num(v) ?: s(v)?.toDoubleOrNull() ?: return null
            return if (n != 0.0) Role.USER else Role.ASSISTANT
        }
        if (col == "is_bot") {
            val n = num(v) ?: s(v)?.toDoubleOrNull() ?: return null
            return if (n != 0.0) Role.ASSISTANT else Role.USER
        }
        val raw = s(v)?.trim()?.lowercase() ?: return null
        ChatImportParser.mapRole(raw)?.let { return it }
        return when (raw) {
            "0", "me", "self", "question", "prompt", "query", "request", "human_message", "humanmessage" -> Role.USER
            "1", "2", "response", "answer", "reply", "ai_message", "aimessage", "llm", "character", "char" -> Role.ASSISTANT
            else -> null
        }
    }

    private fun genericText(v: Any?): String {
        val raw = when (v) {
            is ByteArray -> runCatching { String(v, Charsets.UTF_8) }.getOrDefault("")
            else -> s(v) ?: ""
        }.trim()
        if (raw.startsWith("[") || raw.startsWith("{")) {
            // Content-part arrays / objects: take their text.
            runCatching {
                val any = org.json.JSONTokener(raw).nextValue()
                val sb = StringBuilder()
                fun walk(x: Any?) {
                    when (x) {
                        is JSONArray -> for (i in 0 until x.length()) walk(x.opt(i))
                        is JSONObject -> {
                            val type = x.optString("type").lowercase()
                            when {
                                x.has("text") && (type.isEmpty() || type.contains("text")) -> para(sb, x.optString("text"))
                                x.has("value") && type == "text" -> para(sb, x.optString("value"))
                                x.has("content") && x.opt("content") is String -> para(sb, x.optString("content"))
                                x.has("parts") -> walk(x.opt("parts"))
                            }
                        }
                        is String -> para(sb, x)
                    }
                }
                walk(any)
                if (sb.isNotEmpty()) return sb.toString()
            }
        }
        return raw
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun has(cols: Map<String, Set<String>>, table: String, vararg c: String): Boolean {
        val t = cols.entries.firstOrNull { it.key.equals(table, ignoreCase = true) }?.value ?: return false
        return c.all { it.lowercase() in t }
    }

    private fun q(id: String) = "\"" + id.replace("\"", "\"\"") + "\""

    private fun s(v: Any?): String? = when (v) {
        null -> null
        is ByteArray -> runCatching { String(v, Charsets.UTF_8) }.getOrNull()
        is Double -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
        else -> v.toString()
    }

    private fun num(v: Any?): Double? = when (v) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }

    private fun para(sb: StringBuilder, s: String) {
        val t = s.trim()
        if (t.isEmpty()) return
        if (sb.isNotEmpty()) sb.append("\n\n")
        sb.append(t)
    }

    private fun merge(msgs: List<ImportedMessage>): List<ImportedMessage> {
        val out = ArrayList<ImportedMessage>(msgs.size)
        for (m in msgs) {
            val last = out.lastOrNull()
            if (last != null && last.role == m.role) out[out.lastIndex] = last.copy(text = last.text + "\n\n" + m.text)
            else out.add(m)
        }
        return out
    }
}
