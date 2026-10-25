"""[T-spectra-v3] Thinking-card strings for the remaining locales."""
import pathlib
T = {
 'de': ('Denkt nach', 'Denkprozess', '%1$d s nachgedacht', '%1$s Zeichen', 'Kopieren', 'Volltext'),
 'es': ('Pensando', 'Proceso de razonamiento', 'Pensó %1$d s', '%1$s caracteres', 'Copiar', 'Texto completo'),
 'fil': ('Nag-iisip', 'Proseso ng pag-iisip', 'Nag-isip nang %1$d s', '%1$s karakter', 'Kopyahin', 'Buong teksto'),
 'fr': ('Réflexion', 'Raisonnement', 'Réflexion : %1$d s', '%1$s caractères', 'Copier', 'Texte intégral'),
 'hr': ('Razmišlja', 'Tijek razmišljanja', 'Razmišljao %1$d s', '%1$s znakova', 'Kopiraj', 'Cijeli tekst'),
 'in': ('Berpikir', 'Proses berpikir', 'Berpikir %1$d dtk', '%1$s karakter', 'Salin', 'Teks lengkap'),
 'ko': ('생각 중', '사고 과정', '%1$d초 동안 생각함', '%1$s자', '복사', '전체 보기'),
 'ms': ('Sedang berfikir', 'Proses pemikiran', 'Berfikir %1$d saat', '%1$s aksara', 'Salin', 'Teks penuh'),
 'pl': ('Myśli', 'Proces myślenia', 'Myślał %1$d s', '%1$s znaków', 'Kopiuj', 'Pełny tekst'),
 'pt-rBR': ('Pensando', 'Raciocínio', 'Pensou por %1$d s', '%1$s caracteres', 'Copiar', 'Texto completo'),
 'ro': ('Se gândește', 'Proces de gândire', 'A gândit %1$d s', '%1$s caractere', 'Copiază', 'Text complet'),
 'ru': ('Думает', 'Ход рассуждений', 'Думал %1$d с', '%1$s симв.', 'Копировать', 'Полный текст'),
 'th': ('กำลังคิด', 'กระบวนการคิด', 'คิดอยู่ %1$d วินาที', '%1$s อักขระ', 'คัดลอก', 'ข้อความเต็ม'),
 'tr': ('Düşünüyor', 'Düşünme süreci', '%1$d sn düşündü', '%1$s karakter', 'Kopyala', 'Tam metin'),
}
K = ['spectra_thinking_live', 'spectra_thinking_done', 'spectra_thinking_done_secs',
     'spectra_thinking_chars', 'spectra_thinking_copy', 'spectra_thinking_full']
base = pathlib.Path('src/android/app/src/main/res')
for loc, vals in T.items():
    p = base / f'values-{loc}' / 'strings.xml'
    s = p.read_text()
    add = ''.join(f'    <string name="{k}">{v.replace(chr(39), chr(92) + chr(39))}</string>\n'
                  for k, v in zip(K, vals) if f'name="{k}"' not in s)
    if add:
        i = s.rindex('</resources>')
        p.write_text(s[:i] + '    <!-- [T-spectra-v3] thinking card -->\n' + add + s[i:])
print('ok')
