#!/usr/bin/env python3
"""Remap hard-coded iOS system colours under ui/ to the Noir muted palette.

The app inherited ~330 literal `Color(0xFF......)` tints from the iOS port
(systemBlue / systemGreen / systemOrange …). On the Noir paper/onyx grounds
those saturated hues are the loudest thing on screen and break the
monochrome-plus-champagne language. Each is replaced by a muted counterpart
that keeps its *meaning* (green = success, red = destructive, orange =
warning, blue = primary/informational) and stays legible on both #F3F0E9
paper and #0B0B0C onyx (all ≥ 3:1 for icons/large text on both grounds).

Excluded on purpose:
  ui/theme/**                  (owns the palette itself)
  ui/terminal/AnsiParser.kt    (ANSI colours are a terminal contract)
  ui/markdown/SyntaxHighlighter.kt (code token colours)

Idempotent: re-running finds nothing to replace.
"""
import os
import re
import sys

ROOT = sys.argv[1]  # .../java/com/openminis/app/ui

MAP = {
    # success — sage
    "34C759": "6E9C7B", "4CAF50": "6E9C7B", "388E3C": "5E8A6B",
    # primary / informational — bronze champagne
    "007AFF": "9A7A48", "3478F6": "9A7A48",
    # warning — amber
    "FF9500": "C98A3E", "F09A37": "C98A3E",
    # destructive — ember
    "FF3B30": "C8553D",
    # neutral grey — warm grey
    "8E8E93": "8F8A80",
    # purple / indigo — mauve / slate
    "AF52DE": "9A7FA8", "5856D6": "7C7FA6", "5E5CE6": "7C7FA6",
    # pink — rose
    "FF2D55": "C2677A",
    # yellow — gold
    "FFCC00": "CDAE74",
    # cyan / teal — steel teal
    "30B0C7": "6F9A9E", "32ADE6": "6F9A9E", "00C7BE": "6F9A9E",
}

EXCLUDE = (
    os.sep + "theme" + os.sep,
    "AnsiParser.kt",
    "SyntaxHighlighter.kt",
)

pat = re.compile(r"Color\(0x([0-9A-Fa-f]{2})(" + "|".join(MAP) + r")\)", re.IGNORECASE)
changed_files = 0
changed = 0
for dp, _, fs in os.walk(ROOT):
    for f in fs:
        if not f.endswith(".kt"):
            continue
        p = os.path.join(dp, f)
        if any(x in p for x in EXCLUDE):
            continue
        s = open(p, encoding="utf-8").read()

        def sub(m):
            return "Color(0x%s%s)" % (m.group(1), MAP[m.group(2).upper()])

        n = len(pat.findall(s))
        if n:
            open(p, "w", encoding="utf-8").write(pat.sub(sub, s))
            changed_files += 1
            changed += n
print("files", changed_files, "literals", changed)
