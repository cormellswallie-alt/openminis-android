"""[T-spectra-v3] Tool detail sheet -> v3 "step inspector".

- white sheet, 28dp top corners, grab handle
- header: accent tool tile + step title + one status line
  (status · duration · time · n/N); action + close buttons on the right
- the bottom status row is folded into the header; prev/next become
  40dp tonal circles. Every action (terminal / browser / transcript / copy,
  prev / next, close) is unchanged.
"""
import sys
P = 'src/android/app/src/main/java/com/openminis/app/ui/chat/ChatToolDetailUI.kt'
s = open(P, encoding='utf-8').read()

def once(old, new):
    global s
    n = s.count(old)
    if n != 1:
        sys.exit(f'anchor count {n}: {old[:90]!r}')
    s = s.replace(old, new)

# 1. sheet container
once("""        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(ChatColors.secondaryBg),
        ) {
            // ── Top Nav Bar (iOS: X button + "Minis Computer" + action button) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChatColors.sheetHeaderBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {""",
"""        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = ChatColors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(ChatColors.background),
        ) {
            // [T-spectra-v3] Grab handle.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .background(ChatColors.tertiaryText.copy(alpha = 0.35f), RoundedCornerShape(2.dp)),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 12.dp, top = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {""")

# 2. cut the leading close button + "Minis Computer" title
a = s.index("                // Close button (iOS: xmark in circle)\n                Box(\n                    modifier = Modifier\n                        .size(32.dp)\n                        .background(ChatColors.secondaryBg, CircleShape)")
b = s.index("                Spacer(modifier = Modifier.weight(1f))\n", a)
close_box = s[a:b]
c = s.index("                Spacer(modifier = Modifier.weight(1f))\n\n                // Action button: dispatches per tool kind.", b)
header = """                // [T-spectra-v3] Step identity: tool tile + title + one status line.
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(toolAccent.copy(alpha = if (ChatColors.isDark) 0.16f else 0.10f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLive) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = toolAccent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            toolIconFor(block.toolName),
                            contentDescription = toolTitleLabel(block.toolName),
                            tint = toolAccent,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                val hdrDurationMs = if (com.openminis.app.agent.jobs.HelperRunner.isSubAgentToolName(block.toolName)) {
                    runCatching { org.json.JSONObject(block.content).optLong("elapsed_s", -1L) }.getOrDefault(-1L)
                        .takeIf { it >= 0 }?.let { it * 1000L } ?: block.durationMs
                } else block.durationMs
                val (hdrStatus, hdrStatusColor) = when {
                    isLive -> stringResource(R.string.spectra_step_running) to (if (ChatColors.isDark) com.openminis.app.ui.theme.Noir.OkOnOnyx else com.openminis.app.ui.theme.Noir.Ok)
                    block.toolStatus == ToolBlockStatus.FAILED -> stringResource(R.string.spectra_step_failed) to ToolErrorColor
                    block.toolStatus == ToolBlockStatus.TIMEOUT -> stringResource(R.string.spectra_step_timeout) to ToolErrorColor
                    block.toolStatus == ToolBlockStatus.CANCELLED -> stringResource(R.string.spectra_step_cancelled) to ChatColors.secondaryText
                    else -> stringResource(R.string.spectra_step_done) to ChatColors.secondaryText
                }
                val hdrMeta = buildList {
                    add(toolTitleLabel(block.toolName))
                    if (hdrDurationMs > 0 && !isLive) add(formatToolDuration(hdrDurationMs))
                    if (block.startTimeMs > 0L) add(formatStepTimestamp(block.startTimeMs))
                    if (toolBlocks.size > 1) add("${currentIdx + 1} / ${toolBlocks.size}")
                }.joinToString("  ·  ")
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.toolTitle.ifEmpty { toolDisplayName(block.toolName) },
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatColors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            text = hdrStatus,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = hdrStatusColor,
                            maxLines = 1,
                        )
                        if (hdrMeta.isNotEmpty()) {
                            Text(
                                text = "  ·  $hdrMeta",
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                color = ChatColors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

"""
s = s[:a] + header + s[c + len("                Spacer(modifier = Modifier.weight(1f))\n\n"):]

# 3. re-append close button after the copyDone effect (end of header row)
anchor = """                LaunchedEffect(copyDone) {
                    if (copyDone) {
                        kotlinx.coroutines.delay(2000)
                        copyDone = false
                    }
                }
"""
once(anchor, anchor + "                Spacer(modifier = Modifier.width(8.dp))\n" + close_box.rstrip() + "\n")

# buttons: 32dp bordered circles -> 38dp tonal circles (both header buttons)
old_btn = """                        .size(32.dp)
                        .background(ChatColors.secondaryBg, CircleShape)
                        .border(0.5.dp, ChatColors.inputIconBorder, CircleShape)"""
if s.count(old_btn) != 2:
    sys.exit(f'btn count {s.count(old_btn)}')
s = s.replace(old_btn, """                        .size(38.dp)
                        .background(ChatColors.secondaryBg, CircleShape)""")

# 4. header divider -> hairline
once("            HorizontalDivider(thickness = 1.dp, color = ChatColors.sheetHeaderBorder)",
     "            HorizontalDivider(thickness = 0.5.dp, color = ChatColors.separator)")

# 5. drop the bottom status row (now in the header)
a = s.index("""                HorizontalDivider(thickness = 0.5.dp, color = ChatColors.separator)

                // Tool info row: status icon + title/subtitle + duration
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp, bottom = 4.dp),""")
b = s.index("""                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),""", a)
s = s[:a] + "                HorizontalDivider(thickness = 0.5.dp, color = ChatColors.separator)\n\n" + s[b:]
once("""                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),""", """                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 10.dp),""")
# prev / next -> 42dp tonal circles
for nm in ["onClick = { if (currentIdx > 0) currentIdx-- },\n                        enabled = currentIdx > 0,",
           "onClick = { if (currentIdx < toolBlocks.lastIndex) currentIdx++ },\n                        enabled = currentIdx < toolBlocks.lastIndex,"]:
    once(nm + "\n                        modifier = Modifier.size(32.dp),",
         nm + "\n                        modifier = Modifier.size(42.dp).background(ChatColors.secondaryBg, CircleShape),")
open(P, 'w', encoding='utf-8').write(s)
print('ok')
