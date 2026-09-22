#!/usr/bin/env python3
"""Generate validated, paste-ready Lua translations for community copy."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import sys
from pathlib import Path

from languages import SUPPORTED_TARGETS, TranslationTarget
from lua_catalog import (
    CopyField, LuaTranslationError, batches, export_copy, parse_response_with_repair,
    recover_items, render_failure, render_table,
)
from providers.base import TranslatorError
from translation_runtime import add_provider_arguments, create_translator, load_domain_brief, parse_target


DEFAULT_OUTPUT = Path("build/translation-drafts/lua")


def instructions(target: TranslationTarget, domain: str) -> str:
    return f"""Translate the supplied JSON copy entries from English into {target.language} (BCP-47 locale {target.locale}).

Return only one JSON object with this exact shape: {{"translations":[{{"id":"unchanged id","value":"translated text"}}]}}. Return exactly one item for every input item, in the same order. Keep every id unchanged. Translate only value. Preserve meaning without omissions or additions. Preserve Markdown structure, URLs, code, placeholders, technical identifiers, and newlines exactly where present. Use consistent Track & Graph terminology and natural, concise UI language. Write in the target language's native writing system; do not substitute a neighboring language or its script.

The following English domain brief is context only, not text to translate:
--- DOMAIN BRIEF ---
{domain}
--- END DOMAIN BRIEF ---"""


def source_json(fields: list[CopyField]) -> str:
    return json.dumps({"entries": [{"id": f.id, "value": f.source} for f in fields]}, ensure_ascii=False)


def main() -> int:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--function", help="regenerate every inline field for one function")
    group.add_argument("--shared", action="store_true", help="translate shared-copy records")
    parser.add_argument("--shared-key", action="append", help="with --shared, translate only this key; repeatable")
    parser.add_argument("--all-shared", action="store_true", help="with --shared, include already complete records")
    parser.add_argument("--target", type=parse_target, action="append", help="LOCALE=LANGUAGE; repeatable (default: every target)")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--batch-characters", type=int, default=12000)
    parser.add_argument("--workers", type=int, default=8)
    parser.add_argument(
        "--reuse-responses",
        action="store_true",
        help="reuse a retained full-locale response when it still validates; request only invalid/missing locales",
    )
    add_provider_arguments(parser)
    args = parser.parse_args()
    if args.shared_key and not args.shared:
        parser.error("--shared-key requires --shared")
    if args.all_shared and not args.shared:
        parser.error("--all-shared requires --shared")

    kind = "shared" if args.shared else "functions"
    selector = args.function
    fields = export_copy(kind, selector)
    targets = tuple(args.target) if args.target else SUPPORTED_TARGETS
    if args.shared_key:
        wanted = set(args.shared_key)
        fields = [field for field in fields if field.id in wanted]
        missing = wanted - {field.id for field in fields}
        if missing:
            parser.error("unknown shared keys: " + ", ".join(sorted(missing)))
    elif args.shared and not args.all_shared:
        target_locales = {target.locale for target in targets}
        fields = [field for field in fields if not target_locales.issubset(field.existing_locales)]
    if not fields:
        print("No copy requires translation.")
        return 0

    locales = ["en", *(target.locale for target in targets)]
    domain = load_domain_brief(args.domain_brief)
    translator = create_translator(args.provider, args.model)
    field_batches = batches(fields, args.batch_characters)
    output_root = args.output_dir / (args.function or "shared")
    output_root.mkdir(parents=True, exist_ok=True)
    retained_draft: dict[str, object] = {}
    draft_path = output_root / "draft.json"
    if args.reuse_responses and draft_path.exists():
        try:
            loaded_draft = json.loads(draft_path.read_text(encoding="utf-8"))
            if loaded_draft.get("kind") == kind and isinstance(loaded_draft.get("translations"), dict):
                retained_draft = loaded_draft["translations"]
        except (json.JSONDecodeError, OSError):
            pass

    def translate_target(target: TranslationTarget) -> tuple[str, dict[str, str], list[dict[str, object]], list[str]]:
        translated: dict[str, str] = {}
        usage: list[dict[str, object]] = []
        failures: list[str] = []
        retained_values = retained_draft.get(target.locale)
        if isinstance(retained_values, dict) and set(retained_values) == {field.id for field in fields} and all(isinstance(value, str) for value in retained_values.values()):
            return target.locale, retained_values, usage, failures
        retained = output_root / f"{target.locale}.batch-1.response.json"
        if args.reuse_responses and retained.exists():
            try:
                values, repaired = parse_response_with_repair(fields, retained.read_text(encoding="utf-8"))
                notes = ["repaired one JSON syntax error locally"] if repaired else []
                return target.locale, values, usage, notes
            except LuaTranslationError:
                pass
        for index, batch in enumerate(field_batches, 1):
            raw_path = output_root / f"{target.locale}.batch-{index}.response.json"
            if args.reuse_responses and raw_path.exists():
                try:
                    values, repaired = parse_response_with_repair(batch, raw_path.read_text(encoding="utf-8"))
                    translated.update(values)
                    if repaired:
                        failures.append(f"batch {index}: repaired one JSON syntax error locally")
                    continue
                except LuaTranslationError:
                    pass
            try:
                result = translator.translate(instructions=instructions(target, domain), source_text=source_json(batch))
                raw_path.write_text(result.text, encoding="utf-8")
                usage.append(result.usage)
                values, repaired = parse_response_with_repair(batch, result.text)
                translated.update(values)
                if repaired:
                    failures.append(f"batch {index}: repaired one JSON syntax error locally")
            except (TranslatorError, LuaTranslationError) as error:
                raw = raw_path.read_text(encoding="utf-8") if raw_path.exists() else ""
                recovered = recover_items(raw, {field.id for field in batch})
                failure_path = output_root / f"{target.locale}.batch-{index}.failure.lua"
                failure_path.write_text(render_failure(batch, recovered, target.locale, str(error)), encoding="utf-8")
                failures.append(f"batch {index}: {error}; recovery: {failure_path}")
        return target.locale, translated, usage, failures

    results: dict[str, dict[str, str]] = {}
    had_failure = False
    with concurrent.futures.ThreadPoolExecutor(max_workers=min(args.workers, len(targets))) as executor:
        futures = {executor.submit(translate_target, target): target for target in targets}
        for future in concurrent.futures.as_completed(futures):
            target = futures[future]
            locale, values, usage, failures = future.result()
            results[locale] = values
            had_failure = had_failure or bool([f for f in failures if "repaired one" not in f])
            print(json.dumps({"locale": locale, "language": target.language, "translated": len(values), "failures": failures, "request_usage": usage}, ensure_ascii=False))

    complete_fields = [field for field in fields if all(field.id in results.get(locale, {}) for locale in (target.locale for target in targets))]
    draft: dict[str, object] = {"kind": kind, "locales": [target.locale for target in targets], "translations": results}
    draft_path.write_text(json.dumps(draft, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lua_lines = ["-- Generated translation draft. Review before copying into source."]
    for field in complete_fields:
        translated = {locale: results[locale][field.id] for locale in results}
        lua_lines.extend([f"-- {field.id}", render_table(field.source, translated, locales, ""), ""])
    (output_root / "draft.lua").write_text("\n".join(lua_lines), encoding="utf-8")
    print(json.dumps({"summary": {"fields": len(fields), "complete_fields": len(complete_fields), "targets": len(targets), "batches_per_target": len(field_batches), "output": str(output_root), "failed": had_failure}}, ensure_ascii=False))
    return int(had_failure or len(complete_fields) != len(fields))


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (LuaTranslationError, TranslatorError, ValueError) as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1) from error
