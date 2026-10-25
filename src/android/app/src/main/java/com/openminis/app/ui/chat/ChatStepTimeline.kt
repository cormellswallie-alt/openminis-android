package com.openminis.app.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.ui.components.DecorativeSpinner
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.Noir

/*
 * [T-noir-step-timeline] Tool calls and thinking blocks render as one
 * connected timeline instead of a stack of identical beige capsules.
 *
 *  • Every step is a row: a 26dp tinted node (the tool's own ink from the
 *    Noir step spectrum) + title + mono duration. No capsule fill — the
 *    colour lives in the node, the row itself stays quiet.
 *  • Consecutive steps are joined by a hairline rail through the nodes.
 *  • A finished run of [STEP_RUN_COLLAPSE_MIN]+ steps folds into one summary
 *    card ("6 steps · read · edit · run") which expands in place.
 *
 * The flat list keeps one LazyColumn row per step (indices used by scroll
 * anchoring and selection stay valid); folded members simply render nothing.
 */

/** Runs shorter than this stay expanded — folding two rows saves nothing. */
internal const val STEP_RUN_COLLAPSE_MIN = 3

/** Node geometry shared by rows and the rail. */
internal val StepNodeSize: Dp = 28.dp
internal val StepRowHeight: Dp = 46.dp
internal val StepTextIndent: Dp = 40.dp // node + gap: where row text starts

/** The rows between two LazyColumn items — see the item wrapper in ChatScreen. */
internal val StepItemGap: Dp = 2.dp

internal data class StepRunSummary(
    val stepCount: Int,
    val toolCount: Int,
    val thinkingCount: Int,
    val failedCount: Int,
    val totalMs: Long,
    /** Up to four tool names in order, for the node stack. */
    val nodeTools: List<String>,
    /** Distinct tool titles in order, for the subtitle. */
    val titles: List<String>,
    /** [T-spectra-v3] Every step in order: tool name (null = thinking) and its duration. */
    val segments: List<Pair<String?, Long>> = emptyList(),
    /** [T-spectra-v4-thought] The run is still being produced. */
    val live: Boolean = false,
    /** The newest step is a thinking block that is still streaming. */
    val liveThinking: Boolean = false,
    /** One line for the folded card: newest thought / running tool / titles. */
    val preview: String = "",
)

internal data class StepRunInfo(
    val runId: String,
    val isHead: Boolean,
    val railUp: Boolean,
    val railDown: Boolean,
    val collapsible: Boolean,
    /** Present on the head only. */
    val summary: StepRunSummary?,
    /** [T-spectra-v4-thought] Position inside the run. */
    val index: Int = 0,
    val size: Int = 1,
    /** Rendered as one thinking card (Appearance → fold tool calls). */
    val card: Boolean = false,
    /** Expansion when the user has not touched this run. */
    val defaultExpanded: Boolean = false,
)

/** [T-spectra-v4-thought] A non-head row welded into a thinking card. */
internal val StepRunInfo?.isCardMember: Boolean
    get() = this != null && card && !isHead

private fun FlatChatItem.stepBlock(): AssistantBlock? = when (this) {
    is FlatChatItem.AssistantThinking -> block
    is FlatChatItem.AssistantToolUse -> block
    else -> null
}

private fun AssistantBlock.isLiveTool(): Boolean =
    toolStatus == ToolBlockStatus.RUNNING ||
        toolStatus == ToolBlockStatus.STREAMING ||
        toolStatus == ToolBlockStatus.PENDING

private fun AssistantBlock.isFailedTool(): Boolean =
    toolStatus == ToolBlockStatus.FAILED || toolStatus == ToolBlockStatus.TIMEOUT

/**
 * Groups consecutive step rows of [items] (chronological order) into runs.
 *
 * @param isStep which items render as a timeline step (thinking blocks that are
 *   visible at the current level, tool calls that render as a pill — sub agent
 *   delegation cards are excluded because they are full cards of their own).
 * @param streaming whether the conversation is streaming right now: the
 *   trailing run of a live turn never folds, so progress stays visible.
 */
internal fun computeStepRuns(
    items: List<FlatChatItem>,
    streaming: Boolean,
    // [T-spectra-v4-thought] Every run (any length, live or not) becomes one
    // thinking card; text between runs (an interim summary) closes the card.
    cardMode: Boolean = false,
    // Whether a live card opens by itself (Appearance → expand thinking).
    autoExpandLive: Boolean = true,
    isStep: (FlatChatItem) -> Boolean,
): Map<String, StepRunInfo> {
    val out = HashMap<String, StepRunInfo>()
    // Index of the last "real content" row, computed once: keeps this O(n)
    // even though it reruns whenever the streaming overlay rebuilds the list.
    val lastContentIdx = items.indexOfLast {
        it !is FlatChatItem.AssistantTyping && it !is FlatChatItem.AssistantInfo
    }
    var i = 0
    while (i < items.size) {
        if (!isStep(items[i])) { i++; continue }
        var j = i
        while (j + 1 < items.size && isStep(items[j + 1])) j++
        val run = items.subList(i, j + 1)
        val blocks = run.mapNotNull { it.stepBlock() }
        val anyLive = run.any { item ->
            when (item) {
                is FlatChatItem.AssistantThinking -> item.isLastBlockOverall && item.messageIsStreaming
                is FlatChatItem.AssistantToolUse -> item.block.isLiveTool()
                else -> false
            }
        }
        // Content after the run (text, next turn) means the run is history.
        val hasContentAfter = lastContentIdx > j
        val runLive = anyLive || (streaming && !hasContentAfter)
        val collapsible = if (cardMode) true else run.size >= STEP_RUN_COLLAPSE_MIN && !anyLive &&
            (!streaming || hasContentAfter)
        val lastItem = run.last()
        val liveThinking = lastItem is FlatChatItem.AssistantThinking &&
            lastItem.isLastBlockOverall && lastItem.messageIsStreaming
        val preview = if (!cardMode) "" else run.asReversed().firstNotNullOfOrNull { item ->
            when (item) {
                is FlatChatItem.AssistantThinking ->
                    thinkingPreviewLine(item.block.content).takeIf { it.isNotEmpty() }
                is FlatChatItem.AssistantToolUse ->
                    if (runLive || run.none { it is FlatChatItem.AssistantThinking })
                        item.block.toolTitle.ifEmpty { item.block.toolName }
                    else null
                else -> null
            }
        }.orEmpty()
        val tools = run.filterIsInstance<FlatChatItem.AssistantToolUse>()
        val summary = StepRunSummary(
            stepCount = run.size,
            toolCount = tools.size,
            thinkingCount = run.size - tools.size,
            failedCount = tools.count { it.block.isFailedTool() },
            totalMs = blocks.sumOf { it.durationMs.coerceAtLeast(0L) },
            nodeTools = tools.map { it.block.toolName }.take(4),
            titles = tools.map { it.block.toolTitle.ifEmpty { it.block.toolName } }
                .distinct().take(6),
            segments = run.mapNotNull { item ->
                when (item) {
                    is FlatChatItem.AssistantToolUse -> item.block.toolName to item.block.durationMs
                    is FlatChatItem.AssistantThinking -> null to item.block.durationMs
                    else -> null
                }
            },
            live = runLive,
            liveThinking = liveThinking,
            preview = preview,
        )
        val runId = run.first().key
        run.forEachIndexed { idx, item ->
            out[item.key] = StepRunInfo(
                runId = runId,
                isHead = idx == 0,
                railUp = idx > 0,
                railDown = idx < run.size - 1,
                collapsible = collapsible,
                summary = if (idx == 0) summary else null,
                index = idx,
                size = run.size,
                card = cardMode,
                defaultExpanded = cardMode && runLive && autoExpandLive,
            )
        }
        i = j + 1
    }
    return out
}

/** True when the item is a folded (invisible) member of a collapsed run. */
internal fun StepRunInfo?.isFoldedAway(expanded: SnapshotStateMap<String, Boolean>): Boolean =
    this != null && collapsible && !isHead && !(expanded[runId] ?: defaultExpanded)

/**
 * Renders [content] inside its run: the head of a foldable run carries the
 * summary card; folded members render nothing.
 */
@Composable
internal fun StepRunSlot(
    info: StepRunInfo?,
    expandedRuns: SnapshotStateMap<String, Boolean>,
    content: @Composable (railUp: Boolean, railDown: Boolean) -> Unit,
) {
    if (info == null) { content(false, false); return }
    if (info.card) { ThoughtCardSlot(info, expandedRuns, content); return }
    val expanded = !info.collapsible || (expandedRuns[info.runId] ?: info.defaultExpanded)
    if (!expanded && !info.isHead) return
    if (info.isHead && info.collapsible && info.summary != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StepRunSummaryCard(
                summary = info.summary,
                expanded = expanded,
                onToggle = { expandedRuns[info.runId] = !expanded },
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                content(false, info.railDown)
            }
        }
    } else {
        content(info.railUp, info.railDown)
    }
}

/**
 * Hairline rail through the node centre. Segments stop 3dp short of the node
 * so the circle floats on the line; the up-segment reaches [StepItemGap] above
 * the row so neighbouring rows join seamlessly across the list spacing.
 */
internal fun Modifier.stepRail(
    railUp: Boolean,
    railDown: Boolean,
    color: Color,
    nodeCenterY: Dp = StepRowHeight / 2,
): Modifier = if (!railUp && !railDown) this else drawBehind {
    val x = (StepNodeSize / 2).toPx()
    val cy = nodeCenterY.toPx()
    val gapAroundNode = (StepNodeSize / 2 + 4.dp).toPx()
    val stroke = 1.dp.toPx()
    if (railUp) {
        drawLine(color, Offset(x, -StepItemGap.toPx()), Offset(x, cy - gapAroundNode), stroke)
    }
    if (railDown) {
        drawLine(color, Offset(x, cy + gapAroundNode), Offset(x, size.height), stroke)
    }
}

@Composable
internal fun stepRailColor(): Color =
    if (ChatColors.isDark) Noir.OnyxHairline else Noir.PaperHairline

/** Thinking ink: gold that holds contrast on both grounds. */
@Composable
internal fun thinkingInk(): Color = if (ChatColors.isDark) Noir.ThinkOnOnyx else Noir.ThinkOnPaper

/**
 * The tinted circle every step hangs from. While [running], a thin arc in the
 * accent spins around it (shared decorative clock — cheap).
 */
@Composable
internal fun StepNode(
    icon: ImageVector,
    accent: Color,
    running: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = StepNodeSize,
) {
    // [T-spectra-v3] Neutral squircle; the tool hue lives only in the glyph.
    // No glow, no coloured disc — colour marks type, motion marks state.
    val dark = ChatColors.isDark
    val shape = RoundedCornerShape(size * 0.34f)
    val fill = if (dark) Noir.Onyx2 else Noir.PaperSunk
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(fill, shape),
        )
        if (running) {
            DecorativeSpinner(
                color = if (dark) Noir.Ivory else Noir.Ink,
                modifier = Modifier.size(size + 6.dp),
                strokeWidth = 1.5.dp,
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size * 0.54f),
        )
    }
}

/** Fingerprint bar: one segment per step, width ∝ duration, colour = tool type. */
@Composable
internal fun StepFingerprint(
    segments: List<Pair<String?, Long>>,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
) {
    if (segments.isEmpty()) return
    val think = thinkingInk()
    Row(modifier = modifier.height(height), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.forEach { (tool, ms) ->
            // Floor so instant steps still show; cap so one long build does not erase the rest.
            val w = (ms.coerceAtLeast(0L) / 1000f).coerceIn(0.8f, 12f)
            Box(
                modifier = Modifier
                    .weight(w)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(if (tool == null) think else toolAccentColor(tool)),
            )
        }
    }
}

internal fun formatStepRunDuration(ms: Long): String? {
    if (ms <= 0L) return null
    val s = ms / 1000.0
    return when {
        s < 10 -> String.format("%.1fs", s)
        s < 60 -> String.format("%.0fs", s)
        else -> "${(ms / 60_000)}m${((ms / 1000) % 60).toString().padStart(2, '0')}s"
    }
}

/** [T-spectra-v3] Folded run: check · "N steps · time" · fingerprint bar · titles. */
@Composable
internal fun StepRunSummaryCard(
    summary: StepRunSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val dark = ChatColors.isDark
    val shape = RoundedCornerShape(18.dp)
    val cardBg = if (dark) Noir.Onyx1 else Noir.PaperCard
    val hairline = if (dark) Noir.OnyxHairline else Noir.PaperHairline
    val okColor = if (summary.failedCount > 0) (if (dark) Noir.ErrOnOnyx else Noir.Err)
        else (if (dark) Noir.OkOnOnyx else Noir.Ok)
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(240),
        label = "stepRunChevron",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(if (dark) 0.dp else 6.dp, shape, ambientColor = Color(0x140A0A0B), spotColor = Color(0x1F0A0A0B))
            .clip(shape)
            .background(cardBg, shape)
            .border(0.5.dp, hairline, shape)
            .clickable(onClick = onToggle)
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(okColor.copy(alpha = if (dark) 0.18f else 0.12f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = okColor, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.noir_steps_done, summary.stepCount),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            formatStepRunDuration(summary.totalMs)?.let {
                Text(
                    text = "  " + it,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (summary.failedCount > 0) {
                Text(
                    text = "  ·  " + stringResource(R.string.noir_steps_failed, summary.failedCount),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = okColor,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(chevron),
            )
        }
        StepFingerprint(
            segments = summary.segments.ifEmpty { summary.nodeTools.map { it to 1000L } },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        if (summary.titles.isNotEmpty()) {
            Text(
                text = summary.titles.joinToString("  ·  "),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** Opaque blend of [top] over [base] at [amount]. */
internal fun lerpOver(base: Color, top: Color, amount: Float): Color = Color(
    red = base.red + (top.red - base.red) * amount,
    green = base.green + (top.green - base.green) * amount,
    blue = base.blue + (top.blue - base.blue) * amount,
    alpha = 1f,
)

/**
 * One-line live preview of a thinking block: the last non-blank line, with
 * markdown emphasis stripped. Shown beside the collapsed "Thinking" label.
 */
internal fun thinkingPreviewLine(content: String): String {
    if (content.isBlank()) return ""
    val tail = if (content.length > 600) content.substring(content.length - 600) else content
    val line = tail.lineSequence().map { it.trim() }.lastOrNull { it.isNotEmpty() } ?: return ""
    return line.trimStart('#', '>', '-', '*', ' ').replace("**", "").replace("`", "").take(160)
}


// ─── [T-spectra-v4-thought] Thinking card ──────────────────────────────────
//
// One run of steps = one card. The card is drawn as welded segments (one per
// LazyColumn row: TOP / MIDDLE / BOTTOM, or LONE when folded) so list indices,
// scroll anchoring and per-row recomposition stay exactly as before.

internal enum class ThoughtSeg { LONE, TOP, MIDDLE, BOTTOM }

@Composable
internal fun thoughtCardFill(): Color = if (ChatColors.isDark) Noir.Onyx1 else Color(0xFFF8F8F9)

@Composable
internal fun thoughtCardEdge(): Color = if (ChatColors.isDark) Noir.OnyxHairline else Noir.PaperHairline

internal val ThoughtCardRadius: Dp = 20.dp

/** Fill + hairline of one segment; open edges extend past the clip. */
internal fun Modifier.thoughtSurface(seg: ThoughtSeg, fill: Color, edge: Color): Modifier = drawBehind {
    val r = ThoughtCardRadius.toPx()
    val sw = 1.dp.toPx()
    val o = sw / 2f
    val w = size.width
    val h = size.height
    val roundTop = seg == ThoughtSeg.LONE || seg == ThoughtSeg.TOP
    val roundBottom = seg == ThoughtSeg.LONE || seg == ThoughtSeg.BOTTOM
    val ext = r * 2f
    clipRect(0f, 0f, w, h) {
        val top = if (roundTop) 0f else -ext
        val bottom = if (roundBottom) h else h + ext
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
private fun ThoughtCardSlot(
    info: StepRunInfo,
    expandedRuns: SnapshotStateMap<String, Boolean>,
    content: @Composable (railUp: Boolean, railDown: Boolean) -> Unit,
) {
    val expanded = expandedRuns[info.runId] ?: info.defaultExpanded
    if (!expanded && !info.isHead) return
    val last = info.index == info.size - 1
    val seg = when {
        info.isHead && (!expanded || info.size == 1) -> ThoughtSeg.LONE
        info.isHead -> ThoughtSeg.TOP
        last -> ThoughtSeg.BOTTOM
        else -> ThoughtSeg.MIDDLE
    }
    val closes = seg == ThoughtSeg.LONE || seg == ThoughtSeg.BOTTOM
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (info.isHead) 6.dp else 0.dp, bottom = if (closes) 6.dp else 0.dp)
            .thoughtSurface(seg, thoughtCardFill(), thoughtCardEdge())
            .padding(start = 12.dp, end = 10.dp, bottom = if (closes && expanded) 6.dp else 0.dp),
    ) {
        if (info.isHead && info.summary != null) {
            ThoughtCardHeader(
                runId = info.runId,
                summary = info.summary,
                expanded = expanded,
                onToggle = { expandedRuns[info.runId] = !expanded },
            )
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                        .height(1.dp)
                        .background(thoughtCardEdge()),
                )
            }
        }
        if (expanded) content(info.index > 0, !last)
    }
}

@Composable
private fun ThoughtCardHeader(
    runId: String,
    summary: StepRunSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val dark = ChatColors.isDark
    val meta = MaterialTheme.colorScheme.onSurfaceVariant
    val live = summary.live
    val startedAt = androidx.compose.runtime.remember(runId) { android.os.SystemClock.elapsedRealtime() }
    var liveSecs by androidx.compose.runtime.remember(runId) { androidx.compose.runtime.mutableLongStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(runId, live) {
        while (live) {
            liveSecs = (android.os.SystemClock.elapsedRealtime() - startedAt) / 1000
            kotlinx.coroutines.delay(1000)
        }
    }
    val hasThinking = summary.thinkingCount > 0
    val title = when {
        live && hasThinking -> stringResource(R.string.spectra_thinking_live)
        live -> summary.preview.ifEmpty { stringResource(R.string.spectra_thinking_live) }
        hasThinking -> stringResource(R.string.spectra_thinking_done)
        else -> stringResource(R.string.noir_steps_done, summary.stepCount)
    }
    val metaParts = buildList {
        val t = if (live) formatStepRunDuration(liveSecs * 1000) else formatStepRunDuration(summary.totalMs)
        t?.let { add(it) }
        if (hasThinking && summary.stepCount > 1) add(stringResource(R.string.spectra_thought_steps, summary.stepCount))
        if (summary.toolCount > 0 && hasThinking) add(stringResource(R.string.spectra_thought_tools, summary.toolCount))
    }
    val err = if (dark) Noir.ErrOnOnyx else Noir.Err
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(240),
        label = "thoughtCardChevron",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(StepNodeSize), contentAlignment = Alignment.Center) {
                com.openminis.app.ui.theme.SpectraOrb(
                    size = 22.dp,
                    state = when {
                        live && summary.liveThinking -> com.openminis.app.ui.theme.OrbState.Think
                        live -> com.openminis.app.ui.theme.OrbState.Live
                        else -> com.openminis.app.ui.theme.OrbState.Done
                    },
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = "  " + metaParts.joinToString(" · "),
                            fontSize = 12.5.sp,
                            color = meta,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    if (summary.failedCount > 0) {
                        Text(
                            text = " · " + stringResource(R.string.noir_steps_failed, summary.failedCount),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = err,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                val line = if (live && !hasThinking) "" else summary.preview
                if (!expanded && line.isNotEmpty()) {
                    Text(
                        text = line,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = meta,
                modifier = Modifier.size(20.dp).rotate(chevron),
            )
        }
        if (!expanded && !live && summary.stepCount >= 2) {
            StepFingerprint(
                segments = summary.segments.ifEmpty { summary.nodeTools.map { it to 1000L } },
                height = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = StepTextIndent, end = 26.dp, top = 10.dp),
            )
        }
    }
}
