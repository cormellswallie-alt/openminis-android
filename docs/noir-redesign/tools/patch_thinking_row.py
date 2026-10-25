#!/usr/bin/env python3
"""Rewrites ThinkingBlock's full-width gold card into a timeline step:
gold node + "Thinking" + a live one-line preview, expanding into an inset
panel. Expand/collapse latch, tail window, hard cap and inner-scroll
auto-follow are kept verbatim. Idempotent."""
import sys

p = sys.argv[1]
s = open(p, encoding="utf-8").read()
if "thinkingPreviewLine(" in s:
    print("already patched"); sys.exit(0)

sig_old = "internal fun ThinkingBlock(block: AssistantBlock, isStreaming: Boolean, isLast: Boolean = true) {"
sig_new = ("internal fun ThinkingBlock(\n"
           "    block: AssistantBlock,\n"
           "    isStreaming: Boolean,\n"
           "    isLast: Boolean = true,\n"
           "    // [T-noir-step-timeline] rail to neighbouring steps.\n"
           "    railUp: Boolean = false,\n"
           "    railDown: Boolean = false,\n"
           ") {")
assert s.count(sig_old) == 1
s = s.replace(sig_old, sig_new)

a = s.index("    val thinkingBlue = Color(0xFF9A7A48)\n")
b = s.index("        // Expanded content. Mirrors iOS ThinkingBlockView")
head_new = '''    // [T-noir-step-timeline] Gold is reserved for thinking: it is the one
    // step kind drawn in the brand ink, everything else uses the spectrum.
    val thinkingInkColor = thinkingInk()
    val charCount = block.content.length
    val charLabel = when {
        charCount >= 1000 -> "${charCount / 1000}K"
        else -> "$charCount"
    }
    // [T-thinking-render-perf-android] Tail window / hard cap — see history.
    val thinkingWindowSize = 8000
    val thinkingHardCap = 100_000
    val overHardCap = charCount > thinkingHardCap
    var showFullContent by remember(block.id) { mutableStateOf(false) }
    val liveThinking = isStreaming && block.toolStatus != ToolBlockStatus.SUCCESS
    // Live one-liner beside the label: O(600) per token, never the whole block.
    val preview = remember(block.id, charCount) { thinkingPreviewLine(block.content) }
    val chevronAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "thinkingChevron",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .stepRail(railUp, railDown, stepRailColor()),
    ) {
        // Header row — only the header reacts to taps (a drag-release in the
        // inner scroller must not toggle).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    // [T-android-thinking-collapse-latch] Record the direction.
                    if (!overHardCap) userIntent = !expanded
                    if (overHardCap) showFullContent = true
                    else expanded = !expanded
                }
                .height(StepRowHeight)
                .padding(end = 6.dp),
        ) {
            StepNode(
                icon = Icons.Default.Psychology,
                accent = thinkingInkColor,
                running = liveThinking,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.noir_step_thinking),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = thinkingInkColor,
                maxLines = 1,
            )
            if (!expanded && preview.isNotEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = preview,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            if (charCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = charLabel,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    softWrap = false,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            if (overHardCap) {
                Text(
                    text = stringResource(R.string.thinking_view_full),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = thinkingInkColor,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp).rotate(chevronAngle),
                )
            }
        }

'''
s = s[:a] + head_new + s[b:]

# Inner panel: inset under the label, soft gold-tinted ground, gold rule.
old_panel = """            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .heightIn(max = 300.dp)
                    .verticalScroll(scrollState),
            ) {"""
new_panel = """            Column(
                modifier = Modifier
                    .padding(start = StepTextIndent, top = 2.dp, bottom = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(thinkingInkColor.copy(alpha = if (ChatColors.isDark) 0.08f else 0.06f))
                    .drawBehind {
                        drawRect(
                            color = thinkingInkColor.copy(alpha = 0.55f),
                            size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height),
                        )
                    }
                    .heightIn(max = 300.dp)
                    .verticalScroll(scrollState)
                    .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            ) {"""
assert s.count(old_panel) == 1
s = s.replace(old_panel, new_panel)

old_txt = """                Text(
                    text = displayContent,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    lineHeight = 19.sp,
                )"""
new_txt = """                Text(
                    text = displayContent,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    lineHeight = 20.sp,
                )"""
assert s.count(old_txt) == 1
s = s.replace(old_txt, new_txt)

old_dlg = """                        text = "Deep Thinking",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9A7A48),"""
new_dlg = """                        text = stringResource(R.string.noir_step_thinking),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = thinkingInk(),"""
assert s.count(old_dlg) == 1
s = s.replace(old_dlg, new_dlg)

open(p, "w", encoding="utf-8").write(s)
print("patched ThinkingBlock")
