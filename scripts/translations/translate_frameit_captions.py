#!/usr/bin/env python3
"""Translate the eight Frameit screenshot captions."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import re
from dataclasses import dataclass
from pathlib import Path

from languages import SUPPORTED_TARGETS, TranslationTarget, play_locale
from translation_runtime import (
    add_provider_arguments,
    create_translator,
    load_domain_brief,
    parse_target,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_FRAMEIT_ROOT = PROJECT_ROOT / "fastlane/frameit/screenshots"
DEFAULT_RAW_OUTPUT_DIR = Path("/tmp/track-and-graph-frameit-translations")
SOURCE_LOCALE = "en-GB"
CAPTION_KEYS = tuple(str(number) for number in range(1, 9))
CAPTION_RE = re.compile(r'^"(?P<key>[1-8])"\s*=\s*"(?P<value>(?:[^"\\]|\\.)*)";$')


@dataclass(frozen=True)
class TranslationOutcome:
    target: TranslationTarget
    captions: dict[str, str] | None
    issues: tuple[str, ...]
    raw_path: Path
    usage: dict[str, object]


def _unescape(value: str) -> str:
    return value.replace(r'\"', '"').replace(r"\\", "\\")


def _escape(value: str) -> str:
    return value.replace("\\", r"\\").replace('"', r'\"').replace("\n", r"\n")


def parse_title_strings(text: str) -> dict[str, str]:
    captions: dict[str, str] = {}
    for line_number, raw_line in enumerate(text.splitlines(), 1):
        line = raw_line.strip()
        if not line:
            continue
        match = CAPTION_RE.fullmatch(line)
        if match is None:
            raise ValueError(f"invalid title.strings syntax on line {line_number}: {raw_line}")
        key = match.group("key")
        if key in captions:
            raise ValueError(f"duplicate caption key {key}")
        captions[key] = _unescape(match.group("value"))
    if tuple(sorted(captions, key=int)) != CAPTION_KEYS:
        raise ValueError("title.strings must contain exactly keys 1 through 8")
    return captions


def parse_response(text: str) -> dict[str, str]:
    candidate = text.strip()
    if candidate.startswith("```") and candidate.endswith("```"):
        candidate = candidate.split("\n", 1)[1].rsplit("```", 1)[0].strip()
    value = json.loads(candidate)
    if not isinstance(value, dict) or set(value) != set(CAPTION_KEYS):
        raise ValueError("response must be an object containing exactly keys 1 through 8")
    if not all(isinstance(value[key], str) and value[key].strip() for key in CAPTION_KEYS):
        raise ValueError("every caption must be a non-empty string")
    return {key: value[key].strip() for key in CAPTION_KEYS}


def validate_translation(source: dict[str, str], translated: dict[str, str]) -> tuple[str, ...]:
    issues: list[str] = []
    for key in CAPTION_KEYS:
        if translated[key].count("Lua") != source[key].count("Lua"):
            issues.append(f"caption {key} changed the number of 'Lua' occurrences")
        if "\n" in translated[key]:
            issues.append(f"caption {key} contains a newline")
    return tuple(issues)


def render_title_strings(captions: dict[str, str]) -> str:
    return "\n".join(f'"{key}" = "{_escape(captions[key])}";' for key in CAPTION_KEYS) + "\n"


def instructions(target: TranslationTarget, domain_brief: str) -> str:
    locale_note = ""
    if target.locale == "sq":
        locale_note = "\n- Albanian must use the Latin alphabet; do not confuse it with Macedonian."
    return f"""Translate eight short Google Play screenshot captions from British English into {target.language} ({target.locale}).

Return only one valid JSON object with exactly the string keys "1" through "8". Do not wrap it in Markdown.

Requirements:
- Translate every caption naturally and concisely; add or omit no product claims.
- Preserve `Lua` exactly.
- Use the target language's native script.
- Keep each value on one line and suitable as a short heading above a phone screenshot.
- Do not translate or renumber the JSON keys.{locale_note}

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
        captions = parse_response(result.text)
        issues = validate_translation(source, captions)
    except (json.JSONDecodeError, ValueError) as error:
        return TranslationOutcome(target, None, (str(error),), raw_path, result.usage)
    return TranslationOutcome(target, captions, issues, raw_path, result.usage)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    selection = parser.add_mutually_exclusive_group(required=True)
    selection.add_argument("--all-targets", action="store_true")
    selection.add_argument("--target", action="append", type=parse_target)
    parser.add_argument("--frameit-root", type=Path, default=DEFAULT_FRAMEIT_ROOT)
    parser.add_argument("--raw-output-dir", type=Path, default=DEFAULT_RAW_OUTPUT_DIR)
    parser.add_argument("--max-workers", type=int, default=8)
    add_provider_arguments(parser)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    targets = SUPPORTED_TARGETS if args.all_targets else tuple(args.target)
    source = parse_title_strings(
        (args.frameit_root / SOURCE_LOCALE / "title.strings").read_text(encoding="utf-8")
    )
    domain_brief = load_domain_brief(args.domain_brief)
    translator = create_translator(args.provider, args.model)
    outcomes: list[TranslationOutcome] = []
    request_failures: list[str] = []

    with concurrent.futures.ThreadPoolExecutor(max_workers=min(args.max_workers, len(targets))) as executor:
        futures = {
            executor.submit(
                translate_target,
                translator,
                target,
                source,
                domain_brief,
                args.raw_output_dir,
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
                    "status": "request_failed",
                    "issues": [str(error)],
                }, ensure_ascii=False), flush=True)
                continue
            outcomes.append(outcome)
            if outcome.captions is not None and not outcome.issues:
                destination = args.frameit_root / play_locale(target.locale) / "title.strings"
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_text(render_title_strings(outcome.captions), encoding="utf-8")
            print(json.dumps({
                "locale": target.locale,
                "play_locale": play_locale(target.locale),
                "status": "written" if outcome.captions is not None and not outcome.issues else "invalid",
                "issues": outcome.issues,
                "raw_response": str(outcome.raw_path),
                "request_usage": outcome.usage,
            }, ensure_ascii=False), flush=True)

    invalid = [outcome.target.locale for outcome in outcomes if outcome.captions is None or outcome.issues]
    print(json.dumps({
        "summary": {
            "requested": len(targets),
            "written": len(outcomes) - len(invalid),
            "invalid": sorted(invalid + request_failures),
        }
    }, ensure_ascii=False))
    return 1 if invalid or request_failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
