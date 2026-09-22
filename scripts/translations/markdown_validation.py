"""Deterministic checks that translation preserved Markdown structure."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path


URL_RE = re.compile(r"https?://[^\s<>)]+")
LINK_DESTINATION_RE = re.compile(r"!?\[[^\]\n]*\]\(\s*(<?[^)\s]+>?)")
INLINE_CODE_RE = re.compile(r"(`+)(.+?)\1")
HTML_COMMENT_RE = re.compile(r"<!--[\s\S]*?-->")
HTML_TAG_RE = re.compile(r"</?[A-Za-z][^>]*>")
ESCAPE_RE = re.compile(r"\\.")
REFERENCE_ID_RE = re.compile(
    r"(?:^\s*\[([^]\n]+)\]:|\]\[([^]\n]+)\]|\[\^([^]\n]+)\])", re.MULTILINE
)
TASK_MARKER_RE = re.compile(r"^\s*[-+*]\s+\[([ xX])\]", re.MULTILINE)
MARKDOWN_MARKERS = ("**", "~~", "![", "](", "`", "|", "<!--", "-->")


@dataclass(frozen=True)
class ValidationResult:
    checks: dict[str, bool]

    @property
    def failures(self) -> tuple[str, ...]:
        return tuple(name for name, passed in self.checks.items() if not passed)

    @property
    def is_valid(self) -> bool:
        return not self.failures


def _fenced_blocks(markdown: str) -> list[str]:
    blocks: list[str] = []
    lines = markdown.splitlines(keepends=True)
    index = 0
    while index < len(lines):
        match = re.match(r"^ {0,3}(`{3,}|~{3,})", lines[index])
        if not match:
            index += 1
            continue
        marker = match.group(1)
        start = index
        index += 1
        close = re.compile(rf"^ {{0,3}}{re.escape(marker[0])}{{{len(marker)},}}\s*$")
        while index < len(lines):
            if close.match(lines[index].rstrip("\r\n")):
                index += 1
                break
            index += 1
        blocks.append("".join(lines[start:index]))
    return blocks


def _indented_code(markdown: str) -> list[str]:
    return [line for line in markdown.splitlines() if re.match(r"^(?: {4}|\t)", line)]


def _front_matter(markdown: str) -> str | None:
    if not markdown.startswith("---\n"):
        return None
    positions = [position for marker in ("\n---\n", "\n...\n") if (position := markdown.find(marker, 4)) >= 0]
    return None if not positions else markdown[: min(positions) + 5]


def _reference_ids(markdown: str) -> list[str]:
    return [
        next(group for group in match.groups() if group is not None)
        for match in REFERENCE_ID_RE.finditer(markdown)
    ]


def _block_signature(markdown: str) -> list[str]:
    signature: list[str] = []
    in_fence = False
    fence_character = ""
    for line in markdown.splitlines():
        stripped = line.strip()
        fence = re.match(r"^ {0,3}(`{3,}|~{3,})", line)
        if fence:
            marker = fence.group(1)
            signature.append("FENCE")
            if not in_fence:
                in_fence, fence_character = True, marker[0]
            elif marker[0] == fence_character:
                in_fence = False
        elif in_fence:
            signature.append("CODE")
        elif not stripped:
            signature.append("BLANK")
        elif re.match(r"^ {0,3}(?:-{3,}|\*{3,}|_{3,})\s*$", line):
            signature.append("HR")
        elif match := re.match(r"^( {0,3})(#{1,6})\s+", line):
            signature.append(f"H{len(match.group(2))}")
        elif re.match(r"^\s*[-+*]\s+", line):
            signature.append("UL")
        elif re.match(r"^\s*\d+[.)]\s+", line):
            signature.append("OL")
        elif re.match(r"^\s*>", line):
            signature.append("QUOTE")
        elif re.match(r"^ {4}|^\t", line):
            signature.append("INDENTED_CODE")
        elif "|" in line:
            signature.append("TABLE")
        else:
            signature.append("TEXT")
    while signature and signature[-1] == "BLANK":
        signature.pop()
    return signature


def validate_markdown(source: str, translated: str) -> ValidationResult:
    checks = {
        "urls_exact": URL_RE.findall(source) == URL_RE.findall(translated),
        "link_destinations_exact": LINK_DESTINATION_RE.findall(source) == LINK_DESTINATION_RE.findall(translated),
        "fenced_code_exact": _fenced_blocks(source) == _fenced_blocks(translated),
        "indented_code_exact": _indented_code(source) == _indented_code(translated),
        "inline_code_exact": [m.group(0) for m in INLINE_CODE_RE.finditer(source)]
        == [m.group(0) for m in INLINE_CODE_RE.finditer(translated)],
        "front_matter_exact": _front_matter(source) == _front_matter(translated),
        "html_comments_exact": HTML_COMMENT_RE.findall(source) == HTML_COMMENT_RE.findall(translated),
        "html_tags_exact": HTML_TAG_RE.findall(source) == HTML_TAG_RE.findall(translated),
        "escapes_exact": ESCAPE_RE.findall(source) == ESCAPE_RE.findall(translated),
        "reference_ids_exact": _reference_ids(source) == _reference_ids(translated),
        "task_markers_exact": TASK_MARKER_RE.findall(source) == TASK_MARKER_RE.findall(translated),
        "block_structure_exact": _block_signature(source) == _block_signature(translated),
        "markdown_marker_counts_exact": all(
            source.count(marker) == translated.count(marker) for marker in MARKDOWN_MARKERS
        ),
    }
    return ValidationResult(checks)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("translations", type=Path, nargs="+")
    args = parser.parse_args()
    source = args.source.read_text(encoding="utf-8")
    invalid: list[dict[str, object]] = []
    for path in args.translations:
        result = validate_markdown(source, path.read_text(encoding="utf-8"))
        if not result.is_valid:
            invalid.append({"file": str(path), "issues": list(result.failures)})
    print(json.dumps({"valid": len(args.translations) - len(invalid), "invalid": invalid}))
    return int(bool(invalid))


if __name__ == "__main__":
    sys.exit(main())
