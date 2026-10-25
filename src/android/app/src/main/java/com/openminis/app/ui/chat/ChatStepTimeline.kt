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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
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
internal val StepNodeSize: Dp = 26.dp
internal val StepRowHeight: Dp = 38.dp
internal val StepTextIndent: Dp = 36.dp // node + gap: where row text starts

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
)

internal data class StepRunInfo(
    val runId: String,
    val isHead: Boolean,
    val railUp: Boolean,
    val railDown: Boolean,
    val collapsible: Boolean,
    /** Present on the head only. */
    val summary: StepRunSummary?,
)

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
        val collapsible = run.size >= STEP_RUN_COLLAPSE_MIN && !anyLive &&
            (!streaming || hasContentAfter)
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
            )
        }
        i = j + 1
    }
    return out
}

/** True when the item is a folded (invisible) member of a collapsed run. */
internal fun StepRunInfo?.isFoldedAway(expanded: SnapshotStateMap<String, Boolean>): Boolean =
    this != null && collapsible && !isHead && expanded[runId] != true

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
    val expanded = !info.collapsible || expandedRuns[info.runId] == true
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
    val gapAroundNode = (StepNodeSize / 2 + 3.dp).toPx()
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
    if (ChatColors.isDark) Noir.OnyxHairlineStrong else Noir.PaperHairlineStrong

/** Thinking ink: gold that holds contrast on both grounds. */
@Composable
internal fun thinkingInk(): Color = if (ChatColors.isDark) Noir.GoldOnOnyx else Noir.GoldOnPaper

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
    val fillAlpha = if (ChatColors.isDark) 0.20f else 0.13f
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .background(accent.copy(alpha = fillAlpha), CircleShape),
        )
        if (running) {
            DecorativeSpinner(
                color = accent,
                modifier = Modifier.size(size),
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

internal fun formatStepRunDuration(ms: Long): String? {
    if (ms <= 0L) return null
    val s = ms / 1000.0
    return when {
        s < 10 -> String.format("%.1fs", s)
        s < 60 -> String.format("%.0fs", s)
        else -> "${(ms / 60_000)}m${((ms / 1000) % 60).toString().padStart(2, '0')}s"
    }
}

/** Folded run: overlapping coloured nodes, count, titles, total time. */
@Composable
internal fun StepRunSummaryCard(
    summary: StepRunSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val cardBg = if (ChatColors.isDark) Noir.Onyx1 else Noir.PaperCard
    val hairline = if (ChatColors.isDark) Noir.OnyxHairline else Noir.PaperHairline
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(240),
        label = "stepRunChevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(shape)
            .background(cardBg, shape)
            .border(0.5.dp, hairline, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Node stack — each node ringed in the card colour so overlaps read clean.
        val nodes = buildList<Pair<ImageVector, Color>> {
            summary.nodeTools.forEach { add(toolIconFor(it) to toolAccentColor(it)) }
            if (summary.thinkingCount > 0 && size < 4) add(Icons.Default.Psychology to thinkingInk())
            if (isEmpty()) add(Icons.Default.Check to Noir.Stone)
        }
        Box(modifier = Modifier.width(22.dp + 14.dp * (nodes.size - 1)).height(26.dp)) {
            nodes.forEachIndexed { idx, (icon, accent) ->
                Box(
                    modifier = Modifier
                        .offset(x = 14.dp * idx, y = 2.dp)
                        .size(22.dp)
                        .background(cardBg, CircleShape)
                        .padding(1.5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .background(
                                // Opaque mix so the ring underneath never shows through.
                                lerpOver(cardBg, accent, if (ChatColors.isDark) 0.28f else 0.18f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.noir_steps_done, summary.stepCount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (summary.failedCount > 0) {
                    Text(
                        text = "  ·  " + stringResource(R.string.noir_steps_failed, summary.failedCount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Noir.Ember,
                        maxLines = 1,
                    )
                }
            }
            if (summary.titles.isNotEmpty()) {
                Text(
                    text = summary.titles.joinToString("  ·  "),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        formatStepRunDuration(summary.totalMs)?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = it,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp).rotate(chevron),
        )
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
