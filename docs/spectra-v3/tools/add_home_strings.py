"""[T-spectra-v3] Home hero strings (all 18 locales)."""
import pathlib
K = ['spectra_greet_morning', 'spectra_greet_noon', 'spectra_greet_afternoon', 'spectra_greet_evening',
     'spectra_home_question', 'spectra_home_sessions', 'spectra_home_scheduled']
T = {
 '':      ('Good morning', 'Good afternoon', 'Good afternoon', 'Good evening', 'What shall we do today?', '%1$d chats', '%1$d scheduled'),
 'zh':    ('早上好', '中午好', '下午好', '晚上好', '今天想让我做点什么？', '%1$d 个对话', '%1$d 个定时任务'),
 'zh-rTW':('早安', '午安', '午安', '晚安', '今天想讓我做點什麼？', '%1$d 個對話', '%1$d 個排程任務'),
 'ja':    ('おはようございます', 'こんにちは', 'こんにちは', 'こんばんは', '今日は何をしましょう？', '%1$d 件の会話', '%1$d 件の予約'),
 'ko':    ('좋은 아침이에요', '좋은 오후예요', '좋은 오후예요', '좋은 저녁이에요', '오늘은 무엇을 할까요?', '대화 %1$d개', '예약 %1$d개'),
 'de':    ('Guten Morgen', 'Guten Tag', 'Guten Tag', 'Guten Abend', 'Was machen wir heute?', '%1$d Chats', '%1$d geplant'),
 'es':    ('Buenos días', 'Buenas tardes', 'Buenas tardes', 'Buenas noches', '¿Qué hacemos hoy?', '%1$d chats', '%1$d programadas'),
 'fil':   ('Magandang umaga', 'Magandang tanghali', 'Magandang hapon', 'Magandang gabi', 'Ano ang gagawin natin ngayon?', '%1$d chat', '%1$d naka-iskedyul'),
 'fr':    ('Bonjour', 'Bonjour', 'Bon après-midi', 'Bonsoir', 'Que faisons-nous aujourd’hui ?', '%1$d discussions', '%1$d planifiées'),
 'hr':    ('Dobro jutro', 'Dobar dan', 'Dobar dan', 'Dobra večer', 'Što ćemo danas?', '%1$d razgovora', '%1$d zakazano'),
 'in':    ('Selamat pagi', 'Selamat siang', 'Selamat sore', 'Selamat malam', 'Apa yang kita kerjakan hari ini?', '%1$d obrolan', '%1$d terjadwal'),
 'ms':    ('Selamat pagi', 'Selamat tengah hari', 'Selamat petang', 'Selamat malam', 'Apa kita nak buat hari ini?', '%1$d sembang', '%1$d berjadual'),
 'pl':    ('Dzień dobry', 'Dzień dobry', 'Dzień dobry', 'Dobry wieczór', 'Co dziś robimy?', 'Czaty: %1$d', 'Zaplanowane: %1$d'),
 'pt-rBR':('Bom dia', 'Boa tarde', 'Boa tarde', 'Boa noite', 'O que vamos fazer hoje?', '%1$d conversas', '%1$d agendadas'),
 'ro':    ('Bună dimineața', 'Bună ziua', 'Bună ziua', 'Bună seara', 'Ce facem azi?', '%1$d conversații', '%1$d programate'),
 'ru':    ('Доброе утро', 'Добрый день', 'Добрый день', 'Добрый вечер', 'Чем займёмся сегодня?', 'Чатов: %1$d', 'Запланировано: %1$d'),
 'th':    ('สวัสดีตอนเช้า', 'สวัสดีตอนกลางวัน', 'สวัสดีตอนบ่าย', 'สวัสดีตอนเย็น', 'วันนี้ให้ช่วยอะไรดี?', '%1$d แชต', '%1$d ตั้งเวลา'),
 'tr':    ('Günaydın', 'İyi günler', 'İyi günler', 'İyi akşamlar', 'Bugün ne yapalım?', '%1$d sohbet', '%1$d zamanlanmış'),
}
base = pathlib.Path('src/android/app/src/main/res')
for loc, vals in T.items():
    p = base / ('values' + ('-' + loc if loc else '')) / 'strings.xml'
    s = p.read_text()
    esc = lambda v: v.replace("'", "\\'").replace('?', '?')
    add = ''.join(f'    <string name="{k}">{esc(v)}</string>\n' for k, v in zip(K, vals) if f'name="{k}"' not in s)
    if add:
        i = s.rindex('</resources>')
        p.write_text(s[:i] + '    <!-- [T-spectra-v3] home hero -->\n' + add + s[i:])
print('ok', len(T))
