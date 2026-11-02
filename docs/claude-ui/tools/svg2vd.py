"""[T-claude-ui] Convert the design's provider logos (path-only SVG, 24x24)
into Android VectorDrawables: res/drawable/om_logo_<name>.xml.

Monochrome logos (no fill / currentColor) get fill #FF1F1E1C and are tinted at
runtime. Gradients (Gemini) are flattened to their first stop.
Usage: python3 svg2vd.py <logos-dir> <res/drawable>
"""
import re, sys, pathlib

src, out = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])


def color(v, grads):
    if v is None or v in ('currentColor', ''):
        return '#FF1F1E1C'
    m = re.match(r'url\(#([^)]+)\)', v)
    if m:
        return grads.get(m.group(1), '#FF1F1E1C')
    if v == 'none':
        return None
    v = v.strip()
    if re.fullmatch(r'#[0-9a-fA-F]{3}', v):
        v = '#' + ''.join(c * 2 for c in v[1:])
    if re.fullmatch(r'#[0-9a-fA-F]{6}', v):
        return '#FF' + v[1:].upper()
    return '#FF1F1E1C'


for f in sorted(src.glob('*.svg')):
    s = f.read_text()
    s = re.sub(r'<metadata>.*?</metadata>', '', s, flags=re.S)
    grads = {}
    for gid, body in re.findall(r'<linearGradient[^>]*id="([^"]+)"[^>]*>(.*?)</linearGradient>', s, flags=re.S):
        stop = re.search(r'stop-color="([^"]+)"', body)
        if stop:
            grads[gid] = color(stop.group(1), {})
    svg_fill = re.search(r'<svg[^>]*\sfill="([^"]+)"', s)
    default = svg_fill.group(1) if svg_fill else None
    vb = re.search(r'viewBox="([\d.\s-]+)"', s).group(1).split()
    paths = []
    for attrs in re.findall(r'<path([^>]*)/?>', s):
        d = re.search(r'\sd="([^"]+)"', attrs)
        if not d:
            continue
        fl = re.search(r'\sfill="([^"]+)"', attrs)
        fr = re.search(r'fill-rule="([^"]+)"', attrs)
        op = re.search(r'\sopacity="([^"]+)"', attrs) or re.search(r'fill-opacity="([^"]+)"', attrs)
        c = color(fl.group(1) if fl else default, grads)
        if c is None:
            continue
        p = f'    <path android:fillColor="{c}" android:pathData="{d.group(1)}"'
        if fr and fr.group(1) == 'evenodd':
            p += ' android:fillType="evenOdd"'
        if op:
            p += f' android:fillAlpha="{op.group(1)}"'
        paths.append(p + '/>')
    name = 'om_logo_' + f.stem.replace('-color', '').replace('-', '_')
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
           f'    android:width="24dp" android:height="24dp"\n'
           f'    android:viewportWidth="{vb[2]}" android:viewportHeight="{vb[3]}">\n'
           + '\n'.join(paths) + '\n</vector>\n')
    (out / (name + '.xml')).write_text(xml)
    print(name, len(paths))
