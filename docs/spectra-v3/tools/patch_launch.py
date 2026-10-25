"""[T-spectra-v3] Replace NoirLaunchOverlay with the "Ignition" sequence and
swap the API 31+ splash icon to a hairline ring (the overlay's first frame)."""
import pathlib
here = pathlib.Path(__file__).parent
res = pathlib.Path('src/android/app/src/main/res')
p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/theme/NoirLaunch.kt')
s = p.read_text()

a = s.index('@Composable\nfun NoirLaunchOverlay(')
b = s.index('/**\n * The Minis pearl:')
s = s[:a] + (here / 'launch_overlay.kt.txt').read_text() + '\n' + s[b:]

s = s.replace('    internal const val TOTAL_MS = 1450\n    internal const val EXIT = 0.74f',
              '    internal const val TOTAL_MS = 1700\n    internal const val EXIT = 0.74f')
for imp in ['androidx.compose.foundation.layout.Row',
            'androidx.compose.ui.graphics.StrokeCap',
            'androidx.compose.ui.graphics.drawscope.Stroke',
            'androidx.compose.ui.graphics.drawscope.rotate',
            'androidx.compose.ui.graphics.drawscope.scale']:
    if f'import {imp}\n' not in s:
        s = s.replace('import androidx.compose.ui.unit.dp\n', f'import {imp}\nimport androidx.compose.ui.unit.dp\n', 1)
p.write_text(s)

# Splash icon: hairline ring, 72dp ring track centred on the 288dp canvas.
(res / 'drawable' / 'noir_splash_pearl.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<!-- [T-spectra-v3] Android 12+ system splash icon: a single hairline circle at
     window centre. It is the exact first frame of NoirLaunchOverlay
     ("Ignition"), which then draws the spectrum ring along this track.
     Ring track radius 31.7dp = 72dp orb minus half the 0.24r ring width. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="288dp"
    android:height="288dp"
    android:viewportWidth="288"
    android:viewportHeight="288">
    <path
        android:pathData="M144,112.3 a31.7,31.7 0 1,1 0,63.4 a31.7,31.7 0 1,1 0,-63.4 Z"
        android:strokeColor="@color/spectra_splash_ring"
        android:strokeWidth="1"
        android:fillColor="#00000000" />
</vector>
''')

def set_color(path, name, value, comment):
    t = path.read_text()
    if f'name="{name}"' in t:
        import re
        t = re.sub(r'(<color name="%s">)[^<]+' % name, r'\g<1>' + value, t)
    else:
        t = t.replace('</resources>', f'    <!-- {comment} -->\n    <color name="{name}">{value}</color>\n</resources>')
    path.write_text(t)

set_color(res / 'values' / 'colors.xml', 'spectra_splash_ring', '#1F0A0A0B', '[T-spectra-v3] splash hairline')
set_color(res / 'values-night' / 'colors.xml', 'spectra_splash_ring', '#33FFFFFF', '[T-spectra-v3] splash hairline')
set_color(res / 'values-night' / 'colors.xml', 'noir_window_bg', '#FF0A0A0B', '')
print('ok')
