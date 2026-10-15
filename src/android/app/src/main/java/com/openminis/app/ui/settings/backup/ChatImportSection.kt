package com.openminis.app.ui.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.openminis.app.R
import com.openminis.app.importer.ChatHistoryImporter
import com.openminis.app.importer.ChatImportParser
import com.openminis.app.logging.AppLogger
import com.openminis.app.ui.settings.SettingsSection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [chat-import] Settings › Backup & Restore › Restore › "Import chat history".
 * Picks a file with the system document picker and imports it via
 * [ChatHistoryImporter].
 */
@Composable
internal fun ChatImportSection(enabled: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf<String?>(null) }
    var resultTitle by remember { mutableStateOf<String?>(null) }
    var resultBody by remember { mutableStateOf<String?>(null) }

    val readingText = stringResource(R.string.chat_import_reading)
    val doneTitle = stringResource(R.string.chat_import_done_title)
    val failedTitle = stringResource(R.string.chat_import_failed_title)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        running = true
        progressText = readingText
        scope.launch {
            try {
                val report = ChatHistoryImporter.importUri(context, uri) { p ->
                    val text = when (p) {
                        is ChatHistoryImporter.Progress.Reading -> readingText
                        is ChatHistoryImporter.Progress.Writing ->
                            context.getString(R.string.chat_import_writing, p.done, p.total)
                    }
                    scope.launch(Dispatchers.Main) { progressText = text }
                }
                resultTitle = doneTitle
                resultBody = context.getString(
                    R.string.chat_import_done_body,
                    report.conversationsImported,
                    report.messagesImported,
                    report.conversationsSkipped,
                    report.formats.joinToString(", ").ifEmpty { "—" },
                ) + if (report.warnings.isNotEmpty()) {
                    "\n\n" + report.warnings.take(5).joinToString("\n")
                } else ""
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: ChatImportParser.UnsupportedFormatException) {
                resultTitle = failedTitle
                resultBody = e.message
            } catch (e: Throwable) {
                AppLogger.error("ChatImport", "import failed: ${e.javaClass.simpleName}: ${e.message}")
                resultTitle = failedTitle
                resultBody = "${e.javaClass.simpleName}: ${e.message ?: ""}"
            } finally {
                withContext(Dispatchers.Main) {
                    running = false
                    progressText = null
                }
            }
        }
    }

    SettingsSection(
        header = stringResource(R.string.chat_import_section),
        footer = stringResource(R.string.chat_import_footer),
    ) {
        RestoreSourceRow(
            icon = Icons.Outlined.Forum,
            iconColor = Color(0xFF34C759),
            label = if (running) progressText ?: readingText else stringResource(R.string.chat_import_row),
            enabled = enabled && !running,
            onClick = {
                launcher.launch(
                    arrayOf(
                        "application/json",
                        "application/zip",
                        "application/x-zip-compressed",
                        "text/*",
                        "application/octet-stream",
                        "application/x-ndjson",
                        // [chat-import-db] SQLite databases
                        "application/vnd.sqlite3",
                        "application/x-sqlite3",
                        "*/*",
                    ),
                )
            },
            showDivider = false,
        )
    }

    if (resultTitle != null) {
        AlertDialog(
            onDismissRequest = { resultTitle = null; resultBody = null },
            title = { Text(resultTitle ?: "") },
            text = { Text(resultBody ?: "") },
            confirmButton = {
                TextButton(onClick = { resultTitle = null; resultBody = null }) {
                    Text(stringResource(R.string.chat_import_ok))
                }
            },
        )
    }
}
