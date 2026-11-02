"""[T-claude-ui] Markdown: serif prose, Claude code card.

Design (board 3a/10a): reply body Noto Serif SC 16.5/1.8, headings serif
SemiBold, code card = white fill + #e6e2db hairline + 14dp radius with a
#f5f3ef header strip carrying the language label and a copy icon.
"""
import pathlib, sys

p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/chat/StreamingMarkdownText.kt')
s = p.read_text()

def rep(a, b, n=1):
    global s
    c = s.count(a)
    if c != n:
        sys.exit(f'expected {n} got {c}: {a[:70]!r}')
    s = s.replace(a, b)

rep('private val BaseFontSizeDefault = 16.sp\nprivate val BaseLineHeightDefault = 24.sp',
    'private val BaseFontSizeDefault = 16.5.sp\nprivate val BaseLineHeightDefault = 29.sp')

# Serif body: every MdText gets the Claude serif unless the caller merged a
# monospace family on top (code paths pass fontFamily explicitly to Text,
# not MdText, so they are unaffected).
rep('''        style = LocalTextStyle.current.merge(
            TextStyle(
                lineHeightStyle = LineHeightStyle(''',
    '''        style = LocalTextStyle.current.merge(
            TextStyle(
                fontFamily = com.openminis.app.ui.theme.ClaudeSerif,
                lineHeightStyle = LineHeightStyle(''')

old_card = '''            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.codeBg),
            ) {
                // Header row: language label + copy button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = block.language.ifEmpty { "code" },
                        fontSize = 11.sp,
                        color = MdCodeLangColor,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (copied) "Copied" else "Copy code",
                        tint = if (copied) Color(0xFF23A26D) else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(block.code))
                                copied = true
                            },
                    )
                }'''
new_card = '''            // [T-claude-ui] Code card: white sheet, warm hairline, header strip.
            val claudeDark = com.openminis.app.ui.theme.LocalChatPalette.current.isDark
            val cardEdge = if (claudeDark) Color(0x1FFFFFFF) else Color(0xFFE6E2DB)
            val cardHead = if (claudeDark) Color(0xFF2C2C2A) else Color(0xFFF5F3EF)
            val cardMeta = if (claudeDark) Color(0xFFA19D94) else Color(0xFF77736C)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.codeBg)
                    .border(1.dp, cardEdge, RoundedCornerShape(14.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardHead)
                        .padding(start = 14.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = block.language.ifEmpty { "code" },
                        fontSize = 12.sp,
                        fontFamily = com.openminis.app.ui.theme.ClaudeMono,
                        color = cardMeta,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(block.code))
                                copied = true
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = if (copied) "Copied" else "Copy code",
                            tint = if (copied) Color(0xFF4F8A72) else cardMeta,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(cardEdge))'''
rep(old_card, new_card)

rep('''                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = block.code,
                            fontSize = BaseFontSize * 0.85f,
                            fontFamily = FontFamily.Monospace,''',
    '''                            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 2.dp),
                    ) {
                        Text(
                            text = block.code,
                            fontSize = 12.5.sp,
                            fontFamily = com.openminis.app.ui.theme.ClaudeMono,''')
rep('''                            color = colors.codeText,
                            lineHeight = BaseLineHeight * 0.9f,''',
    '''                            color = colors.codeText,
                            lineHeight = 21.sp,''')

p.write_text(s)
print('markdown patched')
