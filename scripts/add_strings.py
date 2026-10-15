#!/usr/bin/env python3
"""Append string resources to values/, values-zh/, values-zh-rTW/.

Usage: add_strings.py strings.json
JSON: {"key": {"en": "...", "zh": "...", "tw": "..."}, ...}
Values are inserted raw (caller escapes apostrophes as \\' and & as &amp;).
Existing keys are replaced in place.
"""
import json, os, re, sys

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/android/app/src/main/res")
TARGETS = {"en": "values", "zh": "values-zh", "tw": "values-zh-rTW"}


def esc(v: str) -> str:
    # apostrophes must be escaped for aapt2; leave already-escaped ones alone
    return re.sub(r"(?<!\\)'", r"\\'", v)


def main():
    data = json.load(open(sys.argv[1], encoding="utf-8"))
    for lang, d in TARGETS.items():
        p = os.path.join(RES, d, "strings.xml")
        s = open(p, encoding="utf-8").read()
        for key, vals in data.items():
            v = vals.get(lang) or vals.get("zh" if lang == "tw" else "en")
            line = f'    <string name="{key}">{esc(v)}</string>'
            pat = re.compile(r'^\s*<string name="' + re.escape(key) + r'"[^>]*>.*?</string>\s*$', re.M | re.S)
            if pat.search(s):
                s = pat.sub(line, s, count=1)
            else:
                i = s.rstrip().rfind("</resources>")
                s = s[:i] + line + "\n" + s[i:]
        open(p, "w", encoding="utf-8").write(s)
        print("updated", p)


main()
