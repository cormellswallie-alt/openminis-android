"""[T-spectra-v3] User turn = page heading: no bubble, left aligned, large type."""
import pathlib
p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/chat/ChatUserMessageUI.kt')
s = p.read_text()

def rep(old, new, count=1):
    global s
    assert s.count(old) >= 1, old[:80]
    s = s.replace(old, new, count)

rep('''        val bubbleMaxWidth = this.maxWidth * 0.8f
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.weight(1f).widthIn(min = 60.dp))
        Box(
            modifier = Modifier
                .widthIn(max = bubbleMaxWidth)''',
'''        // [T-spectra-v3] The question is the page heading: full width, left aligned.
        val bubbleMaxWidth = this.maxWidth
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = bubbleMaxWidth)''')
rep('''            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Attachments ABOVE''', '''            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Attachments ABOVE''')
rep('''                        val bubbleBg = if (isQueued) Color.Transparent else userBubbleColor''',
'''                        @Suppress("UNUSED_VARIABLE") val legacyBubble = userBubbleColor
                        val bubbleBg = Color.Transparent''')
rep('''                                val r = 22.dp.toPx()''', '''                                val r = 16.dp.toPx()''')
rep('''                                Text(
                                    text = message.content,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.5.sp),
                                    modifier = bubbleModifier
                                        .background(bubbleBg, shape)
                                        .clip(shape)
                                        .then(dashedStroke)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                )''',
'''                                // Short questions read as a heading (19sp
                                // SemiBold); long pastes drop to 16sp Medium
                                // so a log dump never becomes a wall of bold.
                                val isShort = message.content.length <= 160 &&
                                    message.content.count { it == '\\n' } < 4
                                Text(
                                    text = message.content,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = if (isShort) 19.sp else 16.sp,
                                        lineHeight = if (isShort) 28.sp else 25.sp,
                                        fontWeight = if (isShort) androidx.compose.ui.text.font.FontWeight.SemiBold
                                        else androidx.compose.ui.text.font.FontWeight.Medium,
                                        letterSpacing = if (isShort) (-0.01).em else 0.em,
                                    ),
                                    modifier = bubbleModifier
                                        .background(bubbleBg, shape)
                                        .clip(shape)
                                        .then(dashedStroke)
                                        .padding(
                                            horizontal = if (isQueued) 14.dp else 2.dp,
                                            vertical = if (isQueued) 10.dp else 2.dp,
                                        ),
                                )''')
rep('''        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),''',
    '''        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),''')
rep('''            .padding(top = if (precededByUser) 14.dp else 10.dp, bottom = 4.dp),''',
    '''            .padding(start = 2.dp, top = if (precededByUser) 16.dp else 18.dp, bottom = 6.dp),''')
if 'import androidx.compose.ui.unit.em' not in s:
    s = s.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.em\nimport androidx.compose.ui.unit.sp', 1)
p.write_text(s)
print('ok')
