package com.openminis.app.ui.chat

// [T-android-split-chat] Assistant-message + tool-pill + thinking rendering
// extracted verbatim from ChatScreen.kt: AssistantHeader, AssistantMessageView,
// BoundsTrackedBlock, InlineErrorBanner, ToolStopButton,
// formatToolDetailsForClipboard, ToolCallPill, ThinkingBlock.
// Full import block copied (unused=warnings); externally-called ones internal.

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.automirrored.filled.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.openminis.app.BuildConfig
import com.openminis.app.R
import com.openminis.app.data.FileMentionIndex
import com.openminis.app.logging.AppLogger
import com.openminis.app.ui.components.MinisAlertDialog
import com.openminis.app.ui.settings.autoExpandThinkingEnabled
import com.openminis.app.ui.components.MinisMenu
import com.openminis.app.ui.components.MinisMenuDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.ExpressiveLoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.openminis.app.ui.theme.toShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.openminis.app.offload.OffloadPermissionManager
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import com.openminis.app.data.model.LLMModel
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.model.ModelGroup
import com.openminis.app.data.model.ProviderConfig
import com.openminis.app.data.model.ProviderType
import com.openminis.app.data.model.RoutingStrategy
import com.openminis.app.data.model.ThinkingLevel
import com.openminis.app.data.repository.ChatRepository
import com.openminis.app.data.repository.MemoryRepository
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.ui.browser.BrowserSheet
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.components.MinisTextButton
import com.openminis.app.ui.components.DecorativeSpinner
import com.openminis.app.ui.components.rememberDecorativeTick
import com.openminis.app.ui.components.decorativePhase
import androidx.compose.ui.draw.drawWithCache

@Composable
internal fun AssistantHeader() {
    val soulMeta by com.openminis.app.agent.SoulStore.cachedMetadata.collectAsState()
    val displayName = soulMeta.name.ifBlank { com.openminis.app.agent.SoulMetadata.DEFAULT.name }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 14.dp, bottom = 6.dp),
    ) {
        val sparkleGradient = Brush.linearGradient(
            colors = listOf(SparkleColor1, SparkleColor2),
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Sunny.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            com.openminis.app.ui.settings.SoulIconGlyph(
                icon = soulMeta.icon,
                sizeDp = 16.dp,
                emojiSp = 13.sp,
                sparkleTint = sparkleGradient,
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.1).sp,
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * [T-android-usage-capsule-time] Token usage for a finished assistant turn,
 * plus the wall-clock time it finished — "ctx:57k in:2 out:408 cache:57k 22:30".
 *
 * Port of iOS `usageCapsule` / `usageSummary` (ChatMessageViews.swift:688,718):
 * same field order, same k-abbreviation, same rule that ctx / cache /
 * +cache appear only when non-zero. The time is the Android-side addition and
 * is appended last, in the same monospaced style, so the line reads as one
 * unit rather than a separate badge.
 *
 * Renders nothing without usage, which also covers a still-streaming turn and
 * any user message.
 */
@Composable
internal fun UsageCapsule(usage: ChatTokenUsage?, completedAt: Long?) {
    if (usage == null) return
    // Counts only. The time is a separate Text so it can be pushed to the far
    // edge; concatenating it made the whole line read as one run of digits.
    val summary = remember(usage) {
        buildString {
            if (usage.latestContextTokens > 0) append("ctx:${formatTokenCount(usage.latestContextTokens)} ")
            append("in:${formatTokenCount(usage.inputTokens)}")
            append(" out:${formatTokenCount(usage.outputTokens)}")
            if (usage.cacheReadTokens > 0) append(" cache:${formatTokenCount(usage.cacheReadTokens)}")
            if (usage.cacheCreationTokens > 0) append(" +cache:${formatTokenCount(usage.cacheCreationTokens)}")
        }
    }
    // 24-hour HH:mm, no seconds. Locale.US pins the pattern's digits while the
    // DEFAULT time zone keeps it the user's local clock — a locale-formatted
    // time could render 12-hour with AM/PM.
    val clock = remember(completedAt) {
        completedAt?.let {
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(it))
        }
    }
    val ink = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                    alpha = if (ChatColors.isDark) 0.52f else 0.72f,
                ),
                CircleShape,
            )
            .border(
                0.75.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
                CircleShape,
            )
            .clip(CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        // iOS uses the `speedometer` SF Symbol; Speed is the same idea in
        // Material's set.
        Icon(
            Icons.Default.Speed,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = summary,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = ink,
            maxLines = 1,
        )
        clock?.let {
            // Pushed to the trailing edge so the finish time reads as its own
            // fact rather than another number in the counts.
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = ink,
                maxLines = 1,
            )
        }
    }
}

/**
 * 1234 -> "1.2k", 57000 -> "57k". Mirrors iOS `formatTokenCount`: a whole
 * multiple of 1000 drops the decimal, anything else keeps one place.
 */
private fun formatTokenCount(count: Int): String {
    if (count < 1000) return count.toString()
    val k = count / 1000.0
    return if (k % 1.0 == 0.0) "${k.toInt()}k" else String.format(java.util.Locale.US, "%.1fk", k)
}

@Composable
internal fun AssistantMessageView(
    message: ChatMessage,
    onRetry: (() -> Unit)? = null,
    // [T-android-usage-capsule-style] Whether this message's usage capsule is
    // currently revealed. Owned by the ViewModel so the flat renderer — which
    // emits the capsule as a separate list item — can share the same state.
    revealed: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        AssistantHeader()

        // Render blocks in original order — text, thinking, and tool calls interleaved
        // exactly as they arrived in the stream (each assistant turn may contain multiple
        // text ↔ tool_use transitions, which must be preserved for coherent reading).
        val toolPillBlocks = message.toolBlocks.filter { it.kind == "tool_use" }
        val lastThinkingId = message.toolBlocks.lastOrNull { it.kind == "thinking" }?.id
        // [T-android-thinking-auto-collapse] Mirror the FlatChatItem path's
        // `isLastBlockOverall` so the legacy renderer's ThinkingBlock also
        // flips !isStreaming when a sibling block arrives (id of the last
        // block of ANY kind in the message). See ChatFlatItems builder.
        val lastBlockIdOverall = message.toolBlocks.lastOrNull()?.id
        // Backward compat: if there are no text blocks but message.content is non-empty
        // (e.g. legacy sessions saved before the text-block migration), fall back to
        // rendering message.content after all tool blocks.
        val hasAnyTextBlock = message.toolBlocks.any { it.kind == "text" }
        val lastTextBlockIndex = message.toolBlocks.indexOfLast { it.kind == "text" }
        message.toolBlocks.forEachIndexed { index, block ->
            when (block.kind) {
                "thinking" -> {
                    // T300: same per-message thinking-level gate as the
                    // FlatChatItem path. AssistantMessageView is currently
                    // unreferenced (legacy pre-FlatChatItem code) but the
                    // gate stays here so any future re-introduction
                    // doesn't silently bring back the always-render bug.
                    val effectiveLevel = message.thinkingLevel
                        ?: com.openminis.app.data.model.ThinkingLevel.MEDIUM
                    if (effectiveLevel.isEnabled) {
                        // [T-android-thinking-auto-collapse] Stream signal
                        // requires THIS block to be the trailing block of
                        // any kind, not just the last thinking — see
                        // FlatChatItem path + iOS ThinkingBlockView parity.
                        val isTrailingThinking = block.id == lastBlockIdOverall
                        ThinkingBlock(
                            block,
                            isStreaming = isTrailingThinking && message.isStreaming,
                            isLast = block.id == lastThinkingId,
                        )
                    }
                }
                "info" -> {
                    FallbackInfoBlock(block)
                }
                "text" -> {
                    if (block.content.isNotEmpty()) {
                        val isLastTextBlock = index == lastTextBlockIndex
                        val streaming = message.isStreaming && isLastTextBlock
                        // T-android-gc-storm-issue17: defensive guard on the legacy
                        // pre-FlatChatItem path too.
                        LargeContentGuard(
                            content = block.content,
                            isStreaming = streaming,
                            stableKey = "legacy-text:${message.id}:${block.id}",
                        ) {
                            StreamingMarkdownText(
                                content = block.content,
                                // Only the trailing text block is "still streaming"; earlier
                                // text blocks (before a tool call) are frozen.
                                isStreaming = streaming,
                            )
                        }
                    }
                }
                else -> {
                    // tool_use
                    ToolCallPill(block, allToolBlocks = toolPillBlocks)
                }
            }
        }

        // Typing indicator when streaming with no content yet (info-only blocks don't count)
        val hasRealBlocks = message.toolBlocks.any { it.kind != "info" }
        if (message.isStreaming && message.content.isEmpty() && !hasRealBlocks) {
            TypingIndicator()
        }

        // Legacy fallback: render message.content when no text blocks exist (old sessions).
        if (!hasAnyTextBlock && message.content.isNotEmpty()) {
            LargeContentGuard(
                content = message.content,
                isStreaming = message.isStreaming,
                stableKey = "legacy-fallback:${message.id}",
            ) {
                StreamingMarkdownText(
                    content = message.content,
                    isStreaming = message.isStreaming,
                )
            }
        }

        // Inline error banner (iOS: red exclamation + error text + Retry button)
        if (message.error != null) {
            InlineErrorBanner(error = message.error, onRetry = onRetry)
        }

        // [T-android-usage-capsule-time] Last line of the turn, after the error
        // banner: the usage belongs to the run as a whole, including a run that
        // ended badly.
        // Same !isStreaming gate the flat path applies, so both renderers
        // reveal the capsule at the same moment.
        //
        // [T-android-usage-capsule-style] Revealed only while the user has
        // toggled this message on. The tap that sets it lives in the flat
        // renderer, which is the path that actually runs; this legacy view
        // honours the same state so a future revival cannot regress to the
        // always-visible footer.
        AnimatedVisibility(
            visible = !message.isStreaming && revealed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            UsageCapsule(message.tokenUsage, message.completedAt)
        }
    }
}

/**
 * Wraps a per-message LazyColumn item, registering its bounds (in window
 * coordinates) into [LocalMessageBoundsRegistry] so the selection toolbar
 * can look up which message a selection rect belongs to. The slot key
 * disambiguates multiple items belonging to the same message id (e.g. a
 * message with several text blocks).
 */
@Composable
internal fun BoundsTrackedBlock(
    messageId: String,
    slotKey: String,
    markdown: String,
    content: @Composable () -> Unit,
) {
    val registry = LocalMessageBoundsRegistry.current
    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            registry?.put(messageId, slotKey, coords.boundsInWindow(), markdown)
        },
    ) {
        content()
    }
    androidx.compose.runtime.DisposableEffect(messageId, slotKey) {
        onDispose { registry?.remove(messageId, slotKey) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun InlineErrorBanner(error: String, onRetry: (() -> Unit)? = null) {
    val clipboard = LocalClipboardManager.current
    val errContainer = MaterialTheme.colorScheme.errorContainer
    val onErrContainer = MaterialTheme.colorScheme.onErrorContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(errContainer)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(error))
                },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = error,
            color = onErrContainer,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.chat_longpress_retry),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ToolStopButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.stop_tool)
    Box(
        modifier = modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable(
                    onClickLabel = label,
                    onClick = onStop,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(MaterialTheme.colorScheme.error),
            )
        }
    }
}

// ─── Tool Call Capsule (iOS: Capsule(), inline, tool-colored icon + title + duration) ─

/**
 * [T-android-tool-bubble-longpress-menu] Render a tool_use block as a
 * human-readable, paste-back-friendly clipboard string: the tool call
 * (name + id + pretty-printed input JSON) followed by the tool result
 * (char count + status + the result text). Input JSON is pretty-printed
 * when it parses as a JSON object/array; otherwise it's emitted verbatim
 * so a malformed / partial args string still copies usefully.
 */
internal fun formatToolDetailsForClipboard(block: AssistantBlock): String {
    val prettyInput = run {
        val raw = block.toolArgs
        if (raw.isBlank()) return@run "(none)"
        try {
            when (raw.trimStart().firstOrNull()) {
                '{' -> org.json.JSONObject(raw).toString(2)
                '[' -> org.json.JSONArray(raw).toString(2)
                else -> raw
            }
        } catch (_: Exception) {
            raw
        }
    }
    val statusLabel = when (block.toolStatus) {
        ToolBlockStatus.SUCCESS -> "success"
        ToolBlockStatus.FAILED -> "error"
        ToolBlockStatus.TIMEOUT -> "timeout"
        ToolBlockStatus.CANCELLED -> "cancelled"
        ToolBlockStatus.RUNNING, ToolBlockStatus.STREAMING, ToolBlockStatus.PENDING -> "running"
        null -> "unknown"
    }
    val resultText = block.content
    return buildString {
        append("## Tool Call\n")
        append("name: ").append(block.toolName).append('\n')
        append("id: ").append(block.id).append('\n')
        append("input:\n").append(prettyInput).append('\n')
        append('\n')
        append("## Tool Result\n")
        append("(").append(resultText.length).append(" chars, ").append(statusLabel).append(")\n")
        if (resultText.isNotEmpty()) append(resultText)
    }
}
/**
 * [T-android-decorative-anim-perf] One full left-to-right shimmer sweep.
 * Stepping happens on the shared decorative clock (~30 fps); see
 * DecorativeAnimation.kt for why a 2.8 s decorative sweep does not earn the
 * panel's full refresh rate.
 */
private const val SHIMMER_PERIOD_MS = 2800


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolCallPill(
    block: AssistantBlock,
    allToolBlocks: List<AssistantBlock> = listOf(block),
    onRetry: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onOpenTerminalWithCommand: (String) -> Unit = {},
    // T261: detail open routes through ChatViewModel so the sheet survives
    // LazyColumn item disposal. Default no-op for the legacy
    // AssistantMessageView call site (currently dead code).
    onOpenDetail: (String) -> Unit = {},
    // [T-android-tool-bubble-longpress-menu] Long-press actions. Null
    // disables the corresponding menu item (e.g. re-run is null while
    // streaming or when there's no preceding user turn to re-run from).
    onRerunFromHere: (() -> Unit)? = null,
    onCopyDetails: (() -> Unit)? = null,
) {
    if (com.openminis.app.BuildConfig.DEBUG && false) {
        android.util.Log.d("ToolChain[UI]", "ToolCallPill render: id=${block.id} name=${block.toolName} title=${block.toolTitle} status=${block.toolStatus} contentLen=${block.content.length} argsLen=${block.toolArgs.length}")
    }

    val isRunning = block.toolStatus == ToolBlockStatus.RUNNING ||
        block.toolStatus == ToolBlockStatus.STREAMING ||
        block.toolStatus == ToolBlockStatus.PENDING
    val isDone = block.toolStatus == ToolBlockStatus.SUCCESS
    val isFailed = block.toolStatus == ToolBlockStatus.FAILED ||
        block.toolStatus == ToolBlockStatus.TIMEOUT
    val isCancelled = block.toolStatus == ToolBlockStatus.CANCELLED

    val toolAccent = toolAccentColor(block.toolName)
    val controlSummary = helperControlSummary(block).takeIf {
        com.openminis.app.agent.jobs.HelperRunner.isSubAgentToolName(block.toolName)
    }
    val toolIcon = if (controlSummary != null) Icons.Default.Tune else toolIconFor(block.toolName)

    val iconTint = when {
        isFailed -> ToolErrorColor
        isCancelled -> ToolCancelColor
        else -> toolAccent
    }

    val displayIcon = toolIcon

    val durationText = if (block.durationMs > 0 && !isRunning) {
        val seconds = block.durationMs / 1000.0
        if (seconds < 10) String.format("%.1fs", seconds)
        else String.format("%.0fs", seconds)
    } else null

    val shimmerTick = if (isRunning) rememberDecorativeTick() else null
    var showToolMenu by remember { mutableStateOf(false) }

    val pillShape = RoundedCornerShape(16.dp)
    val pillBg = when {
        isFailed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (ChatColors.isDark) 0.42f else 0.58f)
        isRunning -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (ChatColors.isDark) 0.48f else 0.65f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val pillBorder = when {
        isFailed -> MaterialTheme.colorScheme.error.copy(alpha = 0.30f)
        isRunning -> toolAccent.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
    }
    val badgeShape = if (isRunning) {
        MaterialShapes.Sunny.toShape()
    } else {
        MaterialShapes.Cookie9Sided.toShape()
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
      Box(modifier = Modifier.weight(1f, fill = false)) {
        Row(
            modifier = Modifier
                .background(pillBg, pillShape)
                .border(1.dp, pillBorder, pillShape)
                .clip(pillShape)
                .combinedClickable(
                    onClick = { onOpenDetail(block.id) },
                    onLongClick = if (onRerunFromHere != null || onCopyDetails != null) {
                        { showToolMenu = true }
                    } else null,
                )
                .padding(start = 7.dp, end = 10.dp)
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        iconTint.copy(alpha = if (ChatColors.isDark) 0.24f else 0.16f),
                        badgeShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    displayIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp),
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = controlSummary ?: block.toolTitle.ifEmpty { block.toolName },
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isRunning) {
                    StreamingDotsText()
                }
            }

            if (!isRunning) {
                val (statusIcon, statusTint) = when {
                    isFailed -> Icons.Default.Error to ToolErrorColor
                    isCancelled -> Icons.Default.Cancel to ToolCancelColor
                    isDone -> Icons.Default.CheckCircle to ToolCheckColor
                    else -> Icons.Default.CheckCircle to ToolCheckColor
                }
                Spacer(modifier = Modifier.width(7.dp))
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusTint,
                    modifier = Modifier.size(14.dp),
                )
            }

            if (durationText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = durationText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = false,
                        maxLines = 1,
                    )
                }
            }
            if (isRunning && onStop != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ToolStopButton(onStop = onStop)
            } else if (!isRunning) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        if (shimmerTick != null) {
            val shimmerColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(pillShape)
                    .graphicsLayer {
                        val phase = decorativePhase(shimmerTick.value, SHIMMER_PERIOD_MS)
                        translationX = (-1f + phase * 3f) * size.width
                    }
                    .drawWithCache {
                        val band = size.width * 0.6f
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                shimmerColor.copy(alpha = 0f),
                                shimmerColor.copy(alpha = 0.16f),
                                shimmerColor.copy(alpha = 0f),
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(band, 0f),
                        )
                        onDrawBehind {
                            drawRect(brush = brush, size = androidx.compose.ui.geometry.Size(band, size.height))
                        }
                    },
            )
        }
        val toolMenuWidthDp = minOf(220, LocalConfiguration.current.screenWidthDp).dp
        MinisMenu(
            expanded = showToolMenu,
            onDismissRequest = { showToolMenu = false },
            offset = androidx.compose.ui.unit.DpOffset(0.dp, 6.dp),
            modifier = Modifier.widthIn(max = toolMenuWidthDp),
            minWidth = toolMenuWidthDp,
        ) {
            if (onRerunFromHere != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tool_longpress_rerun_from_here)) },
                    onClick = { showToolMenu = false; onRerunFromHere() },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
            if (onCopyDetails != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tool_longpress_copy_details)) },
                    onClick = { showToolMenu = false; onCopyDetails() },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
      }
    }
}

// iOS-style bouncing dots (3 dots, easeInOut, staggered delay)
// [T-android-split-chat] BouncingDots / StreamingDotsText / TypingIndicator
// moved verbatim to ChatIndicators.kt (same package, now `internal`).

// ─── Thinking Block (Material 3 Expressive Chain-of-Thought Card) ────────────

/**
 * [T-android-thinking-collapse-latch] Should a thinking block collapse itself
 * now that streaming has ended?
 *
 * Extracted from [ThinkingBlock] so the rule is testable: the composable needs
 * a live composition, but this is the whole user-visible behaviour — "it folds
 * itself away when the model stops thinking, unless you deliberately held it
 * open."
 *
 * @param isStreaming whether THIS block is still the streaming trailing block.
 * @param userIntent the user's last explicit toggle on this block: true =
 *   they expanded it, false = they collapsed it, null = never touched.
 */
internal fun shouldAutoCollapseThinking(isStreaming: Boolean, userIntent: Boolean?): Boolean {
    if (isStreaming) return false
    // An explicit collapse is already collapsed; re-collapsing is a no-op, but
    // returning false keeps the intent explicit. Everything else — untouched,
    // or expanded-while-streaming — folds away at stream end.
    return userIntent != false
}

@Composable
internal fun ThinkingBlock(block: AssistantBlock, isStreaming: Boolean, isLast: Boolean = true) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val autoExpandThinking = remember { autoExpandThinkingEnabled(context) }
    var expanded by remember(block.id) { mutableStateOf(autoExpandThinking && isLast && isStreaming) }
    var userIntent by remember(block.id) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(block.id, isStreaming) {
        // One-shot auto-collapse when streaming for this block ends.
        if (shouldAutoCollapseThinking(isStreaming, userIntent)) expanded = false
    }
    val activeStreaming = isStreaming && block.toolStatus != ToolBlockStatus.SUCCESS
    val thinkingAccent = MaterialTheme.colorScheme.primary
    val charCount = block.content.length
    val charLabel = remember(charCount) {
        when {
            charCount >= 10_000 -> "${charCount / 1000}K"
            charCount >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", charCount / 1000.0)
            else -> "$charCount"
        }
    }
    val thinkingWindowSize = 8000
    val thinkingHardCap = 100_000
    val overHardCap = charCount > thinkingHardCap
    var showFullContent by remember(block.id) { mutableStateOf(false) }
    var copiedThought by remember(block.id) { mutableStateOf(false) }
    if (copiedThought) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            copiedThought = false
        }
    }

    val cardShape = RoundedCornerShape(if (expanded) 24.dp else 18.dp)
    val cardBg = if (activeStreaming) {
        MaterialTheme.colorScheme.secondaryContainer.copy(
            alpha = if (ChatColors.isDark) 0.42f else 0.56f,
        )
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val cardBorder = if (activeStreaming) {
        thinkingAccent.copy(alpha = 0.36f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "thinkingChevron",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(cardBg, cardShape)
            .border(1.dp, cardBorder, cardShape)
            .clip(cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    if (!overHardCap) userIntent = !expanded
                    if (overHardCap) showFullContent = true
                    else expanded = !expanded
                }
                .padding(horizontal = 2.dp, vertical = 1.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = if (activeStreaming) MaterialShapes.Sunny.toShape()
                        else MaterialShapes.SoftBurst.toShape(),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (activeStreaming) {
                    ExpressiveLoadingIndicator(
                        color = thinkingAccent,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = thinkingAccent,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = stringResource(
                    if (activeStreaming) R.string.thinking_header_streaming
                    else R.string.thinking_header_done,
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (activeStreaming) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (activeStreaming) {
                StreamingDotsText()
            }
            Spacer(modifier = Modifier.weight(1f))
            if (charCount > 0) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            CircleShape,
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = charLabel,
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            if ((expanded || overHardCap) && charCount > 0) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            CircleShape,
                        )
                        .clip(CircleShape)
                        .clickable {
                            clipboard.setText(AnnotatedString(block.content))
                            copiedThought = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (copiedThought) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy thought",
                        tint = if (copiedThought) thinkingAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.5.dp),
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            CircleShape,
                        )
                        .clip(CircleShape)
                        .clickable { showFullContent = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = stringResource(R.string.thinking_view_full),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.5.dp),
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
            if (overHardCap) {
                Text(
                    text = stringResource(R.string.thinking_view_full),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = thinkingAccent,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = chevronRotation },
                    )
                }
            }
        }

        // Collapsed live thought snippet while streaming
        if (!expanded && activeStreaming && charCount > 0) {
            val liveSnippet = remember(charCount) { extractLiveThoughtSnippet(block.content) }
            if (liveSnippet.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 1.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(thinkingAccent.copy(alpha = 0.75f), CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = liveSnippet,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded && !overHardCap) {
            val scrollState = rememberScrollState()
            val isTruncated = charCount > thinkingWindowSize
            val displayContent = remember(charCount) {
                if (isTruncated) {
                    val full = block.content
                    val start = charCount - thinkingWindowSize
                    val nl = full.indexOf('\n', start)
                    if (nl in start until start + 200) full.substring(nl + 1)
                    else full.substring(start)
                } else {
                    block.content
                }
            }
            var userScrolledAway by remember(block.id) { mutableStateOf(false) }
            LaunchedEffect(scrollState, block.id) {
                snapshotFlow {
                    Triple(
                        scrollState.value,
                        scrollState.maxValue,
                        scrollState.isScrollInProgress,
                    )
                }.collect { (v, max, dragging) ->
                    val gap = (max - v).coerceAtLeast(0)
                    when {
                        dragging && gap > 4 -> userScrolledAway = true
                        gap <= 4 -> userScrolledAway = false
                    }
                }
            }
            LaunchedEffect(scrollState, block.id, isStreaming) {
                if (!isStreaming) return@LaunchedEffect
                snapshotFlow { block.content.length }
                    .collect {
                        if (userScrolledAway) return@collect
                        scrollState.scrollTo(scrollState.maxValue)
                    }
            }

            val segments = remember(displayContent) { parseThoughtSegments(displayContent) }
            val wellShape = RoundedCornerShape(16.dp)
            val wellBg = if (ChatColors.isDark) {
                MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
            val wellBorder = MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = if (ChatColors.isDark) 0.36f else 0.48f,
            )
            val canScrollUp = scrollState.canScrollBackward
            val canScrollDown = scrollState.canScrollForward

            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .background(wellBg, wellShape)
                    .border(1.dp, wellBorder, wellShape)
                    .clip(wellShape),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .then(
                            if (canScrollUp || canScrollDown) {
                                Modifier
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        val h = size.height
                                        if (h > 1f) {
                                            val topFade = (16.dp.toPx() / h).coerceIn(0.01f, 0.18f)
                                            val bottomFade = (1f - (18.dp.toPx() / h).coerceIn(0.01f, 0.18f))
                                            val stops = buildList {
                                                if (canScrollUp) {
                                                    add(0f to Color.Transparent)
                                                    add(topFade to Color.Black)
                                                } else {
                                                    add(0f to Color.Black)
                                                }
                                                if (canScrollDown) {
                                                    add(bottomFade to Color.Black)
                                                    add(1f to Color.Transparent)
                                                } else {
                                                    add(1f to Color.Black)
                                                }
                                            }.toTypedArray()
                                            drawRect(
                                                brush = Brush.verticalGradient(colorStops = stops),
                                                blendMode = BlendMode.DstIn,
                                            )
                                        }
                                    }
                            } else Modifier,
                        )
                        .verticalScroll(scrollState)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (isTruncated) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                                    RoundedCornerShape(10.dp),
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showFullContent = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.thinking_truncated_hint,
                                    displayContent.length / 1000,
                                    charCount / 1000,
                                ),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = null,
                                tint = thinkingAccent,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    ThoughtTimelineContent(
                        segments = segments,
                        activeStreaming = activeStreaming,
                        wellBackground = wellBg,
                    )
                }

                if (userScrolledAway && canScrollDown) {
                    Surface(
                        onClick = {
                            userScrolledAway = false
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to latest thought",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
            }
        }

        if (showFullContent) {
            ThinkingFullContentDialog(
                content = block.content,
                onDismiss = { showFullContent = false },
            )
        }
    }
}

private sealed class ThoughtSegment {
    data class Heading(val title: String) : ThoughtSegment()
    data class Paragraph(val text: String) : ThoughtSegment()
    data class Bullet(val marker: String, val text: String) : ThoughtSegment()
    data class Quote(val text: String) : ThoughtSegment()
    data class Code(val lang: String, val code: String) : ThoughtSegment()
}

private val THOUGHT_HEADING_REGEX = Regex("""^\s*#{1,4}\s+(.+?)\s*$""")
private val THOUGHT_BOLD_TITLE_REGEX = Regex("""^\s*\*\*([^*]{2,90})\*\*:?\s*$""")
private val THOUGHT_BULLET_REGEX = Regex("""^\s*([-*•]|\d+[.)])\s+(.+)$""")

private fun extractLiveThoughtSnippet(raw: String): String {
    if (raw.isBlank()) return ""
    val start = (raw.length - 420).coerceAtLeast(0)
    val tail = raw.substring(start)
    val lines = tail.split('\n')
    for (i in lines.indices.reversed()) {
        val trimmed = lines[i].trim()
        if (trimmed.isEmpty() || trimmed.startsWith("```")) continue
        return trimmed
            .replace(Regex("""^#{1,4}\s+"""), "")
            .replace(Regex("""^([-*•]|\d+[.)]|>)\s+"""), "")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .trim()
    }
    return ""
}

private fun parseThoughtSegments(raw: String): List<ThoughtSegment> {
    if (raw.isBlank()) return emptyList()
    val result = ArrayList<ThoughtSegment>()
    val lines = raw.split('\n')
    val paraBuf = StringBuilder()
    var inFence = false
    var fenceLang = ""
    val codeBuf = StringBuilder()

    fun flushParagraph() {
        if (paraBuf.isNotEmpty()) {
            val text = paraBuf.toString().trim()
            if (text.isNotEmpty()) {
                result.add(ThoughtSegment.Paragraph(text))
            }
            paraBuf.setLength(0)
        }
    }

    for (line in lines) {
        val trimmedStart = line.trimStart()
        if (trimmedStart.startsWith("```")) {
            if (inFence) {
                result.add(ThoughtSegment.Code(fenceLang, codeBuf.toString().trimEnd()))
                codeBuf.setLength(0)
                fenceLang = ""
                inFence = false
            } else {
                flushParagraph()
                inFence = true
                fenceLang = trimmedStart.removePrefix("```").trim()
            }
            continue
        }
        if (inFence) {
            if (codeBuf.isNotEmpty()) codeBuf.append('\n')
            codeBuf.append(line)
            continue
        }
        if (trimmedStart.isEmpty()) {
            flushParagraph()
            continue
        }
        val headingMatch = THOUGHT_HEADING_REGEX.matchEntire(line)
            ?: THOUGHT_BOLD_TITLE_REGEX.matchEntire(line)
        if (headingMatch != null) {
            flushParagraph()
            result.add(ThoughtSegment.Heading(headingMatch.groupValues[1].trim()))
            continue
        }
        if (trimmedStart.startsWith(">")) {
            flushParagraph()
            val quoteText = trimmedStart.removePrefix(">").trimStart()
            if (quoteText.isNotEmpty()) {
                result.add(ThoughtSegment.Quote(quoteText))
            }
            continue
        }
        val bulletMatch = THOUGHT_BULLET_REGEX.matchEntire(line)
        if (bulletMatch != null) {
            flushParagraph()
            val rawMarker = bulletMatch.groupValues[1]
            val cleanMarker = if (rawMarker == "-" || rawMarker == "*") "•" else rawMarker
            result.add(ThoughtSegment.Bullet(cleanMarker, bulletMatch.groupValues[2].trim()))
            continue
        }
        if (paraBuf.isNotEmpty()) paraBuf.append(' ')
        paraBuf.append(trimmedStart)
    }
    if (inFence && codeBuf.isNotEmpty()) {
        result.add(ThoughtSegment.Code(fenceLang, codeBuf.toString().trimEnd()))
    }
    flushParagraph()
    return result
}

private fun formatThoughtInline(
    text: String,
    boldColor: Color,
    codeColor: Color,
    codeBg: Color,
): AnnotatedString {
    if (!text.contains('*') && !text.contains('`')) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var i = 0
        val n = text.length
        while (i < n) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = codeColor,
                                background = codeBg,
                            ),
                        ) {
                            append("\u00A0" + text.substring(i + 1, end) + "\u00A0")
                        }
                        i = end + 1
                    } else {
                        append('`')
                        i++
                    }
                }
                text[i] == '*' && (i + 1 < n && text[i + 1] != ' ') -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i + 1 && end - i <= 120) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append('*')
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

@Composable
private fun ThoughtTimelineContent(
    segments: List<ThoughtSegment>,
    activeStreaming: Boolean,
    wellBackground: Color,
) {
    if (segments.isEmpty()) return
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val railColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val headingColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val inlineCodeBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val codeCardBg = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
        alpha = if (ChatColors.isDark) 0.65f else 0.82f,
    )
    val lastHeadingIndex = remember(segments) {
        segments.indexOfLast { it is ThoughtSegment.Heading }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = 4.dp.toPx()
                val stroke = 1.5.dp.toPx()
                if (size.height > 12.dp.toPx()) {
                    drawLine(
                        color = railColor,
                        start = Offset(x, 8.dp.toPx()),
                        end = Offset(x, size.height - 6.dp.toPx()),
                        strokeWidth = stroke,
                    )
                }
            },
    ) {
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is ThoughtSegment.Heading -> {
                    val isTrailingStep = activeStreaming && index == lastHeadingIndex
                    val dotColor = if (isTrailingStep) primaryColor else primaryColor.copy(alpha = 0.72f)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (index == 0) 0.dp else 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(wellBackground, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isTrailingStep) 7.dp else 6.dp)
                                    .background(dotColor, CircleShape),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatThoughtInline(
                                text = segment.title,
                                boldColor = headingColor,
                                codeColor = tertiaryColor,
                                codeBg = inlineCodeBg,
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = headingColor,
                        )
                    }
                }

                is ThoughtSegment.Paragraph -> {
                    val showAnchorDot = index == 0
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (showAnchorDot) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(9.dp)
                                    .background(wellBackground, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(primaryColor.copy(alpha = 0.65f), CircleShape),
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Spacer(modifier = Modifier.width(17.dp))
                        }
                        Text(
                            text = formatThoughtInline(
                                text = segment.text,
                                boldColor = headingColor,
                                codeColor = tertiaryColor,
                                codeBg = inlineCodeBg,
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.5.sp,
                            ),
                            color = bodyColor,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                is ThoughtSegment.Bullet -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 17.dp),
                    ) {
                        Text(
                            text = segment.marker,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor.copy(alpha = 0.85f),
                            modifier = Modifier.widthIn(min = 14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatThoughtInline(
                                text = segment.text,
                                boldColor = headingColor,
                                codeColor = tertiaryColor,
                                codeBg = inlineCodeBg,
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            ),
                            color = bodyColor,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                is ThoughtSegment.Quote -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 17.dp)
                            .drawBehind {
                                drawRoundRect(
                                    color = tertiaryColor.copy(alpha = 0.6f),
                                    topLeft = Offset.Zero,
                                    size = androidx.compose.ui.geometry.Size(2.5.dp.toPx(), size.height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                                )
                            }
                            .padding(start = 10.dp, top = 1.dp, bottom = 1.dp),
                    ) {
                        Text(
                            text = formatThoughtInline(
                                text = segment.text,
                                boldColor = headingColor,
                                codeColor = tertiaryColor,
                                codeBg = inlineCodeBg,
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                fontStyle = FontStyle.Italic,
                            ),
                            color = bodyColor.copy(alpha = 0.9f),
                        )
                    }
                }

                is ThoughtSegment.Code -> {
                    val hScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 17.dp, top = 2.dp, bottom = 2.dp)
                            .background(codeCardBg, RoundedCornerShape(12.dp))
                            .border(
                                0.75.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        if (segment.lang.isNotEmpty()) {
                            Text(
                                text = segment.lang,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Box(modifier = Modifier.horizontalScroll(hScroll)) {
                            Text(
                                text = segment.code,
                                fontSize = 11.5.sp,
                                lineHeight = 16.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = headingColor.copy(alpha = 0.92f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * [T-thinking-render-perf-android] Full-screen Chain-of-Thought reader.
 * Uses rich structured timeline formatting up to 32K chars and falls back
 * to native [android.widget.TextView] above 32K so 100K+ streams stay O(1).
 */
@Composable
private fun ThinkingFullContentDialog(content: String, onDismiss: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    if (copied) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.SoftBurst.toShape()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.thinking_header_done),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${content.length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        onClick = {
                            clipboard.setText(AnnotatedString(content))
                            copied = true
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(android.R.string.ok),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                if (content.length <= 32_000) {
                    val fullSegments = remember(content) { parseThoughtSegments(content) }
                    val dialogScroll = rememberScrollState()
                    val wellBg = if (ChatColors.isDark) {
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    }
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(dialogScroll)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(wellBg, RoundedCornerShape(20.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                        RoundedCornerShape(20.dp),
                                    )
                                    .padding(16.dp),
                            ) {
                                ThoughtTimelineContent(
                                    segments = fullSegments,
                                    activeStreaming = false,
                                    wellBackground = wellBg,
                                )
                            }
                        }
                    }
                } else {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            android.widget.ScrollView(ctx).apply {
                                addView(
                                    android.widget.TextView(ctx).apply {
                                        textSize = 13f
                                        setTextColor(textColor.toArgb())
                                        setTextIsSelectable(true)
                                        setPadding(40, 28, 40, 64)
                                        text = content
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}
