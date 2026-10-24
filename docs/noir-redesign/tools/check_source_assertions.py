#!/usr/bin/env python3
"""[T-noir-redesign] Source-shape test regression check (no Gradle needed).

Many unit tests in this repo read a production .kt file as text and assert
`src.contains("...")` / `!src.contains("...")`. For every test that names a
file this branch changed, extract those literal needles and report:

  MISSING  - a positively-asserted needle that existed in the base version
             of the file but is gone now (likely test break)
  APPEARED - a negatively-asserted needle (`!x.contains(...)`/assertFalse)
             that is now present but wasn't before

Needles that were already absent/present on base are ignored (they target
other files or were already in that state).
"""
import os, re, subprocess, sys

ROOT = subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True).stdout.strip()
os.chdir(ROOT)
BASE = sys.argv[1] if len(sys.argv) > 1 else "feat/agent-efficiency"
TEST = "src/android/app/src/test/java"

changed = subprocess.run(["git", "diff", "--name-only", BASE, "--", "src/android/app/src/main"],
                         capture_output=True, text=True).stdout.split()
changed_kt = [c for c in changed if c.endswith((".kt", ".xml"))]


def base_src(p):
    r = subprocess.run(["git", "show", f"{BASE}:{p}"], capture_output=True, text=True)
    return r.stdout


def kotlin_unescape(s):
    return s.replace('\\"', '"').replace("\\$", "$").replace("\\\\", "\\").replace("\\n", "\n")


pos_re = re.compile(r'(?<![!\w])(\w+)\.contains\(\s*"((?:[^"\\]|\\.)*)"\s*\)')
neg_re = re.compile(r'!\s*(\w+)\.contains\(\s*"((?:[^"\\]|\\.)*)"\s*\)')
raw_pos_re = re.compile(r'(?<![!\w])(\w+)\.contains\(\s*"""(.*?)"""\s*\)', re.S)

problems = 0
checked = 0
for dp, _, fs in os.walk(TEST):
    for f in fs:
        if not f.endswith(".kt"):
            continue
        tp = os.path.join(dp, f)
        t = open(tp, encoding="utf-8").read()
        targets = [c for c in changed_kt if os.path.basename(c) in t]
        if not targets:
            continue
        pos = [kotlin_unescape(m.group(2)) for m in pos_re.finditer(t)]
        pos += [m.group(2) for m in raw_pos_re.finditer(t)]
        neg = [kotlin_unescape(m.group(2)) for m in neg_re.finditer(t)]
        for c in targets:
            before = base_src(c)
            after = open(c, encoding="utf-8").read()
            for n in pos:
                if "$" in n and "{" in n:
                    continue  # template — can't evaluate statically
                checked += 1
                if n in before and n not in after:
                    problems += 1
                    print(f"MISSING  {f} -> {os.path.basename(c)}: {n[:110]!r}")
            for n in neg:
                checked += 1
                if n not in before and n in after:
                    problems += 1
                    print(f"APPEARED {f} -> {os.path.basename(c)}: {n[:110]!r}")

print(f"\nchanged files: {len(changed_kt)}  needle checks: {checked}  problems: {problems}")
sys.exit(1 if problems else 0)
