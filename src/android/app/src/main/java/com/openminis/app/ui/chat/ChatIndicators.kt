package com.openminis.app.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.drawNoirPearl
import com.openminis.app.ui.components.rememberDecorativeTick
import com.openminis.app.ui.components.decorativePhase
import com.openminis.app.ui.components.decorativePingPong

// [T-android-split-chat] Self-contained "thinking / streaming" dot indicators
// extracted verbatim from ChatScreen.kt. `internal` so the chat package can
// still reference them. No logic change — code moved as-is.

@Composable
internal fun BouncingDots(color: Color) {
    // [T-android-decorative-anim-perf] Was three infinite transitions read in
    // composition and animated through PADDING — a layout-phase property, so
    // every frame recomposed and re-laid-out the row. Now one shared 30 fps
    // tick, read inside the layer lambda, moving translationY only.
    val tick = rememberDecorativeTick()
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .graphicsLayer {
                        val p = decorativePingPong(decorativePhase(tick.value, 700, i * 120))
                        translationY = -2.dp.toPx() * p
                    }
                    .background(color, CircleShape),
            )
        }
    }
}

// iOS-style streaming "..." after tool title — 3 dots bouncing inline with text
@Composable
internal fun StreamingDotsText() {
    // [T-android-decorative-anim-perf] `Modifier.offset(y = …)` in its
    // non-lambda form is a layout-phase read; the dots re-laid-out the pill
    // title row every frame. Layer translation instead, from the shared tick.
    val tick = rememberDecorativeTick()
    Row {
        repeat(3) { i ->
            Text(
                text = ".",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    val p = decorativePingPong(decorativePhase(tick.value, 700, i * 120))
                    translationY = -3.dp.toPx() * p
                },
            )
        }
    }
}

// ─── Compaction progress ──────────────────────────────────────────────────────

/**
 * [T-android-compact-progress] Live status while a compaction runs, with a
 * Cancel affordance.
 *
 * Compaction used to present as a single unchanging flag for however long it
 * took — which on a slow or rate-limited model could be many minutes, and is
 * indistinguishable from a hang. This ticks once a second so the elapsed count
 * visibly moves, and names the split segment when the run had to fall back to
 * summarising halves, so "still working" is legible rather than inferred.
 */
@Composable
internal fun CompactProgressIndicator(
    progress: ChatViewModel.CompactProgress,
    onCancel: () -> Unit,
) {
    // Re-reads the clock every second; the changing value is what makes the
    // row demonstrably alive.
    var elapsedSec by remember(progress.startedAtMs) { mutableStateOf(0) }
    LaunchedEffect(progress.startedAtMs) {
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - progress.startedAtMs) / 1000L).toInt()
            kotlinx.coroutines.delay(1000)
        }
    }

    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (progress.depth > 0) {
                // Only surfaced once a split actually happened — saying
                // "segment 1" on the common single-call path would imply a
                // complexity that isn't there.
                stringResource(
                    R.string.compact_progress_split,
                    elapsedSec,
                    progress.callsIssued,
                    progress.callBudget,
                )
            } else {
                stringResource(R.string.compact_progress, elapsedSec)
            },
            fontSize = 14.sp,
            color = ChatColors.tertiaryText,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.cancel),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ─── Typing Indicator ─────────────────────────────────────────────────────────

/**
 * [T-noir-redesign] "Minis is thinking" — a breathing pearl beside the label,
 * with a soft gold light sweeping across the text.
 *
 * Perf contract is unchanged from [T-android-decorative-anim-perf]: one shared
 * 30 fps [rememberDecorativeTick], read ONLY inside draw/layer lambdas, so the
 * indicator never recomposes or re-lays-out per frame. The shimmer is drawn
 * with [BlendMode.SrcAtop] over an offscreen layer, i.e. it tints the glyphs
 * only — no per-frame TextStyle/Brush allocation in composition.
 */
@Composable
internal fun TypingIndicator() {
    val tick = rememberDecorativeTick()
    // Live Soul name → "<custom name> is thinking…" when the user renamed
    // the assistant in Soul settings (SoulStore.cachedMetadata StateFlow).
    val soulMeta by com.openminis.app.agent.SoulStore.cachedMetadata.collectAsState()
    val soulName = soulMeta.name.trim().ifEmpty { "Minis" }
    val isDark = ChatColors.isDark
    val gold = com.openminis.app.ui.theme.Noir.Iris

    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Breathing pearl: 1.6s in/out scale + halo, layer-only.
        Box(
            modifier = Modifier
                .size(16.dp)
                .drawBehind {
                    val p = decorativePingPong(decorativePhase(tick.value, 1600, 0))
                    drawCircle(
                        color = gold.copy(alpha = 0.10f + 0.16f * p),
                        radius = size.minDimension * (0.62f + 0.22f * p),
                    )
                }
                .graphicsLayer {
                    val p = decorativePingPong(decorativePhase(tick.value, 1600, 0))
                    val k = 0.9f + 0.1f * p
                    scaleX = k
                    scaleY = k
                }
                .drawBehind { drawNoirPearl(isDark) },
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.chat_typing_indicator, soulName) + "…",
            fontSize = 15.sp,
            color = ChatColors.tertiaryText,
            modifier = Modifier
                .graphicsLayer {
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // 2.2s sweep, band 40% of width, travelling past both ends.
                    val t = decorativePhase(tick.value, 2200, 0)
                    val band = size.width * 0.4f
                    val x = -band + (size.width + 2 * band) * t
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.5f to gold.copy(alpha = if (isDark) 0.95f else 0.85f),
                            1f to Color.Transparent,
                            startX = x - band / 2,
                            endX = x + band / 2,
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop,
                    )
                },
        )
    }
}
