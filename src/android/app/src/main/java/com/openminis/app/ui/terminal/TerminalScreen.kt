package com.openminis.app.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.sandbox.TerminalSession
import com.openminis.app.terminal.MinisOpenUrlBroker
import com.openminis.app.ui.terminal.canvas.TerminalInputView
import com.openminis.app.ui.terminal.canvas.TerminalNativeViewCompose
import com.openminis.app.ui.terminal.canvas.rememberTerminalInputController
import com.openminis.app.ui.terminal.emulator.TerminalEmulator

// M3 Expressive Dark Console Palette (Midnight Slate + Electric Cobalt-Iris + Emerald)
private val TerminalBg = Color(0xFF090C12)
private val TerminalHeaderBg = Color(0xFF0F141E)
private val TerminalFg = Color(0xFFE6EAF5)
private val TerminalMuted = Color(0xFF94A0B8)
private val TerminalEmerald = Color(0xFF3DDB84)
private val TerminalCoral = Color(0xFFFF756B)
private val AccessoryDeckBg = Color(0xFF141B29)
private val AccessoryDeckBorder = Color(0xFF242F45)
private val KeyPillBg = Color(0xFF1E273B)
private val KeyPillActiveBg = Color(0xFF5668FF)
private val TopSquircleBg = Color(0xFF1A2233)

@Composable
fun TerminalScreen(
    terminalSession: TerminalSession,
    onBack: () -> Unit,
    initCommand: String? = null,
    sessionId: String? = null,
) {
    val emulator = remember { TerminalEmulator() }
    val inputController = rememberTerminalInputController()
    var ctrlActive by remember { mutableStateOf(false) }

    // Pipe PTY output → emulator.
    LaunchedEffect(terminalSession) {
        terminalSession.outputBytes.collect { bytes ->
            emulator.feed(bytes)
        }
    }

    // Wire emulator responses (DSR etc.) back to the PTY.
    DisposableEffect(terminalSession, emulator) {
        emulator.onResponse = { data -> terminalSession.sendRawBytes(data) }
        onDispose { emulator.onResponse = null }
    }

    // Clear emulator when session clearOutput() ticks.
    val clearVersion by terminalSession.clearVersion.collectAsStateEffect()
    LaunchedEffect(clearVersion) {
        if (clearVersion > 0) {
            emulator.feed("\u001Bc".toByteArray())   // RIS — full reset
        }
    }

    // Start session + optional initCommand.
    LaunchedEffect(Unit) {
        if (!terminalSession.isRunning) terminalSession.start(sessionId = sessionId)
        if (!initCommand.isNullOrBlank()) {
            kotlinx.coroutines.delay(500)
            terminalSession.sendText(initCommand)
        }
    }

    val hasHardwareKeyboard = com.openminis.app.ui.components.rememberHasHardwareKeyboard()
    LaunchedEffect(hasHardwareKeyboard) {
        if (hasHardwareKeyboard) inputController.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose { terminalSession.stop() }
    }

    DisposableEffect(Unit) {
        MinisOpenUrlBroker.setTerminalVisible(true)
        onDispose { MinisOpenUrlBroker.setTerminalVisible(false) }
    }

    var previewUrl by remember { mutableStateOf<String?>(null) }
    val pendingUrl by MinisOpenUrlBroker.pendingUrl.collectAsStateEffect()
    LaunchedEffect(pendingUrl) {
        val uri = pendingUrl ?: return@LaunchedEffect
        if (MinisOpenUrlBroker.isWebScheme(uri.scheme)) {
            previewUrl = uri.toString()
        }
        MinisOpenUrlBroker.consume()
    }

    val accessoryBarHeightDp = 50.dp

    Box(modifier = Modifier.fillMaxSize().background(TerminalBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .padding(bottom = accessoryBarHeightDp),
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                TerminalNativeViewCompose(
                    emulator = emulator,
                    onResize = { cols, rows ->
                        emulator.resize(cols, rows)
                        terminalSession.setWindowSize(cols, rows)
                    },
                    onTap = { inputController.requestFocus() },
                )
                TerminalInputView(
                    onInput = { bytes ->
                        emulator.scrollOffset = 0
                        if (ctrlActive && bytes.size == 1) {
                            val ch = bytes[0].toInt().toChar().uppercaseChar()
                            if (ch in 'A'..'Z') {
                                terminalSession.sendRawBytes(byteArrayOf((ch - 'A' + 1).toByte()))
                                ctrlActive = false
                                return@TerminalInputView
                            }
                        }
                        terminalSession.sendRawBytes(bytes)
                    },
                    applicationCursorKeys = emulator.applicationCursorKeys,
                    controller = inputController,
                    modifier = Modifier.size(1.dp),
                )
            }
        }

        // Top bar pinned at top — never moves with IME.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.systemBars)
                .height(56.dp)
                .background(TerminalHeaderBg),
        ) {
            TerminalTopBar(
                boundSession = sessionId != null,
                onClose = {
                    terminalSession.stop()
                    onBack()
                },
                onClear = {
                    terminalSession.sendRawBytes(byteArrayOf(0x15))
                    terminalSession.clearOutput()
                    emulator.feed("\u001Bc".toByteArray())
                },
            )
        }

        // Floating M3 Expressive Console Key Deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            KeyboardAccessoryBar(
                ctrlActive = ctrlActive,
                keyboardVisible = inputController.isFocused,
                onCtrlToggle = { ctrlActive = !ctrlActive },
                onToggleKeyboard = {
                    if (inputController.isFocused) inputController.clearFocus()
                    else inputController.requestFocus()
                },
                onSendRaw = { bytes ->
                    emulator.scrollOffset = 0
                    terminalSession.sendRawBytes(bytes)
                },
                onArrow = { dir ->
                    emulator.scrollOffset = 0
                    val prefix = if (emulator.applicationCursorKeys)
                        byteArrayOf(0x1B, 'O'.code.toByte())
                    else byteArrayOf(0x1B, '['.code.toByte())
                    terminalSession.sendRawBytes(prefix + byteArrayOf(dir.code.toByte()))
                },
            )
        }

        previewUrl?.let { url ->
            com.openminis.app.ui.components.UrlPreviewSheet(
                url = url,
                onDismiss = { previewUrl = null },
            )
        }
    }
}

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateEffect(): androidx.compose.runtime.State<T> {
    val state = remember { androidx.compose.runtime.mutableStateOf(value) }
    LaunchedEffect(this) { collect { state.value = it } }
    return state
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TerminalTopBar(
    boundSession: Boolean,
    onClose: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConsoleActionButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.common_close),
            tint = TerminalFg,
            onClick = onClose,
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = TopSquircleBg,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(MaterialShapes.Pentagon.toShape())
                        .background(TerminalEmerald),
                )
                Text(
                    stringResource(R.string.terminal_title),
                    color = TerminalFg,
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                )
                if (boundSession) {
                    Text(
                        "· /var/minis",
                        color = TerminalMuted,
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        ConsoleActionButton(
            icon = Icons.Default.Brush,
            contentDescription = stringResource(R.string.terminal_clear),
            tint = TerminalEmerald,
            onClick = onClear,
        )
    }
}

@Composable
private fun ConsoleActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TopSquircleBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun KeyboardAccessoryBar(
    ctrlActive: Boolean,
    keyboardVisible: Boolean,
    onCtrlToggle: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onSendRaw: (ByteArray) -> Unit,
    onArrow: (Char) -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AccessoryDeckBg)
            .border(1.dp, AccessoryDeckBorder, RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickCommandButton(
                label = stringResource(if (keyboardVisible) R.string.terminal_hide_keyboard else R.string.terminal_show_keyboard),
                icon = if (keyboardVisible) Icons.Default.KeyboardHide else Icons.Outlined.Keyboard,
                onClick = onToggleKeyboard,
            )
            QuickCommandButton("Esc", iconText = "⎋") { onSendRaw(byteArrayOf(0x1B)) }
            QuickCommandButton("Tab", icon = Icons.AutoMirrored.Filled.KeyboardTab) { onSendRaw(byteArrayOf(0x09)) }
            QuickCommandButton("⏎", iconText = "⏎") { onSendRaw(byteArrayOf(0x0D)) }
            QuickCommandButton("Ctrl", iconText = "^", isActive = ctrlActive, onClick = onCtrlToggle)
            QuickCommandButton("\u2191", icon = Icons.Default.KeyboardArrowUp) { onArrow('A') }
            QuickCommandButton("\u2193", icon = Icons.Default.KeyboardArrowDown) { onArrow('B') }
            QuickCommandButton("\u2190", icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft) { onArrow('D') }
            QuickCommandButton("\u2192", icon = Icons.AutoMirrored.Filled.KeyboardArrowRight) { onArrow('C') }
            QuickCommandButton("C-c", icon = Icons.Outlined.Cancel, accent = TerminalCoral) { onSendRaw(byteArrayOf(0x03)) }
            QuickCommandButton("C-d", icon = Icons.Default.Eject) { onSendRaw(byteArrayOf(0x04)) }
            QuickCommandButton("C-z", icon = Icons.Outlined.PauseCircle) { onSendRaw(byteArrayOf(0x1A)) }
        }
    }
}

@Composable
private fun QuickCommandButton(
    label: String,
    icon: ImageVector? = null,
    iconText: String? = null,
    isActive: Boolean = false,
    accent: Color = TerminalEmerald,
    onClick: () -> Unit,
) {
    val bg = if (isActive) KeyPillActiveBg else KeyPillBg
    val fg = if (isActive) Color.White else accent
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        when {
            icon != null -> Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
            iconText != null -> Text(
                iconText,
                color = fg,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
            )
        }
        if (label != iconText) {
            Text(
                label,
                color = if (isActive) Color.White else TerminalFg,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp,
                ),
                maxLines = 1,
            )
        }
    }
}
