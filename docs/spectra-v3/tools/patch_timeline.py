"""[T-spectra-v3] Step timeline: squircle nodes, fingerprint summary card."""
import pathlib
p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/chat/ChatStepTimeline.kt')
s = p.read_text()

def rep(old, new):
    global s
    assert old in s, old[:90]
    s = s.replace(old, new, 1)

rep('internal val StepNodeSize: Dp = 26.dp\ninternal val StepRowHeight: Dp = 38.dp\ninternal val StepTextIndent: Dp = 36.dp',
    'internal val StepNodeSize: Dp = 28.dp\ninternal val StepRowHeight: Dp = 46.dp\ninternal val StepTextIndent: Dp = 40.dp')

# summary gains fingerprint segments
rep('''    /** Distinct tool titles in order, for the subtitle. */
    val titles: List<String>,
)''', '''    /** Distinct tool titles in order, for the subtitle. */
    val titles: List<String>,
    /** [T-spectra-v3] Every step in order: tool name (null = thinking) and its duration. */
    val segments: List<Pair<String?, Long>> = emptyList(),
)''')
rep('''            titles = tools.map { it.block.toolTitle.ifEmpty { it.block.toolName } }
                .distinct().take(6),
        )''', '''            titles = tools.map { it.block.toolTitle.ifEmpty { it.block.toolName } }
                .distinct().take(6),
            segments = run.mapNotNull { item ->
                when (item) {
                    is FlatChatItem.AssistantToolUse -> item.block.toolName to item.block.durationMs
                    is FlatChatItem.AssistantThinking -> null to item.block.durationMs
                    else -> null
                }
            },
        )''')

# rail: start/stop relative to the squircle
rep('''    val gapAroundNode = (StepNodeSize / 2 + 3.dp).toPx()''', '''    val gapAroundNode = (StepNodeSize / 2 + 4.dp).toPx()''')
rep('''    if (ChatColors.isDark) Noir.OnyxHairlineStrong else Noir.PaperHairlineStrong''',
    '''    if (ChatColors.isDark) Noir.OnyxHairline else Noir.PaperHairline''')

# StepNode: squircle, neutral fill, tinted glyph, ink spinner
a = s.index('internal fun StepNode(')
b = s.index('internal fun formatStepRunDuration')
s = s[:a] + '''internal fun StepNode(
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

''' + s[b:]

# Summary card: v3 "sum" — white card with soft shadow, check, count + time, fingerprint
a = s.index('/** Folded run: overlapping coloured nodes, count, titles, total time. */')
b = s.index('/** Opaque blend of [top] over [base] at [amount]. */')
s = s[:a] + '''/** [T-spectra-v3] Folded run: check · "N steps · time" · fingerprint bar · titles. */
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

''' + s[b:]

for imp in ['import androidx.compose.foundation.layout.Arrangement', 'import androidx.compose.ui.draw.shadow']:
    if imp not in s:
        s = s.replace('import androidx.compose.ui.Alignment\n', 'import androidx.compose.ui.Alignment\n' + imp + '\n', 1)
p.write_text(s)
print('ok')
