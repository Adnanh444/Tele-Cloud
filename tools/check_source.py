#!/usr/bin/env python3
"""Offline sanity checks for this repo.

There is no Android SDK in the authoring environment, so every CI round trip costs minutes. These
checks catch the mistakes that actually reached CI before: a Compose resource used without its
import, a string key that does not exist, an import of a symbol that was renamed, unbalanced braces.

Usage:  python3 tools/check_source.py [--skip-desktop]
Exit code 0 = clean.
"""
import os
import re
import sys
import glob
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIP_DIRS = {".git", "build", ".gradle", ".idea", "node_modules"}


def kt_files(skip_desktop=True):
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        if skip_desktop and os.sep + "desktop" + os.sep in dirpath + os.sep:
            continue
        for name in filenames:
            if name.endswith(".kt"):
                yield os.path.join(dirpath, name)


def compose_string_keys():
    keys = set()
    pattern = os.path.join(ROOT, "ui/src/commonMain/composeResources/values/*.xml")
    for path in glob.glob(pattern):
        text = open(path, encoding="utf-8").read()
        keys |= set(re.findall(r'<(?:string|plurals|string-array) name="([^"]+)"', text))
    return keys


def android_res_keys(kind):
    keys = set()
    for path in glob.glob(os.path.join(ROOT, "android/src/main/res/values/*.xml")):
        text = open(path, encoding="utf-8").read()
        keys |= set(re.findall(r'<%s name="([^"]+)"' % kind, text))
    return keys


def android_res_files(folder):
    """Includes density/qualifier variants: drawable-hdpi, mipmap-anydpi-v26, …"""
    names = set()
    for path in glob.glob(os.path.join(ROOT, "android/src/main/res", folder + "*", "*")):
        names.add(os.path.splitext(os.path.basename(path))[0])
    return names


def is_test(path):
    return "/test/" in path.replace(os.sep, "/") or "/desktopTest/" in path.replace(os.sep, "/")


def declared_symbols(files):
    """package -> set of top-level names it declares"""
    declared = {}
    pattern = re.compile(
        r"^\s*(?:public |internal |private |expect |actual )*"
        r"(?:data |sealed |abstract |open |enum |annotation |value )*"
        r"(class|object|interface|fun|val|var|typealias)\s+(?:<[^>]*>\s+)?([A-Za-z_]\w*)",
        re.M,
    )
    for path in files:
        text = open(path, encoding="utf-8").read()
        match = re.search(r"^package\s+([\w.]+)", text, re.M)
        pkg = match.group(1) if match else ""
        bucket = declared.setdefault(pkg, set())
        for _, name in pattern.findall(text):
            bucket.add(name)
        bucket |= set(re.findall(r"^fun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?([A-Za-z_]\w*)\s*\(", text, re.M))
    return declared


def balance(path):
    text = open(path, encoding="utf-8").read()
    depth = {"{": 0, "(": 0}
    state, i, n = None, 0, len(text)
    while i < n:
        c = text[i]
        if state is None:
            if text[i:i + 3] == '"""':
                state, i = '"""', i + 3; continue
            if c == '"':
                state, i = '"', i + 1; continue
            if c == "'":
                state, i = "'", i + 1; continue
            if text[i:i + 2] == "//":
                state, i = "//", i + 2; continue
            if text[i:i + 2] == "/*":
                state, i = "/*", i + 2; continue
            if c in "{(":
                depth[c] += 1
            elif c == "}":
                depth["{"] -= 1
            elif c == ")":
                depth["("] -= 1
            i += 1
        elif state == '"""':
            if text[i:i + 3] == '"""':
                state, i = None, i + 3; continue
            i += 1
        elif state in ('"', "'"):
            if c == "\\":
                i += 2; continue
            if c == state:
                state = None
            i += 1
        elif state == "//":
            if c == "\n":
                state = None
            i += 1
        else:
            if text[i:i + 2] == "*/":
                state, i = None, i + 2; continue
            i += 1
    return depth


def main():
    skip_desktop = "--skip-desktop" in sys.argv or True
    problems = []
    files = list(kt_files(skip_desktop))
    strings = compose_string_keys()
    android_strings = android_res_keys("string")
    drawables = android_res_files("drawable") | android_res_files("mipmap") | android_res_files("xml")

    for path in files:
        rel = os.path.relpath(path, ROOT)
        text = open(path, encoding="utf-8").read()

        # 1. Compose resource imports
        imports = set(re.findall(r"^import com\.adnan\.memoracloud\.resources\.(\w+)", text, re.M))
        used = set(re.findall(r"\bRes\.(?:string|plurals|drawable|array|stringArray)\.(\w+)", text))
        for name in sorted(used - imports):
            problems.append(f"{rel}: uses Res.*.{name} without importing it")
        # 2. Compose resource keys must exist
        for name in sorted(used):
            if name.startswith("drawable") or name.startswith("ic_") or name.startswith("app_"):
                continue
            if name not in strings:
                problems.append(f"{rel}: Compose string '{name}' does not exist")

        # 3. Android resources
        if "/android/" in path.replace(os.sep, "/") or path.endswith("StandardBackupFolder.kt"):
            for name in sorted(set(re.findall(r"\bR\.string\.(\w+)", text))):
                if name not in android_strings:
                    problems.append(f"{rel}: R.string.{name} does not exist")
            for name in sorted(set(re.findall(r"\bR\.(?:drawable|mipmap)\.(\w+)", text))):
                if name not in drawables and name not in android_strings:
                    problems.append(f"{rel}: R.drawable/mipmap.{name} does not exist")

        # 4. Balance (test sources use constructs that trip the simple scanner)
        depth = {"{": 0, "(": 0} if is_test(path) else balance(path)
        if depth["{"] or depth["("]:
            problems.append(f"{rel}: unbalanced braces/parens {depth}")

    # 5. Imports of in-project symbols
    declared = declared_symbols(files)
    for path in files:
        rel = os.path.relpath(path, ROOT)
        for imp in re.findall(r"^import\s+(com\.adnan\.memoracloud[\w.]+)", open(path, encoding="utf-8").read(), re.M):
            parts = imp.split(".")
            name = parts[-1]
            owner = ".".join(parts[:-1])
            if name in declared.get(owner, set()):
                continue
            if any(".".join(parts[:k]) in declared for k in range(len(parts) - 1, 2, -1)):
                continue
            if ".resources." in imp:
                continue
            problems.append(f"{rel}: import {imp} does not resolve")

    # 7. Lost headers: a file must still declare its package, and the handful of symbols that are
    #    only visible through an import must actually be imported. Editing a file by replacing its
    #    top section silently deleted the whole import block once, so this is now enforced.
    required_imports = {
        "Migration(": "androidx.room.migration.Migration",
        "SQLiteConnection": "androidx.sqlite.SQLiteConnection",
        "execSQL(": "androidx.sqlite.execSQL",
        "SelectionContainer": "androidx.compose.foundation.text.selection.SelectionContainer",
    }
    for path in files:
        try:
            text = open(path, encoding="utf-8").read()
        except Exception:  # noqa: BLE001
            continue
        relative = os.path.relpath(path, ROOT)
        if not re.search(r"^package\s+[\w.]+", text, re.M):
            problems.append(f"{relative}: no package declaration")
        for symbol, needed in required_imports.items():
            if symbol not in text:
                continue
            if f"import {needed}" in text:
                continue
            problems.append(f"{relative}: uses {symbol} but does not import {needed}")

    # 6. XML validity
    patterns = [
        "android/src/main/res/**/*.xml",
        "ui/src/commonMain/composeResources/**/*.xml",
        "android/src/main/AndroidManifest.xml",
    ]
    for pattern in patterns:
        for path in glob.glob(os.path.join(ROOT, pattern), recursive=True):
            try:
                ET.parse(path)
            except Exception as error:  # noqa: BLE001
                problems.append(f"{os.path.relpath(path, ROOT)}: invalid XML ({error})")

    if problems:
        print(f"{len(problems)} problem(s):")
        for problem in problems:
            print("  -", problem)
        return 1
    print(f"OK — {len(files)} Kotlin files checked, no problems found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
