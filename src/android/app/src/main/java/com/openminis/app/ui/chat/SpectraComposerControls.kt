package com.openminis.app.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.data.model.ThinkingLevel
import com.openminis.app.ui.components.decorativePhase
import com.openminis.app.ui.components.decorativePingPong
import com.openminis.app.ui.components.rememberDecorativeTick
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.Noir
import com.openminis.app.ui.theme.OrbState
import com.openminis.app.ui.theme.SpectraOrb
import kotlin.math.roundToInt

/*
 * [T-spectra-v3-composer] Composer controls — what lives INSIDE the input card
 * (the card itself is untouched):
 *
 *  - SpectraModelChip: the model switch that used to sit under the chat title.
 *    Same picker, same tap target semantics, same fallback red pulse — it just
 *    moved next to the place you type, so the header can be the chat's name.
 *  - SpectraDepthChip: current thinking depth in English ("High") with a tiny
 *    rising meter. Tap opens the depth tray; long-press opens the classic
 *    list sheet (kept so nothing is lost).
 *  - SpectraDepthSlider: a horizontal detent slider. Every step up changes the
 *    rail colour (Off grey → Low teal → Medium blue → High violet → XHigh
 *    magenta → Max ember → Ultra crimson). The top detent of whatever the
 *    model supports is the "peak": the fill turns into the flowing Spectra
 *    spectrum, the thumb becomes a live orb, a ring bursts once on arrival and
 *    sparks drift off the rail. Animation reads the shared decorative tick in
 *    draw only, so the tray never recomposes per frame.
 */

/** Level names are designed in English on purpose (product decision). */
internal val ThinkingLevel.depthLabel: String get() = displayName

internal fun depthColor(level: ThinkingLevel, dark: Boolean): Color = when (level) {
    ThinkingLevel.OFF -> if (dark) Color(0xFF6B6D74) else Color(0xFF9A9CA3)
    ThinkingLevel.LOW -> if (dark) Color(0xFF5FD6BC) else Color(0xFF13A388)
    ThinkingLevel.MEDIUM -> if (dark) Color(0xFF8DB2FF) else Color(0xFF2F74F0)
    ThinkingLevel.HIGH -> if (dark) Color(0xFFB7A6FF) else Color(0xFF6E56F0)
    ThinkingLevel.XHIGH -> if (dark) Color(0xFFF09AD9) else Color(0xFFB8429F)
    ThinkingLevel.MAX -> if (dark) Color(0xFFFF9A86) else Color(0xFFE0533A)
    ThinkingLevel.ULTRA -> if (dark) Color(0xFFFF7A9A) else Color(0xFFE11D48)
}

/** Deepened orb spectrum so the peak fill holds contrast on white. */
private val PeakSpectrumLight = listOf(
    Color(0xFF19B394), Color(0xFF3F7CF2), Color(0xFF7A5CF2),
    Color(0xFFD552B6), Color(0xFF2FA6E3), Color(0xFF19B394),
)
private val PeakSpectrumDark = Noir.Spectrum

private val noPad = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

@Composable
private fun chipBg(): Color = if (ChatColors.isDark) Noir.Onyx2 else Noir.PaperSunk

// ─── Model chip ────────────────────────────────────────────────────────────

@Composable
internal fun SpectraModelChip(
    label: String,
    groupBound: Boolean,
    resolved: Boolean,
    fastOn: Boolean,
    pulse: Float,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = ChatColors.isDark
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(chipBg())
            .background(Color.Red.copy(alpha = 0.32f * pulse))
            .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
            .padding(start = 11.dp, end = 7.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(
                    if (resolved) (if (dark) Noir.OkOnOnyx else Noir.Ok) else Color(0xFFEE8A2A),
                    CircleShape,
                ),
        )
        if (groupBound) {
            Icon(
                Icons.Default.Layers,
                contentDescription = null,
                tint = ChatColors.tertiaryText,
                modifier = Modifier.size(12.dp),
            )
        }
        if (fastOn) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(13.dp).background(Color(0xFFEE8A2A), CircleShape),
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
        Text(
            text = label,
            fontSize = 13.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = ChatColors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = noPad,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = ChatColors.tertiaryText,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ─── Depth chip ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SpectraDepthChip(
    level: ThinkingLevel,
    stops: List<ThinkingLevel>,
    open: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = ChatColors.isDark
    val tint by animateColorAsState(depthColor(level, dark), tween(260), label = "depthChip")
    val idx = stops.indexOf(level).coerceAtLeast(0)
    val ratio = if (stops.size > 1) idx / (stops.size - 1f) else 0f
    val peak = idx == stops.lastIndex && stops.size > 1 && level.isEnabled
    val idle = if (dark) Noir.Onyx3 else Noir.PaperHairlineStrong
    val haptics = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(chipBg())
            .then(
                if (open) Modifier.border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(50)) else Modifier,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(start = 10.dp, end = 11.dp),
    ) {
        // Rising four-bar meter; filled bars carry the level colour (the
        // peak paints them with the spectrum instead).
        Canvas(Modifier.size(width = 13.dp, height = 12.dp)) {
            val bars = 4
            val gap = 1.5.dp.toPx()
            val bw = (size.width - gap * (bars - 1)) / bars
            val lit = if (!level.isEnabled) 0 else (ratio * bars).roundToInt().coerceIn(1, bars)
            for (b in 0 until bars) {
                val h = size.height * (0.35f + 0.65f * (b + 1) / bars)
                val c = when {
                    b >= lit -> idle
                    peak -> (if (dark) PeakSpectrumDark else PeakSpectrumLight)[b % 5]
                    else -> tint
                }
                drawRoundRect(
                    color = c,
                    topLeft = Offset(b * (bw + gap), size.height - h),
                    size = Size(bw, h),
                    cornerRadius = CornerRadius(bw / 2f),
                )
            }
        }
        Text(
            text = level.depthLabel,
            fontSize = 13.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (level.isEnabled) tint else ChatColors.secondaryText,
            maxLines = 1,
            style = noPad,
        )
    }
}

// ─── Depth tray (inline, inside the composer card) ─────────────────────────

@Composable
internal fun SpectraDepthTray(
    current: ThinkingLevel,
    available: List<ThinkingLevel>,
    onSelect: (ThinkingLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stops = remember(available) {
        listOf(ThinkingLevel.OFF) + available.filter { it != ThinkingLevel.OFF }
    }
    val dark = ChatColors.isDark
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (dark) Noir.Onyx2 else Noir.PaperSunk)
            .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 10.dp),
    ) {
        SpectraDepthSlider(stops = stops, current = current, onSelect = onSelect)
    }
}

@Composable
internal fun SpectraDepthSlider(
    stops: List<ThinkingLevel>,
    current: ThinkingLevel,
    onSelect: (ThinkingLevel) -> Unit,
) {
    val dark = ChatColors.isDark
    val n = stops.size
    val selIdx = (if (current.isEnabled) stops.indexOf(current) else 0).coerceAtLeast(0)
    var dragFrac by remember { mutableStateOf<Float?>(null) }
    val settled by animateFloatAsState(
        targetValue = if (n > 1) selIdx / (n - 1f) else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "depthThumb",
    )
    val frac = dragFrac ?: settled
    val liveIdx = dragFrac?.let { (it * (n - 1)).roundToInt().coerceIn(0, n - 1) } ?: selIdx
    val liveLevel = stops[liveIdx]
    val color by animateColorAsState(depthColor(liveLevel, dark), tween(280), label = "depthRail")
    val isPeak = n > 1 && liveIdx == n - 1 && liveLevel.isEnabled
    val haptics = LocalHapticFeedback.current
    val latestSelect by rememberUpdatedState(onSelect)

    // Detent haptic while dragging across stops.
    LaunchedEffect(liveIdx) {
        if (dragFrac != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    // One-shot ring burst every time the peak is reached.
    val burst = remember { Animatable(1f) }
    LaunchedEffect(isPeak) {
        if (isPeak) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
        }
    }
    val tick: State<Long>? = if (isPeak) rememberDecorativeTick() else null

    // Header: label + live level name.
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Thinking depth",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = ChatColors.secondaryText,
            style = noPad,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = liveLevel.depthLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (liveLevel.isEnabled) color else ChatColors.secondaryText,
            style = noPad,
        )
        if (isPeak) {
            Spacer6()
            Text(
                text = "Peak",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (dark) Noir.Onyx else Color.White,
                style = noPad,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }

    val thumb = 24.dp
    val railH = 6.dp
    val density = LocalDensity.current
    val trackCol = if (dark) Noir.Onyx3 else Noir.PaperSunk2
    val dotIdle = if (dark) Noir.IvoryFaint else Noir.InkFaint
    val dotLit = if (dark) Noir.Onyx.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.9f)
    val spectrum = if (dark) PeakSpectrumDark else PeakSpectrumLight

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(40.dp)
            .pointerInput(n) {
                val pad = (thumb / 2).toPx()
                fun fracAt(x: Float): Float {
                    val span = (size.width - pad * 2).coerceAtLeast(1f)
                    return ((x - pad) / span).coerceIn(0f, 1f)
                }
                detectTapGestures { o ->
                    val idx = (fracAt(o.x) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latestSelect(stops[idx])
                }
            }
            .pointerInput(n) {
                val pad = (thumb / 2).toPx()
                fun fracAt(x: Float): Float {
                    val span = (size.width - pad * 2).coerceAtLeast(1f)
                    return ((x - pad) / span).coerceIn(0f, 1f)
                }
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragFrac = fracAt(o.x) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFrac = fracAt(change.position.x)
                    },
                    onDragEnd = {
                        val f = dragFrac
                        if (f != null) {
                            val idx = (f * (n - 1)).roundToInt().coerceIn(0, n - 1)
                            latestSelect(stops[idx])
                        }
                        dragFrac = null
                    },
                    onDragCancel = { dragFrac = null },
                )
            }
            .drawBehind {
                val pad = (thumb / 2).toPx()
                val cy = size.height / 2f
                val th = railH.toPx()
                val x0 = pad
                val x1 = size.width - pad
                val tx = x0 + frac * (x1 - x0)
                val r = CornerRadius(th / 2f)
                drawRoundRect(trackCol, Offset(x0 - th / 2f, cy - th / 2f), Size(x1 - x0 + th, th), r)
                val t = tick?.value ?: 0L
                val fillBrush = if (isPeak) {
                    val band = 150.dp.toPx()
                    val shift = band * 2f * decorativePhase(t, 2600, 0)
                    Brush.linearGradient(
                        colors = spectrum,
                        start = Offset(x0 - shift, 0f),
                        end = Offset(x0 - shift + band, 0f),
                        tileMode = TileMode.Mirror,
                    )
                } else {
                    SolidColor(color)
                }
                if (frac > 0.001f) {
                    drawRoundRect(fillBrush, Offset(x0 - th / 2f, cy - th / 2f), Size(tx - x0 + th, th), r)
                }
                // Detent dots.
                if (n > 1) {
                    for (i in 0 until n) {
                        val x = x0 + i / (n - 1f) * (x1 - x0)
                        if (kotlin.math.abs(x - tx) < pad * 0.9f) continue
                        drawCircle(if (x <= tx) dotLit else dotIdle, radius = 1.7.dp.toPx(), center = Offset(x, cy))
                    }
                }
                if (isPeak) {
                    // Sparks drifting up off the lit rail.
                    for (k in 0 until 6) {
                        val p = decorativePhase(t, 1700, k * 290)
                        val along = ((k * 0.173f + 0.11f) % 1f)
                        val sx = x0 + (tx - x0) * along
                        val sy = cy - th / 2f - p * 15.dp.toPx()
                        drawCircle(
                            color = spectrum[k % 5].copy(alpha = (1f - p) * 0.85f),
                            radius = (1.9f - p).dp.toPx(),
                            center = Offset(sx + (decorativePingPong(p) - 0.5f) * 4.dp.toPx(), sy),
                        )
                    }
                    // Soft breathing halo behind the orb thumb.
                    val g = decorativePingPong(decorativePhase(t, 2200, 0))
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to spectrum[2].copy(alpha = 0.30f + 0.2f * g),
                            1f to Color.Transparent,
                            center = Offset(tx, cy),
                            radius = (thumb.toPx() * (0.95f + 0.2f * g)),
                        ),
                        radius = thumb.toPx() * (0.95f + 0.2f * g),
                        center = Offset(tx, cy),
                    )
                }
                val b = burst.value
                if (b < 1f) {
                    drawCircle(
                        color = spectrum[1].copy(alpha = (1f - b) * 0.6f),
                        radius = (thumb / 2).toPx() * (1f + 1.9f * b),
                        center = Offset(tx, cy),
                        style = Stroke(width = (2.2f * (1f - b) + 0.6f).dp.toPx()),
                    )
                }
            },
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val padPx = with(density) { (thumb / 2).toPx() }
        val thumbX = padPx + frac * (wPx - padPx * 2)
        val thumbMod = Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset((thumbX - padPx).roundToInt(), 0) }
            .size(thumb)
        if (isPeak) {
            SpectraOrb(size = thumb, state = OrbState.Live, modifier = thumbMod)
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = thumbMod
                    .shadow(if (dark) 0.dp else 3.dp, CircleShape)
                    .background(if (dark) Noir.Ivory else Color.White, CircleShape)
                    .border(2.5.dp, color, CircleShape),
            ) {
                Box(Modifier.size(6.dp).background(color, CircleShape))
            }
        }
    }

    // Stop labels, centred under their detents (clamped at the edges).
    val liveIdxState by rememberUpdatedState(liveIdx)
    Layout(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        content = {
            stops.forEachIndexed { i, lv ->
                val on = i == liveIdxState
                Text(
                    text = lv.depthLabel,
                    fontSize = 12.5.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on && lv.isEnabled) color else if (on) ChatColors.primaryText else ChatColors.tertiaryText,
                    maxLines = 1,
                    softWrap = false,
                    style = noPad,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { latestSelect(lv) },
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, maxWidth = Int.MAX_VALUE.coerceAtMost(constraints.maxWidth))) }
        val w = constraints.maxWidth
        val pad = (thumb / 2).roundToPx()
        val h = placeables.maxOfOrNull { it.height } ?: 0
        layout(w, h) {
            placeables.forEachIndexed { i, p ->
                val cx = if (n > 1) pad + i * (w - pad * 2) / (n - 1) else w / 2
                val x = (cx - p.width / 2).coerceIn(0, (w - p.width).coerceAtLeast(0))
                p.placeRelative(x, 0)
            }
        }
    }
}

@Composable
private fun Spacer6() {
    Box(Modifier.width(6.dp))
}
