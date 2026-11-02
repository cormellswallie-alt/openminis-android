"""[T-claude-ui] Tool step row -> design board round 5 ("思维链中的工具调用").

A step is a quiet line hanging from the thought rail: 16dp stroke glyph in a
20dp gutter, `title` in body ink + `target` in muted ink, meta on the right,
a small check / spinner / failure mark. No tinted node, no lifted card.
"""
import pathlib, re

p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/chat/ChatAssistantMessageUI.kt')
s = p.read_text()

a = s.index('    val dark = ChatColors.isDark\n    val stepShape = RoundedCornerShape(if (isRunning) 18.dp else 14.dp)')
b = s.index('        @Suppress("UNUSED_VARIABLE") val unusedShimmer = shimmerTick')

new = '''    val dark = ChatColors.isDark
    val stepShape = RoundedCornerShape(10.dp)
    val metaColor = if (dark) Noir.IvoryMute else Noir.InkMute
    val glyphColor = when {
        isFailed -> Noir.Ember
        isRunning -> if (dark) Noir.Ivory else Noir.Ink
        else -> if (dark) Noir.IvoryMute else Color(0xFF8A867F)
    }
    @Suppress("UNUSED_VARIABLE") val unusedAccent = nodeAccent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .stepRail(railUp, railDown, railColor),
    ) {
      Box(modifier = Modifier.weight(1f, fill = true)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(stepShape)
                .combinedClickable(
                    onClick = { onOpenDetail(block.id) },
                    onLongClick = if (onRerunFromHere != null || onCopyDetails != null) {
                        { showToolMenu = true }
                    } else null,
                )
                .heightIn(min = StepRowHeight)
                .padding(end = 2.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(StepGutter), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = displayIcon,
                    contentDescription = null,
                    tint = glyphColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // [T-android-subagent-control-capsule] Prefer the
                    // operation description for a control call.
                    text = controlSummary ?: block.toolTitle.ifEmpty { block.toolName },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = when {
                        isCancelled -> metaColor
                        dark -> Noir.IvorySoft
                        else -> Noir.InkSoft
                    },
                    textDecoration = if (isCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = block.toolName,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isFailed) Noir.Ember else Color(0xFF8A867F),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (durationText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = durationText,
                    fontSize = 12.5.sp,
                    color = if (isFailed) Noir.Ember else metaColor,
                    softWrap = false,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            when {
                isRunning -> androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 1.8.dp,
                    color = if (dark) Noir.Ivory else Noir.Ink,
                    trackColor = if (dark) Noir.OnyxHairline else Noir.PaperSunk2,
                )
                isFailed -> Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = Noir.Ember,
                    modifier = Modifier.size(15.dp),
                )
                isDone -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF8A867F),
                    modifier = Modifier.size(14.dp),
                )
            }
            // T168: per-tool stop button for running blocks only.
            if (isRunning && onStop != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ToolStopButton(onStop = onStop)
            }
        }
'''
s = s[:a] + new + s[b:]
if 'import androidx.compose.material.icons.outlined.ErrorOutline' not in s:
    s = s.replace('import androidx.compose.material.icons.Icons\n',
                  'import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.outlined.ErrorOutline\n', 1)
p.write_text(s)
print('tool row patched')
