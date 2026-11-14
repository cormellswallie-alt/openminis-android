package com.openminis.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.settings.SettingsSection

/**
 * Session Token Usage bottom sheet — Material 3 Expressive articulated bento cards
 * inside [StandardChatSheet].
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
        heightFraction = 0.62f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            val s = stats
            val onText = stringResource(R.string.common_on)
            val offText = stringResource(R.string.common_off)
            val yesText = stringResource(R.string.common_yes)
            val noText = stringResource(R.string.common_no)

            SettingsSection(header = stringResource(R.string.token_usage_section_context)) {
                val hasWindow = contextWindow != null
                val hasMaxOut = maxOutput != null
                StatRow(
                    label = stringResource(R.string.token_usage_context_used),
                    value = formatTokens(s?.context ?: 0),
                    showDivider = hasWindow || hasMaxOut,
                )
                contextWindow?.let {
                    StatRow(
                        label = stringResource(R.string.token_usage_context_window),
                        value = formatTokens(it),
                        showDivider = hasMaxOut,
                    )
                }
                maxOutput?.let {
                    StatRow(
                        label = stringResource(R.string.token_usage_max_output),
                        value = formatTokens(it),
                        showDivider = false,
                    )
                }
            }

            thinking?.let { t ->
                SettingsSection(header = stringResource(R.string.token_usage_section_thinking)) {
                    StatRow(
                        label = stringResource(R.string.token_usage_thinking_label),
                        value = if (t.enabled) onText else offText,
                        showDivider = true,
                    )
                    if (t.enabled) {
                        StatRow(
                            label = stringResource(R.string.token_usage_thinking_level),
                            value = t.level,
                            showDivider = true,
                        )
                    }
                    StatRow(
                        label = stringResource(R.string.token_usage_thinking_supported),
                        value = if (t.supported) yesText else noText,
                        showDivider = false,
                    )
                }
            }

            SettingsSection(header = stringResource(R.string.token_usage_section_tokens)) {
                val inputTotal = (s?.input ?: 0L) + (s?.cacheRead ?: 0L) + (s?.cacheWrite ?: 0L)
                StatRow(
                    label = stringResource(R.string.token_usage_input_with_cache),
                    value = formatTokens(inputTotal),
                    showDivider = true,
                )
                StatRow(
                    label = stringResource(R.string.token_usage_output),
                    value = formatTokens(s?.output ?: 0L),
                    showDivider = false,
                )
            }

            SettingsSection(header = stringResource(R.string.token_usage_section_cache)) {
                val hitRate = s?.cacheHitRate
                StatRow(
                    label = stringResource(R.string.token_usage_cache_read),
                    value = formatTokens(s?.cacheRead ?: 0L),
                    showDivider = true,
                )
                StatRow(
                    label = stringResource(R.string.token_usage_cache_write),
                    value = formatTokens(s?.cacheWrite ?: 0L),
                    showDivider = hitRate != null,
                )
                hitRate?.let {
                    StatRow(
                        label = stringResource(R.string.token_usage_cache_hit_rate),
                        value = String.format(java.util.Locale.US, "%.1f%%", it),
                        showDivider = false,
                    )
                }
            }

            SettingsSection(header = stringResource(R.string.token_usage_section_speed)) {
                val speed = s?.outputTokensPerSecond
                StatRow(
                    label = stringResource(R.string.token_usage_output_speed),
                    value = if (speed != null) String.format(java.util.Locale.US, "%.1f tok/s", speed) else "—",
                    showDivider = false,
                )
            }

            SettingsSection(header = stringResource(R.string.token_usage_section_agent_loop)) {
                StatRow(
                    label = stringResource(R.string.token_usage_total_loops),
                    value = (s?.loopCount ?: 0).toString(),
                    showDivider = false,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showDivider) {
            ExpressiveArticulatedSeam()
        }
    }
}

// iOS formatting: "%.1fM" >= 1M, "%.1fK" >= 1k, raw otherwise.
private fun formatTokens(n: Long): String = when {
    n >= 1_000_000L -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000L -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}

private fun formatTokens(n: Int): String = formatTokens(n.toLong())
