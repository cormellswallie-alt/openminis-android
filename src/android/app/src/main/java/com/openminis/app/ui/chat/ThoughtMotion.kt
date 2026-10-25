package com.openminis.app.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.ui.components.decorativePhase
import com.openminis.app.ui.components.decorativePingPong
import com.openminis.app.ui.components.rememberDecorativeTick
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.Noir
import com.openminis.app.ui.theme.OrbState
import com.openminis.app.ui.theme.SpectraOrb

/*
 * [T-spectra-v5-thought] "Folio" — the thinking card, third pass.
 *
 * Typography: English thinking is set in Newsreader (OFL, Production Type),
 * a text serif cut for screens — pinned at opsz 16. It reads as a private
 * notebook next to the MiSans answer. Chinese stays MiSans.
 *
 * Motion vocabulary (all on the shared 30 fps decorative clock, read in draw
 * where possible):
 *  • arrive  — a new live card rises 14dp, scales 0.97→1 on a soft spring.
 *  • unfold  — steps expand from the top; collapse folds them back into the
 *              header while the card's bottom edge follows them up.
 *  • trace   — the thinking underline carries a travelling comet while live.
 *  • ink     — streamed words develop from faint to full ink; a violet caret
 *              blinks at the end of the stream.
 *  • settle  — when a card finishes, its edge breathes violet once.
 *  • pop     — tool glyphs join the header stack on a springy scale.
 */

internal val ThoughtSerif: FontFamily = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Emphasized-decelerate: fast start, long gentle landing. */
internal val ThoughtEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** True for content rendered inside a thinking card (tool rows adapt). */
internal val LocalInThoughtCard = staticCompositionLocalOf { false }

internal object ThoughtMotion {
    private val seen = object : LinkedHashMap<String, Boolean>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > 4000
    }

    /**
     * True only the first time [key] is shown in this process — entrance
     * animations must not replay when a row scrolls back into the viewport.
     */
    fun firstSight(key: String): Boolean = synchronized(seen) {
        if (seen.containsKey(key)) false else { seen[key] = true; true }
    }

    /** runId → uptime when the card finished while on screen (settle glow). */
    val settledAt = mutableStateMapOf<String, Long>()

    /** runId → uptime of the last tap that unfolded the card (stagger). */
    val unfoldedAt = HashMap<String, Long>()
}

internal fun isMostlyLatin(s: String): Boolean {
    var latin = 0
    var cjk = 0
    for (c in s) {
        when {
            c in 'a'..'z' || c in 'A'..'Z' -> latin++
            c.code in 0x3040..0x30FF || c.code in 0x3400..0x9FFF || c.code in 0xAC00..0xD7AF -> cjk++
        }
    }
    return latin >= 4 && cjk * 4 < latin
}

@Composable
internal fun thoughtBodyColor(): Color = if (ChatColors.isDark) Noir.IvorySoft else Noir.InkSoft

@Composable
internal fun thoughtSerifStyle(): TextStyle = androidx.compose.material3.LocalTextStyle.current.merge(
    fontFamily = ThoughtSerif,
    fontStyle = if (
        com.openminis.app.ui.settings.rememberAppearanceFlag(
            com.openminis.app.ui.settings.KEY_THINKING_ITALIC, false,
        ).value
    ) FontStyle.Italic else FontStyle.Normal,
    fontSize = 16.sp,
    lineHeight = 25.sp,
    letterSpacing = 0.003.em,
    fontFeatureSettings = "kern, liga, pnum",
)

@Composable
internal fun thoughtSansStyle(): TextStyle = androidx.compose.material3.LocalTextStyle.current.merge(
    fontSize = 14.5.sp,
    lineHeight = 24.sp,
)

/**
 * Light markdown for thinking: `**Heading**` lines (how reasoning summaries
 * title their sections) become small sans heads, inline `**bold**` becomes
 * medium weight, `code` goes mono. Markers are stripped.
 */
internal fun thoughtRich(
    src: String,
    heading: SpanStyle,
    strong: SpanStyle,
    code: SpanStyle,
): AnnotatedString = buildAnnotatedString {
    val lines = src.split('\n')
    lines.forEachIndexed { li, line ->
        if (li > 0) append('\n')
        val t = line.trim()
        if (t.length > 4 && t.startsWith("**") && t.endsWith("**") && t.indexOf("**", 2) == t.length - 2) {
            withStyle(heading) { append(t.substring(2, t.length - 2)) }
            return@forEachIndexed
        }
        if (t.startsWith("#") && t.trimStart('#').startsWith(" ")) {
            withStyle(heading) { append(t.trimStart('#').trim()) }
            return@forEachIndexed
        }
        var i = 0
        while (i < line.length) {
            if (line.startsWith("**", i)) {
                val e = line.indexOf("**", i + 2)
                if (e > i + 2) {
                    withStyle(strong) { append(line.substring(i + 2, e)) }
                    i = e + 2
                } else {
                    append("**"); i += 2
                }
            } else if (line[i] == '`') {
                val e = line.indexOf('`', i + 1)
                if (e > i + 1) {
                    withStyle(code) { append(line.substring(i + 1, e)) }
                    i = e + 1
                } else {
                    append('`'); i++
                }
            } else {
                var j = i + 1
                while (j < line.length && line[j] != '`' && !line.startsWith("**", j)) j++
                append(line.substring(i, j))
                i = j
            }
        }
    }
}

@Composable
internal fun rememberThoughtRich(text: String, serif: Boolean): AnnotatedString {
    val dark = ChatColors.isDark
    val headColor = MaterialTheme.colorScheme.onSurface
    val codeBg = if (dark) Noir.Onyx2 else Noir.PaperSunk
    return remember(text, serif, dark) {
        thoughtRich(
            text,
            heading = SpanStyle(
                fontFamily = com.openminis.app.ui.theme.SpectraSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                fontStyle = FontStyle.Normal,
                color = headColor,
            ),
            strong = SpanStyle(fontWeight = FontWeight.Medium, color = headColor),
            code = SpanStyle(
                fontFamily = com.openminis.app.ui.terminal.JetBrainsMonoFontFamily,
                fontSize = if (serif) 0.82.em else 0.9.em,
                fontStyle = FontStyle.Normal,
                background = codeBg,
            ),
        )
    }
}

/**
 * Card paper for one welded segment: white sheet, hairline and a soft drop
 * shadow. Open edges extend past this segment's clip so neighbouring items
 * join without a seam (the shadow too — it is cut to the same band).
 */
internal fun Modifier.thoughtPaper(
    seg: ThoughtSeg,
    fill: Color,
    edge: Color,
    shadow: Color,
): Modifier = drawBehind {
    val r = ThoughtCardRadius.toPx()
    val sw = 1.dp.toPx()
    val o = sw / 2f
    val w = size.width
    val h = size.height
    val roundTop = seg == ThoughtSeg.LONE || seg == ThoughtSeg.TOP
    val roundBottom = seg == ThoughtSeg.LONE || seg == ThoughtSeg.BOTTOM
    val ext = r * 3f
    val top = if (roundTop) 0f else -ext
    val bottom = if (roundBottom) h else h + ext
    if (shadow.alpha > 0f) {
        val pad = 28.dp.toPx()
        clipRect(-pad, if (roundTop) -pad else 0f, w + pad, if (roundBottom) h + pad else h) {
            drawIntoCanvas { c ->
                val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                p.color = fill.toArgb()
                p.setShadowLayer(16.dp.toPx(), 0f, 5.dp.toPx(), shadow.toArgb())
                c.nativeCanvas.drawRoundRect(0f, top, w, bottom, r, r, p)
            }
        }
    }
    clipRect(0f, 0f, w, h) {
        drawRoundRect(
            color = fill,
            topLeft = Offset(0f, top),
            size = Size(w, bottom - top),
            cornerRadius = CornerRadius(r, r),
        )
        val st = if (roundTop) o else -ext
        val sb = if (roundBottom) h - o else h + ext
        drawRoundRect(
            color = edge,
            topLeft = Offset(o, st),
            size = Size(w - sw, sb - st),
            cornerRadius = CornerRadius(r - o, r - o),
            style = Stroke(width = sw),
        )
    }
}

@Composable
internal fun thoughtShadow(): Color = if (ChatColors.isDark) Color.Transparent else Color(0x1A0A0A0B)

/**
 * The thinking underline. [live]: faint track + a violet comet that travels
 * left → right. Done: a still hairline that [reveal]s from the left.
 */
@Composable
internal fun ThoughtTrace(
    live: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    restAlpha: Float = 0.28f,
    reveal: Float = 1f,
) {
    val liveP by animateFloatAsState(if (live) 1f else 0f, tween(520, easing = FastOutSlowInEasing), label = "traceLive")
    val tick = if (live || liveP > 0f) rememberDecorativeTick() else null
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                val y = size.height / 2f
                val sw = 1.dp.toPx()
                val restW = size.width * reveal.coerceIn(0f, 1f)
                if (restW > 0f) {
                    drawLine(
                        color.copy(alpha = restAlpha * (1f - liveP) + 0.12f * liveP),
                        Offset(0f, y), Offset(restW, y), sw, cap = StrokeCap.Round,
                    )
                }
                if (tick != null && liveP > 0f) {
                    val ph = decorativePhase(tick.value, 1800, 0)
                    val cw = size.width * 0.34f
                    val x = -cw + (size.width + cw) * FastOutSlowInEasing.transform(ph)
                    clipRect(0f, 0f, size.width, size.height) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to color.copy(alpha = 0f),
                                0.7f to color.copy(alpha = 0.95f * liveP),
                                1f to color.copy(alpha = 0f),
                                startX = x, endX = x + cw,
                            ),
                            topLeft = Offset(x, y - 0.75.dp.toPx()),
                            size = Size(cw, 1.5.dp.toPx()),
                        )
                    }
                }
            },
    )
}

/**
 * Thinking text while it streams: words develop from faint to full ink over
 * ~420 ms, and a violet caret blinks after the last glyph. [totalLen] is the
 * raw stream length (the tail window and stripped markdown shift offsets, so
 * fresh text is located from the END of [rich]).
 */
@Composable
internal fun StreamingThoughtText(
    rich: AnnotatedString,
    totalLen: Int,
    style: TextStyle,
    color: Color,
    caret: Color,
    modifier: Modifier = Modifier,
) {
    val tick = rememberDecorativeTick()
    val now = tick.value
    val fadeNs = 420_000_000L
    // [len, appearedAtNanos] — ascending by len; marks[0] is the settled base.
    val marks = remember { ArrayList<LongArray>().apply { add(longArrayOf(totalLen.toLong(), Long.MIN_VALUE / 4)) } }
    val lastLen = marks.last()[0]
    if (totalLen.toLong() < lastLen) {
        marks.clear(); marks.add(longArrayOf(totalLen.toLong(), Long.MIN_VALUE / 4))
    } else if (totalLen.toLong() > lastLen) {
        marks.add(longArrayOf(totalLen.toLong(), now))
    }
    while (marks.size > 1 && now - marks[1][1] >= fadeNs) marks.removeAt(0)
    val shown = if (marks.size <= 1) rich else {
        val b = AnnotatedString.Builder(rich)
        val n = rich.length
        for (i in 1 until marks.size) {
            val fromEnd0 = (totalLen - marks[i - 1][0]).toInt()
            val fromEnd1 = (totalLen - marks[i][0]).toInt()
            val s = (n - fromEnd0).coerceIn(0, n)
            val e = (n - fromEnd1).coerceIn(0, n)
            if (e > s) {
                val a = ((now - marks[i][1]).toFloat() / fadeNs).coerceIn(0f, 1f)
                b.addStyle(SpanStyle(color = color.copy(alpha = color.alpha * (0.16f + 0.84f * a))), s, e)
            }
        }
        b.toAnnotatedString()
    }
    val layout = remember { arrayOfNulls<TextLayoutResult>(1) }
    Text(
        text = shown,
        style = style,
        color = color,
        onTextLayout = { layout[0] = it },
        modifier = modifier.drawWithContent {
            drawContent()
            val l = layout[0] ?: return@drawWithContent
            val len = l.layoutInput.text.length
            if (len == 0) return@drawWithContent
            val r = l.getCursorRect(len)
            val blink = decorativePingPong(decorativePhase(tick.value, 1050, 0))
            val h = r.height * 0.72f
            drawRoundRect(
                color = caret.copy(alpha = 0.35f + 0.65f * blink),
                topLeft = Offset(r.left + 2.dp.toPx(), r.top + (r.height - h) / 2f),
                size = Size(2.dp.toPx(), h),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        },
    )
}

/**
 * Header of a thinking card (shared by the card and the standalone block):
 * orb · title + timer/meta · one-line preview · tool stack · chevron.
 */
@Composable
internal fun ThoughtHeader(
    orb: OrbState,
    title: String,
    meta: String,
    liveSecs: Long?,
    preview: String,
    expanded: Boolean,
    failed: String?,
    tools: List<String>,
    toolsKey: String,
    animateTools: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = thinkingInk()
    val dark = ChatColors.isDark
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(top = 14.dp, bottom = 12.dp),
    ) {
        Box(modifier = Modifier.size(StepNodeSize), contentAlignment = Alignment.Center) {
            SpectraOrb(size = 24.dp, state = orb)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = {
                        (fadeIn(tween(240, delayMillis = 80)) + slideInVertically(tween(320, easing = ThoughtEase)) { it / 2 }) togetherWith
                            (fadeOut(tween(120)) + slideOutVertically(tween(200)) { -it / 2 })
                    },
                    label = "thoughtTitle",
                    modifier = Modifier.weight(1f, fill = false),
                ) { t ->
                    Text(
                        text = t,
                        fontSize = 15.5.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.01).em,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (liveSecs != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%d:%02d".format(liveSecs / 60, liveSecs % 60),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        color = ink,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                if (meta.isNotEmpty()) {
                    AnimatedContent(
                        targetState = meta,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(260, easing = ThoughtEase)) { it }) togetherWith
                                (fadeOut(tween(120)) + slideOutVertically(tween(180)) { -it })
                        },
                        label = "thoughtMeta",
                    ) { m ->
                        Text(
                            text = "  $m",
                            fontSize = 12.5.sp,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            color = metaColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                if (failed != null) {
                    Text(
                        text = " · $failed",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (dark) Noir.ErrOnOnyx else Noir.Err,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            AnimatedVisibility(
                visible = !expanded && preview.isNotEmpty(),
                enter = fadeIn(tween(220, delayMillis = 60)) + expandVertically(tween(260, easing = ThoughtEase)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(220, easing = ThoughtEase)),
            ) {
                AnimatedContent(
                    targetState = preview,
                    contentKey = { it.take(18) },
                    transitionSpec = {
                        (fadeIn(tween(260, delayMillis = 40)) + slideInVertically(tween(300, easing = ThoughtEase)) { it / 3 }) togetherWith
                            fadeOut(tween(140))
                    },
                    label = "thoughtPreview",
                ) { p ->
                    val serif = remember(p.take(80)) { isMostlyLatin(p.take(80)) }
                    Text(
                        text = p,
                        fontFamily = if (serif) ThoughtSerif else null,
                        fontStyle = if (serif) FontStyle.Italic else FontStyle.Normal,
                        fontSize = if (serif) 15.sp else 13.5.sp,
                        lineHeight = 20.sp,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
        if (tools.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            ThoughtToolStack(tools = tools, key = toolsKey, animate = animateTools)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (trailing != null) {
            trailing()
        } else {
            ThoughtChevron(expanded = expanded)
        }
    }
}

@Composable
internal fun ThoughtChevron(expanded: Boolean) {
    val angle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "thoughtChevron",
    )
    val dark = ChatColors.isDark
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .background(if (dark) Noir.Onyx2 else Noir.PaperSunk, CircleShape),
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp).rotate(angle),
        )
    }
}

/** Overlapping tool glyphs used in this card; new ones pop in. */
@Composable
internal fun ThoughtToolStack(tools: List<String>, key: String, animate: Boolean) {
    val shown = tools.take(4)
    val extra = tools.size - shown.size
    val dark = ChatColors.isDark
    val ring = thoughtCardFill()
    val fill = if (dark) Noir.Onyx2 else Noir.PaperSunk
    val step: Dp = 15.dp
    val bubble: Dp = 22.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(bubble + step * (shown.size - 1)).height(bubble)) {
            shown.forEachIndexed { i, name ->
                val k = "$key/tool/$name"
                val pop = remember(k) { Animatable(if (animate && ThoughtMotion.firstSight(k)) 0f else 1f) }
                LaunchedEffect(k) {
                    if (pop.value < 1f) pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = step * i)
                        .size(bubble)
                        .graphicsLayer {
                            val p = pop.value
                            scaleX = 0.4f + 0.6f * p
                            scaleY = 0.4f + 0.6f * p
                            alpha = p.coerceIn(0f, 1f)
                        }
                        .background(ring, CircleShape)
                        .padding(1.5.dp)
                        .background(fill, CircleShape),
                ) {
                    Icon(
                        imageVector = toolIconFor(name),
                        contentDescription = null,
                        tint = toolAccentColor(name),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        if (extra > 0) {
            Text(
                text = "+$extra",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/** Entrance for a newly created live card: rise + scale on a soft spring. */
internal fun Modifier.thoughtArrive(progress: () -> Float): Modifier = graphicsLayer {
    val p = progress()
    alpha = p.coerceIn(0f, 1f)
    translationY = (1f - p) * 14.dp.toPx()
    val s = 0.97f + 0.03f * p
    scaleX = s
    scaleY = s
    transformOrigin = TransformOrigin(0.5f, 0f)
}
