#!/usr/bin/env python3
"""Rewrites ToolCallPill's capsule into a timeline step row (node + title +
duration, hairline rail). Behaviour (click, long-press menu, stop, shimmer)
is untouched. Idempotent: refuses to run twice."""
import sys

p = sys.argv[1]
s = open(p, encoding="utf-8").read()
if "stepRail(railUp, railDown" in s:
    print("already patched"); sys.exit(0)

# 1) signature
sig_old = """    onRerunFromHere: (() -> Unit)? = null,
    onCopyDetails: (() -> Unit)? = null,
) {
    // T-android-jank-profile: this log was firing on every ToolCallPill"""
sig_new = """    onRerunFromHere: (() -> Unit)? = null,
    onCopyDetails: (() -> Unit)? = null,
    // [T-noir-step-timeline] Hairline rail joining this step to its
    // neighbours in the same run (see ChatStepTimeline.kt).
    railUp: Boolean = false,
    railDown: Boolean = false,
) {
    // T-android-jank-profile: this log was firing on every ToolCallPill"""
assert s.count(sig_old) == 1
s = s.replace(sig_old, sig_new)

# 2) body: capsule -> step row
start = """    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
      Box(modifier = Modifier.weight(1f, fill = false)) {"""
end = """        // [T-android-tool-bubble-longpress-menu] Long-press menu anchored to
        // the pill. Items mirror"""
a = s.index(start)
b = s.index(end, a)
new_body = """    // [T-noir-step-timeline] The capsule became a timeline row: colour moved
    // from a beige fill on every pill into the tool's own tinted node, so a
    // long run reads as distinct kinds of work instead of a wall of
    // identical lozenges. Failure turns the node ember; cancel greys it.
    val railColor = stepRailColor()
    val nodeAccent = when {
        isFailed -> Noir.Ember
        isCancelled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        else -> toolAccent
    }
    val stepShape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .stepRail(railUp, railDown, railColor),
    ) {
      Box(modifier = Modifier.weight(1f, fill = false)) {
        Row(
            modifier = Modifier
                .clip(stepShape)
                .combinedClickable(
                    onClick = { onOpenDetail(block.id) },
                    onLongClick = if (onRerunFromHere != null || onCopyDetails != null) {
                        { showToolMenu = true }
                    } else null,
                )
                .height(StepRowHeight)
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Node — the typed tool icon in its spectrum ink; a thin arc
            // spins around it while the tool runs.
            StepNode(icon = displayIcon, accent = nodeAccent, running = isRunning)

            Spacer(modifier = Modifier.width(10.dp))

            // [T-step-timestamp v2 aa8b1128] Start time + elapsed duration live
            // in the tool detail bottom sheet header (formatStepTimestamp).
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // [T-android-subagent-control-capsule] Prefer the
                    // operation description for a control call.
                    text = controlSummary ?: block.toolTitle.ifEmpty { block.toolName },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isCancelled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isRunning) {
                    StreamingDotsText()
                }
            }

            if (durationText != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = durationText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isFailed) Noir.Ember.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    softWrap = false,
                    maxLines = 1,
                )
            }
            // T168: per-tool stop button for running blocks only.
            if (isRunning && onStop != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ToolStopButton(onStop = onStop)
            }
        }
        // [T-android-decorative-anim-perf] Shimmer overlay on its own layer.
        // Tinted with the tool's ink so the sweep is visible on Paper too.
        if (shimmerTick != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(stepShape)
                    .graphicsLayer {
                        val phase = decorativePhase(shimmerTick.value, SHIMMER_PERIOD_MS)
                        translationX = (-1f + phase * 3f) * size.width
                    }
                    .drawWithCache {
                        val band = size.width * 0.6f
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                toolAccent.copy(alpha = 0f),
                                toolAccent.copy(alpha = 0.12f),
                                toolAccent.copy(alpha = 0f),
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(band, 0f),
                        )
                        onDrawBehind {
                            drawRect(brush = brush, size = androidx.compose.ui.geometry.Size(band, size.height))
                        }
                    },
            )
        }
"""
s = s[:a] + new_body + s[b:]
open(p, "w", encoding="utf-8").write(s)
print("patched ToolCallPill")
