"""[T-spectra-v4-thought] Swap ThinkingBlock + ThinkingActionChip for the v4
card body (thinking_block_v4.kt.txt) and add the imports it needs."""
import pathlib, re
here = pathlib.Path(__file__).parent
p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/chat/ChatAssistantMessageUI.kt')
s = p.read_text()
start = s.index('@Composable\ninternal fun ThinkingBlock(')
end = s.index('/**\n * [T-thinking-render-perf-android] Full-screen viewer')
s = s[:start] + (here / 'thinking_block_v4.kt.txt').read_text() + s[end:]
need = [
    'androidx.compose.material.icons.filled.Translate',
    'androidx.compose.material.icons.filled.ContentCopy',
    'androidx.compose.material.icons.filled.OpenInFull',
    'androidx.compose.material.icons.filled.KeyboardArrowDown',
    'androidx.compose.animation.AnimatedVisibility',
    'androidx.compose.foundation.rememberScrollState',
    'androidx.compose.foundation.verticalScroll',
    'androidx.compose.foundation.border',
    'androidx.compose.foundation.shape.CircleShape',
    'androidx.compose.ui.draw.drawBehind',
    'androidx.compose.ui.draw.rotate',
    'androidx.compose.ui.geometry.Offset',
    'androidx.compose.foundation.layout.height',
    'androidx.compose.foundation.layout.heightIn',
    'androidx.compose.runtime.rememberCoroutineScope',
    'androidx.compose.runtime.snapshotFlow',
    'androidx.compose.ui.graphics.Brush',
    'kotlinx.coroutines.launch',
]
missing = [i for i in need if not re.search(r'^import ' + re.escape(i) + r'$', s, re.M)]
if missing:
    i = s.index('\nimport ') + 1
    s = s[:i] + ''.join(f'import {m}\n' for m in missing) + s[i:]
p.write_text(s)
print('ok, added imports:', missing)
