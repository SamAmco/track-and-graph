"""Extraction, validation, rendering, and application for Lua catalog copy."""

from __future__ import annotations

import json
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from markdown_validation import validate_markdown


REPO_ROOT = Path(__file__).resolve().parents[2]
LUA_ROOT = REPO_ROOT / "lua"


class LuaTranslationError(ValueError):
    pass


@dataclass(frozen=True)
class CopyField:
    id: str
    source: str
    markdown: bool
    function_id: str | None = None
    path: str | None = None
    existing_locales: frozenset[str] = frozenset()


def export_copy(kind: str, selector: str | None = None) -> list[CopyField]:
    command = ["lua", "tools/export-translatable-copy.lua", kind]
    if selector:
        command.append(selector)
    result = subprocess.run(command, cwd=LUA_ROOT, text=True, capture_output=True, check=False)
    if result.returncode:
        raise LuaTranslationError(result.stderr.strip() or result.stdout.strip())
    payload = json.loads(result.stdout)
    fields: list[CopyField] = []
    if kind == "shared":
        for item in payload["items"]:
            fields.append(CopyField(
                item["id"], item["source"], False,
                existing_locales=frozenset(item.get("existing", {})),
            ))
    else:
        for function in payload["items"]:
            for field in function["fields"]:
                fields.append(CopyField(
                    f'{function["id"]}/{field["id"]}', field["source"],
                    bool(field["markdown"]), function["id"], function["path"],
                ))
    return fields


def parse_response(fields: Iterable[CopyField], response_text: str) -> dict[str, str]:
    expected = {field.id: field for field in fields}
    try:
        payload = json.loads(response_text)
    except json.JSONDecodeError as error:
        raise LuaTranslationError(f"response is not valid JSON: {error}") from error
    if not isinstance(payload, dict) or set(payload) != {"translations"}:
        raise LuaTranslationError("response must contain only a 'translations' array")
    if not isinstance(payload["translations"], list):
        raise LuaTranslationError("'translations' must be an array")
    parsed: dict[str, str] = {}
    for item in payload["translations"]:
        if not isinstance(item, dict) or set(item) != {"id", "value"}:
            raise LuaTranslationError("each translation must contain only 'id' and 'value'")
        identifier, value = item["id"], item["value"]
        if not isinstance(identifier, str) or identifier not in expected:
            raise LuaTranslationError(f"unexpected translation id: {identifier!r}")
        if identifier in parsed:
            raise LuaTranslationError(f"duplicate translation id: {identifier}")
        if not isinstance(value, str) or not value.strip():
            raise LuaTranslationError(f"translation {identifier} must be a non-empty string")
        if expected[identifier].markdown:
            failures = validate_markdown(expected[identifier].source, value).failures
            if failures:
                raise LuaTranslationError(f"translation {identifier} changed Markdown: {', '.join(failures)}")
        parsed[identifier] = value
    missing = sorted(expected.keys() - parsed.keys())
    if missing:
        raise LuaTranslationError("response omitted translation ids: " + ", ".join(missing))
    return parsed


def parse_response_with_repair(
    fields: Iterable[CopyField], response_text: str
) -> tuple[dict[str, str], bool]:
    fields = tuple(fields)
    original_error: LuaTranslationError | None = None
    try:
        return parse_response(fields, response_text), False
    except LuaTranslationError as error:
        original_error = error
        try:
            json.loads(response_text)
        except json.JSONDecodeError as json_error:
            positions = range(max(0, json_error.pos - 2), min(len(response_text), json_error.pos + 2) + 1)
        else:
            raise original_error
    candidates: dict[str, dict[str, str]] = {}
    for position in positions:
        variants = [response_text[:position] + c + response_text[position:] for c in ('}', ']', ',', '"', ':')]
        if position < len(response_text):
            variants.append(response_text[:position] + response_text[position + 1:])
        for candidate in variants:
            try:
                values = parse_response(fields, candidate)
            except LuaTranslationError:
                continue
            candidates[json.dumps(values, ensure_ascii=False, sort_keys=True)] = values
    if len(candidates) != 1:
        raise LuaTranslationError("automatic JSON repair did not find exactly one fully validated result") from original_error
    return next(iter(candidates.values())), True


def batches(fields: Iterable[CopyField], max_characters: int) -> list[list[CopyField]]:
    result: list[list[CopyField]] = []
    current: list[CopyField] = []
    size = 0
    for field in fields:
        field_size = len(field.id) + len(field.source) + 80
        if current and size + field_size > max_characters:
            result.append(current)
            current, size = [], 0
        current.append(field)
        size += field_size
    if current:
        result.append(current)
    return result


def lua_string(value: str, indent: str = "") -> str:
    if "\n" not in value:
        return json.dumps(value, ensure_ascii=False)
    equals = ""
    while f"]{equals}]" in value:
        equals += "="
    return f"[{equals}[\n{value.rstrip()}\n{indent}]{equals}]"


def render_table(source: str, translations: dict[str, str], locales: Iterable[str], indent: str) -> str:
    lines = ["{"]
    values = {"en": source, **translations}
    for locale in locales:
        value = values[locale]
        rendered = lua_string(value, indent + "\t")
        lines.append(f'{indent}\t["{locale}"] = {rendered},')
    lines.append(indent + "}")
    return "\n".join(lines)


def render_failure(fields: Iterable[CopyField], recovered: dict[str, str], locale: str, error: str) -> str:
    lines = [f"-- Translation failure for {locale}: {error}", "-- Repair the FIXME values, then use this file as a recovery reference.", "return {"]
    for field in fields:
        value = recovered.get(field.id, field.source)
        marker = "" if field.id in recovered else " -- FIXME: provider value missing or invalid"
        rendered = lua_string(value, "\t")
        lines.append(f"\t[{json.dumps(field.id)}] = {rendered},{marker}")
    lines.append("}\n")
    return "\n".join(lines)


def recover_items(response_text: str, expected_ids: set[str]) -> dict[str, str]:
    recovered: dict[str, str] = {}
    decoder = json.JSONDecoder()
    for match in re.finditer(r'\{\s*"id"\s*:', response_text):
        try:
            item, _ = decoder.raw_decode(response_text, match.start())
        except json.JSONDecodeError:
            continue
        if isinstance(item, dict) and set(item) == {"id", "value"} and item["id"] in expected_ids and isinstance(item["value"], str):
            recovered[item["id"]] = item["value"]
    return recovered


def _matching_brace(source: str, opening: int) -> int:
    depth = 0
    index = opening
    while index < len(source):
        if source.startswith("--", index):
            long_comment = re.match(r"--\[(=*)\[", source[index:])
            if long_comment:
                closing = "]" + long_comment.group(1) + "]"
                end = source.find(closing, index + long_comment.end())
                index = len(source) if end < 0 else end + len(closing)
            else:
                end = source.find("\n", index)
                index = len(source) if end < 0 else end + 1
            continue
        long_string = re.match(r"\[(=*)\[", source[index:])
        if long_string:
            closing = "]" + long_string.group(1) + "]"
            end = source.find(closing, index + long_string.end())
            if end < 0:
                raise LuaTranslationError("unterminated Lua long string")
            index = end + len(closing)
            continue
        if source[index] in "'\"":
            quote = source[index]
            index += 1
            while index < len(source):
                if source[index] == "\\":
                    index += 2
                elif source[index] == quote:
                    index += 1
                    break
                else:
                    index += 1
            continue
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return index
        index += 1
    raise LuaTranslationError("unterminated Lua table")


def _assignment_spans(source: str, name: str) -> list[tuple[int, int, str]]:
    spans: list[tuple[int, int, str]] = []
    for match in re.finditer(rf"(?m)^(?P<indent>[ \t]*){re.escape(name)}\s*=\s*(?P<open>\{{)", source):
        opening = match.start("open")
        spans.append((opening, _matching_brace(source, opening) + 1, match.group("indent")))
    return spans


def apply_function_translations(
    source: str,
    fields: list[CopyField],
    translations: dict[str, dict[str, str]],
    locales: list[str],
) -> str:
    replacements: list[tuple[int, int, str]] = []
    by_suffix = {field.id.split("/", 1)[1]: field for field in fields}
    for name in ("title", "description"):
        spans = _assignment_spans(source, name)
        if len(spans) != 1:
            raise LuaTranslationError(f"expected exactly one {name} translation table")
        field = by_suffix[name]
        start, end, indent = spans[0]
        values = {locale: translations[locale][field.id] for locale in locales if locale != "en"}
        replacements.append((start, end, render_table(field.source, values, locales, indent)))

    config_fields = [field for field in fields if "/config/" in field.id]
    name_spans = _assignment_spans(source, "name")
    if len(name_spans) != len(config_fields):
        raise LuaTranslationError(
            f"expected {len(config_fields)} inline config name tables, found {len(name_spans)}"
        )
    for field, (start, end, indent) in zip(config_fields, name_spans, strict=True):
        values = {locale: translations[locale][field.id] for locale in locales if locale != "en"}
        replacements.append((start, end, render_table(field.source, values, locales, indent)))

    for start, end, replacement in sorted(replacements, reverse=True):
        source = source[:start] + replacement + source[end:]
    return source


def apply_shared_translations(
    source: str,
    fields: list[CopyField],
    translations: dict[str, dict[str, str]],
    locales: list[str],
) -> str:
    replacements: list[tuple[int, int, str]] = []
    for field in fields:
        id_match = re.search(rf'_id\s*=\s*{re.escape(json.dumps(field.id))}', source)
        if not id_match:
            raise LuaTranslationError(f"could not locate shared record {field.id}")
        candidates: list[tuple[int, int]] = []
        for opening in (match.start() for match in re.finditer(r"\{", source[:id_match.start()])):
            closing = _matching_brace(source, opening) + 1
            if closing >= id_match.end():
                candidates.append((opening, closing))
        if not candidates:
            raise LuaTranslationError(f"could not locate table for shared record {field.id}")
        start, end = min(candidates, key=lambda span: span[1] - span[0])
        line_start = source.rfind("\n", 0, start) + 1
        indent = source[line_start:start]
        if indent.strip():
            indent = "\t"
        values = {"en": field.source, **{locale: translations[locale][field.id] for locale in locales if locale != "en"}}
        lines = ["{", f'{indent}\t_id = {json.dumps(field.id, ensure_ascii=False)},']
        for locale in locales:
            lines.append(f'{indent}\t["{locale}"] = {lua_string(values[locale], indent + "\t")},')
        lines.append(indent + "}")
        replacements.append((start, end, "\n".join(lines)))
    for start, end, replacement in sorted(replacements, reverse=True):
        source = source[:start] + replacement + source[end:]
    return source
