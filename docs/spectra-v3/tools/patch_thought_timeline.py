#!/usr/bin/env python3
"""[T-spectra-v4-thought] ChatStepTimeline: one thinking card per run of steps."""
import pathlib, sys

P = pathlib.Path("src/android/app/src/main/java/com/openminis/app/ui/chat/ChatStepTimeline.kt")
s = P.read_text()

def once(old, new, label):
    global s
    c = s.count(old)
    if c != 1:
        sys.exit(f"[{label}] expected 1 match, got {c}")
    s = s.replace(old, new)

once('''    val segments: List<Pair<String?, Long>> = emptyList(),
)''', '''    val segments: List<Pair<String?, Long>> = emptyList(),
    /** [T-spectra-v4-thought] The run is still being produced. */
    val live: Boolean = false,
    /** The newest step is a thinking block that is still streaming. */
    val liveThinking: Boolean = false,
    /** One line for the folded card: newest thought / running tool / titles. */
    val preview: String = "",
)''', "summary")

once('''    /** Present on the head only. */
    val summary: StepRunSummary?,
)''', '''    /** Present on the head only. */
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
    get() = this != null && card && !isHead''', "info")

once('''internal fun computeStepRuns(
    items: List<FlatChatItem>,
    streaming: Boolean,
    isStep: (FlatChatItem) -> Boolean,
): Map<String, StepRunInfo> {''', '''internal fun computeStepRuns(
    items: List<FlatChatItem>,
    streaming: Boolean,
    // [T-spectra-v4-thought] Every run (any length, live or not) becomes one
    // thinking card; text between runs (an interim summary) closes the card.
    cardMode: Boolean = false,
    // Whether a live card opens by itself (Appearance → expand thinking).
    autoExpandLive: Boolean = true,
    isStep: (FlatChatItem) -> Boolean,
): Map<String, StepRunInfo> {''', "sig")

once('''        val collapsible = run.size >= STEP_RUN_COLLAPSE_MIN && !anyLive &&
            (!streaming || hasContentAfter)''', '''        val runLive = anyLive || (streaming && !hasContentAfter)
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
        }.orEmpty()''', "collapsible")

once('''                    else -> null
                }
            },
        )''', '''                    else -> null
                }
            },
            live = runLive,
            liveThinking = liveThinking,
            preview = preview,
        )''', "summary-fill")

once('''                summary = if (idx == 0) summary else null,
            )''', '''                summary = if (idx == 0) summary else null,
                index = idx,
                size = run.size,
                card = cardMode,
                defaultExpanded = cardMode && runLive && autoExpandLive,
            )''', "info-fill")

once('''internal fun StepRunInfo?.isFoldedAway(expanded: SnapshotStateMap<String, Boolean>): Boolean =
    this != null && collapsible && !isHead && expanded[runId] != true''',
'''internal fun StepRunInfo?.isFoldedAway(expanded: SnapshotStateMap<String, Boolean>): Boolean =
    this != null && collapsible && !isHead && !(expanded[runId] ?: defaultExpanded)''', "folded")

once('''    if (info == null) { content(false, false); return }
    val expanded = !info.collapsible || expandedRuns[info.runId] == true''',
'''    if (info == null) { content(false, false); return }
    if (info.card) { ThoughtCardSlot(info, expandedRuns, content); return }
    val expanded = !info.collapsible || (expandedRuns[info.runId] ?: info.defaultExpanded)''', "slot")

s += '''

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
'''

for imp in [
    "import androidx.compose.ui.graphics.drawscope.clipRect",
    "import androidx.compose.ui.graphics.drawscope.Stroke",
    "import androidx.compose.ui.geometry.CornerRadius",
    "import androidx.compose.ui.geometry.Size",
    "import androidx.compose.runtime.setValue",
]:
    if imp not in s:
        s = s.replace("import androidx.compose.ui.geometry.Offset\n", "import androidx.compose.ui.geometry.Offset\n" + imp + "\n", 1)

P.write_text(s)
print("ok")
