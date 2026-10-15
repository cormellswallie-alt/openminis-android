package com.openminis.app.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.openminis.app.MinisApp
import com.openminis.app.data.db.ChatSessionEntity
import com.openminis.app.data.db.MessageEntity
import com.openminis.app.data.repository.ChatRepository
import com.openminis.app.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/**
 * [chat-import] Writes conversations parsed by [ChatImportParser] into this
 * app's own sessions/messages tables so they show up in the session list and
 * can be continued like any other chat.
 *
 * - Messages are stored as `[{"type":"text","value":…}]` — the same parts_json
 *   shape the chat screen writes for plain user/assistant turns.
 * - De-duplication: each session's id is a UUID derived from a hash of its
 *   content, so importing the same file twice skips what already exists.
 * - Original timestamps / order are preserved when the source has them.
 * - Imported sessions are tagged `source = "import"`.
 */
object ChatHistoryImporter {
    private const val TAG = "ChatImport"
    const val SOURCE_TAG = "import"

    /** Rows above this are truncated (Room CursorWindow is 2 MB; stay far below). */
    private const val MAX_TEXT_CHARS = 400_000

    data class Report(
        val conversationsImported: Int,
        val messagesImported: Int,
        val conversationsSkipped: Int,
        val formats: Set<String>,
        val warnings: List<String>,
    )

    sealed class Progress {
        data class Reading(val fileName: String?) : Progress()
        data class Writing(val done: Int, val total: Int) : Progress()
    }

    suspend fun importUri(
        context: Context,
        uri: Uri,
        onProgress: (Progress) -> Unit = {},
    ): Report = withContext(Dispatchers.IO) {
        val name = displayName(context, uri)
        onProgress(Progress.Reading(name))
        val size = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
            }
        }.getOrNull()
        if (size != null && size > ChatImportParser.MAX_INPUT_BYTES) {
            throw ChatImportParser.UnsupportedFormatException("File is too large (${size / (1024 * 1024)} MB)")
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw java.io.IOException("Cannot open the selected file")
        installSqliteOpener(context)
        val parsed = ChatImportParser.parse(bytes, name)
        AppLogger.info(TAG, "parsed file=$name bytes=${bytes.size} conversations=${parsed.conversations.size}")
        val app = context.applicationContext as MinisApp
        write(app, parsed, onProgress)
    }

    private suspend fun write(
        app: MinisApp,
        parsed: ChatImportParser.ParseResult,
        onProgress: (Progress) -> Unit,
    ): Report {
        val dao = app.chatRepository.dao
        var convOk = 0
        var msgOk = 0
        var skipped = 0
        val total = parsed.conversations.size
        // Batches keep each transaction short so the UI's own DB reads are not
        // blocked for the whole import.
        parsed.conversations.chunked(50).forEachIndexed { batchIdx, batch ->
            kotlin.coroutines.coroutineContext.ensureActive()
            app.database.withTransaction {
                for (conv in batch) {
                    val msgs = conv.messages.filter { it.text.isNotBlank() }
                    if (msgs.isEmpty()) { skipped++; continue }
                    val sessionId = stableSessionId(conv, msgs)
                    if (dao.getSession(sessionId) != null) { skipped++; continue }

                    val now = System.currentTimeMillis()
                    val created = conv.createdAt ?: msgs.firstNotNullOfOrNull { it.timestamp } ?: now
                    // Monotonic per-message times: fill gaps by +1s from the previous one.
                    var t = created
                    val times = msgs.map { m ->
                        val ts = m.timestamp?.takeIf { it >= t } ?: (t + 1000)
                        t = ts
                        ts
                    }
                    val title = (conv.title?.trim()?.takeIf { it.isNotEmpty() }
                        ?: (msgs.firstOrNull { it.role == ChatImportParser.Role.USER } ?: msgs.first()).text)
                        .lineSequence().first().take(80)
                    val last = msgs.last()
                    dao.insertSession(
                        ChatSessionEntity(
                            id = sessionId,
                            title = title,
                            modelId = "",
                            createdAt = created,
                            updatedAt = times.last(),
                            lastMessage = last.text.take(200),
                            source = SOURCE_TAG,
                            memoryEnabled = 1,
                        ),
                    )
                    msgs.forEachIndexed { i, m ->
                        val text = if (m.text.length > MAX_TEXT_CHARS) {
                            m.text.take(MAX_TEXT_CHARS) + "\n\n[… truncated during import]"
                        } else m.text
                        dao.insertMessage(
                            MessageEntity(
                                id = UUID.nameUUIDFromBytes("$sessionId#$i".toByteArray()).toString(),
                                sessionId = sessionId,
                                role = if (m.role == ChatImportParser.Role.USER) "user" else "assistant",
                                partsJson = JSONArray().put(JSONObject().put("type", "text").put("value", text)).toString(),
                                createdAt = times[i],
                                sortOrder = i,
                            ),
                        )
                        msgOk++
                    }
                    // Preview through the app's own extractor for consistency with the list.
                    ChatRepository.extractTextPreview(
                        JSONArray().put(JSONObject().put("type", "text").put("value", last.text)).toString(),
                    )?.let { dao.updateLastMessage(sessionId, it, times.last()) }
                    convOk++
                }
            }
            onProgress(Progress.Writing(minOf((batchIdx + 1) * 50, total), total))
        }
        AppLogger.info(TAG, "import done conversations=$convOk messages=$msgOk skipped=$skipped")
        return Report(
            conversationsImported = convOk,
            messagesImported = msgOk,
            conversationsSkipped = skipped,
            formats = parsed.conversations.map { it.source }.toSet(),
            warnings = parsed.warnings,
        )
    }

    /**
     * [chat-import-db] SQLite files are copied to the cache dir and opened
     * READ-ONLY with the platform SQLiteDatabase (no extra dependency). The
     * copy is deleted afterwards along with any -journal / -wal side files.
     */
    private fun installSqliteOpener(context: Context) {
        val cacheDir = context.cacheDir
        ChatImportParser.sqliteOpener = { bytes ->
            val f = java.io.File(cacheDir, "chat-import-${System.nanoTime()}.db")
            try {
                f.writeBytes(bytes)
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    f.path, null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY or
                        android.database.sqlite.SQLiteDatabase.NO_LOCALIZED_COLLATORS,
                )
                try {
                    SqliteChatImport.parse(AndroidDb(db))
                } finally {
                    db.close()
                }
            } catch (e: android.database.sqlite.SQLiteException) {
                throw ChatImportParser.UnsupportedFormatException(
                    "Cannot read this database (${e.message ?: e.javaClass.simpleName}). " +
                        "Encrypted databases (e.g. SQLCipher) are not supported.",
                )
            } finally {
                for (suffix in listOf("", "-journal", "-wal", "-shm")) {
                    java.io.File(f.path + suffix).delete()
                }
            }
        }
    }

    private class AndroidDb(private val db: android.database.sqlite.SQLiteDatabase) : SqliteChatImport.Db {
        override fun tables(): List<String> =
            query("SELECT name FROM sqlite_master WHERE type='table'").mapNotNull { it["name"] as? String }

        override fun columns(table: String): List<String> =
            query("PRAGMA table_info(\"" + table.replace("\"", "\"\"") + "\")").mapNotNull { it["name"] as? String }

        override fun query(sql: String, args: List<Any?>): List<Map<String, Any?>> {
            val out = ArrayList<Map<String, Any?>>()
            db.rawQuery(sql, args.map { it?.toString() }.toTypedArray()).use { c ->
                val names = c.columnNames
                while (c.moveToNext()) {
                    val row = HashMap<String, Any?>(names.size * 2)
                    for (i in names.indices) {
                        val v: Any? = when (c.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> null
                            android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                            android.database.Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
                            else -> c.getString(i)
                        }
                        // Both exact and lower-case keys, so adapters can use either.
                        row[names[i]] = v
                        row[names[i].lowercase()] = v
                    }
                    out.add(row)
                }
            }
            return out
        }
    }

    /** Same content → same id, so re-importing a file does not duplicate chats. */
    private fun stableSessionId(
        conv: ChatImportParser.ImportedConversation,
        msgs: List<ChatImportParser.ImportedMessage>,
    ): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update("minis-import-v1|".toByteArray())
        md.update((conv.title ?: "").toByteArray())
        for (m in msgs) {
            md.update(0)
            md.update(m.role.name.toByteArray())
            md.update(0)
            md.update(m.text.toByteArray())
        }
        return UUID.nameUUIDFromBytes(md.digest()).toString()
    }

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment
}
