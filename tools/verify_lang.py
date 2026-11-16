#!/usr/bin/env python3
"""Check legacy 1.9.4 .lang files against en_US.lang.

Same keys, non-empty values, UTF-8 without a BOM. Proper names that must
stay in Latin (Tribal Power, Amazonite, Chromite, Fluorite, Acooma) are
required wherever the English line contains them. es_MX.lang must match
es_ES.lang. Forge 1.9 has no test task this can hook; run it directly:

  python tools/verify_lang.py
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / "src" / "main" / "resources" / "assets" / "tribalpower" / "lang"
LOCALES = (
    "es_ES",
    "es_MX",
    "fr_FR",
    "de_DE",
    "pt_BR",
    "it_IT",
    "ru_RU",
    "pl_PL",
    "ja_JP",
    "ko_KR",
    "zh_CN",
)
KEEP = ("Tribal Power", "Amazonite", "Chromite", "Fluorite", "Acooma")


def parse(path: Path) -> tuple[list[str], dict[str, str], list[str]]:
    errors: list[str] = []
    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        errors.append(f"{path.name}: UTF-8 BOM is not allowed")
    try:
        text = raw.decode("utf-8-sig")
    except UnicodeDecodeError as exc:
        return [], {}, [f"{path.name}: not UTF-8 ({exc})"]
    keys: list[str] = []
    data: dict[str, str] = {}
    for line_no, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if "=" not in line:
            errors.append(f"{path.name}:{line_no}: line has no '='")
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        if key in data:
            errors.append(f"{path.name}: duplicate key {key}")
        data[key] = value
        keys.append(key)
        if value.strip() == "":
            errors.append(f"{path.name}: empty value for {key}")
    return keys, data, errors


def main() -> int:
    errors: list[str] = []
    english_path = LANG / "en_US.lang"
    if not english_path.is_file():
        print("missing en_US.lang")
        return 1
    _, english, parse_errors = parse(english_path)
    errors.extend(parse_errors)
    files = {"en_US": english}
    for locale in LOCALES:
        path = LANG / f"{locale}.lang"
        if not path.is_file():
            errors.append(f"missing {locale}.lang")
            continue
        _, data, parse_errors = parse(path)
        errors.extend(parse_errors)
        files[locale] = data
        if set(data) != set(english):
            missing = sorted(set(english) - set(data))
            extra = sorted(set(data) - set(english))
            errors.append(
                f"{locale}.lang: key set differs (missing {missing[:3]}, extra {extra[:3]})"
            )
        for key, source in english.items():
            value = data.get(key)
            if not isinstance(value, str):
                continue
            for token in KEEP:
                if token in source and token not in value:
                    errors.append(f"{locale}:{key}: dropped {token!r}")
    if "es_ES" in files and "es_MX" in files and files["es_ES"] != files["es_MX"]:
        errors.append("es_MX.lang does not match es_ES.lang")
    if errors:
        print(f"verify_lang: {len(errors)} problem(s)")
        for line in errors:
            print(" -", line)
        return 1
    print(f"verify_lang: ok ({len(english)} keys, {len(LOCALES)} locales)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
