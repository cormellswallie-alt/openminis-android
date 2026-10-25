"""[T-spectra-v5-bubble] User message bubble returns (Appearance switch, default
on) with seven tones; switch off keeps the v3 "question as heading"."""
import pathlib
root = pathlib.Path('src/android/app/src/main')
ui = root / 'java/com/openminis/app/ui'


def sub(path, old, new, count=1):
    s = path.read_text()
    assert s.count(old) == count, (str(path), old[:70], s.count(old))
    path.write_text(s.replace(old, new))


p = ui / 'chat/ChatUserMessageUI.kt'
# --- read prefs once per row
sub(p, '''    var showMenu by remember { mutableStateOf(false) }
    val isQueued = message.isQueued
    val haptics = LocalHapticFeedback.current
''', '''    var showMenu by remember { mutableStateOf(false) }
    val isQueued = message.isQueued
    val haptics = LocalHapticFeedback.current
    // [T-spectra-v5-bubble] Bubble (default) vs v3 heading; seven tones.
    val bubbleMode by com.openminis.app.ui.settings.rememberAppearanceFlag(
        com.openminis.app.ui.settings.KEY_USER_BUBBLE, true,
    )
    val bubbleToneKey by com.openminis.app.ui.settings.rememberAppearanceString(
        com.openminis.app.ui.settings.KEY_USER_BUBBLE_TONE, "mist",
    )
    val bubbleTone = com.openminis.app.ui.settings.UserBubbleTone.of(bubbleToneKey)
''')
sub(p, '''            .padding(start = 2.dp, top = if (precededByUser) 16.dp else 18.dp, bottom = 6.dp),''',
    '''            .padding(
                start = if (bubbleMode) 0.dp else 2.dp,
                top = if (bubbleMode) (if (precededByUser) 10.dp else 16.dp)
                else (if (precededByUser) 16.dp else 18.dp),
                bottom = 6.dp,
            ),''')
sub(p, '''        val bubbleMaxWidth = this.maxWidth
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(''', '''        val bubbleMaxWidth = if (bubbleMode) this.maxWidth * 0.8f else this.maxWidth
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (bubbleMode) Spacer(modifier = Modifier.weight(1f).widthIn(min = 60.dp))
        Box(''')
sub(p, '''            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {''', '''            Column(
                horizontalAlignment = if (bubbleMode) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(if (bubbleMode) 6.dp else 8.dp),
            ) {''')
sub(p, '''                        nonImageUris = message.attachmentUris,
                        onPreviewFile = onPreviewFile,
                    )''', '''                        nonImageUris = message.attachmentUris,
                        onPreviewFile = onPreviewFile,
                        alignEnd = bubbleMode,
                    )''')
sub(p, '''                        val textColor = if (isQueued) secondaryTextColor else ChatColors.userBubbleText
                        @Suppress("UNUSED_VARIABLE") val legacyBubble = userBubbleColor
                        val bubbleBg = Color.Transparent''', '''                        val dark = ChatColors.isDark
                        @Suppress("UNUSED_VARIABLE") val legacyBubble = userBubbleColor
                        val textColor = when {
                            isQueued -> secondaryTextColor
                            bubbleMode -> bubbleTone.text(dark)
                            else -> ChatColors.userBubbleText
                        }
                        val bubbleBg = if (bubbleMode && !isQueued) bubbleTone.fill(dark) else Color.Transparent''')
sub(p, '''                                val isShort = message.content.length <= 160 &&
                                    message.content.count { it == '\\n' } < 4''', '''                                val isShort = !bubbleMode && message.content.length <= 160 &&
                                    message.content.count { it == '\\n' } < 4''')
sub(p, '''                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = if (isShort) 19.sp else 16.sp,
                                        lineHeight = if (isShort) 28.sp else 25.sp,
                                        fontWeight = if (isShort) androidx.compose.ui.text.font.FontWeight.SemiBold
                                        else androidx.compose.ui.text.font.FontWeight.Medium,''', '''                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = if (bubbleMode) 16.5.sp else if (isShort) 19.sp else 16.sp,
                                        lineHeight = if (bubbleMode) 24.sp else if (isShort) 28.sp else 25.sp,
                                        fontWeight = if (bubbleMode) androidx.compose.ui.text.font.FontWeight.Normal
                                        else if (isShort) androidx.compose.ui.text.font.FontWeight.SemiBold
                                        else androidx.compose.ui.text.font.FontWeight.Medium,''')
sub(p, '''                                        .padding(
                                            horizontal = if (isQueued) 14.dp else 2.dp,
                                            vertical = if (isQueued) 10.dp else 2.dp,
                                        ),''', '''                                        .padding(
                                            horizontal = if (isQueued || bubbleMode) 16.dp else 2.dp,
                                            vertical = if (isQueued || bubbleMode) 10.dp else 2.dp,
                                        ),''')
sub(p, '''    onPreviewFile: (Uri, String) -> Unit = { _, _ -> },
) {
    var previewImageIndex by remember { mutableStateOf<Int?>(null) }''', '''    onPreviewFile: (Uri, String) -> Unit = { _, _ -> },
    // [T-spectra-v5-bubble] Right-aligned above a bubble, left under a heading.
    alignEnd: Boolean = false,
) {
    var previewImageIndex by remember { mutableStateOf<Int?>(null) }''')
sub(p, '''        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),''',
    '''        horizontalArrangement = Arrangement.spacedBy(6.dp, if (alignEnd) Alignment.End else Alignment.Start),''')
s = p.read_text()
for imp in ['androidx.compose.runtime.getValue', 'androidx.compose.foundation.layout.Spacer',
            'androidx.compose.foundation.layout.widthIn']:
    if f'import {imp}\n' not in s:
        i = s.index('\nimport ') + 1
        s = s[:i] + f'import {imp}\n' + s[i:]
p.write_text(s)

# --- Appearance: new section before the thinking card section
p = ui / 'settings/AppearanceScreen.kt'
sub(p, '''    var thinkingItalic by remember { mutableStateOf(prefs.getBoolean(KEY_THINKING_ITALIC, false)) }
''', '''    var thinkingItalic by remember { mutableStateOf(prefs.getBoolean(KEY_THINKING_ITALIC, false)) }
    var userBubble by remember { mutableStateOf(prefs.getBoolean(KEY_USER_BUBBLE, true)) }
    var userBubbleTone by remember {
        mutableStateOf(UserBubbleTone.of(prefs.getString(KEY_USER_BUBBLE_TONE, "mist")))
    }
''')
sub(p, '''        // [T-spectra-v4-thought] -- Thinking Cards --
        SettingsSection(''', '''        // [T-spectra-v5-bubble] -- Your messages --
        SettingsSection(
            header = stringResource(R.string.user_bubble_section),
            footer = stringResource(R.string.user_bubble_footer),
        ) {
            SettingsSwitchRow(
                icon = androidx.compose.material.icons.Icons.Outlined.ChatBubbleOutline,
                iconColor = tileBlue,
                title = stringResource(R.string.user_bubble_title),
                checked = userBubble,
                onCheckedChange = {
                    userBubble = it
                    prefs.edit().putBoolean(KEY_USER_BUBBLE, it).apply()
                },
            )
            UserBubbleTonePicker(
                selected = userBubbleTone,
                enabled = userBubble,
                onSelect = {
                    userBubbleTone = it
                    prefs.edit().putString(KEY_USER_BUBBLE_TONE, it.key).apply()
                },
            )
        }

        // [T-spectra-v4-thought] -- Thinking Cards --
        SettingsSection(''')
s = p.read_text()
imp = 'import androidx.compose.material.icons.outlined.ChatBubbleOutline\n'
if imp not in s:
    i = s.index('\nimport ') + 1
    s = s[:i] + imp + s[i:]
p.write_text(s)

# --- strings
K = ['user_bubble_section', 'user_bubble_title', 'user_bubble_footer', 'user_bubble_preview',
     'user_bubble_tone_mist', 'user_bubble_tone_ink', 'user_bubble_tone_iris', 'user_bubble_tone_ocean',
     'user_bubble_tone_jade', 'user_bubble_tone_rose', 'user_bubble_tone_coral']
T = {
 '': ('Your messages', 'Message bubble', 'Off: your message reads as a left-aligned heading with no bubble.', 'Help me tidy up this week\'s notes',
      'Mist', 'Ink', 'Iris', 'Ocean', 'Jade', 'Rose', 'Coral'),
 'zh': ('我的消息', '消息气泡', '关闭后，你发的消息不带气泡，左对齐显示为标题。', '帮我整理一下这周的笔记',
        '雾灰', '墨黑', '鸢尾紫', '海蓝', '青玉', '玫瑰', '珊瑚'),
 'zh-rTW': ('我的訊息', '訊息泡泡', '關閉後，你傳送的訊息不帶泡泡，靠左顯示為標題。', '幫我整理一下這週的筆記',
            '霧灰', '墨黑', '鳶尾紫', '海藍', '青玉', '玫瑰', '珊瑚'),
 'ja': ('自分のメッセージ', 'メッセージの吹き出し', 'オフにすると、送信したメッセージは吹き出しなしで左寄せの見出しになります。', '今週のメモを整理して',
        'ミスト', 'インク', 'アイリス', 'オーシャン', 'ジェイド', 'ローズ', 'コーラル'),
 'ko': ('내 메시지', '메시지 말풍선', '끄면 보낸 메시지가 말풍선 없이 왼쪽 정렬 제목으로 표시됩니다.', '이번 주 메모 정리해 줘',
        '미스트', '잉크', '아이리스', '오션', '제이드', '로즈', '코랄'),
 'de': ('Deine Nachrichten', 'Nachrichtenblase', 'Aus: Deine Nachricht erscheint linksbündig als Überschrift ohne Blase.', 'Hilf mir, die Notizen dieser Woche zu ordnen',
        'Nebel', 'Tinte', 'Iris', 'Ozean', 'Jade', 'Rose', 'Koralle'),
 'es': ('Tus mensajes', 'Burbuja de mensaje', 'Desactivado: tu mensaje se muestra como título alineado a la izquierda, sin burbuja.', 'Ayúdame a ordenar las notas de esta semana',
        'Niebla', 'Tinta', 'Iris', 'Océano', 'Jade', 'Rosa', 'Coral'),
 'fil': ('Mga mensahe mo', 'Bubble ng mensahe', 'Kapag naka-off: lalabas ang mensahe mo bilang heading sa kaliwa, walang bubble.', 'Tulungan mo akong ayusin ang mga tala ngayong linggo',
         'Mist', 'Ink', 'Iris', 'Ocean', 'Jade', 'Rose', 'Coral'),
 'fr': ('Vos messages', 'Bulle de message', 'Désactivé : votre message s\'affiche comme un titre aligné à gauche, sans bulle.', 'Aide-moi à ranger mes notes de la semaine',
        'Brume', 'Encre', 'Iris', 'Océan', 'Jade', 'Rose', 'Corail'),
 'hr': ('Vaše poruke', 'Oblačić poruke', 'Isključeno: poruka se prikazuje kao naslov poravnat lijevo, bez oblačića.', 'Pomozi mi srediti bilješke ovog tjedna',
        'Magla', 'Tinta', 'Iris', 'Ocean', 'Žad', 'Ruža', 'Koralj'),
 'in': ('Pesan Anda', 'Gelembung pesan', 'Nonaktif: pesan Anda tampil sebagai judul rata kiri tanpa gelembung.', 'Bantu rapikan catatan minggu ini',
        'Kabut', 'Tinta', 'Iris', 'Samudra', 'Giok', 'Mawar', 'Koral'),
 'ms': ('Mesej anda', 'Gelembung mesej', 'Mati: mesej anda dipaparkan sebagai tajuk sejajar kiri tanpa gelembung.', 'Bantu kemas nota minggu ini',
        'Kabus', 'Dakwat', 'Iris', 'Lautan', 'Jed', 'Mawar', 'Karang'),
 'pl': ('Twoje wiadomości', 'Dymek wiadomości', 'Wyłączone: wiadomość wyświetla się jako nagłówek wyrównany do lewej, bez dymka.', 'Pomóż mi uporządkować notatki z tego tygodnia',
        'Mgła', 'Atrament', 'Iris', 'Ocean', 'Jadeit', 'Róża', 'Koral'),
 'pt-rBR': ('Suas mensagens', 'Balão de mensagem', 'Desativado: sua mensagem aparece como título alinhado à esquerda, sem balão.', 'Me ajude a organizar as notas desta semana',
            'Névoa', 'Tinta', 'Íris', 'Oceano', 'Jade', 'Rosa', 'Coral'),
 'ro': ('Mesajele tale', 'Balon de mesaj', 'Dezactivat: mesajul apare ca titlu aliniat la stânga, fără balon.', 'Ajută-mă să ordonez notițele săptămânii',
        'Ceață', 'Cerneală', 'Iris', 'Ocean', 'Jad', 'Trandafir', 'Coral'),
 'ru': ('Ваши сообщения', 'Пузырь сообщения', 'Выкл.: сообщение показывается как заголовок слева, без пузыря.', 'Помоги разобрать заметки за неделю',
        'Туман', 'Чернила', 'Ирис', 'Океан', 'Нефрит', 'Роза', 'Коралл'),
 'th': ('ข้อความของคุณ', 'บับเบิลข้อความ', 'ปิด: ข้อความของคุณจะแสดงเป็นหัวข้อชิดซ้ายโดยไม่มีบับเบิล', 'ช่วยจัดโน้ตของสัปดาห์นี้หน่อย',
        'หมอก', 'หมึก', 'ไอริส', 'มหาสมุทร', 'หยก', 'กุหลาบ', 'ปะการัง'),
 'tr': ('Mesajların', 'Mesaj balonu', 'Kapalı: mesajın balonsuz, sola hizalı bir başlık olarak görünür.', 'Bu haftanın notlarını toplamama yardım et',
        'Sis', 'Mürekkep', 'İris', 'Okyanus', 'Yeşim', 'Gül', 'Mercan'),
}
n = 0
for loc, vals in T.items():
    f = root / 'res' / ('values' + (f'-{loc}' if loc else '')) / 'strings.xml'
    s = f.read_text()
    if 'name="user_bubble_section"' in s:
        continue
    esc = lambda v: v.replace("'", "\\'")
    add = ''.join(f'    <string name="{k}">{esc(v)}</string>\n' for k, v in zip(K, vals))
    i = s.rindex('</resources>')
    f.write_text(s[:i] + add + s[i:])
    n += 1
print('ok, locales', n)
