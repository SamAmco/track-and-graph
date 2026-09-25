#!/usr/bin/env python3
"""Translate Google Play listing metadata into the shared target languages."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
from dataclasses import dataclass
from pathlib import Path

from languages import SUPPORTED_TARGETS, TranslationTarget, play_locale
from translation_runtime import (
    DEFAULT_DOMAIN_BRIEF,
    add_provider_arguments,
    create_translator,
    load_domain_brief,
    parse_target,
)


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_METADATA_ROOT = REPO_ROOT / "fastlane/metadata/android"
DEFAULT_SOURCE_LOCALE = "en-GB"
DEFAULT_RAW_OUTPUT_DIR = Path("/tmp/track-and-graph-fastlane-translations")
FIELDS = ("title", "short_description", "full_description")
CHARACTER_LIMITS = {"title": 30, "short_description": 80, "full_description": 4000}

@dataclass(frozen=True)
class TranslationOutcome:
    target: TranslationTarget
    values: dict[str, str] | None
    issues: tuple[str, ...]
    usage: dict[str, object]
    raw_path: Path


def load_source(metadata_root: Path, source_locale: str) -> dict[str, str]:
    source_dir = metadata_root / source_locale
    return {
        field: (source_dir / f"{field}.txt").read_text(encoding="utf-8").strip()
        for field in FIELDS
    }


def parse_response(text: str) -> dict[str, str]:
    candidate = text.strip()
    if candidate.startswith("```") and candidate.endswith("```"):
        candidate = candidate.split("\n", 1)[1].rsplit("```", 1)[0].strip()
    value = json.loads(candidate)
    if not isinstance(value, dict):
        raise ValueError("response is not a JSON object")
    if set(value) != set(FIELDS):
        raise ValueError(f"response must contain exactly these fields: {', '.join(FIELDS)}")
    if not all(isinstance(value[field], str) and value[field].strip() for field in FIELDS):
        raise ValueError("every response field must be a non-empty string")
    return {field: value[field].strip() for field in FIELDS}


def validate_translation(source: dict[str, str], translated: dict[str, str]) -> tuple[str, ...]:
    issues: list[str] = []
    if not translated["title"].startswith("Track & Graph: "):
        issues.append("title must start with exact text 'Track & Graph: '")
    for field, limit in CHARACTER_LIMITS.items():
        length = len(translated[field])
        if length > limit:
            issues.append(f"{field} is {length} characters; limit is {limit}")
    source_description = source["full_description"]
    translated_description = translated["full_description"]
    if translated_description.count("Track & Graph") != source_description.count("Track & Graph"):
        issues.append("full_description changed the number of 'Track & Graph' occurrences")
    if translated_description.count("Lua") != source_description.count("Lua"):
        issues.append("full_description changed the number of 'Lua' occurrences")
    if len(translated_description.split("\n\n")) != len(source_description.split("\n\n")):
        issues.append("full_description changed the blank-line-separated block count")
    return tuple(issues)


def instructions(target: TranslationTarget, domain_brief: str) -> str:
    locale_note = ""
    if target.locale == "sq":
        locale_note = "\n- Albanian must use the Latin alphabet; do not confuse it with Macedonian."
    return f"""Translate a Google Play store listing from British English into {target.language} ({target.locale}).

Return only one valid JSON object with exactly these string fields: title, short_description, full_description. Do not wrap it in Markdown.

Requirements:
- Translate every user-facing phrase naturally and faithfully; add or omit no product claims.
- Preserve the product name `Track & Graph` byte-for-byte everywhere. Preserve `Lua` exactly.
- The title must begin exactly `Track & Graph: ` and be at most 30 Unicode characters total. Translate "Habit Tracker" concisely; use the target-language equivalent of "Habits" if needed to fit.
- short_description must be at most 80 Unicode characters.
- full_description must be at most 4000 Unicode characters and retain exactly the source's five text blocks separated by blank lines.
- Preserve the precise meaning that the app is completely free and open source, has no ads, has no accounts, has no paywalled features, stores data only on the user's device, and supports backups.
- Use the native script and idiomatic store-listing language for the target locale.
- Do not translate JSON field names.{locale_note}

App terminology context:
{domain_brief}"""


def translate_target(translator, target: TranslationTarget, source: dict[str, str], domain_brief: str, raw_dir: Path) -> TranslationOutcome:
    result = translator.translate(
        instructions=instructions(target, domain_brief),
        source_text=json.dumps(source, ensure_ascii=False),
    )
    raw_dir.mkdir(parents=True, exist_ok=True)
    raw_path = raw_dir / f"{target.locale}.txt"
    raw_path.write_text(result.text, encoding="utf-8")
    try:
        values = parse_response(result.text)
        issues = validate_translation(source, values)
    except (json.JSONDecodeError, ValueError) as error:
        return TranslationOutcome(target, None, (str(error),), result.usage, raw_path)
    return TranslationOutcome(target, values, issues, result.usage, raw_path)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    selection = parser.add_mutually_exclusive_group(required=True)
    selection.add_argument("--all-targets", action="store_true")
    selection.add_argument("--target", action="append", type=parse_target)
    parser.add_argument("--metadata-root", type=Path, default=DEFAULT_METADATA_ROOT)
    parser.add_argument("--source-locale", default=DEFAULT_SOURCE_LOCALE)
    parser.add_argument("--raw-output-dir", type=Path, default=DEFAULT_RAW_OUTPUT_DIR)
    parser.add_argument("--max-workers", type=int, default=8)
    add_provider_arguments(parser)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    targets = SUPPORTED_TARGETS if args.all_targets else tuple(args.target)
    source = load_source(args.metadata_root, args.source_locale)
    domain_brief = load_domain_brief(args.domain_brief)
    translator = create_translator(args.provider, args.model)
    outcomes: list[TranslationOutcome] = []
    request_failures: list[str] = []

    with concurrent.futures.ThreadPoolExecutor(max_workers=min(args.max_workers, len(targets))) as executor:
        futures = {
            executor.submit(
                translate_target, translator, target, source, domain_brief, args.raw_output_dir
            ): target
            for target in targets
        }
        for future in concurrent.futures.as_completed(futures):
            target = futures[future]
            try:
                outcome = future.result()
            except Exception as error:
                request_failures.append(target.locale)
                print(json.dumps({
                    "locale": target.locale,
                    "play_locale": play_locale(target.locale),
                    "status": "request_failed",
                    "issues": [str(error)],
                    "raw_response": None,
                }, ensure_ascii=False), flush=True)
                continue
            outcomes.append(outcome)
            if outcome.values is not None and not outcome.issues:
                destination = args.metadata_root / play_locale(outcome.target.locale)
                destination.mkdir(parents=True, exist_ok=True)
                for field, value in outcome.values.items():
                    (destination / f"{field}.txt").write_text(value + "\n", encoding="utf-8")
            print(json.dumps({
                "locale": outcome.target.locale,
                "play_locale": play_locale(outcome.target.locale),
                "status": "written" if outcome.values is not None and not outcome.issues else "invalid",
                "issues": outcome.issues,
                "raw_response": str(outcome.raw_path),
                "request_usage": outcome.usage,
            }, ensure_ascii=False), flush=True)

    invalid = [outcome for outcome in outcomes if outcome.values is None or outcome.issues]
    print(json.dumps({
        "summary": {
            "requested": len(targets),
            "written": len(outcomes) - len(invalid),
            "invalid": sorted(
                [outcome.target.locale for outcome in invalid] + request_failures
            ),
        }
    }, ensure_ascii=False))
    return 1 if invalid or request_failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
