package com.openminis.app.ui.chat

// [T-android-split-chat] Tool-detail sheet + reveal/editor helpers extracted
// verbatim from ChatScreen.kt: ToolDetailSheet, extractShellCommand,
// extractPartialJsonString, chunkToolOutput, initialRevealChunks,
// LazyRevealToolText, EditorCard. Full import block copied (unused=warnings).
// Externally-called ones are internal; cluster-only ones stay private.

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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
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
import com.openminis.app.tools.AskUserTool
import com.openminis.app.ui.browser.BrowserSheet
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.ExpressiveTokens
import com.openminis.app.ui.DisplayBitmapLimits
import com.openminis.app.ui.components.MinisTextButton
import com.openminis.app.ui.components.MinisTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.openminis.app.ui.theme.ExpressiveLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ToolDetailSheet(
    toolBlocks: List<AssistantBlock>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onOpenTerminalWithCommand: (String) -> Unit = {},
    onOpenBrowserForUrl: (String) -> Unit = {},
    /** [T-agent-detail-buttons] delegate_task block: the bar's chat-bubble
     *  button opens the child's full-screen transcript. */
    onOpenAgentTranscript: ((childSessionId: String) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentIdx by remember { mutableStateOf(initialIndex.coerceIn(0, toolBlocks.lastIndex.coerceAtLeast(0))) }
    val block = toolBlocks.getOrNull(currentIdx) ?: return onDismiss()

    val isLive = block.toolStatus == ToolBlockStatus.RUNNING ||
        block.toolStatus == ToolBlockStatus.STREAMING ||
        block.toolStatus == ToolBlockStatus.PENDING
    val toolAccent = toolAccentColor(block.toolName)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = ExpressiveTokens.SheetTopRadius,
            topEnd = ExpressiveTokens.SheetTopRadius,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        },
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            // ── Top Nav Bar (M3 Expressive: tonal close + Sunny emblem + tonal action) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(MaterialShapes.Sunny.toShape())
                            .background(toolAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = toolIconFor(block.toolName),
                            contentDescription = null,
                            tint = toolAccent,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Text(
                        text = "Minis Computer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val clipboardManager = LocalClipboardManager.current
                var copyDone by remember { mutableStateOf(false) }
                val isShellTool = block.toolName == "shell_execute"
                val isBrowserTool = block.toolName == "browser_use"
                val toolArgsForAction = remember(block.toolArgs) {
                    try { org.json.JSONObject(block.toolArgs) } catch (_: Exception) { org.json.JSONObject() }
                }
                val browserActionUrl = remember(block.id, block.browserURL, block.toolArgs, toolBlocks) {
                    if (!isBrowserTool) ""
                    else {
                        val direct = block.browserURL?.takeIf { it.isNotEmpty() }
                        val argsUrl = toolArgsForAction.optString("url", "").takeIf { it.isNotEmpty() }
                        direct ?: argsUrl ?: run {
                            val idx = toolBlocks.indexOfFirst { it.id == block.id }
                            if (idx > 0) {
                                (idx - 1 downTo 0).firstNotNullOfOrNull { i ->
                                    val prev = toolBlocks[i]
                                    if (prev.toolName != "browser_use") return@firstNotNullOfOrNull null
                                    prev.browserURL?.takeIf { it.isNotEmpty() }
                                        ?: try {
                                            org.json.JSONObject(prev.toolArgs)
                                                .optString("url", "")
                                                .takeIf { it.isNotEmpty() }
                                        } catch (_: Exception) { null }
                                }
                            } else null
                        } ?: ""
                    }
                }
                val hasBrowserUrl = isBrowserTool && browserActionUrl.isNotEmpty()
                val isAgentTool = com.openminis.app.agent.jobs.HelperRunner.isSubAgentToolName(block.toolName)
                val agentChildId = if (isAgentTool) com.openminis.app.agent.jobs.HelperRunner.childSessionIdFrom(block.content) else null
                Surface(
                    onClick = {
                        if (isAgentTool) {
                            if (agentChildId != null) onOpenAgentTranscript?.invoke(agentChildId)
                        } else if (isShellTool) {
                            val command = extractShellCommand(toolArgsForAction, block)
                            if (command.isNotBlank() && command != "Shell command") {
                                AppLogger.info(
                                    "ChatScreen",
                                    "opening terminal with prefill: ${command.take(120)}",
                                )
                                onDismiss()
                                onOpenTerminalWithCommand(command)
                            }
                        } else if (hasBrowserUrl) {
                            AppLogger.info(
                                "ChatScreen",
                                "browser_use action → open in session pool: ${browserActionUrl.take(160)}",
                            )
                            onOpenBrowserForUrl(browserActionUrl)
                        } else if (block.content.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(block.content))
                            copyDone = true
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            when {
                                copyDone -> Icons.Default.Check
                                isAgentTool -> Icons.AutoMirrored.Outlined.Chat
                                isShellTool -> Icons.Default.Terminal
                                isBrowserTool -> Icons.Outlined.Public
                                else -> Icons.Default.ContentCopy
                            },
                            contentDescription = when {
                                isAgentTool -> "Open agent conversation"
                                isShellTool -> "Open in terminal"
                                isBrowserTool -> "Open in session browser"
                                else -> "Copy"
                            },
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                LaunchedEffect(copyDone) {
                    if (copyDone) {
                        kotlinx.coroutines.delay(2000)
                        copyDone = false
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // ── Content Area ──
            val outputScrollState = rememberScrollState()
            val toolArgsObj = remember(block.toolArgs) {
                try { org.json.JSONObject(block.toolArgs) } catch (_: Exception) { org.json.JSONObject() }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (block.toolName) {
                    com.openminis.app.agent.jobs.HelperRunner.TOOL_NAME -> HelperDetailContent(block)

                    AskUserTool.NAME -> QuestionnaireDetailContent(block)

                    // ── Shell: rounded obsidian terminal card ──
                    "shell_execute" -> {
                        val command = extractShellCommand(toolArgsObj, block)
                        val sheetMonitor = rememberSystemResourceMonitor(active = isLive)
                        val terminalShape = RoundedCornerShape(20.dp)
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                                .padding(top = 12.dp, bottom = 14.dp),
                        ) {
                            val cardMinHeight = maxWidth * 3f / 4f
                            val hudReservePadding = if (isLive) 30.dp else 0.dp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .heightIn(min = cardMinHeight)
                                    .clip(terminalShape)
                                    .background(Color(0xFF0D1117))
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF30363D),
                                        shape = terminalShape,
                                    ),
                            ) {
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(outputScrollState)
                                            .padding(16.dp)
                                            .padding(bottom = hudReservePadding),
                                    ) {
                                        Text(
                                            text = "$ $command",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFE6EDF3),
                                            lineHeight = 18.sp,
                                        )
                                        if (block.content.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val urlClick = LocalMarkdownUrlClickHandler.current
                                            val linkified = remember(block.content, urlClick) {
                                                if (urlClick != null) {
                                                    com.openminis.app.ui.util.linkifyUrls(
                                                        text = block.content,
                                                        onClick = urlClick,
                                                        linkColor = Color(0xFF79C0FF),
                                                    )
                                                } else androidx.compose.ui.text.AnnotatedString(block.content)
                                            }
                                            Text(
                                                text = linkified,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF56D364),
                                                lineHeight = 18.sp,
                                            )
                                        }
                                    }
                                }
                                if (isLive) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color(0xFF161B22))
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = sheetMonitor.formattedCpu(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF56D364),
                                        )
                                        Text(
                                            text = sheetMonitor.formattedMem(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF56D364),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── File Edit: M3 Expressive diff editor card ──
                    "file_edit" -> {
                        val path = toolArgsObj.optString("path", "")
                            .ifEmpty { extractPartialJsonString("path", block.toolArgs) ?: "" }
                        val fileName = if (path.contains("/")) path.substringAfterLast("/") else path
                        val oldStr = toolArgsObj.optString("old_string", "")
                            .ifEmpty { extractPartialJsonString("old_string", block.toolArgs) ?: "" }
                        val newStr = toolArgsObj.optString("new_string", "")
                            .ifEmpty { extractPartialJsonString("new_string", block.toolArgs) ?: "" }

                        val isDark = ChatColors.isDark
                        val cardBg = MaterialTheme.colorScheme.surfaceContainer
                        val cardBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        val redBg = if (isDark) Color(0xFF49181D) else Color(0xFFFFE8EA)
                        val redText = if (isDark) Color(0xFFFF8A8A) else Color(0xFFB31D28)
                        val greenBg = if (isDark) Color(0xFF163D22) else Color(0xFFE6FFEC)
                        val greenText = if (isDark) Color(0xFF7EE787) else Color(0xFF1A7F37)
                        val diffCardShape = RoundedCornerShape(20.dp)

                        val bytes = (oldStr.toByteArray(Charsets.UTF_8).size +
                            newStr.toByteArray(Charsets.UTF_8).size)
                        val sizeLabel = when {
                            isLive -> "streaming…"
                            bytes < 1024 -> "$bytes B"
                            else -> "${bytes / 1024} KB"
                        }
                        val resultDetail = block.content
                            .takeIf { it.isNotEmpty() && !isLive }
                            ?.let { Regex("""\(([^)]+)\)""").find(it)?.value }

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            val cardMinHeight = maxWidth * 3f / 4f
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .heightIn(min = cardMinHeight)
                                    .clip(diffCardShape)
                                    .background(cardBg)
                                    .border(1.dp, cardBorder, diffCardShape),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = toolAccent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (fileName.isNotEmpty()) fileName else "(file)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "($sizeLabel)",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isLive) toolAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                HorizontalDivider(thickness = 1.dp, color = cardBorder)

                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(outputScrollState),
                                        verticalArrangement = Arrangement.spacedBy(0.dp),
                                    ) {
                                        if (oldStr.isNotEmpty()) {
                                            oldStr.lines().forEach { line ->
                                                Text(
                                                    text = "- $line",
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = redText,
                                                    lineHeight = 18.sp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(redBg)
                                                        .padding(horizontal = 14.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                        if (newStr.isNotEmpty()) {
                                            newStr.lines().forEach { line ->
                                                Text(
                                                    text = "+ $line",
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = greenText,
                                                    lineHeight = 18.sp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(greenBg)
                                                        .padding(horizontal = 14.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isLive && (path.isNotEmpty() || resultDetail != null)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    HorizontalDivider(thickness = 1.dp, color = cardBorder)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Edited",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            if (path.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = path,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                        }
                                        if (resultDetail != null) {
                                            Text(
                                                text = resultDetail,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── File Read / File Write: M3 Expressive editor card ──
                    "file_read", "file_write" -> {
                        val path = toolArgsObj.optString("path", "")
                            .ifEmpty { extractPartialJsonString("path", block.toolArgs) ?: "" }
                        val fileName = if (path.contains("/")) path.substringAfterLast("/") else path
                        val displayText = if (block.toolName == "file_write") {
                            toolArgsObj.optString("content", "")
                                .ifEmpty { extractPartialJsonString("content", block.toolArgs) ?: "" }
                                .ifEmpty { block.content }
                        } else {
                            block.content
                        }
                        EditorCard(
                            title = fileName.ifEmpty { "file" },
                            icon = if (block.toolName == "file_read") Icons.Default.Description
                                   else Icons.AutoMirrored.Filled.NoteAdd,
                            iconTint = MaterialTheme.colorScheme.primary,
                            titleColor = MaterialTheme.colorScheme.onSurface,
                            sizeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            bodyText = displayText,
                            bodyColor = MaterialTheme.colorScheme.onSurface,
                            isStreaming = isLive,
                            scrollState = outputScrollState,
                            trailingText = if (block.toolName == "file_write" &&
                                block.content.isNotEmpty() && block.content != displayText) block.content else null,
                        )
                    }

                    // ── Browser: action capsule + URL bar + screenshot + result card ──
                    "browser_use" -> {
                        val action = toolArgsObj.optString("action", "")
                        val argsUrl = toolArgsObj.optString("url", "")
                        val resolvedUrl = block.browserURL?.takeIf { it.isNotEmpty() }
                            ?: argsUrl.takeIf { it.isNotEmpty() }
                            ?: run {
                                val blockIdx = toolBlocks.indexOfFirst { it.id == block.id }
                                if (blockIdx > 0) {
                                    (blockIdx - 1 downTo 0).firstNotNullOfOrNull { i ->
                                        val prev = toolBlocks[i]
                                        prev.browserURL?.takeIf { it.isNotEmpty() }
                                            ?: try {
                                                org.json.JSONObject(prev.toolArgs).optString("url", "").takeIf { it.isNotEmpty() }
                                            } catch (_: Exception) { null }
                                    }
                                } else null
                            } ?: ""
                        val liveBitmap = rememberBrowserLiveSnapshot(block)
                        val savedBitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(
                            initialValue = null,
                            block.imageFilePath,
                            toolBlocks.map { it.imageFilePath },
                        ) {
                            value = withContext(Dispatchers.IO) {
                                block.imageFilePath?.let { path ->
                                    DisplayBitmapLimits.decodeFileBounded(path)
                                } ?: run {
                                    val blockIdx = toolBlocks.indexOfFirst { it.id == block.id }
                                    if (blockIdx <= 0) null
                                    else (blockIdx - 1 downTo 0).firstNotNullOfOrNull { i ->
                                        val prev = toolBlocks[i]
                                        if (prev.toolName == "browser_use" && prev.imageFilePath != null) {
                                            DisplayBitmapLimits.decodeFileBounded(prev.imageFilePath)
                                        } else null
                                    }
                                }
                            }
                        }
                        val screenshotBitmap = liveBitmap ?: savedBitmap

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(outputScrollState)
                                .padding(top = 12.dp, bottom = 16.dp),
                        ) {
                            if (action.isNotEmpty() || resolvedUrl.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (action.isNotEmpty()) {
                                        Text(
                                            text = action,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .padding(horizontal = 11.dp, vertical = 3.dp),
                                        )
                                    }
                                    if (resolvedUrl.isNotEmpty()) {
                                        val clipboardManager = LocalClipboardManager.current
                                        Text(
                                            text = resolvedUrl,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                                                .clip(CircleShape)
                                                .clickable { clipboardManager.setText(AnnotatedString(resolvedUrl)) }
                                                .padding(horizontal = 12.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }

                            if (screenshotBitmap != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val aspect = screenshotBitmap.width.toFloat() /
                                    screenshotBitmap.height.coerceAtLeast(1)
                                Image(
                                    bitmap = screenshotBitmap.asImageBitmap(),
                                    contentDescription = "Browser screenshot",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .aspectRatio(aspect)
                                        .shadow(6.dp, RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(16.dp),
                                        ),
                                )
                            } else if (isLive) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ExpressiveLoadingIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            "Loading...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            if (block.content.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val resultShape = RoundedCornerShape(20.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainer, resultShape)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                            resultShape,
                                        )
                                        .clip(resultShape),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.Language,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Result",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                    Text(
                                        text = block.content,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(14.dp),
                                    )
                                }
                            }
                        }
                    }

                    // ── Memory: tinted M3 Expressive editor card ──
                    "memory_write", "memory_get" -> {
                        val memContent = if (block.toolName == "memory_write") {
                            toolArgsObj.optString("content", "")
                                .ifEmpty { extractPartialJsonString("content", block.toolArgs) ?: "" }
                                .ifEmpty { block.content }
                        } else {
                            block.content
                        }
                        val keywords = toolArgsObj.optString("keywords", "")
                            .ifEmpty { extractPartialJsonString("keywords", block.toolArgs) ?: "" }
                        val prefix = if (keywords.isNotEmpty() && block.toolName == "memory_get")
                            "Keywords: $keywords\n\n" else ""
                        EditorCard(
                            title = block.toolName,
                            icon = Icons.Default.Psychology,
                            iconTint = ToolMemoryAccent,
                            titleColor = ToolMemoryAccent,
                            sizeColor = ToolMemoryAccent.copy(alpha = 0.7f),
                            bodyText = prefix + memContent,
                            bodyColor = MaterialTheme.colorScheme.onSurface,
                            isStreaming = isLive,
                            scrollState = outputScrollState,
                        )
                    }

                    // ── read_image: inline image preview + metadata ──
                    "read_image" -> {
                        val imgPath = block.imageFilePath
                        val bitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(
                            initialValue = null,
                            imgPath,
                        ) {
                            value = if (imgPath == null) null else withContext(Dispatchers.IO) {
                                val f = File(imgPath)
                                if (!f.exists()) null
                                else DisplayBitmapLimits.decodeFileBounded(f.absolutePath)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(outputScrollState)
                                .padding(16.dp),
                        ) {
                            val bmp = bitmap
                            if (bmp != null) {
                                val aspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1)
                                val imgShape = RoundedCornerShape(16.dp)
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(aspect)
                                        .clip(imgShape)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                            imgShape,
                                        ),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            if (block.content.isNotEmpty()) {
                                Text(
                                    text = block.content,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }

                    // ── Default: generic text output ──
                    else -> {
                        if (block.content.isNotEmpty()) {
                            val genericShape = RoundedCornerShape(20.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(outputScrollState)
                                    .padding(14.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainer, genericShape)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                            genericShape,
                                        )
                                        .clip(genericShape)
                                        .padding(16.dp),
                                ) {
                                    androidx.compose.foundation.text.selection.SelectionContainer {
                                        LazyRevealToolText(
                                            bodyText = block.content,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            scrollState = outputScrollState,
                                            linkify = true,
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(MaterialShapes.SoftBurst.toShape())
                                            .background(toolAccent.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isLive) {
                                            ExpressiveLoadingIndicator(
                                                color = toolAccent,
                                                modifier = Modifier.size(32.dp),
                                            )
                                        } else {
                                            Icon(
                                                toolIconFor(block.toolName),
                                                contentDescription = null,
                                                tint = toolAccent,
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isLive) "Running..." else "No output",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Bottom Bar (M3 Expressive: tool info + tonal step pagination) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .navigationBarsPadding(),
            ) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isLive) {
                        ExpressiveLoadingIndicator(
                            modifier = Modifier.size(20.dp),
                            color = toolAccent,
                        )
                    } else {
                        val (icon, tint) = when (block.toolStatus) {
                            ToolBlockStatus.SUCCESS -> Icons.Default.CheckCircle to ToolCheckColor
                            ToolBlockStatus.FAILED -> Icons.Default.Error to ToolErrorColor
                            ToolBlockStatus.CANCELLED -> Icons.Default.Cancel to ToolCancelColor
                            ToolBlockStatus.TIMEOUT -> Icons.Default.Schedule to ToolErrorColor
                            else -> Icons.Default.CheckCircle to ToolCheckColor
                        }
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = toolTitleLabel(block.toolName),
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = block.toolTitle.ifEmpty { block.toolName },
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    val shownDurationMs = if (com.openminis.app.agent.jobs.HelperRunner.isSubAgentToolName(block.toolName)) {
                        runCatching { org.json.JSONObject(block.content).optLong("elapsed_s", -1L) }.getOrDefault(-1L)
                            .takeIf { it >= 0 }?.let { it * 1000L } ?: block.durationMs
                    } else block.durationMs
                    if ((shownDurationMs > 0 && !isLive) || block.startTimeMs > 0L) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            if (shownDurationMs > 0 && !isLive) {
                                Text(
                                    text = formatToolDuration(shownDurationMs),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (block.startTimeMs > 0L) {
                                Text(
                                    text = formatStepTimestamp(block.startTimeMs),
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val hasPrev = currentIdx > 0
                    Surface(
                        onClick = { if (hasPrev) currentIdx-- },
                        enabled = hasPrev,
                        shape = CircleShape,
                        color = if (hasPrev) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = if (hasPrev) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        if (isLive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(ToolCheckColor, CircleShape),
                                )
                                Text(
                                    "Live",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } else {
                            Text(
                                "${currentIdx + 1} / ${toolBlocks.size}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val hasNext = currentIdx < toolBlocks.lastIndex
                    Surface(
                        onClick = { if (hasNext) currentIdx++ },
                        enabled = hasNext,
                        shape = CircleShape,
                        color = if (hasNext) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = if (hasNext) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper: extract shell command from args or content (mirrors iOS toolDescription logic).
// Also tolerant of *partial* streaming JSON (JSONObject.optString returns "" for
// truncated objects, so fall back to a streaming-safe substring scan).
internal fun extractShellCommand(args: org.json.JSONObject, block: AssistantBlock): String {
    // 1. Try toolArgs "command" field (works once JSON is complete)
    val fromArgs = args.optString("command", "")
    if (fromArgs.isNotEmpty()) return fromArgs
    // 2. Streaming fallback: scan the raw toolArgs buffer for `"command":"…`
    //    which may not yet close. Mirrors iOS extractPartialStringValue.
    val partial = extractPartialJsonString("command", block.toolArgs)
    if (!partial.isNullOrEmpty()) return partial
    // 3. Try parsing "$ <command>" from first line of content (iOS fallback)
    if (block.content.startsWith("$ ")) {
        val firstLine = block.content.lineSequence().firstOrNull() ?: ""
        if (firstLine.length > 2) return firstLine.drop(2)
    }
    // 4. iOS fallback: generic "Shell command"
    return "Shell command"
}

/**
 * Tolerant partial-JSON string extractor — UI-side mirror of
 * ChatViewModel.extractPartialStringValue. Used so detail-sheet renderers
 * (shell command, file path, write content) can show live content while the
 * model is still streaming the tool input JSON.
 */
internal fun extractPartialJsonString(key: String, json: String): String? {
    if (json.isEmpty()) return null
    val patterns = listOf("\"$key\": \"", "\"$key\":\"")
    for (p in patterns) {
        val at = json.indexOf(p)
        if (at < 0) continue
        val after = json.substring(at + p.length)
        var i = 0
        val n = after.length
        while (i < n) {
            val c = after[i]
            if (c == '\\') { i += 2; continue }
            if (c == '"') {
                return after.substring(0, i)
                    .replace("\\n", "\n").replace("\\t", "\t")
                    .replace("\\\"", "\"").replace("\\/", "/")
                    .replace("\\\\", "\\")
            }
            i++
        }
        // No closing quote yet (still streaming) — return what we have.
        return after.replace("\\n", "\n").replace("\\t", "\t")
            .replace("\\\"", "\"").replace("\\/", "/")
            .replace("\\\\", "\\")
    }
    return null
}

/**
 * Shared editor-card layout used by file_read / file_write / memory_* detail
 * views. Mirrors iOS `fileEditorContent` + `memoryEditorContent` from
 * ToolLiveSheet.swift: an inner card with:
 *  - 10dp rounded corners + 0.5dp outline
 *  - header row (icon + title + size label) on a slightly darker strip
 *  - divider
 *  - monospaced body padded to 14dp (horizontal) / 14dp (vertical)
 *  - optional trailing footer text below the card
 *
 * Colors are parameterized so callers can tint the whole card (e.g. memory
 * uses pink everywhere; file uses neutral primary text).
 */

// ─── [T-android-tool-result-lazy-render] ────────────────────────────────────
// Opening a tool-result detail (ToolDetailSheet) with a large payload — a big
// memory_get / file read — janked: the body was a single Text laid out at once
// inside a verticalScroll Column (which, like a ScrollView, does NOT virtualize),
// so the whole 70KB+ string was composed + measured on open. Mirrors iOS
// commit 9d81ba18 (ToolLiveSheet lazy-reveal).
//
// We chunk the body into 40-line groups and reveal an initial window (~200
// lines / 10KB, whichever is fewer), growing the window each time the user
// reaches the bottom (auto-bump on the footer's onGloballyPositioned) or taps
// "Load more" / "Load all". Only the revealed Texts are composed, so open is
// cheap regardless of total size. Still scrollable and still selectable —
// rendered inside the caller's SelectionContainer; the reveal window's Texts
// register with the same plain-Compose SelectionRegistrar (NOT MinisTextKit,
// which is the chat-list markdown layer and isn't involved here).

private const val LAZY_TOOL_CHUNK_LINES = 40
private const val LAZY_TOOL_INITIAL_CHUNKS = 5            // ~200 lines
private const val LAZY_TOOL_BATCH_CHUNKS = 5              // +200 lines per reveal
private const val LAZY_TOOL_INITIAL_BYTE_CAP = 10 * 1024 // clamp first window to ~10KB

/** Split [text] into ordered 40-line chunks. Each chunk keeps its trailing
 *  newline so concatenation round-trips. */
private fun chunkToolOutput(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val lines = text.split("\n")
    val out = ArrayList<String>((lines.size / LAZY_TOOL_CHUNK_LINES) + 1)
    var i = 0
    while (i < lines.size) {
        val end = minOf(i + LAZY_TOOL_CHUNK_LINES, lines.size)
        // Re-join with "\n"; the final chunk omits the trailing separator that
        // split() consumed, which is fine — we never re-concatenate for display.
        out.add(lines.subList(i, end).joinToString("\n"))
        i = end
    }
    return out
}

/** Initial reveal count: up to [LAZY_TOOL_INITIAL_CHUNKS], further clamped so
 *  the first window stays under [LAZY_TOOL_INITIAL_BYTE_CAP] (covers a few very
 *  long lines that fit in < 5 chunks but exceed 10KB). */
private fun initialRevealChunks(chunks: List<String>): Int {
    if (chunks.isEmpty()) return 0
    var count = 0
    var bytes = 0
    for (chunk in chunks.take(LAZY_TOOL_INITIAL_CHUNKS)) {
        bytes += chunk.toByteArray(Charsets.UTF_8).size
        count++
        if (bytes >= LAZY_TOOL_INITIAL_BYTE_CAP) break
    }
    return count.coerceAtLeast(1)
}

/**
 * [T-android-tool-result-lazy-render] Render [bodyText] with incremental reveal.
 * Builds the revealed prefix as ONE AnnotatedString (optionally linkified) so
 * selection/copy spans the whole revealed window as a single contiguous Text,
 * then a "Load more / Load all" footer. Caller supplies the surrounding
 * verticalScroll (passed in as [scrollState]) + SelectionContainer.
 *
 * Auto-reveal is gated on the scroll position reaching the bottom — NOT on the
 * footer being positioned. The body lives inside a `verticalScroll` Column,
 * which composes + positions ALL children (it doesn't virtualize), so a
 * position-based trigger would fire for the off-screen footer immediately and
 * reveal everything at once, defeating the laziness. Watching
 * `scrollState.value` vs `maxValue` instead only grows the window once the user
 * has actually scrolled near the end of what's currently revealed.
 */
@Composable
private fun LazyRevealToolText(
    bodyText: String,
    color: Color,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
    linkify: Boolean = false,
) {
    val chunks = remember(bodyText) { chunkToolOutput(bodyText) }
    // Reset the reveal window whenever the underlying text changes (e.g. the
    // user pages to a different tool block, which swaps bodyText).
    var revealed by remember(bodyText) { mutableStateOf(initialRevealChunks(chunks)) }
    val total = chunks.size
    val shownText = remember(bodyText, revealed) {
        chunks.take(revealed.coerceIn(1, total.coerceAtLeast(1))).joinToString("\n")
    }
    val urlClick = LocalMarkdownUrlClickHandler.current
    val displayed = remember(shownText, urlClick, linkify) {
        if (linkify && urlClick != null) {
            com.openminis.app.ui.util.linkifyUrls(text = shownText, onClick = urlClick)
        } else androidx.compose.ui.text.AnnotatedString(shownText)
    }

    // Auto-grow the window when the user scrolls within ~600px of the bottom of
    // the currently-revealed content. derivedStateOf keeps the predicate from
    // recomposing on every scroll pixel; it only flips at the threshold.
    val nearBottom by remember {
        derivedStateOf {
            val max = scrollState.maxValue
            max > 0 && max != Int.MAX_VALUE && scrollState.value >= max - 600
        }
    }
    LaunchedEffect(nearBottom, revealed, total) {
        if (nearBottom && revealed < total) {
            revealed = (revealed + LAZY_TOOL_BATCH_CHUNKS).coerceAtMost(total)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = displayed,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        if (revealed < total) {
            val remainingChunks = total - revealed
            val nextLines = minOf(LAZY_TOOL_BATCH_CHUNKS, remainingChunks) * LAZY_TOOL_CHUNK_LINES
            val remainingLines = remainingChunks * LAZY_TOOL_CHUNK_LINES
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MinisTonalButton(
                    onClick = {
                        revealed = (revealed + LAZY_TOOL_BATCH_CHUNKS).coerceAtMost(total)
                    },
                ) {
                    Text(
                        text = "Load more ($nextLines lines)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                MinisTonalButton(
                    onClick = { revealed = total },
                ) {
                    Text(
                        text = "Load all (~$remainingLines)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditorCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    titleColor: Color,
    sizeColor: Color,
    bodyText: String,
    bodyColor: Color,
    isStreaming: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    trailingText: String? = null,
) {
    val bytes = bodyText.toByteArray(Charsets.UTF_8).size
    val sizeLabel = when {
        bodyText.isEmpty() -> null
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val headerBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val cardBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val cardShape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(cardBg)
                .border(1.dp, cardBorder, cardShape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (sizeLabel != null) {
                    Text(
                        text = if (isStreaming) "($sizeLabel received)" else "($sizeLabel)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isStreaming) MaterialTheme.colorScheme.primary else sizeColor,
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = cardBorder)

            if (bodyText.isNotEmpty()) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    LazyRevealToolText(
                        bodyText = bodyText,
                        color = bodyColor,
                        scrollState = scrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                    )
                }
            } else if (isStreaming) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(24.dp),
                        color = iconTint,
                    )
                }
            }
        }
        if (trailingText != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = trailingText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
