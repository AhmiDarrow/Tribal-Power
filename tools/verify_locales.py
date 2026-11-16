#!/usr/bin/env python3
"""Check Tribal Power languages against English.

English is the source of truth (assets/tribalpower/lang/en_us.json and
assets/tribalpower/codex/en_us). Every shipped locale must parse, carry the
same keys, and use the same printf arguments. Minecraft itself falls back to
en_us when a key is missing; this script is what keeps the files in step so a
missing line is a failed build rather than a raw key in game.

Codex pages may translate only prose fields (text, title, name, caption,
description, landing). Ids, coordinates, item ids and link targets stay as
in English.

Usage:
  python tools/verify_locales.py            # lang + codex
  python tools/verify_locales.py --lang-only
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG_DIR = ROOT / "src" / "main" / "resources" / "assets" / "tribalpower" / "lang"
CODEX_DIR = ROOT / "src" / "main" / "resources" / "assets" / "tribalpower" / "codex"
DATA_DIR = ROOT / "src" / "main" / "resources" / "data" / "tribalpower"
JAVA_DIR = ROOT / "src" / "main" / "java"
SOUNDS = ROOT / "src" / "main" / "resources" / "assets" / "tribalpower" / "sounds.json"

LOCALES = (
    "es_es",
    "es_mx",
    "fr_fr",
    "de_de",
    "pt_br",
    "it_it",
    "ru_ru",
    "pl_pl",
    "ja_jp",
    "ko_kr",
    "zh_cn",
)

BRANDS = ("Tribal Power", "Remedy", "Ninjacat", "Chocobos Reborn", "Hope this helps")
CODEX_PROSE = {"text", "title", "name", "caption", "description", "landing"}

# %(n$)?type   or  %%
_TOKEN = re.compile(r"%(?:(\d+)\$)?([sdif])|%%")
_ITEM = re.compile(r"\{item:[a-zA-Z0-9_.:/-]+\}")
_LINK = re.compile(r"\]\(([a-z0-9_]+)\)")
# A quote followed by + is a prefix ("item.tribalpower.spirit_" + id), not a key.
_JAVA_KEY = re.compile(
    r"Component\.translatable(?:WithFallback)?\(\s*\"([^\"\\]+)\"(?!\s*\+)"
)
_LANG_KEY_LINE = re.compile(r"^  \"((?:\\.|[^\"\\])*)\"\s*:", re.M)


class Report:
    def __init__(self) -> None:
        self.errors: list[str] = []

    def add(self, message: str) -> None:
        self.errors.append(message)

    def ok(self) -> bool:
        return not self.errors


def load_json(path: Path, report: Report):
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError as exc:
        report.add(f"{path}: not valid UTF-8 ({exc})")
        return None
    except OSError as exc:
        report.add(f"{path}: {exc}")
        return None
    if text.startswith("\ufeff"):
        report.add(f"{path}: UTF-8 BOM is not allowed")
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError as exc:
        report.add(f"{path}: invalid JSON ({exc})")
        return None


def lang_key_lines(path: Path) -> list[str]:
    text = path.read_text(encoding="utf-8")
    return _LANG_KEY_LINE.findall(text)


def conversions(value: str):
    """Return (escaped_percent_count, [(arg_index, type), ...]) or None if mixed.

    Unindexed conversions (%s, %d, %i, %f) are numbered 1..n from the left.
    Indexed (%1$s) and unindexed conversions may not be mixed. %% is one
    escaped percent. A % that is not a conversion and not %% is a literal
    percent (Minecraft leaves it alone) and is ignored, so "60%" and "60 %"
    compare equal.
    """
    percent = 0
    conv = []
    indexed = False
    unindexed = False
    i = 0
    while i < len(value):
        if value[i] != "%":
            i += 1
            continue
        match = _TOKEN.match(value, i)
        if match is None:
            i += 1
            continue
        if match.group(0) == "%%":
            percent += 1
            i = match.end()
            continue
        idx_text, kind = match.group(1), match.group(2)
        if idx_text is None:
            unindexed = True
            conv.append((None, kind))
        else:
            indexed = True
            conv.append((int(idx_text), kind))
        i = match.end()
    if indexed and unindexed:
        return None
    if unindexed:
        conv = [(n + 1, kind) for n, (_, kind) in enumerate(conv)]
    return percent, conv


def same_placeholders(english: str, translated: str) -> bool:
    src = conversions(english)
    dst = conversions(translated)
    if src is None or dst is None:
        return False
    if src[0] != dst[0]:
        return False
    return sorted(src[1]) == sorted(dst[1])


def check_brands(english: str, translated: str, where: str, report: Report) -> None:
    for brand in BRANDS:
        if brand in english and brand not in translated:
            report.add(f"{where}: translation dropped the name {brand!r}")


def check_lang(report: Report) -> dict | None:
    english_path = LANG_DIR / "en_us.json"
    english = load_json(english_path, report)
    if english is None:
        return None
    if not isinstance(english, dict):
        report.add("en_us.json: root must be an object")
        return None
    lines = lang_key_lines(english_path)
    if len(lines) != len(set(lines)):
        seen = set()
        for key in lines:
            if key in seen:
                report.add(f"en_us.json: duplicate key {key}")
            seen.add(key)
    if set(lines) != set(english):
        report.add("en_us.json: key lines do not match the parsed object (nested JSON or a split key)")
    empty = [key for key, value in english.items() if not isinstance(value, str) or value == ""]
    for key in empty[:20]:
        report.add(f"en_us.json: empty or non-string value for {key}")
    bad = [key for key, value in english.items() if isinstance(value, str) and conversions(value) is None]
    for key in bad[:20]:
        report.add(f"en_us.json: malformed placeholder in {key}")

    for locale in LOCALES:
        path = LANG_DIR / f"{locale}.json"
        if not path.is_file():
            report.add(f"missing locale file {path.relative_to(ROOT)}")
            continue
        data = load_json(path, report)
        if data is None:
            continue
        if not isinstance(data, dict):
            report.add(f"{locale}.json: root must be an object")
            continue
        keys = lang_key_lines(path)
        if len(keys) != len(set(keys)):
            report.add(f"{locale}.json: duplicate key")
        if set(data) != set(english):
            missing = sorted(set(english) - set(data))
            extra = sorted(set(data) - set(english))
            report.add(
                f"{locale}.json: key set differs "
                f"(missing {len(missing)}, extra {len(extra)}; "
                f"e.g. missing {missing[:3]}, extra {extra[:3]})"
            )
        shared = set(data) & set(english)
        shown = 0
        for key in english:
            if key not in shared:
                continue
            value = data[key]
            source = english[key]
            if not isinstance(value, str) or value.strip() == "":
                report.add(f"{locale}: empty value for {key}")
                shown += 1
            elif not same_placeholders(source, value):
                report.add(f"{locale}: placeholders differ for {key}")
                shown += 1
            else:
                before = len(report.errors)
                check_brands(source, value, f"{locale}:{key}", report)
                if len(report.errors) != before:
                    shown += 1
            if shown >= 25:
                report.add(f"{locale}: further value errors omitted")
                break
    return english


def check_data_keys(english: dict, report: Report) -> None:
    if not DATA_DIR.is_dir():
        return
    for path in sorted(DATA_DIR.rglob("*.json")):
        data = load_json(path, report)
        if data is None:
            continue
        _walk_translate(data, english, path.relative_to(ROOT).as_posix(), report)


def _walk_translate(node, english: dict, where: str, report: Report) -> None:
    if isinstance(node, dict):
        key = node.get("translate")
        if isinstance(key, str) and "tribalpower" in key and key not in english:
            report.add(f"{where}: translate key not in en_us.json: {key}")
        for value in node.values():
            _walk_translate(value, english, where, report)
    elif isinstance(node, list):
        for value in node:
            _walk_translate(value, english, where, report)


def check_java_keys(english: dict, report: Report) -> None:
    if english is None or not JAVA_DIR.is_dir():
        return
    missing = []
    for path in JAVA_DIR.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        for key in _JAVA_KEY.findall(text):
            # "message.tribalpower.font." + id is a prefix, not a full key.
            if key.endswith("."):
                continue
            if "tribalpower" in key and key not in english:
                missing.append(f"{path.relative_to(ROOT).as_posix()}: {key}")
    for line in missing[:30]:
        report.add(f"Java translation key missing from en_us.json: {line}")
    if len(missing) > 30:
        report.add(f"Java translation keys: {len(missing) - 30} further misses omitted")


def check_sounds(english: dict, report: Report) -> None:
    if english is None or not SOUNDS.is_file():
        return
    data = load_json(SOUNDS, report)
    if not isinstance(data, dict):
        return
    for event, body in data.items():
        if isinstance(body, dict) and isinstance(body.get("subtitle"), str):
            key = body["subtitle"]
            if key not in english:
                report.add(f"sounds.json:{event}: subtitle not in en_us.json: {key}")


def check_codex(report: Report) -> None:
    english_root = CODEX_DIR / "en_us"
    if not english_root.is_dir():
        report.add("codex en_us directory is missing")
        return
    english_files = sorted(p.relative_to(english_root).as_posix() for p in english_root.rglob("*.json"))
    if not english_files:
        report.add("codex en_us has no JSON files")
        return
    for locale in LOCALES:
        root = CODEX_DIR / locale
        if not root.is_dir():
            report.add(f"missing codex locale {locale}")
            continue
        files = sorted(p.relative_to(root).as_posix() for p in root.rglob("*.json"))
        if files != english_files:
            missing = sorted(set(english_files) - set(files))
            extra = sorted(set(files) - set(english_files))
            report.add(
                f"codex {locale}: file set differs (missing {len(missing)}, extra {len(extra)}; "
                f"e.g. missing {missing[:2]}, extra {extra[:2]})"
            )
        shown = 0
        for rel in english_files:
            if rel not in files:
                continue
            src = load_json(english_root / rel, report)
            dst = load_json(root / rel, report)
            if src is None or dst is None:
                continue
            shown = _walk_codex(src, dst, False, f"codex {locale}/{rel}", report, shown)
            if shown >= 20:
                report.add(f"codex {locale}: further prose errors omitted")
                break


def _walk_codex(src, dst, prose: bool, where: str, report: Report, shown: int) -> int:
    if shown >= 20:
        return shown
    if isinstance(src, dict):
        if not isinstance(dst, dict) or set(src) != set(dst):
            report.add(f"{where}: object keys differ")
            return shown + 1
        for key in src:
            shown = _walk_codex(src[key], dst[key], key in CODEX_PROSE, f"{where}:{key}", report, shown)
        return shown
    if isinstance(src, list):
        if not isinstance(dst, list) or len(src) != len(dst):
            report.add(f"{where}: list length differs")
            return shown + 1
        for index, (left, right) in enumerate(zip(src, dst)):
            if prose and isinstance(left, str):
                shown = _check_prose(left, right, f"{where}[{index}]", report, shown)
            else:
                shown = _walk_codex(left, right, False, f"{where}[{index}]", report, shown)
        return shown
    if isinstance(src, str):
        if prose:
            return _check_prose(src, dst, where, report, shown)
        if src != dst:
            report.add(f"{where}: non-prose field changed")
            return shown + 1
        return shown
    if src != dst:
        report.add(f"{where}: value changed")
        return shown + 1
    return shown


def _check_prose(src: str, dst, where: str, report: Report, shown: int) -> int:
    if not isinstance(dst, str) or (src.strip() and not dst.strip()):
        report.add(f"{where}: empty translation")
        return shown + 1
    if not same_placeholders(src, dst):
        report.add(f"{where}: placeholders differ")
        return shown + 1
    if dst.count("**") != src.count("**") or dst.count("**") % 2:
        report.add(f"{where}: bold markers ** are not balanced like English")
        return shown + 1
    if sorted(_ITEM.findall(src)) != sorted(_ITEM.findall(dst)):
        report.add(f"{where}: {{item:}} ids differ from English")
        return shown + 1
    if sorted(_LINK.findall(src)) != sorted(_LINK.findall(dst)):
        report.add(f"{where}: codex link targets differ from English")
        return shown + 1
    before = len(report.errors)
    check_brands(src, dst, where, report)
    if len(report.errors) != before:
        return shown + 1
    return shown


def main() -> int:
    lang_only = "--lang-only" in sys.argv
    report = Report()
    english = check_lang(report)
    if english is not None:
        check_data_keys(english, report)
        check_java_keys(english, report)
        check_sounds(english, report)
    if not lang_only:
        check_codex(report)
    if report.errors:
        print(f"verify_locales: {len(report.errors)} problem(s)")
        for line in report.errors:
            print(" -", line)
        return 1
    scope = "lang" if lang_only else "lang and codex"
    print(f"verify_locales: {scope} ok ({len(LOCALES)} locales)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
