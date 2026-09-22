#!/usr/bin/env python3
"""Apply a complete, validated Lua translation draft to source files."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from languages import ALL_LANGUAGES
from lua_catalog import (
    LUA_ROOT, LuaTranslationError, apply_function_translations,
    apply_shared_translations, export_copy,
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("draft", type=Path)
    args = parser.parse_args()
    payload = json.loads(args.draft.read_text(encoding="utf-8"))
    kind = payload.get("kind")
    if kind not in ("functions", "shared"):
        parser.error("draft kind must be 'functions' or 'shared'")
    translations = payload.get("translations")
    if not isinstance(translations, dict):
        parser.error("draft has no translations object")
    locales = [language.locale for language in ALL_LANGUAGES]
    expected_targets = set(locales) - {"en"}
    if set(translations) != expected_targets:
        missing = sorted(expected_targets - set(translations))
        extra = sorted(set(translations) - expected_targets)
        parser.error(f"draft locale mismatch; missing={missing}, extra={extra}")

    if kind == "shared":
        all_fields = export_copy("shared")
        draft_ids = set().union(*(set(values) for values in translations.values()))
        fields = [field for field in all_fields if field.id in draft_ids]
        if not fields:
            parser.error("draft contains no shared fields")
        for locale in expected_targets:
            if set(translations[locale]) != {field.id for field in fields}:
                parser.error(f"{locale} field set does not match the selected shared records")
        path = LUA_ROOT / "src/community/shared-translations-data.lua"
        updated = apply_shared_translations(path.read_text(encoding="utf-8"), fields, translations, locales)
        path.write_text(updated, encoding="utf-8")
        print(f"Updated {path.relative_to(LUA_ROOT.parent)}")
        return 0

    fields = export_copy("functions")
    by_function: dict[str, list] = {}
    for field in fields:
        by_function.setdefault(field.function_id or "", []).append(field)
    draft_ids = set().union(*(set(values) for values in translations.values()))
    selected = {field.function_id for field in fields if field.id in draft_ids}
    expected_ids = {field.id for field in fields if field.function_id in selected}
    for locale in expected_targets:
        if set(translations[locale]) != expected_ids:
            parser.error(f"{locale} field set does not match selected complete functions")
    for function_id in sorted(selected):
        function_fields = by_function[function_id or ""]
        path = LUA_ROOT / (function_fields[0].path or "")
        updated = apply_function_translations(path.read_text(encoding="utf-8"), function_fields, translations, locales)
        path.write_text(updated, encoding="utf-8")
        print(f"Updated {path.relative_to(LUA_ROOT.parent)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (LuaTranslationError, ValueError, json.JSONDecodeError) as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1) from error
