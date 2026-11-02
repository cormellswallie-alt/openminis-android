"""[T-claude-ui] Static serif cuts for assistant prose and display numbers.

Source: google/fonts ofl/notoserifsc (OFL 1.1), variable [wght].
Noto Serif SC's Latin is the Source Serif design, so one family covers the
design's 'Source Serif 4', 'Noto Serif SC' stack.

Subset: GB2312 hanzi + every glyph in the zh / zh-TW string tables + Latin,
Greek, general punctuation, CJK punctuation, full-width forms. Anything else
falls back to the system face.

Usage (repo root): python3 docs/claude-ui/tools/build_serif.py <NotoSerifSC[wght].ttf>
"""
import sys, pathlib, re
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset

src = pathlib.Path(sys.argv[1])
res = pathlib.Path('src/android/app/src/main/res')
out = res / 'font'

chars = set()
for cp in range(0x4E00, 0xA000):
    ch = chr(cp)
    try:
        ch.encode('gb2312')
        chars.add(cp)
    except UnicodeEncodeError:
        pass
for d in ('values-zh', 'values-zh-rCN', 'values-zh-rTW', 'values-zh-rHK', 'values-b+zh+Hans', 'values-b+zh+Hant'):
    p = res / d / 'strings.xml'
    if p.exists():
        chars.update(ord(c) for c in p.read_text(encoding='utf-8'))
ranges = [(0x20, 0x7E), (0xA0, 0x24F), (0x370, 0x3FF), (0x2000, 0x206F), (0x2190, 0x21FF),
          (0x2200, 0x22FF), (0x2460, 0x24FF), (0x25A0, 0x25FF), (0x3000, 0x303F), (0xFF00, 0xFFEF)]
for a, b in ranges:
    chars.update(range(a, b + 1))

cuts = [(400, 'om_serif_regular.ttf'), (600, 'om_serif_semibold.ttf')]
for w, name in cuts:
    font = TTFont(src)
    inst = instantiateVariableFont(font, {'wght': w}, updateFontNames=False)
    opts = subset.Options()
    opts.layout_features = ['kern', 'liga', 'calt', 'pnum', 'lnum', 'tnum', 'locl', 'vert', 'halt', 'palt']
    opts.name_IDs = ['*']
    opts.notdef_outline = True
    opts.hinting = False
    ss = subset.Subsetter(opts)
    ss.populate(unicodes=sorted(chars))
    ss.subset(inst)
    inst.save(out / name)
    print(name, (out / name).stat().st_size)
