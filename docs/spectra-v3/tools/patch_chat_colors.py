"""[T-spectra-v3] ChatColors: ink primary, no hue in chrome, question has no bubble."""
import pathlib, re
p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/theme/ChatColors.kt')
s = p.read_text()

def setv(block_start, key, val):
    global s
    a = s.index(block_start)
    b = s.index('\n)\n', a)
    blk = s[a:b]
    new, n = re.subn(r'(\n    %s = )[^\n]+' % key, r'\g<1>%s,' % val.replace('\\', '\\\\'), blk, count=1)
    assert n == 1, (block_start, key)
    s = s[:a] + new + s[b:]

L = 'val LightChatPalette = ChatPalette('
D = 'val DarkChatPalette = ChatPalette('
for k, v in {
    'background': 'Noir.PaperCard', 'secondaryBg': 'Noir.PaperCard', 'inputBg': 'Noir.PaperRaised',
    'inputIconBg': 'Noir.PaperSunk', 'inputIconBorder': 'Color.Transparent', 'inputBorder': 'Noir.PaperHairline',
    'userBubble': 'Noir.PaperSunk', 'userBubbleText': 'Noir.Ink',
    'toolBg': 'Noir.Paper', 'toolBorder': 'Noir.PaperHairline', 'toolCapsuleBg': 'Noir.PaperSunk',
    'sendButton': 'Noir.Ink', 'sendButtonDisabled': 'Noir.PaperSunk2',
    'codeBlockBg': 'Color(0xFF111113)', 'codeBlockText': 'Color(0xFFE4E4E7)',
    'inlineCodeBg': 'Noir.PaperSunk', 'inlineCodeText': 'Noir.InkSoft',
    'link': 'Noir.LinkOnPaper', 'blockquoteBar': 'Color(0xFFDCDCE0)', 'thinking': 'Noir.ThinkOnPaper',
    'warningBg': 'Color(0x12E5484D)', 'inputShadow': 'Color(0x1A0A0A0B)', 'toastBg': 'Color(0xF00A0A0B)',
    'fabAccent': 'Noir.Ink',
}.items():
    setv(L, k, v)
for k, v in {
    'background': 'Noir.Onyx', 'secondaryBg': 'Noir.Onyx1', 'inputBg': 'Noir.Onyx1',
    'inputIconBg': 'Noir.Onyx3', 'inputIconBorder': 'Color.Transparent', 'inputBorder': 'Noir.OnyxHairline',
    'userBubble': 'Noir.Onyx2', 'userBubbleText': 'Noir.Ivory',
    'toolBg': 'Noir.Onyx1', 'toolCapsuleBg': 'Noir.Onyx2',
    'sendButton': 'Noir.Ivory', 'sendButtonDisabled': 'Noir.Onyx3',
    'codeBlockBg': 'Color(0xFF111113)', 'codeBlockText': 'Color(0xFFE4E4E7)',
    'inlineCodeBg': 'Noir.Onyx3', 'inlineCodeText': 'Noir.IvorySoft',
    'link': 'Noir.LinkOnOnyx', 'blockquoteBar': 'Color(0xFF3A3A40)', 'thinking': 'Noir.ThinkOnOnyx',
    'warningBg': 'Color(0x1AFF6369)', 'toastBg': 'Color(0xF0F4F4F5)', 'fabAccent': 'Noir.Ivory',
}.items():
    setv(D, k, v)
s = s.replace('val LightChatPalette = ChatPalette(', '''// [T-spectra-v3] Pure white chat, ink text and ink primary. The user's turn
// has no bubble (it is the page heading), so userBubble only backs legacy
// surfaces such as the edit-in-place field.
val LightChatPalette = ChatPalette(''', 1)
p.write_text(s)
print('ok')
