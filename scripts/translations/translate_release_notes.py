#!/usr/bin/env python3
"""Translate one complete Markdown release note per target locale."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
import sys
from pathlib import Path

from markdown_validation import validate_markdown
from providers.base import Translator, TranslatorError
from providers.openai_responses import DEFAULT_MODEL, OpenAIResponsesTranslator
from languages import SUPPORTED_TARGETS, TranslationTarget


DEFAULT_DOMAIN_BRIEF = Path(__file__).with_name("domain_brief.md")
DOMAIN_BRIEF_MAX_CHARACTERS = 8000


def parse_target(value: str) -> TranslationTarget:
    try:
        locale, language = value.split("=", 1)
    except ValueError as error:
        raise argparse.ArgumentTypeError("target must be LOCALE=LANGUAGE") from error
    if not locale.strip() or not language.strip():
        raise argparse.ArgumentTypeError("target must have a locale and language")
    return TranslationTarget(locale.strip(), language.strip())


def _translator(provider: str, api_key: str, model: str) -> Translator:
    if provider == "openai":
        return OpenAIResponsesTranslator(api_key=api_key, model=model)
    raise ValueError(f"unsupported provider: {provider}")


def _instructions(
    target_language: str,
    domain_context: str,
    source: str,
    validation_feedback: tuple[str, ...],
) -> str:
    retry_context = ""
    if validation_feedback:
        details: list[str] = []
        if "indented_code_exact" in validation_feedback:
            indented = [line for line in source.splitlines() if line.startswith(("    ", "\t"))]
            details.append("Copy these indented-code lines exactly:\n" + "\n".join(indented))
        if "markdown_marker_counts_exact" in validation_feedback:
            markers = ("**", "~~", "![", "](", "`", "|", "<!--", "-->")
            counts = ", ".join(f"{marker!r}={source.count(marker)}" for marker in markers)
            details.append("Match these source Markdown-token counts exactly: " + counts)
        retry_context = (
            "\n\nA previous translation failed these structural checks:\n- "
            + "\n- ".join(validation_feedback)
            + "\nCorrect those problems in this attempt.\n"
            + "\n".join(details)
        )
    return f"""Translate this complete Markdown release-note file from English into {target_language}.

Return only the translated Markdown file. Do not wrap it in a code fence and do not add commentary.

Preserve the Markdown structure: heading levels, paragraphs, blank-line layout, thematic breaks, lists, tables, blockquotes, HTML, YAML, and footnotes. Keep every URL, link destination, reference identifier, HTML attribute, inline-code span, fenced code block, indented code block, escaped Markdown character, and technical identifier byte-for-byte unchanged. A line beginning with four spaces or a tab is an indented code block; keep its entire content unchanged. Translate visible human-facing prose, including headings, link labels, image alt text, and human-readable link titles. Preserve all meaning: do not omit, combine, summarize, or invent content. Use natural {target_language} punctuation and word order while retaining valid Markdown.

Use this English domain brief to understand the app and disambiguate its terminology. It is context, not text to translate.

--- DOMAIN BRIEF ---
{domain_context}
--- END DOMAIN BRIEF ---

Before returning, compare the result with the source and restore any changed URL, code, identifier, Markdown delimiter, or block structure. In particular, copy every line beginning with four spaces or a tab directly from the source without translating any part of it.{retry_context}"""


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("markdown", type=Path)
    parser.add_argument(
        "--target",
        type=parse_target,
        action="append",
        help="translate only LOCALE=LANGUAGE; repeat as needed (default: all supported languages)",
    )
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--domain-brief", type=Path, default=DEFAULT_DOMAIN_BRIEF)
    parser.add_argument("--provider", choices=("openai",), default="openai")
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument(
        "--retries", type=int, default=0,
        help="optional paid retries after a provider or validation failure (default: none)",
    )
    args = parser.parse_args()
    if args.retries < 0:
        parser.error("--retries cannot be negative")

    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        parser.error("OPENAI_API_KEY is not set")
    translator = _translator(args.provider, api_key, args.model)
    targets = tuple(args.target) if args.target else SUPPORTED_TARGETS

    source = args.markdown.read_text(encoding="utf-8")
    domain_context = args.domain_brief.read_text(encoding="utf-8").strip()
    if not domain_context:
        parser.error("domain brief is empty")
    if len(domain_context) > DOMAIN_BRIEF_MAX_CHARACTERS:
        parser.error(
            f"domain brief is {len(domain_context)} characters; "
            f"maximum is {DOMAIN_BRIEF_MAX_CHARACTERS}"
        )
    args.output_dir.mkdir(parents=True, exist_ok=True)

    def translate(
        target: TranslationTarget,
    ) -> tuple[Path, list[dict[str, object]], tuple[str, ...]]:
        usages: list[dict[str, object]] = []
        feedback: tuple[str, ...] = ()
        for attempt in range(args.retries + 1):
            try:
                result = translator.translate(
                    instructions=_instructions(target.language, domain_context, source, feedback),
                    source_text=source,
                )
            except TranslatorError:
                if attempt == args.retries:
                    raise
                continue
            usages.append(result.usage)
            validation = validate_markdown(source, result.text)
            feedback = validation.failures
            if validation.is_valid or attempt == args.retries:
                destination = args.output_dir / f"{target.locale}.md"
                destination.write_text(result.text, encoding="utf-8")
                return destination, usages, feedback
        raise AssertionError("retry loop did not return or raise")

    valid_count = 0
    invalid: list[dict[str, object]] = []
    provider_failures: list[dict[str, str]] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(targets)) as executor:
        futures = {executor.submit(translate, target): target for target in targets}
        for future in concurrent.futures.as_completed(futures):
            target = futures[future]
            try:
                destination, usages, validation_failures = future.result()
                is_valid = not validation_failures
                valid_count += int(is_valid)
                if not is_valid:
                    invalid.append({
                        "locale": target.locale,
                        "issues": list(validation_failures),
                        "output": str(destination),
                    })
                print(json.dumps({
                    "locale": target.locale,
                    "language": target.language,
                    "output": str(destination),
                    "valid": is_valid,
                    "validation_failures": list(validation_failures),
                    "attempts": len(usages),
                    "request_usage": usages,
                }, ensure_ascii=False))
            except TranslatorError as error:
                provider_failures.append({"locale": target.locale, "error": str(error)})
    print(json.dumps({
        "summary": {
            "valid": valid_count,
            "invalid": sorted(invalid, key=lambda item: str(item["locale"])),
            "provider_failures": sorted(
                provider_failures, key=lambda item: item["locale"]
            ),
        }
    }, ensure_ascii=False))
    return int(bool(invalid or provider_failures))


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except TranslatorError as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1) from error
