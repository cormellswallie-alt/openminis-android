"""[T-spectra-v3] Tool step row (two-line, live card) and the new thinking card."""
import pathlib
p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/chat/ChatAssistantMessageUI.kt')
s = p.read_text()

# ───────────────────────── Tool row ─────────────────────────
a = s.index('    val stepShape = RoundedCornerShape(12.dp)\n    Row(\n        modifier = Modifier\n            .fillMaxWidth()\n            .stepRail(railUp, railDown, railColor),')
b = s.index('        // [T-android-tool-bubble-longpress-menu] Long-press menu anchored to\n        // the pill.', a)
tool_row = '''    // [T-spectra-v3] Finished steps are one quiet two-line row; the running
    // step lifts into a white card (soft shadow, no glow, no shimmer).
    val dark = ChatColors.isDark
    val stepShape = RoundedCornerShape(if (isRunning) 18.dp else 14.dp)
    val liveCardBg = if (dark) Noir.Onyx1 else Noir.PaperCard
    val liveHair = if (dark) Noir.OnyxHairline else Noir.PaperHairline
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .stepRail(railUp && !isRunning, railDown && !isRunning, railColor),
    ) {
      Box(modifier = Modifier.weight(1f, fill = true)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isRunning) Modifier
                        .padding(vertical = 4.dp)
                        .shadow(if (dark) 0.dp else 10.dp, stepShape, ambientColor = Color(0x140A0A0B), spotColor = Color(0x240A0A0B))
                        .background(liveCardBg, stepShape)
                        .border(0.5.dp, liveHair, stepShape)
                    else Modifier,
                )
                .clip(stepShape)
                .combinedClickable(
                    onClick = { onOpenDetail(block.id) },
                    onLongClick = if (onRerunFromHere != null || onCopyDetails != null) {
                        { showToolMenu = true }
                    } else null,
                )
                .heightIn(min = StepRowHeight)
                .padding(
                    start = if (isRunning) 10.dp else 0.dp,
                    end = if (isRunning) 8.dp else 4.dp,
                    top = if (isRunning) 10.dp else 4.dp,
                    bottom = if (isRunning) 10.dp else 4.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepNode(icon = displayIcon, accent = nodeAccent, running = isRunning)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // [T-android-subagent-control-capsule] Prefer the
                    // operation description for a control call.
                    text = controlSummary ?: block.toolTitle.ifEmpty { block.toolName },
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (isRunning) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isCancelled) metaColor else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 1.dp)) {
                    Text(
                        text = block.toolName,
                        fontSize = 12.5.sp,
                        fontFamily = com.openminis.app.ui.terminal.JetBrainsMonoFamily,
                        color = if (isFailed) nodeAccent else metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isRunning) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StreamingDotsText()
                    }
                }
            }

            if (durationText != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = durationText,
                    fontSize = 12.5.sp,
                    color = if (isFailed) nodeAccent else metaColor,
                    softWrap = false,
                    maxLines = 1,
                )
            }
            if (isDone) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (dark) Noir.OkOnOnyx else Noir.Ok,
                    modifier = Modifier.size(15.dp),
                )
            }
            // T168: per-tool stop button for running blocks only.
            if (isRunning && onStop != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ToolStopButton(onStop = onStop)
            }
        }
        @Suppress("UNUSED_VARIABLE") val unusedShimmer = shimmerTick
'''
s = s[:a] + tool_row + s[b:]
s = s.replace('    val shimmerTick = if (isRunning) rememberDecorativeTick() else null',
              '    val shimmerTick: androidx.compose.runtime.State<Long>? = null', 1)

# ───────────────────────── Thinking card ─────────────────────────
a = s.index('    Column(\n        modifier = Modifier\n            .fillMaxWidth()\n            .stepRail(railUp, railDown, stepRailColor()),\n    ) {\n        // Header row — only the header reacts to taps')
b = s.index('        // [T-thinking-render-perf-android] Hard-cap full-content viewer.', a)
think = '''    // [T-spectra-v3] Thinking card. The node is the assistant orb itself
    // (thinking state: rotating violet ring); violet appears only as the
    // 1.5dp margin line, the depth label and the caret. Body reads like a
    // note: 15.5sp / 1.85 line height. Elapsed time is measured locally
    // (blocks carry no timing): live while streaming, frozen when done.
    val dark = ChatColors.isDark
    val startedAt = remember(block.id) { android.os.SystemClock.elapsedRealtime() }
    var frozenSecs by remember(block.id) { mutableStateOf<Long?>(null) }
    var liveSecs by remember(block.id) { mutableStateOf(0L) }
    LaunchedEffect(block.id, liveThinking) {
        if (liveThinking) {
            while (true) {
                liveSecs = (android.os.SystemClock.elapsedRealtime() - startedAt) / 1000
                kotlinx.coroutines.delay(1000)
            }
        } else if (frozenSecs == null && liveSecs > 0) {
            frozenSecs = (android.os.SystemClock.elapsedRealtime() - startedAt) / 1000
        }
    }
    val secs = frozenSecs ?: liveSecs.takeIf { liveThinking }
    val title = when {
        liveThinking -> stringResource(R.string.spectra_thinking_live)
        secs != null && secs > 0 -> stringResource(R.string.spectra_thinking_done_secs, secs.toInt())
        else -> stringResource(R.string.spectra_thinking_done)
    }
    val meta = buildString {
        if (liveThinking && secs != null) append("${secs}s")
        if (charCount > 0) { if (isNotEmpty()) append("  ·  "); append(stringResource(R.string.spectra_thinking_chars, charLabel)) }
    }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val cardShape = RoundedCornerShape(18.dp)
    val floating = liveThinking && expanded
    val cardBg = if (dark) Noir.Onyx1 else Noir.PaperCard
    val hair = if (dark) Noir.OnyxHairline else Noir.PaperHairline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .stepRail(railUp && !floating, railDown && !floating, stepRailColor())
            .then(
                if (floating) Modifier
                    .padding(vertical = 4.dp)
                    .shadow(if (dark) 0.dp else 10.dp, cardShape, ambientColor = Color(0x140A0A0B), spotColor = Color(0x240A0A0B))
                    .background(cardBg, cardShape)
                    .border(0.5.dp, hair, cardShape)
                    .clip(cardShape)
                    .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 10.dp)
                else Modifier,
            ),
    ) {
        // Header row — only the header reacts to taps (a drag-release in the
        // inner scroller must not toggle).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    // [T-android-thinking-collapse-latch] Record the direction.
                    if (!overHardCap) userIntent = !expanded
                    if (overHardCap) showFullContent = true
                    else expanded = !expanded
                }
                .heightIn(min = StepRowHeight)
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Box(modifier = Modifier.size(StepNodeSize), contentAlignment = Alignment.Center) {
                com.openminis.app.ui.theme.SpectraOrb(
                    size = 22.dp,
                    state = if (liveThinking) com.openminis.app.ui.theme.OrbState.Think
                    else com.openminis.app.ui.theme.OrbState.Done,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (liveThinking) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (meta.isNotEmpty()) {
                        Text(
                            text = "  $meta",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                // Collapsed: keep the latest line (live) / the conclusion (done).
                if (!expanded && preview.isNotEmpty()) {
                    Text(
                        text = preview,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            if (overHardCap) {
                Text(
                    text = stringResource(R.string.thinking_view_full),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = thinkingInkColor,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(chevronAngle),
                )
            }
        }

        // Expanded content. Inner scroller capped at 300dp, auto-follows the
        // tail while streaming, pauses when the user drags away.
        AnimatedVisibility(visible = expanded && !overHardCap) {
            val scrollState = rememberScrollState()
            val isTruncated = charCount > thinkingWindowSize
            val displayContent = remember(charCount) {
                if (isTruncated) {
                    val full = block.content
                    val start = charCount - thinkingWindowSize
                    val nl = full.indexOf('\\n', start)
                    if (nl in start until start + 200) full.substring(nl + 1)
                    else full.substring(start)
                } else {
                    block.content
                }
            }
            var userScrolledAway by remember(block.id) { mutableStateOf(false) }
            LaunchedEffect(scrollState, block.id) {
                snapshotFlow {
                    Triple(scrollState.value, scrollState.maxValue, scrollState.isScrollInProgress)
                }.collect { (v, max, dragging) ->
                    val gap = (max - v).coerceAtLeast(0)
                    when {
                        dragging && gap > 4 -> userScrolledAway = true
                        gap <= 4 -> userScrolledAway = false
                    }
                }
            }
            LaunchedEffect(scrollState, block.id, isStreaming) {
                if (!isStreaming) return@LaunchedEffect
                snapshotFlow { block.content.length }
                    .collect {
                        if (userScrolledAway) return@collect
                        scrollState.scrollTo(scrollState.maxValue)
                    }
            }
            val bodyColor = if (dark) Noir.IvorySoft else Noir.InkSoft
            val fadeColor = if (floating) cardBg else MaterialTheme.colorScheme.background
            Column(modifier = Modifier.padding(start = StepTextIndent, top = 4.dp, bottom = 6.dp, end = 4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // The margin line — the only violet in the body.
                            drawRect(
                                color = thinkingInkColor.copy(alpha = if (dark) 0.70f else 0.55f),
                                size = androidx.compose.ui.geometry.Size(1.5.dp.toPx(), size.height),
                            )
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(scrollState)
                            .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    ) {
                        if (isTruncated) {
                            Text(
                                text = stringResource(
                                    R.string.thinking_truncated_hint,
                                    displayContent.length / 1000,
                                    charCount / 1000,
                                ),
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        Text(
                            text = displayContent,
                            fontSize = 15.sp,
                            lineHeight = 27.sp,
                            color = bodyColor,
                        )
                    }
                    // Soft top fade while the window is scrolled (streaming tail).
                    if (scrollState.value > 0) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBehind {
                                    val h = 28.dp.toPx().coerceAtMost(size.height)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to fadeColor, 1f to fadeColor.copy(alpha = 0f),
                                            startY = 0f, endY = h,
                                        ),
                                        topLeft = Offset(2.dp.toPx(), 0f),
                                        size = androidx.compose.ui.geometry.Size(size.width, h),
                                    )
                                },
                        )
                    }
                }
                if (!liveThinking && charCount > 0) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ThinkingActionChip(
                            icon = Icons.Default.ContentCopy,
                            label = stringResource(R.string.spectra_thinking_copy),
                            onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(block.content)) },
                        )
                        ThinkingActionChip(
                            icon = Icons.Default.OpenInFull,
                            label = stringResource(R.string.spectra_thinking_full),
                            onClick = { showFullContent = true },
                        )
                    }
                }
            }
        }

'''
s = s[:a] + think + s[b:]
s = s.replace('        if (overHardCap && showFullContent) {\n            ThinkingFullContentDialog(',
              '        if (showFullContent) {\n            ThinkingFullContentDialog(', 1)

# Action chip + nicer full reader
chip = '''
@Composable
private fun ThinkingActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val dark = ChatColors.isDark
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (dark) Noir.Onyx2 else Noir.PaperSunk)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * [T-thinking-render-perf-android] Full-screen viewer'''
s = s.replace('\n/**\n * [T-thinking-render-perf-android] Full-screen viewer', chip, 1)
s = s.replace('''                                    textSize = 13f
                                    setTextColor(textColor.toArgb())
                                    setTextIsSelectable(true)
                                    setPadding(36, 24, 36, 48)''', '''                                    textSize = 16.5f
                                    setLineSpacing(0f, 1.7f)
                                    typeface = androidx.core.content.res.ResourcesCompat.getFont(ctx, com.openminis.app.R.font.misans_regular)
                                    setTextColor(textColor.toArgb())
                                    setTextIsSelectable(true)
                                    val pad = (22 * ctx.resources.displayMetrics.density).toInt()
                                    setPadding(pad, pad / 2, pad, pad * 2)''', 1)
s = s.replace('''                    Text(
                        text = stringResource(R.string.noir_step_thinking),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = thinkingInk(),
                    )''', '''                    com.openminis.app.ui.theme.SpectraOrb(size = 22.dp, state = com.openminis.app.ui.theme.OrbState.Done)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.noir_step_thinking),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )''', 1)

for imp in ['import androidx.compose.material.icons.filled.OpenInFull', 'import androidx.compose.ui.draw.shadow',
            'import androidx.compose.material.icons.filled.Check', 'import androidx.compose.foundation.border',
            'import androidx.compose.foundation.layout.heightIn', 'import androidx.compose.material.icons.filled.ContentCopy',
            'import androidx.compose.foundation.layout.Arrangement']:
    if imp not in s:
        s = s.replace('\nimport androidx.compose.ui.Alignment\n', '\nimport androidx.compose.ui.Alignment\n' + imp + '\n', 1)
p.write_text(s)
print('ok')
