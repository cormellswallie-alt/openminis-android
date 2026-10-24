#!/usr/bin/env python3
"""[T-noir-redesign] Feature-preservation gate for the Noir visual refactor.

Compares the working tree against a base ref (default: the commit this branch
forked from) and fails if the refactor removed anything user-facing:

  1. routes      - every `const val` in `object Routes` still declared, and
                   every route still registered via composable(...)/dialog(...)
  2. composables - no top-level/member `fun Name(` annotated @Composable
                   disappeared from ui/ (renames would show up here too)
  3. strings     - no <string>/<plurals> key removed from any values*/strings.xml
  4. prefs       - no `const val KEY_*` preference key removed (settings intact)
  5. manifest    - no activity/service/receiver/provider/intent action removed

Usage: python3 docs/noir-redesign/tools/verify_no_feature_loss.py [base-ref]
"""
import os, re, subprocess, sys

ROOT = subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True).stdout.strip()
os.chdir(ROOT)
BASE = sys.argv[1] if len(sys.argv) > 1 else "feat/agent-efficiency"
APP = "src/android/app/src/main"
JAVA = APP + "/java/com/openminis/app"


def git_show(path):
    r = subprocess.run(["git", "show", f"{BASE}:{path}"], capture_output=True, text=True)
    return r.stdout if r.returncode == 0 else ""


def git_files(prefix):
    r = subprocess.run(["git", "ls-tree", "-r", "--name-only", BASE, prefix], capture_output=True, text=True)
    return [l for l in r.stdout.splitlines() if l]


def work(path):
    try:
        return open(path, encoding="utf-8").read()
    except FileNotFoundError:
        return ""


fails = []


def check(name, before, after):
    gone = sorted(set(before) - set(after))
    status = "OK " if not gone else "FAIL"
    print(f"[{status}] {name}: base={len(set(before))} now={len(set(after))} removed={len(gone)}")
    for g in gone[:40]:
        print("        -", g)
    if gone:
        fails.append(name)


# 1. routes
nav = JAVA + "/ui/navigation/AppNavigation.kt"
def routes(src):
    m = re.search(r"object Routes \{(.*?)\n\}", src, re.S)
    return re.findall(r"const val (\w+)", m.group(1)) if m else []
def registered(src):
    return re.findall(r"(?:composable|dialog)\(\s*(?:route\s*=\s*)?Routes\.(\w+)", src)
check("routes declared", routes(git_show(nav)), routes(work(nav)))
check("routes registered", registered(git_show(nav)), registered(work(nav)))

# 2. composables under ui/
comp_re = re.compile(r"@Composable\s+(?:(?:private|internal|public|inline)\s+)*fun\s+(?:[\w.<>?, ]+\.)?(\w+)\s*\(")
def composables(files, reader):
    out = []
    for f in files:
        if f.endswith(".kt"):
            out += [f"{os.path.basename(f)}::{n}" for n in comp_re.findall(reader(f))]
    return out
base_ui = git_files(JAVA + "/ui")
now_ui = [os.path.join(d, f)[len(ROOT) + 1:] if os.path.isabs(d) else os.path.join(d, f)
          for d, _, fs in os.walk(JAVA + "/ui") for f in fs]
check("@Composable functions", composables(base_ui, git_show), composables(now_ui, work))

# 3. string keys per locale
str_re = re.compile(r'<(?:string|plurals|string-array)\s+name="([^"]+)"')
base_strings, now_strings = [], []
for f in git_files(APP + "/res"):
    if f.endswith("/strings.xml"):
        loc = f.split("/")[-2]
        base_strings += [f"{loc}:{k}" for k in str_re.findall(git_show(f))]
        now_strings += [f"{loc}:{k}" for k in str_re.findall(work(f))]
check("string resources", base_strings, now_strings)

# 4. preference keys
key_re = re.compile(r'const val (KEY_\w+)\s*=\s*"([^"]+)"')
def keys(files, reader):
    return [f"{a}={b}" for f in files if f.endswith(".kt") for a, b in key_re.findall(reader(f))]
base_kt = git_files(JAVA)
now_kt = [os.path.join(d, f) for d, _, fs in os.walk(JAVA) for f in fs]
check("preference keys", keys(base_kt, git_show), keys(now_kt, work))

# 5. manifest components + actions
man = APP + "/AndroidManifest.xml"
man_re = re.compile(r'<(activity|service|receiver|provider|action)\b[^>]*android:name="([^"]+)"')
check("manifest entries", [f"{a}:{n}" for a, n in man_re.findall(git_show(man))],
      [f"{a}:{n}" for a, n in man_re.findall(work(man))])

print()
if fails:
    print("FEATURE LOSS DETECTED in:", ", ".join(fails))
    sys.exit(1)
print("No user-facing feature removed relative to", BASE)
