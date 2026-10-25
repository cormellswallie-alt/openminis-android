"""[T-spectra-v5-thought] Folio thinking card: swap ThinkingBlock body and the
card slot/header for the v5 versions, add imports."""
import pathlib, re
here = pathlib.Path(__file__).parent
base = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/chat')


def add_imports(s, need):
    missing = [i for i in need if not re.search(r'^import ' + re.escape(i) + r'$', s, re.M)]
    if missing:
        i = s.index('\nimport ') + 1
        s = s[:i] + ''.join(f'import {m}\n' for m in missing) + s[i:]
    return s


# 1) ThinkingBlock … ThinkingActionChip → v5
p = base / 'ChatAssistantMessageUI.kt'
s = p.read_text()
a = s.index('@Composable\ninternal fun ThinkingBlock(')
b = s.index('/**\n * [T-thinking-render-perf-android] Full-screen viewer')
s = s[:a] + (here / 'thinking_block_v5.kt.txt').read_text() + s[b:]
s = add_imports(s, [
    'androidx.compose.animation.togetherWith',
    'androidx.compose.animation.animateContentSize',
    'androidx.compose.foundation.layout.RowScope',
])
p.write_text(s)

# 2) ThoughtCardSlot + ThoughtCardHeader → v5
p = base / 'ChatStepTimeline.kt'
s = p.read_text()
a = s.index('@Composable\nprivate fun ThoughtCardSlot(')
b = s.index('@Composable\nprivate fun ThoughtCardHeader(')
# header runs to the next top-level declaration (or EOF)
m = re.compile(r'^(@Composable|internal |private |fun |/\*\*)', re.M)
nxt = None
for mm in m.finditer(s, b + 40):
    nxt = mm.start()
    break
end = nxt if nxt is not None else len(s)
s = s[:a] + (here / 'thought_slot_v5.kt.txt').read_text() + '\n' + s[end:]
s = add_imports(s, ['androidx.compose.ui.graphics.graphicsLayer'])
p.write_text(s)
print('ok')
