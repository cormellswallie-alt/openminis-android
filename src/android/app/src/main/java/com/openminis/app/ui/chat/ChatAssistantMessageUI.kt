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
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
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
import com.openminis.app.ui.theme.Noir
import com.openminis.app.ui.components.MinisTextButton
import com.openminis.app.ui.components.DecorativeSpinner
import com.openminis.app.ui.components.rememberDecorativeTick
import com.openminis.app.ui.components.decorativePhase
import androidx.compose.ui.draw.drawWithCache

@Composable
internal fun AssistantHeader(live: Boolean = false) {
    // [T-soul-md] Identity header = icon + SOUL.md-driven `name`.
    //
    // [T-android-soul-custom-icon] The icon is now the user-settable
    // `SoulMetadata.icon` (emoji or transparent PNG), falling back to the
    // canonical sparkle gradient when unset — so a user who never touches it
    // sees exactly the previous rendering.
    //
    // Deliberately the SAME composable the settings card uses. On iOS these
    // two surfaces were written separately and the chat one silently failed
    // to pick up image icons; sharing the renderer makes that class of
    // divergence impossible rather than merely unlikely.
    val soulMeta by com.openminis.app.agent.SoulStore.cachedMetadata.collectAsState()
    val displayName = soulMeta.name.ifBlank { com.openminis.app.agent.SoulMetadata.DEFAULT.name }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            // [T-android-user-assistant-spacing-16] top=10 so the
            // User→Assistant boundary reads ~16dp: user-bubble bottom(4) +
            // LazyColumn spacedBy(2) + this top(10) = 16. The header→body gap
            // inside the turn is unaffected (that's this row's bottom=2).
            .padding(top = 14.dp, bottom = 6.dp),
    ) {
        val sparkleGradient = Brush.linearGradient(
            colors = listOf(SparkleColor1, SparkleColor2),
        )
        // [T-spectra-v3] The Spectra orb is the assistant's face. A custom
        // Soul icon (emoji / PNG) still wins and sits inside a plain disc so
        // that user setting keeps working.
        val hasCustomIcon = soulMeta.icon.isNotBlank()
        if (hasCustomIcon) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .background(ChatColors.toolCapsuleBg, CircleShape),
            ) {
                com.openminis.app.ui.settings.SoulIconGlyph(
                    icon = soulMeta.icon,
                    sizeDp = 16.dp,
                    emojiSp = 14.sp,
                    sparkleTint = sparkleGradient,
                )
            }
        } else {
            com.openminis.app.ui.theme.SpectraOrb(
                size = 24.dp,
                state = if (live) com.openminis.app.ui.theme.OrbState.Live
                else com.openminis.app.ui.theme.OrbState.Idle,
                modifier = Modifier.padding(1.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = displayName,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
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
    val ink = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(top = 2.dp, bottom = 2.dp)
            // Capsule: pill shape, 8dp / 3dp padding.
            //
            // [T-android-usage-capsule-tint] The fill is derived from the
            // user-bubble colour but NOT `userBubble.copy(alpha = 0.6f)`, which
            // is what a literal reading of the iOS line produces and what this
            // used to do. iOS's `userBubble` is `tertiarySystemFill`, already a
            // translucent grey in both appearances, so 60% of it lands soft.
            // Android's dark value is `0xFF2F3A5C` — fully OPAQUE — so the same
            // arithmetic produced a solid blue-grey slab that read as a filled
            // component rather than an incidental footnote, and clashed with
            // the surrounding chat.
            //
            // Taking the colour at a low alpha over the surface keeps it a
            // tint in both themes: it settles onto whatever is behind it
            // instead of asserting its own block of colour. 0.22 was still
            // reading as a distinct chip against the light chat background;
            // 0.10 leaves the shape legible without the capsule announcing
            // itself, which is what a hidden-by-default easter egg wants.
            .clip(RoundedCornerShape(50))
            .background(ChatColors.userBubble.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE5484D).copy(alpha = 0.12f))
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(error))
                },
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = Color(0xFFE5484D),
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = error,
            color = Color(0xFFE5484D),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE5484D).copy(alpha = 0.15f))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFFE5484D),
                    modifier = Modifier.size(10.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.chat_longpress_retry), color = Color(0xFFE5484D), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * T14: per-card stop affordance shown on running/streaming tool blocks.
 * Mirrors iOS `ToolCapsuleView`'s small red square that appears trailing
 * the tool title when `block.toolStatus == .running`. Tapping it routes to
 * the same global `cancelStream()` callback iOS uses for `onStop?()` —
 * iOS also has no per-tool cancellation API; the per-card button is purely
 * an affordance-discoverability win. Resume banner (T13) makes the global
 * cancel UX recoverable.
 *
 * Visual: 14×14 red rounded square (Color 0xFFFF3B30 = iOS systemRed).
 */
@Composable
private fun ToolStopButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.stop_tool)
    // Outer Box keeps the *layout* footprint at 18×18 (unchanged capsule width).
    // The inner clickable Box is 24×24 and overflows the outer bounds equally on
    // all sides (requiredSize ignores the parent's 18dp constraint), enlarging the
    // touch target to 24 while the visual 10×10 red square stays identical.
    Box(
        modifier = modifier.size(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(
                    onClickLabel = label,
                    onClick = onStop,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE5484D)),
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
    // [T-noir-step-timeline] Hairline rail joining this step to its
    // neighbours in the same run (see ChatStepTimeline.kt).
    railUp: Boolean = false,
    railDown: Boolean = false,
) {
    // T-android-jank-profile: this log was firing on every ToolCallPill
    // recomposition (every streaming token while a tool call is live),
    // showing up as 1.6% main thread time in profiles. Logs at composable
    // top level multiply with the number of pills × recompose rate. Gate
    // behind BuildConfig.DEBUG so production builds skip the string-build
    // entirely, and the rest of release builds don't pay for it.
    if (com.openminis.app.BuildConfig.DEBUG && false) {
        android.util.Log.d("ToolChain[UI]", "ToolCallPill render: id=${block.id} name=${block.toolName} title=${block.toolTitle} status=${block.toolStatus} contentLen=${block.content.length} argsLen=${block.toolArgs.length}")
    }

    // PENDING shares RUNNING's spinner affordance — tool JSON is received but
    // execution hasn't flipped the block to RUNNING yet (brief gap). TIMEOUT
    // shares FAILED's error styling but the icon mapping distinguishes them.
    val isRunning = block.toolStatus == ToolBlockStatus.RUNNING ||
        block.toolStatus == ToolBlockStatus.STREAMING ||
        block.toolStatus == ToolBlockStatus.PENDING
    val isDone = block.toolStatus == ToolBlockStatus.SUCCESS
    val isFailed = block.toolStatus == ToolBlockStatus.FAILED ||
        block.toolStatus == ToolBlockStatus.TIMEOUT
    val isCancelled = block.toolStatus == ToolBlockStatus.CANCELLED

    val toolAccent = toolAccentColor(block.toolName)
    // [T-android-subagent-control-capsule] A status / steer / cancel / resume
    // call is the model operating on its own sub agents — a tool call like any
    // other, so it renders in this shared capsule rather than a bespoke row.
    //
    // Android's first cut (ported from iOS's) was a centred grey line: it read
    // as a section divider rather than an action, said nothing about WHAT was
    // sent, could not be opened, and its own padding made it taller than the
    // capsules around it. The shared capsule fixes all four at once — the same
    // 36dp metrics as every other tool, a tappable detail sheet, and the sub
    // agent accent (toolAccentColor already maps subagent_task to the violet)
    // so it still reads as agent work.
    val controlSummary = helperControlSummary(block).takeIf {
        com.openminis.app.agent.jobs.HelperRunner.isSubAgentToolName(block.toolName)
    }
    val toolIcon = if (controlSummary != null) Icons.Default.Tune else toolIconFor(block.toolName)

    // iOS: always shows tool-type icon, only changes color based on status
    val displayIcon = toolIcon

    // Duration text (iOS: "0.4s" format)
    val durationText = if (block.durationMs > 0 && !isRunning) {
        val seconds = block.durationMs / 1000.0
        if (seconds < 10) String.format("%.1fs", seconds)
        else String.format("%.0fs", seconds)
    } else null

    // T125: drop the spinner that used to replace the tool icon while
    // running. iOS only animates a left→right shimmer sweep across the
    // pill background and keeps the typed icon visible — the spinner
    // both fought the icon for attention and looked stylistically off
    // next to the iOS counterpart. The bottom FloatingToolStatusBar
    // still shows a CircularProgressIndicator (that is the running-tool
    // status surface, where a spinner reads correctly).
    // [T-android-decorative-anim-perf] The running shimmer is driven by the
    // shared ~30 fps decorative clock and drawn as a SIBLING layer over the
    // pill (see the overlay after the pill Row below), not as a draw modifier
    // wrapping it. Wrapping meant every tick re-recorded the pill's icon and
    // title text just to slide a translucent band; a sibling layer moves only
    // itself. The clock is held only while the tool is RUNNING.
    val shimmerTick: androidx.compose.runtime.State<Long>? = null

    // [T-android-tool-bubble-longpress-menu] Long-press menu state, scoped
    // to this pill. The DropdownMenu is anchored to the pill via the Box
    // wrapper below so it opens beneath the tapped bubble.
    var showToolMenu by remember { mutableStateOf(false) }

    // Pill stretches up to the full row width so long titles can ellipsize
    // without pushing the duration out of view. Title takes the remaining
    // space via weight(1f), duration stays fixed-width (softWrap=false).
    // [T-noir-step-timeline] The capsule became a timeline row: colour moved
    // from a beige fill on every pill into the tool's own tinted node, so a
    // long run reads as distinct kinds of work instead of a wall of
    // identical lozenges. Failure turns the node ember; cancel greys it.
    val railColor = stepRailColor()
    val nodeAccent = when {
        isFailed -> Noir.Ember
        isCancelled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        else -> toolAccent
    }
    // [T-spectra-v3] Finished steps are one quiet two-line row; the running
    // step lifts into a white card (soft shadow, no glow, no shimmer).
    val dark = ChatColors.isDark
    val stepShape = RoundedCornerShape(if (isRunning) 18.dp else 14.dp)
    val liveCardBg = if (dark) Noir.Onyx1 else Noir.PaperCard
    val liveHair = if (dark) Noir.OnyxHairline else Noir.PaperHairline
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .stepRail(railUp && !isRunning, railDown && !isRunning, railColor),
    ) {
      Box(modifier = Modifier.weight(1f, fill = true)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isRunning) Modifier
                        .padding(vertical = 4.dp)
                        .shadow(if (dark) 0.dp else 10.dp, stepShape, ambientColor = Color(0x140A0A0B), spotColor = Color(0x240A0A0B))
                        .background(liveCardBg, stepShape)
                        .border(0.5.dp, liveHair, stepShape)
                    else Modifier,
                )
                .clip(stepShape)
                .combinedClickable(
                    onClick = { onOpenDetail(block.id) },
                    onLongClick = if (onRerunFromHere != null || onCopyDetails != null) {
                        { showToolMenu = true }
                    } else null,
                )
                .heightIn(min = StepRowHeight)
                .padding(
                    start = if (isRunning) 10.dp else 0.dp,
                    end = if (isRunning) 8.dp else 4.dp,
                    top = if (isRunning) 10.dp else 4.dp,
                    bottom = if (isRunning) 10.dp else 4.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepNode(icon = displayIcon, accent = nodeAccent, running = isRunning)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // [T-android-subagent-control-capsule] Prefer the
                    // operation description for a control call.
                    text = controlSummary ?: block.toolTitle.ifEmpty { block.toolName },
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (isRunning) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isCancelled) metaColor else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 1.dp)) {
                    Text(
                        text = block.toolName,
                        fontSize = 12.5.sp,
                        fontFamily = com.openminis.app.ui.terminal.JetBrainsMonoFontFamily,
                        color = if (isFailed) nodeAccent else metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isRunning) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StreamingDotsText()
                    }
                }
            }

            if (durationText != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = durationText,
                    fontSize = 12.5.sp,
                    color = if (isFailed) nodeAccent else metaColor,
                    softWrap = false,
                    maxLines = 1,
                )
            }
            if (isDone) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (dark) Noir.OkOnOnyx else Noir.Ok,
                    modifier = Modifier.size(15.dp),
                )
            }
            // T168: per-tool stop button for running blocks only.
            if (isRunning && onStop != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ToolStopButton(onStop = onStop)
            }
        }
        @Suppress("UNUSED_VARIABLE") val unusedShimmer = shimmerTick
        // [T-android-tool-bubble-longpress-menu] Long-press menu anchored to
        // the pill. Items mirror the user-bubble menu's style (MinisMenu +
        // DropdownMenuItem + leading icon). Each item no-ops gracefully if
        // its callback is null (re-run is gated while streaming / when no
        // preceding user turn exists).
        // [T-android-tool-menu-minwidth] Minimum width = min(220dp, screen
        // width) — the menu wants to be 220dp wide, but must never exceed the
        // device width on a narrow screen. screenWidthDp is the usable width in
        // dp; cap max to the same value so the widthIn(min,max) range is always
        // valid (min <= max) even on a sub-220dp display.
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
        // T251: removed inline Retry affordance next to cancelled/failed pills —
        // the pill's own status icon (yellow on FAILED, gray on CANCELLED) is
        // already the unified failure tip. The button was visually noisy and
        // redundant. ToolCallPill keeps the `onRetry` parameter so upstream
        // callers don't need to change; the lambda just isn't surfaced inline
        // any more. retryLast() / retryFromMessage() remain reachable from
        // other entry points (long-press menu, etc.).
        // iOS: Spacer(minLength: 0) — pill stays content-width, not full-row-width
    }
}

// iOS-style bouncing dots (3 dots, easeInOut, staggered delay)
// [T-android-split-chat] BouncingDots / StreamingDotsText / TypingIndicator
// moved verbatim to ChatIndicators.kt (same package, now `internal`).

// ─── Thinking Block (iOS: collapsible "Deep Thinking" section, blue tint) ────

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
internal fun ThinkingBlock(
    block: AssistantBlock,
    isStreaming: Boolean,
    isLast: Boolean = true,
    // [T-noir-step-timeline] rail to neighbouring steps.
    railUp: Boolean = false,
    railDown: Boolean = false,
) {
    // Per-block expand state, keyed by block.id so the user's manual toggle on
    // an earlier (finished) thinking block survives recomposition while a
    // later block is still streaming. The previous LaunchedEffect snapped
    // every non-last block back to collapsed on each `isLast` flip, which
    // fought the user's tap and produced a flicker that read as "tapping the
    // earlier block shows the streaming block's content."
    // [T-thinking-auto-expand-toggle] The initial auto-expand of a new
    // streaming block is gated on the Appearance setting (default ON =
    // historical behavior). When the user turned it off, a new streaming block
    // starts collapsed; a manual header tap still expands it (recording the
    // user's intent below, so nothing fights them). Read once at mount —
    // mirrors iOS ThinkingBlockView,
    // where the same UserDefaults gate sits at the one-shot auto-expand site.
    val context = LocalContext.current
    val autoExpandThinking = remember { autoExpandThinkingEnabled(context) }
    var expanded by remember(block.id) { mutableStateOf(autoExpandThinking && isLast && isStreaming) }
    // [T-android-thinking-collapse-latch] What the user last chose, or null if
    // they have not touched THIS block. Deliberately tri-state rather than the
    // old `userTouched: Boolean`.
    //
    // The bug that flag caused: the block auto-expands while streaming, the
    // user taps once to collapse it and again to re-open and keep reading —
    // and `userTouched` is now latched true forever, so the stream-end
    // auto-collapse below never fires and the block stays open for the rest of
    // the conversation. Reported as "thinking blocks don't collapse again once
    // you've expanded them".
    //
    // "Never fight the user" is still the rule; the flag simply could not tell
    // WHICH way the user had pushed. A user who collapsed a block means "keep
    // it shut" and auto-collapse must not reopen it (it never would) — but a
    // user who EXPANDED it was asking to read it *while it streamed*, which is
    // the same thing the auto-expand does, and there is nothing to defend once
    // the stream ends. So only an explicit collapse suppresses the auto-
    // collapse; an explicit expand lets it run.
    var userIntent by remember(block.id) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(block.id, isStreaming) {
        // One-shot auto-collapse when streaming for this block ends.
        if (shouldAutoCollapseThinking(isStreaming, userIntent)) expanded = false
    }
    // [T-noir-step-timeline] Violet is reserved for thinking: it is the one
    // step kind drawn in the brand ink, everything else uses the spectrum.
    val thinkingInkColor = thinkingInk()
    val charCount = block.content.length
    val charLabel = when {
        charCount >= 1000 -> "${charCount / 1000}K"
        else -> "$charCount"
    }
    // [T-thinking-render-perf-android] Tail window / hard cap — see history.
    val thinkingWindowSize = 8000
    val thinkingHardCap = 100_000
    val overHardCap = charCount > thinkingHardCap
    var showFullContent by remember(block.id) { mutableStateOf(false) }
    val liveThinking = isStreaming && block.toolStatus != ToolBlockStatus.SUCCESS
    // Live one-liner beside the label: O(600) per token, never the whole block.
    val preview = remember(block.id, charCount) { thinkingPreviewLine(block.content) }
    val chevronAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "thinkingChevron",
    )

    // [T-spectra-v3] Thinking card. The node is the assistant orb itself
    // (thinking state: rotating violet ring); violet appears only as the
    // 1.5dp margin line, the depth label and the caret. Body reads like a
    // note: 15.5sp / 1.85 line height. Elapsed time is measured locally
    // (blocks carry no timing): live while streaming, frozen when done.
    val dark = ChatColors.isDark
    val startedAt = remember(block.id) { android.os.SystemClock.elapsedRealtime() }
    var frozenSecs by remember(block.id) { mutableStateOf<Long?>(null) }
    var liveSecs by remember(block.id) { mutableStateOf(0L) }
    LaunchedEffect(block.id, liveThinking) {
        if (liveThinking) {
            while (true) {
                liveSecs = (android.os.SystemClock.elapsedRealtime() - startedAt) / 1000
                kotlinx.coroutines.delay(1000)
            }
        } else if (frozenSecs == null && liveSecs > 0) {
            frozenSecs = (android.os.SystemClock.elapsedRealtime() - startedAt) / 1000
        }
    }
    val secs = frozenSecs ?: liveSecs.takeIf { liveThinking }
    val title = when {
        liveThinking -> stringResource(R.string.spectra_thinking_live)
        secs != null && secs > 0 -> stringResource(R.string.spectra_thinking_done_secs, secs.toInt())
        else -> stringResource(R.string.spectra_thinking_done)
    }
    val meta = buildString {
        if (liveThinking && secs != null) append("${secs}s")
        if (charCount > 0) { if (isNotEmpty()) append("  ·  "); append(stringResource(R.string.spectra_thinking_chars, charLabel)) }
    }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val cardShape = RoundedCornerShape(18.dp)
    val floating = liveThinking && expanded
    val cardBg = if (dark) Noir.Onyx1 else Noir.PaperCard
    val hair = if (dark) Noir.OnyxHairline else Noir.PaperHairline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .stepRail(railUp && !floating, railDown && !floating, stepRailColor())
            .then(
                if (floating) Modifier
                    .padding(vertical = 4.dp)
                    .shadow(if (dark) 0.dp else 10.dp, cardShape, ambientColor = Color(0x140A0A0B), spotColor = Color(0x240A0A0B))
                    .background(cardBg, cardShape)
                    .border(0.5.dp, hair, cardShape)
                    .clip(cardShape)
                    .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 10.dp)
                else Modifier,
            ),
    ) {
        // Header row — only the header reacts to taps (a drag-release in the
        // inner scroller must not toggle).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    // [T-android-thinking-collapse-latch] Record the direction.
                    if (!overHardCap) userIntent = !expanded
                    if (overHardCap) showFullContent = true
                    else expanded = !expanded
                }
                .heightIn(min = StepRowHeight)
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Box(modifier = Modifier.size(StepNodeSize), contentAlignment = Alignment.Center) {
                com.openminis.app.ui.theme.SpectraOrb(
                    size = 22.dp,
                    state = if (liveThinking) com.openminis.app.ui.theme.OrbState.Think
                    else com.openminis.app.ui.theme.OrbState.Done,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (liveThinking) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (meta.isNotEmpty()) {
                        Text(
                            text = "  $meta",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                // Collapsed: keep the latest line (live) / the conclusion (done).
                if (!expanded && preview.isNotEmpty()) {
                    Text(
                        text = preview,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            if (overHardCap) {
                Text(
                    text = stringResource(R.string.thinking_view_full),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = thinkingInkColor,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(chevronAngle),
                )
            }
        }

        // Expanded content. Inner scroller capped at 300dp, auto-follows the
        // tail while streaming, pauses when the user drags away.
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
                    Triple(scrollState.value, scrollState.maxValue, scrollState.isScrollInProgress)
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
            val bodyColor = if (dark) Noir.IvorySoft else Noir.InkSoft
            val fadeColor = if (floating) cardBg else MaterialTheme.colorScheme.background
            Column(modifier = Modifier.padding(start = StepTextIndent, top = 4.dp, bottom = 6.dp, end = 4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // The margin line — the only violet in the body.
                            drawRect(
                                color = thinkingInkColor.copy(alpha = if (dark) 0.70f else 0.55f),
                                size = androidx.compose.ui.geometry.Size(1.5.dp.toPx(), size.height),
                            )
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(scrollState)
                            .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    ) {
                        if (isTruncated) {
                            Text(
                                text = stringResource(
                                    R.string.thinking_truncated_hint,
                                    displayContent.length / 1000,
                                    charCount / 1000,
                                ),
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        Text(
                            text = displayContent,
                            fontSize = 15.sp,
                            lineHeight = 27.sp,
                            color = bodyColor,
                        )
                    }
                    // Soft top fade while the window is scrolled (streaming tail).
                    if (scrollState.value > 0) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBehind {
                                    val h = 28.dp.toPx().coerceAtMost(size.height)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to fadeColor, 1f to fadeColor.copy(alpha = 0f),
                                            startY = 0f, endY = h,
                                        ),
                                        topLeft = Offset(2.dp.toPx(), 0f),
                                        size = androidx.compose.ui.geometry.Size(size.width, h),
                                    )
                                },
                        )
                    }
                }
                if (!liveThinking && charCount > 0) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ThinkingActionChip(
                            icon = Icons.Default.ContentCopy,
                            label = stringResource(R.string.spectra_thinking_copy),
                            onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(block.content)) },
                        )
                        ThinkingActionChip(
                            icon = Icons.Default.OpenInFull,
                            label = stringResource(R.string.spectra_thinking_full),
                            onClick = { showFullContent = true },
                        )
                    }
                }
            }
        }

        // [T-thinking-render-perf-android] Hard-cap full-content viewer. A
        // native TextView (selectable, scrollable) renders arbitrarily large
        // thinking text without the Compose `Text` measure freeze.
        if (showFullContent) {
            ThinkingFullContentDialog(
                content = block.content,
                onDismiss = { showFullContent = false },
            )
        }
    }
}

@Composable
private fun ThinkingActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val dark = ChatColors.isDark
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (dark) Noir.Onyx2 else Noir.PaperSunk)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * [T-thinking-render-perf-android] Full-screen viewer for thinking content
 * that exceeds the inline hard cap. Wraps a native Android [android.widget.TextView]
 * (inside a scroller) — it lays out very large strings far more cheaply than
 * Compose `Text`, and stays selectable.
 */
@Composable
private fun ThinkingFullContentDialog(content: String, onDismiss: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    com.openminis.app.ui.theme.SpectraOrb(size = 22.dp, state = com.openminis.app.ui.theme.OrbState.Done)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.noir_step_thinking),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    MinisTextButton(onClick = onDismiss) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                }
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.ScrollView(ctx).apply {
                            addView(
                                android.widget.TextView(ctx).apply {
                                    textSize = 16.5f
                                    setLineSpacing(0f, 1.7f)
                                    typeface = androidx.core.content.res.ResourcesCompat.getFont(ctx, com.openminis.app.R.font.misans_regular)
                                    setTextColor(textColor.toArgb())
                                    setTextIsSelectable(true)
                                    val pad = (22 * ctx.resources.displayMetrics.density).toInt()
                                    setPadding(pad, pad / 2, pad, pad * 2)
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
