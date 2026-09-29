#!/usr/bin/env python3
"""Draft, translate, write, and validate concise distribution release notes."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
import re
import shlex
import subprocess
import sys
import tempfile
from pathlib import Path

from languages import ALL_LANGUAGES, SUPPORTED_TARGETS, TranslationTarget, play_locale
from markdown_validation import validate_markdown
from providers.base import Translator, TranslatorError
from translation_runtime import (
    add_provider_arguments,
    create_translator,
    load_domain_brief,
    parse_target,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]
BUILD_GRADLE = PROJECT_ROOT / "app/app/build.gradle.kts"
METADATA_ROOT = PROJECT_ROOT / "fastlane/metadata/android"
MAX_CHARACTERS = 500
START_MARKER = "<!-- RELEASE-NOTES-START -->"
END_MARKER = "<!-- RELEASE-NOTES-END -->"
RELEASE_TAG_RE = re.compile(
    r"^(?P<prefix>v|playstore-v|rc-v)(?P<version>\d+(?:\.\d+)*)$"
)
VERSION_CODE_RE = re.compile(r"^\s*versionCode\s*=\s*(\d+)\s*$", re.MULTILINE)
VERSION_NAME_RE = re.compile(
    r'^\s*versionName\s*=\s*"([^"]+)"\s*$', re.MULTILINE
)


def read_version(build_gradle: Path = BUILD_GRADLE) -> tuple[str, str]:
    content = build_gradle.read_text(encoding="utf-8")
    codes = VERSION_CODE_RE.findall(content)
    names = VERSION_NAME_RE.findall(content)
    if len(codes) != 1 or len(names) != 1:
        raise ValueError(
            f"Expected exactly one versionCode and versionName in {build_gradle}"
        )
    return codes[0], names[0]


def _release_tag_key(tag: str) -> tuple[tuple[int, ...], int] | None:
    match = RELEASE_TAG_RE.fullmatch(tag)
    if not match:
        return None
    prefix_rank = {"rc-v": 0, "playstore-v": 1, "v": 2}
    version = tuple(int(part) for part in match.group("version").split("."))
    return version, prefix_rank[match.group("prefix")]


def find_previous_release_tag() -> str:
    result = subprocess.run(
        [
            "jj",
            "log",
            "-r",
            "tags()",
            "--no-graph",
            "-T",
            'self.tags().map(|tag| tag.name()).join("\\n") ++ "\\n"',
        ],
        cwd=PROJECT_ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    tagged = [
        (key, tag)
        for tag in result.stdout.splitlines()
        if (key := _release_tag_key(tag)) is not None
    ]
    if not tagged:
        raise ValueError("No release tag matching v*, rc-v*, or playstore-v* was found")
    return max(tagged)[1]


def commits_since(tag: str) -> str:
    if _release_tag_key(tag) is None:
        raise ValueError(f"Invalid release tag {tag!r}")
    result = subprocess.run(
        [
            "jj",
            "log",
            "-r",
            f"{tag}..@",
            "--no-graph",
            "-T",
            'commit_id.short(12) ++ " " ++ description.first_line() ++ "\\n"',
        ],
        cwd=PROJECT_ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def editor_document(previous_tag: str, commits: str) -> str:
    reference = commits or "(No described commits found.)"
    commented_reference = "\n".join(f"<!-- {line} -->" for line in reference.splitlines())
    return f"""<!-- Write the concise English release notes between the markers. -->
<!-- Google Play permits at most {MAX_CHARACTERS} characters, including line breaks. -->
{START_MARKER}

{END_MARKER}

<!-- Commit reference since {previous_tag}; this section is never published. -->
{commented_reference}
"""


def extract_note(document: str) -> str:
    if document.count(START_MARKER) != 1 or document.count(END_MARKER) != 1:
        raise ValueError("Draft must contain exactly one start marker and one end marker")
    before, remainder = document.split(START_MARKER, 1)
    note, after = remainder.split(END_MARKER, 1)
    if END_MARKER in before or START_MARKER in after:
        raise ValueError("Release-note markers are out of order")
    note = note.strip()
    if not note:
        raise ValueError("English release notes are empty")
    return note


def edit_release_notes(document: str, editor: str | None = None) -> tuple[Path, str]:
    with tempfile.NamedTemporaryFile(
        mode="w", encoding="utf-8", prefix="track-and-graph-release-notes-",
        suffix=".md", delete=False,
    ) as draft:
        draft.write(document)
        path = Path(draft.name)
    command = shlex.split(editor or os.environ.get("EDITOR", "nvim"))
    if not command:
        raise ValueError("Editor command is empty")
    subprocess.run([*command, str(path)], check=True)
    return path, extract_note(path.read_text(encoding="utf-8"))


def translation_instructions(
    target: TranslationTarget, domain_context: str, source: str
) -> str:
    return f"""Translate these concise app release notes from English into {target.language}.

Return only the translated release notes. Do not add commentary, headings, code fences, or quotation marks. Preserve the same Markdown/list structure, line count, URLs, technical identifiers, and release-note items. Preserve the meaning of every item, but phrase it naturally and concisely in {target.language}.

The complete result must be at most {MAX_CHARACTERS} Unicode characters, including spaces and line breaks. The English source is {len(source)} characters. Before returning, count the translated result and shorten its wording if necessary without dropping an item or changing its meaning.

Use this English domain brief only as context:

--- DOMAIN BRIEF ---
{domain_context}
--- END DOMAIN BRIEF ---"""


def text_issues(text: str) -> tuple[str, ...]:
    issues: list[str] = []
    if not text:
        issues.append("release notes are empty")
        return tuple(issues)
    if len(text) > MAX_CHARACTERS:
        issues.append(
            f"release notes are {len(text)} characters; limit is {MAX_CHARACTERS}"
        )
    return tuple(issues)


def note_issues(source: str, translated: str) -> tuple[str, ...]:
    issues = list(text_issues(translated))
    if not translated:
        return tuple(issues)
    issues.extend(validate_markdown(source, translated).failures)
    return tuple(issues)


def destination_for(
    version_code: str,
    target: TranslationTarget,
    metadata_root: Path = METADATA_ROOT,
) -> Path:
    return (
        metadata_root
        / play_locale(target.locale)
        / "changelogs"
        / f"{version_code}.txt"
    )


def write_note(
    version_code: str,
    target: TranslationTarget,
    text: str,
    metadata_root: Path = METADATA_ROOT,
) -> Path:
    destination = destination_for(version_code, target, metadata_root)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(text, encoding="utf-8")
    return destination


def translate_all(
    *,
    source: str,
    version_code: str,
    targets: tuple[TranslationTarget, ...],
    translator: Translator,
    domain_context: str,
    metadata_root: Path = METADATA_ROOT,
) -> tuple[list[dict[str, object]], list[dict[str, str]]]:
    invalid: list[dict[str, object]] = []
    provider_failures: list[dict[str, str]] = []

    def translate(target: TranslationTarget) -> tuple[TranslationTarget, str, dict[str, object]]:
        result = translator.translate(
            instructions=translation_instructions(target, domain_context, source),
            source_text=source,
        )
        return target, result.text.strip(), result.usage

    with concurrent.futures.ThreadPoolExecutor(max_workers=len(targets)) as executor:
        futures = {executor.submit(translate, target): target for target in targets}
        for future in concurrent.futures.as_completed(futures):
            target = futures[future]
            try:
                _, text, usage = future.result()
                destination = write_note(
                    version_code, target, text, metadata_root=metadata_root
                )
                issues = note_issues(source, text)
                if issues:
                    invalid.append(
                        {
                            "locale": target.locale,
                            "characters": len(text),
                            "issues": list(issues),
                            "output": str(destination),
                        }
                    )
                print(
                    json.dumps(
                        {
                            "locale": target.locale,
                            "language": target.language,
                            "characters": len(text),
                            "output": str(destination),
                            "valid": not issues,
                            "issues": list(issues),
                            "request_usage": usage,
                        },
                        ensure_ascii=False,
                    )
                )
            except TranslatorError as error:
                provider_failures.append(
                    {"locale": target.locale, "error": str(error)}
                )
    return invalid, provider_failures


def validate_outputs(
    *,
    version_code: str,
    targets: tuple[TranslationTarget, ...] = ALL_LANGUAGES,
    metadata_root: Path = METADATA_ROOT,
) -> list[dict[str, object]]:
    english_target = ALL_LANGUAGES[0]
    english_path = destination_for(version_code, english_target, metadata_root)
    if not english_path.is_file():
        return [
            {
                "locale": target.locale,
                "issues": [
                    f"missing {destination_for(version_code, target, metadata_root)}"
                ],
            }
            for target in targets
        ]
    source = english_path.read_text(encoding="utf-8").strip()
    invalid: list[dict[str, object]] = []
    for target in targets:
        path = destination_for(version_code, target, metadata_root)
        if not path.is_file():
            invalid.append({"locale": target.locale, "issues": [f"missing {path}"]})
            continue
        text = path.read_text(encoding="utf-8").strip()
        issues = (
            text_issues(text)
            if target.locale == "en"
            else note_issues(source, text)
        )
        if issues:
            invalid.append(
                {
                    "locale": target.locale,
                    "characters": len(text),
                    "issues": list(issues),
                    "output": str(path),
                }
            )
    return invalid


def create_command(args: argparse.Namespace) -> int:
    version_code, version_name = read_version(args.build_gradle)
    previous_tag = find_previous_release_tag()
    draft_path, source = edit_release_notes(
        editor_document(previous_tag, commits_since(previous_tag)), args.editor
    )
    english_target = ALL_LANGUAGES[0]
    english_path = write_note(
        version_code, english_target, source, metadata_root=args.metadata_root
    )
    english_issues = list(text_issues(source))
    try:
        translator = create_translator(args.provider, args.model)
        domain_context = load_domain_brief(args.domain_brief)
    except ValueError as error:
        raise ValueError(
            f"English release notes were retained at {english_path}; {error}"
        ) from error
    targets = tuple(args.target) if args.target else SUPPORTED_TARGETS
    invalid, provider_failures = translate_all(
        source=source,
        version_code=version_code,
        targets=targets,
        translator=translator,
        domain_context=domain_context,
        metadata_root=args.metadata_root,
    )
    if english_issues:
        invalid.append(
            {
                "locale": "en",
                "characters": len(source),
                "issues": english_issues,
                "output": str(english_path),
            }
        )
    print(
        json.dumps(
            {
                "summary": {
                    "versionName": version_name,
                    "versionCode": version_code,
                    "draft": str(draft_path),
                    "valid": len(targets) + 1 - len(invalid) - len(provider_failures),
                    "invalid": sorted(invalid, key=lambda item: str(item["locale"])),
                    "provider_failures": sorted(
                        provider_failures, key=lambda item: item["locale"]
                    ),
                }
            },
            ensure_ascii=False,
        )
    )
    return int(bool(invalid or provider_failures))


def validate_command(args: argparse.Namespace) -> int:
    version_code, version_name = read_version(args.build_gradle)
    invalid = validate_outputs(
        version_code=version_code,
        metadata_root=args.metadata_root,
    )
    print(
        json.dumps(
            {
                "summary": {
                    "versionName": version_name,
                    "versionCode": version_code,
                    "valid": len(ALL_LANGUAGES) - len(invalid),
                    "invalid": invalid,
                }
            },
            ensure_ascii=False,
        )
    )
    return int(bool(invalid))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.set_defaults(build_gradle=BUILD_GRADLE, metadata_root=METADATA_ROOT)
    subparsers = parser.add_subparsers(dest="command", required=True)

    create = subparsers.add_parser("create")
    create.add_argument("--editor")
    create.add_argument(
        "--target",
        type=parse_target,
        action="append",
        help="translate only LOCALE=LANGUAGE; repeat as needed (default: all)",
    )
    add_provider_arguments(create)
    create.set_defaults(handler=create_command)

    validate = subparsers.add_parser("validate")
    validate.set_defaults(handler=validate_command)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        return args.handler(args)
    except (OSError, subprocess.CalledProcessError, ValueError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
