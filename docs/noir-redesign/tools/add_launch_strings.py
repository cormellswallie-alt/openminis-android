#!/usr/bin/env python3
"""Insert the Noir launch-animation strings into every locale, right after
the existing appearance_keep_awake_title entry. Idempotent."""
import os
import sys

RES = sys.argv[1]
ANCHOR = 'name="appearance_keep_awake_title"'

# (section header, title, footer)
T = {
    "values": ("Launch Animation", "Launch Animation",
               "Play the pearl and wordmark sequence when Minis opens from the launcher. Never shown for links, shares or shortcuts."),
    "values-zh": ("开屏动画", "开屏动画",
                  "从桌面图标冷启动时播放珍珠与字标动画。通过链接、分享或快捷方式打开时不会播放。"),
    "values-zh-rTW": ("開屏動畫", "開屏動畫",
                      "從桌面圖示冷啟動時播放珍珠與字標動畫。透過連結、分享或捷徑開啟時不會播放。"),
    "values-ja": ("起動アニメーション", "起動アニメーション",
                  "ホーム画面から起動したときにパールとロゴのアニメーションを再生します。リンク・共有・ショートカットからの起動では表示されません。"),
    "values-ko": ("시작 애니메이션", "시작 애니메이션",
                  "홈 화면에서 앱을 열 때 진주와 워드마크 애니메이션을 재생합니다. 링크, 공유, 바로가기로 열 때는 표시되지 않습니다."),
    "values-de": ("Startanimation", "Startanimation",
                  "Spielt beim Öffnen über das Startbildschirm-Symbol die Perlen- und Schriftzug-Animation ab. Nie bei Links, Freigaben oder Verknüpfungen."),
    "values-es": ("Animación de inicio", "Animación de inicio",
                  "Reproduce la animación de la perla y el logotipo al abrir Minis desde el icono. Nunca se muestra al abrir enlaces, contenido compartido o accesos directos."),
    "values-fr": ("Animation de démarrage", "Animation de démarrage",
                  "Lit l’animation de la perle et du logo à l’ouverture depuis l’icône. Jamais affichée pour les liens, partages ou raccourcis."),
    "values-pt-rBR": ("Animação de abertura", "Animação de abertura",
                      "Reproduz a animação da pérola e do logotipo ao abrir o Minis pelo ícone. Nunca aparece ao abrir links, compartilhamentos ou atalhos."),
    "values-ru": ("Анимация запуска", "Анимация запуска",
                  "Показывать анимацию жемчужины и логотипа при запуске Minis со значка. Не показывается при открытии ссылок, «Поделиться» и ярлыков."),
    "values-pl": ("Animacja startowa", "Animacja startowa",
                  "Odtwarza animację perły i logo przy otwieraniu Minis z ikony. Nigdy przy linkach, udostępnianiu ani skrótach."),
    "values-ro": ("Animație la pornire", "Animație la pornire",
                  "Redă animația perlei și a siglei când Minis este deschis din pictogramă. Nu apare la linkuri, partajări sau comenzi rapide."),
    "values-hr": ("Animacija pokretanja", "Animacija pokretanja",
                  "Reproducira animaciju bisera i logotipa pri otvaranju Minisa s ikone. Nikad za poveznice, dijeljenje ili prečace."),
    "values-tr": ("Açılış animasyonu", "Açılış animasyonu",
                  "Minis simgeden açıldığında inci ve logo animasyonunu oynatır. Bağlantı, paylaşım veya kısayollarla açılışta gösterilmez."),
    "values-in": ("Animasi pembuka", "Animasi pembuka",
                  "Putar animasi mutiara dan logo saat Minis dibuka dari ikon. Tidak ditampilkan saat membuka tautan, berbagi, atau pintasan."),
    "values-ms": ("Animasi pembukaan", "Animasi pembukaan",
                  "Mainkan animasi mutiara dan logo apabila Minis dibuka daripada ikon. Tidak dipaparkan untuk pautan, perkongsian atau pintasan."),
    "values-th": ("แอนิเมชันเปิดแอป", "แอนิเมชันเปิดแอป",
                  "เล่นแอนิเมชันไข่มุกและโลโก้เมื่อเปิด Minis จากไอคอน จะไม่แสดงเมื่อเปิดจากลิงก์ การแชร์ หรือทางลัด"),
    "values-fil": ("Animation sa pagbukas", "Animation sa pagbukas",
                   "I-play ang animation ng perlas at logo kapag binuksan ang Minis mula sa icon. Hindi ipinapakita kapag binuksan mula sa link, share o shortcut."),
}


def esc(t):
    return t.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'").replace('"', '\\"')


for d, (sec, title, foot) in T.items():
    p = os.path.join(RES, d, "strings.xml")
    if not os.path.exists(p):
        print("skip (missing)", d)
        continue
    s = open(p, encoding="utf-8").read()
    if 'name="appearance_section_launch_animation"' in s:
        print("already", d)
        continue
    i = s.find(ANCHOR)
    if i < 0:
        print("NO ANCHOR", d)
        continue
    eol = s.index("\n", i) + 1
    block = (
        '    <string name="appearance_section_launch_animation">%s</string>\n'
        '    <string name="appearance_launch_animation_title">%s</string>\n'
        '    <string name="appearance_launch_animation_footer">%s</string>\n'
    ) % (esc(sec), esc(title), esc(foot))
    open(p, "w", encoding="utf-8").write(s[:eol] + block + s[eol:])
    print("ok", d)
