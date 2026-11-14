package com.openminis.app.ui.browser

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.openminis.app.R
import com.openminis.app.browser.BrowserTabPool
import com.openminis.app.ui.chat.StandardChatSheetHeader
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.components.MinisSmallButton
import com.openminis.app.ui.settings.ExpressiveEmptyStateCard
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape

private val DownloadSuccessEmerald = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserDownloadsSheet(
    tabPool: BrowserTabPool,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val downloads by tabPool.downloads.collectAsState()

    LaunchedEffect(Unit) { tabPool.markDownloadsSeen() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            StandardChatSheetHeader(
                title = stringResource(R.string.browser_downloads_title),
                onDismiss = onDismiss,
                leadingAction = if (downloads.any { it.state != BrowserTabPool.DownloadState.DOWNLOADING }) {
                    {
                        MinisSmallButton(
                            onClick = { tabPool.clearFinishedDownloads() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            elevation = null,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                stringResource(R.string.browser_downloads_clear),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else null,
            )
            Spacer(Modifier.height(8.dp))
            if (downloads.isEmpty()) {
                ExpressiveEmptyStateCard(
                    icon = Icons.Default.Download,
                    title = stringResource(R.string.browser_downloads_empty),
                    badgeShape = MaterialShapes.SoftBurst.toShape(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    itemsIndexed(downloads, key = { _, it -> it.id }) { index, entry ->
                        DownloadRow(
                            entry = entry,
                            onCancel = { tabPool.cancelDownload(entry.id) },
                            onOpen = { openDownloadedFile(context, entry) },
                        )
                        if (index < downloads.size - 1) {
                            ExpressiveArticulatedSeam()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    entry: BrowserTabPool.DownloadEntry,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val (icon, tint) = when (entry.state) {
            BrowserTabPool.DownloadState.DOWNLOADING ->
                Icons.Default.Download to MaterialTheme.colorScheme.primary
            BrowserTabPool.DownloadState.COMPLETED ->
                Icons.Default.CheckCircle to DownloadSuccessEmerald
            BrowserTabPool.DownloadState.FAILED ->
                Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                BrowserTabPool.middleTruncated(entry.filename, head = 24, tail = 12),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            when (entry.state) {
                BrowserTabPool.DownloadState.DOWNLOADING -> {
                    val context = LocalContext.current
                    val doneText = android.text.format.Formatter
                        .formatShortFileSize(context, entry.bytesDone)
                    val statusText = if (entry.totalBytes > 0) {
                        val totalText = android.text.format.Formatter
                            .formatShortFileSize(context, entry.totalBytes)
                        val pct = (entry.bytesDone * 100 / entry.totalBytes).toInt()
                        "$pct% · $doneText / $totalText"
                    } else {
                        doneText
                    }
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (entry.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = {
                                (entry.bytesDone.toFloat() / entry.totalBytes).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        )
                    }
                }
                BrowserTabPool.DownloadState.COMPLETED -> {
                    val context = LocalContext.current
                    Text(
                        android.text.format.Formatter
                            .formatShortFileSize(context, entry.bytesDone),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BrowserTabPool.DownloadState.FAILED -> {
                    Text(
                        entry.failureReason
                            ?: stringResource(R.string.browser_downloads_failed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        when (entry.state) {
            BrowserTabPool.DownloadState.DOWNLOADING -> {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = stringResource(R.string.browser_downloads_cancel),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            BrowserTabPool.DownloadState.COMPLETED -> {
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = stringResource(R.string.browser_downloads_open),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            BrowserTabPool.DownloadState.FAILED -> Unit
        }
    }
}

private fun openDownloadedFile(context: Context, entry: BrowserTabPool.DownloadEntry) {
    val file = entry.destination ?: return
    if (!file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        val ext = file.extension.lowercase()
        val mime = android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(ext) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (e: Exception) {
        android.util.Log.w("BrowserDownloads", "open failed: ${e.message}")
    }
}
