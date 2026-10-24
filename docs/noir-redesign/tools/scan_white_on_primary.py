#!/usr/bin/env python3
"""Find Color.White content drawn on a `colorScheme.primary` background.

Under Noir, `primary` is ivory in dark mode, so white-on-primary becomes
invisible. Reports every Color.White within WINDOW lines of a primary fill.
"""
import os
import re
import sys

ROOT = sys.argv[1]
WINDOW = int(sys.argv[2]) if len(sys.argv) > 2 else 12
prim = re.compile(sys.argv[3] if len(sys.argv) > 3 else r"colorScheme\.primary\b(?!Container)")
white = re.compile(r"Color\.White|Color\(0xFFFFFFFF\)")
for dp, _, fs in os.walk(ROOT):
    for f in fs:
        if not f.endswith(".kt"):
            continue
        p = os.path.join(dp, f)
        lines = open(p, encoding="utf-8").read().split("\n")
        prim_lines = [i for i, l in enumerate(lines) if prim.search(l) and not l.strip().startswith("//")]
        for i, l in enumerate(lines):
            if white.search(l) and not l.strip().startswith("//"):
                near = [j for j in prim_lines if abs(j - i) <= WINDOW]
                if near:
                    print("%s:%d: %s   [primary@%s]" % (os.path.relpath(p, ROOT), i + 1, l.strip()[:110], ",".join(str(j + 1) for j in near[:3])))
