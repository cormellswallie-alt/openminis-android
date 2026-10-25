#!/usr/bin/env python3
"""Insert the step-timeline strings into every locale, before </resources>.
Idempotent. Usage: add_step_strings.py <res-dir>"""
import os
import sys

RES = sys.argv[1]

# (steps done "%1$d", failed "%1$d", thinking label)
T = {
    "values": ("%1$d steps", "%1$d failed", "Thinking"),
    "values-zh": ("已完成 %1$d 个步骤", "%1$d 个失败", "思考"),
    "values-zh-rTW": ("已完成 %1$d 個步驟", "%1$d 個失敗", "思考"),
    "values-ja": ("%1$d ステップ完了", "%1$d 件失敗", "思考"),
    "values-ko": ("%1$d단계 완료", "%1$d개 실패", "생각"),
    "values-de": ("%1$d Schritte", "%1$d fehlgeschlagen", "Denken"),
    "values-es": ("%1$d pasos", "%1$d con error", "Razonamiento"),
    "values-fr": ("%1$d étapes", "%1$d en échec", "Réflexion"),
    "values-pt-rBR": ("%1$d etapas", "%1$d com falha", "Raciocínio"),
    "values-ru": ("Шагов: %1$d", "С ошибкой: %1$d", "Размышление"),
    "values-pl": ("Kroki: %1$d", "Nieudane: %1$d", "Myślenie"),
    "values-ro": ("%1$d pași", "%1$d eșuați", "Gândire"),
    "values-hr": ("Koraka: %1$d", "Neuspjelo: %1$d", "Razmišljanje"),
    "values-tr": ("%1$d adım", "%1$d başarısız", "Düşünme"),
    "values-in": ("%1$d langkah", "%1$d gagal", "Berpikir"),
    "values-ms": ("%1$d langkah", "%1$d gagal", "Berfikir"),
    "values-th": ("%1$d ขั้นตอน", "ล้มเหลว %1$d", "กำลังคิด"),
    "values-fil": ("%1$d hakbang", "%1$d nabigo", "Pag-iisip"),
}


def esc(t):
    return t.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'").replace('"', '\\"')


for d, (done, failed, think) in T.items():
    p = os.path.join(RES, d, "strings.xml")
    if not os.path.exists(p):
        print("skip (missing)", d)
        continue
    s = open(p, encoding="utf-8").read()
    if 'name="noir_steps_done"' in s:
        print("already", d)
        continue
    block = (
        f'    <string name="noir_steps_done">{esc(done)}</string>\n'
        f'    <string name="noir_steps_failed">{esc(failed)}</string>\n'
        f'    <string name="noir_step_thinking">{esc(think)}</string>\n'
    )
    i = s.rfind("</resources>")
    assert i > 0, d
    s = s[:i] + block + s[i:]
    open(p, "w", encoding="utf-8").write(s)
    print("ok", d)
