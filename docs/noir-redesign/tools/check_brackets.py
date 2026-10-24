#!/usr/bin/env python3
"""Bracket balance for Kotlin sources, skipping strings, raw strings,
char literals and comments (incl. nested block comments). Cheap syntax
sanity check for scripted edits when no Kotlin compiler is available."""
import sys

PAIRS = {")": "(", "]": "[", "}": "{"}


def check(src):
    st, i, n, line = [], 0, len(src), 1
    tmpl = []  # stack depth markers for ${ ... } inside strings
    while i < n:
        c = src[i]
        if c == "\n":
            line += 1
        if src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        if src.startswith("/*", i):
            depth, i = 1, i + 2
            while i < n and depth:
                if src.startswith("/*", i):
                    depth, i = depth + 1, i + 2
                elif src.startswith("*/", i):
                    depth, i = depth - 1, i + 2
                else:
                    line += src[i] == "\n"
                    i += 1
            continue
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            if j < 0:
                return f"unterminated raw string at line {line}"
            while src.startswith('"', j + 3):
                j += 1
            line += src.count("\n", i, j + 3)
            i = j + 3
            continue
        if c == '"':
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\":
                    i += 2
                    continue
                if src.startswith("${", i):
                    d, i = 1, i + 2
                    while i < n and d:
                        d += {"{": 1, "}": -1}.get(src[i], 0)
                        i += 1
                    continue
                if src[i] == "\n":
                    return f"newline in string at line {line}"
                i += 1
            i += 1
            continue
        if c == "'":
            j = i + 1
            if j < n and src[j] == "\\":
                j += 1
            k = src.find("'", j + 1)
            if 0 < k - i <= 8:
                i = k + 1
                continue
        if c in "([{":
            st.append((c, line))
        elif c in ")]}":
            if not st or st[-1][0] != PAIRS[c]:
                return f"unexpected '{c}' at line {line} (open: {st[-1] if st else None})"
            st.pop()
        i += 1
    if st:
        return f"unclosed '{st[-1][0]}' from line {st[-1][1]} (+{len(st) - 1} more)"
    return None


bad = 0
for p in sys.argv[1:]:
    err = check(open(p, encoding="utf-8").read())
    if err:
        bad += 1
        print("BAD", p, "->", err)
print(f"{len(sys.argv) - 1} files, {bad} unbalanced")
sys.exit(1 if bad else 0)
