"""[T-spectra-v5-thought] Static, Latin-only Newsreader cuts for thinking text.

Source: google/fonts ofl/newsreader (OFL 1.1), variable [opsz,wght].
Pinned at opsz 16 (text size) so it reads crisp at 15-16sp on phones.
Usage: python3 build_newsreader.py <dir with Newsreader.ttf + Newsreader-Italic.ttf>
"""
import sys, pathlib
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset

src = pathlib.Path(sys.argv[1])
out = pathlib.Path('src/android/app/src/main/res/font')
UNI = ("U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,"
       "U+2000-206F,U+2074,U+20AC,U+2122,U+2190-2199,U+2212,U+2215,U+2248,U+2260,U+2264-2265")
cuts = [
    ('Newsreader.ttf', 400, 'newsreader_regular.ttf'),
    ('Newsreader.ttf', 500, 'newsreader_medium.ttf'),
    ('Newsreader-Italic.ttf', 400, 'newsreader_italic.ttf'),
]
for f, w, name in cuts:
    font = TTFont(src / f)
    inst = instantiateVariableFont(font, {'wght': w, 'opsz': 16}, updateFontNames=False)
    opts = subset.Options()
    opts.layout_features = ['kern', 'liga', 'calt', 'onum', 'pnum', 'lnum', 'tnum']
    opts.name_IDs = ['*']
    opts.notdef_outline = True
    opts.hinting = False
    ss = subset.Subsetter(opts)
    ss.populate(unicodes=subset.parse_unicodes(UNI))
    ss.subset(inst)
    inst.save(out / name)
    print(name, (out / name).stat().st_size)
