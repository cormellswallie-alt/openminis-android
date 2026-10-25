"""[T-spectra-v5-italic] Appearance switch: set English thinking in Newsreader Italic."""
import pathlib
root = pathlib.Path('src/android/app/src/main')
ui = root / 'java/com/openminis/app/ui'


def sub(path, old, new, count=1):
    s = path.read_text()
    assert s.count(old) == count, (path, old[:60], s.count(old))
    path.write_text(s.replace(old, new))


# 1) pref key
sub(ui / 'settings/AppearanceFlags.kt',
    'const val KEY_THINKING_AUTO_TRANSLATE = "chat.thinkingAutoTranslate"  // Boolean, default false\n',
    'const val KEY_THINKING_AUTO_TRANSLATE = "chat.thinkingAutoTranslate"  // Boolean, default false\n\n'
    '// [T-spectra-v5-italic] Thinking body in Newsreader Italic (English text;\n'
    '// MiSans has no italic, so Chinese stays upright rather than faux-slanted).\n'
    'const val KEY_THINKING_ITALIC = "chat.thinkingItalic"  // Boolean, default false\n')

# 2) serif style follows the switch; code spans stay upright
sub(ui / 'chat/ThoughtMotion.kt',
    '''internal fun thoughtSerifStyle(): TextStyle = androidx.compose.material3.LocalTextStyle.current.merge(
    fontFamily = ThoughtSerif,
''',
    '''internal fun thoughtSerifStyle(): TextStyle = androidx.compose.material3.LocalTextStyle.current.merge(
    fontFamily = ThoughtSerif,
    fontStyle = if (
        com.openminis.app.ui.settings.rememberAppearanceFlag(
            com.openminis.app.ui.settings.KEY_THINKING_ITALIC, false,
        ).value
    ) FontStyle.Italic else FontStyle.Normal,
''')
sub(ui / 'chat/ThoughtMotion.kt',
    '''                fontSize = if (serif) 0.82.em else 0.9.em,
                background = codeBg,''',
    '''                fontSize = if (serif) 0.82.em else 0.9.em,
                fontStyle = FontStyle.Normal,
                background = codeBg,''')

# 3) settings row (between "fold tools" and "auto-translate")
p = ui / 'settings/AppearanceScreen.kt'
sub(p,
    '    var thinkingAutoTranslate by remember { mutableStateOf(prefs.getBoolean(KEY_THINKING_AUTO_TRANSLATE, false)) }\n',
    '    var thinkingAutoTranslate by remember { mutableStateOf(prefs.getBoolean(KEY_THINKING_AUTO_TRANSLATE, false)) }\n'
    '    var thinkingItalic by remember { mutableStateOf(prefs.getBoolean(KEY_THINKING_ITALIC, false)) }\n')
sub(p,
    '''                    prefs.edit().putBoolean(KEY_THOUGHT_CARDS, it).apply()
                },
            )
''',
    '''                    prefs.edit().putBoolean(KEY_THOUGHT_CARDS, it).apply()
                },
            )
            SettingsSwitchRow(
                icon = androidx.compose.material.icons.Icons.Outlined.FormatItalic,
                iconColor = tilePurple,
                title = stringResource(R.string.appearance_thinking_italic_title),
                subtitle = stringResource(R.string.appearance_thinking_italic_subtitle),
                checked = thinkingItalic,
                onCheckedChange = {
                    thinkingItalic = it
                    prefs.edit().putBoolean(KEY_THINKING_ITALIC, it).apply()
                },
            )
''')
s = p.read_text()
imp = 'import androidx.compose.material.icons.outlined.FormatItalic\n'
if imp not in s:
    i = s.index('\nimport ') + 1
    p.write_text(s[:i] + imp + s[i:])

# 4) strings
T = {
    '': ('Italic thinking', 'Set English thinking in Newsreader Italic. Chinese stays upright.'),
    'zh': ('思考使用斜体', '英文思考改用 Newsreader 斜体，中文保持正体。'),
    'zh-rTW': ('思考使用斜體', '英文思考改用 Newsreader 斜體，中文保持正體。'),
    'ja': ('思考を斜体で表示', '英語の思考を Newsreader Italic で表示します。日本語・中国語は正体のままです。'),
    'ko': ('생각을 기울임꼴로', '영어 생각을 Newsreader Italic으로 표시합니다. 한중일 문자는 그대로입니다.'),
    'de': ('Denken kursiv', 'Englisches Denken in Newsreader Italic setzen. Chinesisch bleibt aufrecht.'),
    'es': ('Razonamiento en cursiva', 'Muestra el razonamiento en inglés con Newsreader Italic. El chino no cambia.'),
    'fil': ('Italic na pag-iisip', 'Ipakita ang pag-iisip sa Ingles gamit ang Newsreader Italic. Hindi nagbabago ang Chinese.'),
    'fr': ('Réflexion en italique', 'Affiche la réflexion en anglais en Newsreader Italic. Le chinois reste droit.'),
    'hr': ('Razmišljanje u kurzivu', 'Englesko razmišljanje u fontu Newsreader Italic. Kineski ostaje uspravan.'),
    'in': ('Pemikiran miring', 'Tampilkan pemikiran bahasa Inggris dengan Newsreader Italic. Teks Tionghoa tetap tegak.'),
    'ms': ('Pemikiran condong', 'Paparkan pemikiran bahasa Inggeris dalam Newsreader Italic. Teks Cina kekal tegak.'),
    'pl': ('Myślenie kursywą', 'Angielskie myślenie krojem Newsreader Italic. Chiński pozostaje prosty.'),
    'pt-rBR': ('Raciocínio em itálico', 'Mostra o raciocínio em inglês em Newsreader Italic. O chinês não muda.'),
    'ro': ('Gândire cu italic', 'Afișează gândirea în engleză cu Newsreader Italic. Chineza rămâne dreaptă.'),
    'ru': ('Рассуждения курсивом', 'Английские рассуждения шрифтом Newsreader Italic. Китайский остаётся прямым.'),
    'th': ('แสดงความคิดเป็นตัวเอียง', 'แสดงความคิดภาษาอังกฤษด้วย Newsreader Italic ส่วนภาษาจีนคงตัวตรง'),
    'tr': ('İtalik düşünme', 'İngilizce düşünmeyi Newsreader Italic ile göster. Çince dik kalır.'),
}
n = 0
for loc, (title, sub_) in T.items():
    f = root / 'res' / ('values' + (f'-{loc}' if loc else '')) / 'strings.xml'
    s = f.read_text()
    if 'name="appearance_thinking_italic_title"' in s:
        continue
    esc = lambda v: v.replace("'", "\\'")
    add = (f'    <string name="appearance_thinking_italic_title">{esc(title)}</string>\n'
           f'    <string name="appearance_thinking_italic_subtitle">{esc(sub_)}</string>\n')
    i = s.rindex('</resources>')
    f.write_text(s[:i] + add + s[i:])
    n += 1
print('ok, locales', n)
