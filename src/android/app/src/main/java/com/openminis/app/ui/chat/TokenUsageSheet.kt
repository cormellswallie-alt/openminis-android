package com.openminis.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.i18n.uppercaseForDisplay

/**
 * Session Token Usage bottom sheet — mirrors iOS `TokenUsageSheet` and uses
 * the standardized chat sheet shell so its header/dismiss behavior matches
 * every other "⋯" menu sheet.
 *
 * Sections (top to bottom):
 *   - Context: Context Used / Context Window / Max Output
 *   - Thinking (only when the model supports reasoning): On/Off / Level / Supported
 *   - Tokens (Session Total): Input (incl. cache) / Output
 *   - Cache (Session Total): Cache Read / Cache Write
 *   - Agent Loop: Total Loops
 *
 * Data loads asynchronously via [ChatViewModel.loadSessionTokenStats] when the
 * sheet appears; we intentionally don't hold a live subscription — token
 * counters change per API call, not per keystroke, so pull-on-open is enough.
 */
@Composable
fun TokenUsageSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
) {
    var stats by remember { mutableStateOf<ChatViewModel.SessionTokenStats?>(null) }
    val contextWindow = remember { viewModel.currentModelContextWindow }
    val maxOutput = remember { viewModel.currentModelMaxOutputTokens }
    val thinking = remember { viewModel.thinkingInfo() }

    LaunchedEffect(Unit) {
        stats = viewModel.loadSessionTokenStats()
    }

    StandardChatSheet(
        title = stringResource(R.string.token_usage_sheet_title),
        onDismiss = onDismiss,
        // T148: iOS uses .presentationDetents([.medium]) for the same sheet
        // (AIChatView.swift:508). Match that proportion on Android so the
        // half-screen feel is consistent — the token-usage view holds maybe
        // a screenful of stat rows max and looked overgrown at 90%.
        heightFraction = 0.5f,
    ) {
        // [T-claude-ui] Design board r12 "本对话": no cards — serif numbers,
        // hairlines and air. Same data as before, nothing dropped.
        val dark = com.openminis.app.ui.theme.LocalChatPalette.current.isDark
        val hair = if (dark) com.openminis.app.ui.theme.Noir.OnyxHairline else androidx.compose.ui.graphics.Color(0xFFE8E4DA)
        val label = if (dark) com.openminis.app.ui.theme.Noir.IvoryMute else androidx.compose.ui.graphics.Color(0xFF8A867F)
        val faint = if (dark) com.openminis.app.ui.theme.Noir.IvoryFaint else androidx.compose.ui.graphics.Color(0xFF9A968E)
        val ink = if (dark) com.openminis.app.ui.theme.Noir.Ivory else androidx.compose.ui.graphics.Color(0xFF2A2825)
        val clay = if (dark) com.openminis.app.ui.theme.Noir.ClayOnOnyx else com.openminis.app.ui.theme.Noir.Clay
        val serif = com.openminis.app.ui.theme.ClaudeSerif
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val s = stats
            val onText = stringResource(R.string.common_on)
            val offText = stringResource(R.string.common_off)
            val used = s?.context ?: 0
            val win = contextWindow ?: 0
            val frac = if (win > 0) (used.toFloat() / win).coerceIn(0f, 1f) else 0f
            val hot = frac >= 0.85f

            // ── Context ──
            Column {
                SecHead(stringResource(R.string.token_usage_section_context), contextWindow?.let { formatTokens(it) } ?: "", label, faint)
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = if (win > 0) "${Math.round(frac * 100)}%" else formatTokens(used),
                        fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 36.sp,
                        color = if (hot) com.openminis.app.ui.theme.Noir.Err else ink,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (win > 0) "${formatTokens(used)} / ${formatTokens(win)}" else "",
                        fontSize = 13.sp, color = label, modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth().height(6.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                        .background(if (dark) com.openminis.app.ui.theme.Noir.Onyx3 else androidx.compose.ui.graphics.Color(0xFFE6E1D6)),
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth(frac).height(6.dp).background(clay),
                    )
                }
                maxOutput?.let {
                    KvLine(stringResource(R.string.token_usage_max_output), formatTokens(it), label, ink, Modifier.padding(top = 12.dp))
                }
            }
            Hair(hair)

            // ── Tokens ──
            Column {
                SecHead(stringResource(R.string.token_usage_section_tokens), "${s?.loopCount ?: 0} ×", label, faint)
                val inputTotal = (s?.input ?: 0L) + (s?.cacheRead ?: 0L) + (s?.cacheWrite ?: 0L)
                Row {
                    Cell(stringResource(R.string.token_usage_input_with_cache), formatTokens(inputTotal), null, label, ink, serif, Modifier.weight(1f))
                    Spacer(Modifier.width(16.dp))
                    Cell(stringResource(R.string.token_usage_output), formatTokens(s?.output ?: 0L), null, label, ink, serif, Modifier.weight(1f))
                }
                Row {
                    Cell(
                        stringResource(R.string.token_usage_cache_read), formatTokens(s?.cacheRead ?: 0L),
                        s?.cacheHitRate?.let { String.format(java.util.Locale.US, "%.0f%%", it) },
                        label, ink, serif, Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(16.dp))
                    Cell(stringResource(R.string.token_usage_cache_write), formatTokens(s?.cacheWrite ?: 0L), null, label, ink, serif, Modifier.weight(1f))
                }
            }
            Hair(hair)

            // ── Speed ──
            Column {
                SecHead(stringResource(R.string.token_usage_section_speed), "", label, faint)
                val speed = s?.outputTokensPerSecond
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (speed != null) String.format(java.util.Locale.US, "%.0f", speed) else "—",
                        fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 36.sp, color = ink,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("tok/s", fontSize = 13.sp, color = label, modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            thinking?.let { t ->
                Hair(hair)
                Column {
                    SecHead(stringResource(R.string.token_usage_section_thinking), "", label, faint)
                    KvLine(stringResource(R.string.token_usage_thinking_label), if (t.enabled) onText else offText, label, ink)
                    if (t.enabled) KvLine(stringResource(R.string.token_usage_thinking_level), t.level, label, ink, Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SecHead(title: String, right: String, label: androidx.compose.ui.graphics.Color, faint: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = label)
        Spacer(Modifier.weight(1f))
        if (right.isNotEmpty()) Text(right, fontSize = 12.sp, color = faint)
    }
}

@Composable
private fun Hair(c: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(c))
}

@Composable
private fun KvLine(k: String, v: String, label: androidx.compose.ui.graphics.Color, ink: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(k, fontSize = 13.5.sp, color = label)
        Spacer(Modifier.weight(1f))
        Text(v, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ink)
    }
}

@Composable
private fun Cell(
    k: String, v: String, x: String?,
    label: androidx.compose.ui.graphics.Color, ink: androidx.compose.ui.graphics.Color,
    serif: androidx.compose.ui.text.font.FontFamily, modifier: Modifier,
) {
    Column(modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(k, fontSize = 12.5.sp, color = label, maxLines = 1)
        Text(v, fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, color = ink)
        if (x != null) Text(x, fontSize = 12.sp, color = label)
    }
}

@Composable
private fun StatSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title.uppercaseForDisplay(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        content()
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

// iOS formatting: "%.1fM" >= 1M, "%.1fK" >= 1k, raw otherwise.
private fun formatTokens(n: Long): String = when {
    n >= 1_000_000L -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000L -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}

private fun formatTokens(n: Int): String = formatTokens(n.toLong())
