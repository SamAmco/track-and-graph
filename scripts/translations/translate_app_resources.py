#!/usr/bin/env python3
"""Audit or incrementally translate Track & Graph Android string resources."""

from __future__ import annotations

import argparse
import concurrent.futures
import hashlib
import json
import re
import sys
from collections import defaultdict, deque
from dataclasses import dataclass
from pathlib import Path

from android_resources import (
    AndroidResource,
    LocaleAudit,
    ResourceSet,
    ResourceValidationError,
    audit_locale,
    baseline_existing_translations,
    discover_resource_sets,
    load_state,
    make_batches,
    merge_translations,
    parse_batch_response,
    parse_batch_response_with_json_repair,
    read_resource_file,
    read_failure_artifact,
    remove_resolved_failure_artifacts,
    resource_request,
    update_state,
    write_resource_file,
    write_failure_artifact,
    write_state,
)
from languages import SUPPORTED_TARGETS, TranslationTarget
from providers.base import Translator, TranslatorError
from translation_runtime import (
    add_provider_arguments,
    create_translator,
    load_domain_brief,
    parse_target,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_STATE = Path(__file__).with_name("app_translation_state.json")
WORD_RE = re.compile(r"[A-Za-z][A-Za-z0-9']*")
REFERENCE_STOP_WORDS = frozenset(
    {
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "in", "into", "is", "it", "of", "on", "or", "that", "the", "this",
        "to", "with", "you", "your",
    }
)
REFERENCE_NAME_STOP_WORDS = frozenset(
    {"button", "description", "dialog", "label", "message", "screen", "text", "title"}
)


@dataclass(frozen=True)
class LocaleTranslationResult:
    locale: str
    translated: dict[str, tuple[AndroidResource, object]]
    failures: tuple[dict[str, object], ...]
    usages: tuple[dict[str, object], ...]


def _instructions(target_language: str, domain_context: str) -> str:
    return f"""Translate Android user-interface resources from English into {target_language}.

Return only JSON with exactly this shape:
{{"translations":[{{"id":"unchanged input id","value":<translated value>}}]}}

Return every input resource exactly once and preserve each id byte-for-byte. Preserve the value shape. A scalar is either {{"text":"..."}} or {{"xml":"..."}}; do not change which form it uses. Translate only human-facing prose. Preserve printf placeholders such as %1$s, %d, and %%, escaped newlines and tabs, technical identifiers, product names, and inline XML tags. Preserve string-array item count and order. If the input contains reference translations, use them only to maintain terminology and style; do not return them.

For a plurals value, return a list of objects with "quantity" and "value". Use the CLDR plural quantities needed by {target_language}; quantities may differ from English, but must be unique and must include "other". When adding a target-language quantity that is absent from English, translate the English "other" value and preserve its placeholders.

Use natural, concise mobile-UI language. Do not explain the translation or wrap the JSON in Markdown.

Use this English domain brief to understand the app and keep terminology consistent. It is context, not text to translate.

--- DOMAIN BRIEF ---
{domain_context}
--- END DOMAIN BRIEF ---
"""


def _value_text(value: object) -> str:
    if isinstance(value, str):
        return value
    if isinstance(value, dict):
        return " ".join(_value_text(item) for item in value.values())
    if isinstance(value, list):
        return " ".join(_value_text(item) for item in value)
    return ""


def _words(value: object) -> frozenset[str]:
    return frozenset(
        word
        for match in WORD_RE.finditer(_value_text(value))
        if len(word := match.group(0).lower()) > 1 and word not in REFERENCE_STOP_WORDS
    )


def _name_words(resource: AndroidResource) -> frozenset[str]:
    return frozenset(
        part
        for part in re.split(r"[^a-z0-9]+", resource.name.lower())
        if part and part not in REFERENCE_NAME_STOP_WORDS
    )


def _stable_reference_key(reference: tuple[AndroidResource, object]) -> str:
    return hashlib.sha256(reference[0].key.encode("utf-8")).hexdigest()


def _diverse_reference_order(
    references: list[tuple[AndroidResource, object]],
) -> list[tuple[AndroidResource, object]]:
    by_source_set: dict[str, deque[tuple[AndroidResource, object]]] = defaultdict(deque)
    for reference in sorted(references, key=_stable_reference_key):
        by_source_set[reference[0].source_set].append(reference)

    ordered: list[tuple[AndroidResource, object]] = []
    source_sets = sorted(by_source_set)
    while any(by_source_set.values()):
        for source_set in source_sets:
            if by_source_set[source_set]:
                ordered.append(by_source_set[source_set].popleft())
    return ordered


def _select_reference_payload(
    batch: tuple[AndroidResource, ...],
    references: list[tuple[AndroidResource, object]],
    reference_characters: int,
) -> list[dict[str, object]]:
    if reference_characters == 0:
        return []

    batch_keys = {resource.key for resource in batch}
    batch_words = frozenset().union(*(_words(resource.value) for resource in batch))
    batch_name_words = frozenset().union(*(_name_words(resource) for resource in batch))
    batch_source_sets = {resource.source_set for resource in batch}
    unique_references = {
        source.key: (source, translation)
        for source, translation in references
        if source.key not in batch_keys
    }

    relevant: list[tuple[int, int, tuple[AndroidResource, object]]] = []
    fallback: list[tuple[AndroidResource, object]] = []
    for reference in unique_references.values():
        source = reference[0]
        overlap_score = 4 * len(_words(source.value) & batch_words)
        overlap_score += 3 * len(_name_words(source) & batch_name_words)
        if overlap_score:
            same_source_set = int(source.source_set in batch_source_sets)
            relevant.append((overlap_score, same_source_set, reference))
        else:
            fallback.append(reference)

    relevant.sort(
        key=lambda item: (-item[0], -item[1], _stable_reference_key(item[2]))
    )
    candidates = [item[2] for item in relevant]
    candidates.extend(_diverse_reference_order(fallback))

    reference_payload: list[dict[str, object]] = []
    reference_size = 0
    for source, translation in candidates:
        item = {
            "id": source.key,
            "english": source.value,
            "translation": translation,
        }
        size = len(json.dumps(item, ensure_ascii=False, separators=(",", ":")))
        separator_size = int(bool(reference_payload))
        if reference_size + separator_size + size > reference_characters:
            continue
        reference_payload.append(item)
        reference_size += separator_size + size
    return reference_payload


def _source_text(
    batch: tuple[AndroidResource, ...],
    references: list[tuple[AndroidResource, object]],
    reference_characters: int,
) -> str:
    return json.dumps(
        {
            "reference_translations": _select_reference_payload(
                batch, references, reference_characters
            ),
            "resources": [resource_request(resource) for resource in batch],
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )


def _audit_json(audit: LocaleAudit) -> dict[str, object]:
    return {
        "locale": audit.target.locale,
        "language": audit.target.language,
        "missing_count": len(audit.missing),
        "missing": list(audit.missing),
        "stale_count": len(audit.stale),
        "stale": list(audit.stale),
        "pending_count": len(audit.pending),
        "deletion_candidates": list(audit.deletion_candidates),
        "failure_artifacts": list(audit.failure_artifacts),
    }


def _audit_has_issues(audit: LocaleAudit) -> bool:
    return bool(audit.pending or audit.deletion_candidates or audit.failure_artifacts)


def _translate_locale(
    *,
    audit: LocaleAudit,
    translator: Translator,
    project_root: Path,
    resource_sets: tuple[ResourceSet, ...],
    domain_context: str,
    batch_characters: int,
    reference_characters: int,
    retries: int,
) -> LocaleTranslationResult:
    translated: dict[str, tuple[AndroidResource, object]] = {}
    failures: list[dict[str, object]] = []
    usages: list[dict[str, object]] = []
    references = list(audit.references)

    for batch_number, batch in enumerate(
        make_batches(audit.pending, batch_characters), start=1
    ):
        error_message = "translation attempt did not run"
        provider_response: str | None = None
        for attempt in range(retries + 1):
            try:
                result = translator.translate(
                    instructions=_instructions(audit.target.language, domain_context),
                    source_text=_source_text(batch, references, reference_characters),
                )
                provider_response = result.text
                usages.append(result.usage)
                values = parse_batch_response(batch, result.text)
                translated.update(
                    {resource.key: (resource, values[resource.key]) for resource in batch}
                )
                references.extend((resource, values[resource.key]) for resource in batch)
                break
            except (TranslatorError, ResourceValidationError) as error:
                error_message = str(error)
                if attempt == retries:
                    resource_set = next(
                        item for item in resource_sets if item.identifier == batch[0].source_set
                    )
                    artifact = write_failure_artifact(
                        project_root=project_root,
                        target=audit.target,
                        resource_set=resource_set,
                        resources=batch,
                        error=error_message,
                        provider_response=provider_response,
                    )
                    failures.append(
                        {
                            "batch": batch_number,
                            "resource_ids": [resource.key for resource in batch],
                            "error": error_message,
                            "artifact": str(artifact.relative_to(project_root)),
                        }
                    )
    return LocaleTranslationResult(
        locale=audit.target.locale,
        translated=translated,
        failures=tuple(failures),
        usages=tuple(usages),
    )


def _write_locale(
    project_root: Path,
    target: TranslationTarget,
    translated: dict[str, tuple[AndroidResource, object]],
    resource_sets: tuple[ResourceSet, ...],
) -> None:
    for resource_set in resource_sets:
        selected = {
            key: value
            for key, value in translated.items()
            if value[0].source_set == resource_set.identifier
        }
        if not selected:
            continue
        target_path = resource_set.target_path(project_root, target)
        target_file = read_resource_file(target_path, resource_set.identifier)
        write_resource_file(target_path, merge_translations(target_file, selected))


def _load_repaired_failure(
    project_root: Path,
    artifact_path: Path,
    resource_sets: tuple[ResourceSet, ...],
) -> tuple[TranslationTarget, dict[str, tuple[AndroidResource, object]], bool]:
    try:
        artifact_path.relative_to(project_root)
    except ValueError as error:
        raise ValueError("failure artifact must be inside the project") from error
    payload = read_failure_artifact(artifact_path)
    if payload is None or payload.get("version") not in (1, 2):
        raise ValueError(f"not a supported translation failure artifact: {artifact_path}")
    locale = payload.get("locale")
    language = payload.get("language")
    resource_ids = payload.get("resource_ids")
    if not isinstance(locale, str) or not isinstance(language, str):
        raise ValueError("failure artifact has no valid locale and language")
    if not isinstance(resource_ids, list) or not all(
        isinstance(item, str) for item in resource_ids
    ):
        raise ValueError("failure artifact has no valid resource_ids")

    sources: dict[str, AndroidResource] = {}
    for resource_set in resource_sets:
        source_file = read_resource_file(
            resource_set.source_path(project_root), resource_set.identifier
        )
        sources.update(
            (key, resource)
            for key, resource in source_file.resources.items()
            if resource.is_translatable
        )
    unknown = sorted(set(resource_ids) - sources.keys())
    if unknown:
        raise ValueError(f"failure artifact contains unknown resources: {unknown}")
    resources = tuple(sources[resource_id] for resource_id in resource_ids)
    repair_applied = False
    if payload["version"] == 1:
        provider_response = payload.get("provider_response")
        if not isinstance(provider_response, str):
            raise ValueError("failure artifact has no provider response to repair")
        values, repair_applied = parse_batch_response_with_json_repair(
            resources, provider_response
        )
    else:
        unrecovered_ids = payload.get("unrecovered_ids")
        if isinstance(unrecovered_ids, list) and unrecovered_ids:
            raise ResourceValidationError(
                "failure artifact still contains unrecovered English resources: "
                + ", ".join(str(item) for item in unrecovered_ids)
                + "; translate them and remove tools:unrecovered"
            )
        translated_values = payload.get("translated_values")
        if not isinstance(translated_values, dict):
            raise ValueError("failure artifact contains no readable translated resources")
        response = json.dumps(
            {
                "translations": [
                    {"id": resource_id, "value": translated_values.get(resource_id)}
                    for resource_id in resource_ids
                ]
            },
            ensure_ascii=False,
        )
        values = parse_batch_response(resources, response)
    translated = {
        resource.key: (resource, values[resource.key]) for resource in resources
    }
    return TranslationTarget(locale, language), translated, repair_applied


def _targets_for_audit(values: list[TranslationTarget] | None) -> tuple[TranslationTarget, ...]:
    return tuple(values) if values else SUPPORTED_TARGETS


def _add_target_argument(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--target",
        type=parse_target,
        action="append",
        help="select LOCALE=LANGUAGE; repeat as needed",
    )


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Audit or incrementally translate Android string resources"
    )
    parser.add_argument("--project-root", type=Path, default=PROJECT_ROOT)
    parser.add_argument("--state", type=Path, default=DEFAULT_STATE)
    subparsers = parser.add_subparsers(dest="command", required=True)

    audit = subparsers.add_parser("audit", help="report missing, stale, and target-only resources")
    _add_target_argument(audit)
    audit.add_argument(
        "--fail-on-issues",
        action="store_true",
        help="exit non-zero if any locale has missing/stale copy, deletion candidates, or failure artifacts",
    )

    baseline = subparsers.add_parser(
        "baseline",
        help="associate existing translations with the current English source without API calls",
    )
    _add_target_argument(baseline)

    apply_failure = subparsers.add_parser(
        "apply-failure",
        help="validate and merge locally repaired XML from a failure artifact",
    )
    apply_failure.add_argument("artifact", type=Path)

    translate = subparsers.add_parser(
        "translate", help="make paid API calls and write validated translations"
    )
    selection = translate.add_mutually_exclusive_group(required=True)
    selection.add_argument(
        "--target",
        type=parse_target,
        action="append",
        help="translate LOCALE=LANGUAGE; repeat as needed",
    )
    selection.add_argument(
        "--all-targets", action="store_true", help="explicitly translate every supported locale"
    )
    add_provider_arguments(translate)
    translate.add_argument("--batch-characters", type=int, default=15000)
    translate.add_argument("--reference-characters", type=int, default=5000)
    translate.add_argument("--max-workers", type=int, default=8)
    translate.add_argument("--retries", type=int, default=0)
    return parser


def main() -> int:
    parser = _parser()
    args = parser.parse_args()
    project_root = args.project_root.resolve()
    state_path = args.state.resolve()
    try:
        state = load_state(state_path)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        parser.error(str(error))
    resource_sets = discover_resource_sets(project_root)
    if not resource_sets:
        parser.error(f"found no app/*/src/*/res/values/strings.xml files under {project_root}")

    if args.command == "audit":
        audits = [
            audit_locale(project_root, target, resource_sets, state)
            for target in _targets_for_audit(args.target)
        ]
        for audit in audits:
            if not args.fail_on_issues or _audit_has_issues(audit):
                print(json.dumps(_audit_json(audit), ensure_ascii=False))
        print(
            json.dumps(
                {
                    "summary": {
                        "locales": len(audits),
                        "pending": sum(len(audit.pending) for audit in audits),
                        "deletion_candidates": sum(
                            len(audit.deletion_candidates) for audit in audits
                        ),
                        "failure_artifacts": sum(
                            len(audit.failure_artifacts) for audit in audits
                        ),
                    }
                },
                ensure_ascii=False,
            )
        )
        return int(args.fail_on_issues and any(_audit_has_issues(audit) for audit in audits))

    if args.command == "baseline":
        targets = _targets_for_audit(args.target)
        recorded = baseline_existing_translations(
            project_root, targets, resource_sets, state
        )
        write_state(state_path, state)
        print(
            json.dumps(
                {
                    "summary": {
                        "locales_checked": len(targets),
                        "translations_recorded": recorded,
                        "state": str(state_path),
                    }
                },
                ensure_ascii=False,
            )
        )
        return 0

    if args.command == "apply-failure":
        artifact_path = args.artifact
        if not artifact_path.is_absolute():
            artifact_path = project_root / artifact_path
        try:
            target, translated, repair_applied = _load_repaired_failure(
                project_root,
                artifact_path.resolve(),
                resource_sets,
            )
            _write_locale(project_root, target, translated, resource_sets)
            update_state(state, target, translated)
            write_state(state_path, state)
            remaining = audit_locale(project_root, target, resource_sets, state)
            removed = remove_resolved_failure_artifacts(
                project_root=project_root,
                target=target,
                resource_sets=resource_sets,
                pending_resource_ids={resource.key for resource in remaining.pending},
            )
        except (OSError, ValueError, json.JSONDecodeError, ResourceValidationError) as error:
            parser.error(str(error))
        print(
            json.dumps(
                {
                    "locale": target.locale,
                    "applied": len(translated),
                    "json_repair_applied": repair_applied,
                    "remaining": len(remaining.pending),
                    "removed_artifacts": [
                        str(path.relative_to(project_root)) for path in removed
                    ],
                },
                ensure_ascii=False,
            )
        )
        return 0

    if (
        args.batch_characters < 1
        or args.reference_characters < 0
        or args.max_workers < 1
        or args.retries < 0
    ):
        parser.error(
            "batch characters/workers must be positive; reference characters and retries "
            "cannot be negative"
        )
    targets = SUPPORTED_TARGETS if args.all_targets else tuple(args.target)
    try:
        domain_context = load_domain_brief(args.domain_brief)
        translator = create_translator(args.provider, args.model)
    except (OSError, ValueError) as error:
        parser.error(str(error))

    audits = {
        target.locale: audit_locale(project_root, target, resource_sets, state)
        for target in targets
    }
    results: list[LocaleTranslationResult] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.max_workers) as executor:
        futures = {
            executor.submit(
                _translate_locale,
                audit=audits[target.locale],
                translator=translator,
                project_root=project_root,
                resource_sets=resource_sets,
                domain_context=domain_context,
                batch_characters=args.batch_characters,
                reference_characters=args.reference_characters,
                retries=args.retries,
            ): target
            for target in targets
            if audits[target.locale].pending
        }
        for future in concurrent.futures.as_completed(futures):
            target = futures[future]
            result = future.result()
            _write_locale(project_root, target, result.translated, resource_sets)
            update_state(state, target, result.translated)
            write_state(state_path, state)
            remaining = audit_locale(project_root, target, resource_sets, state)
            remove_resolved_failure_artifacts(
                project_root=project_root,
                target=target,
                resource_sets=resource_sets,
                pending_resource_ids={resource.key for resource in remaining.pending},
            )
            results.append(result)
            print(
                json.dumps(
                    {
                        **_audit_json(audits[target.locale]),
                        "translated_count": len(result.translated),
                        "failures": list(result.failures),
                        "request_usage": list(result.usages),
                    },
                    ensure_ascii=False,
                )
            )

    write_state(state_path, state)
    failed = sum(len(result.failures) for result in results)
    translated_count = sum(len(result.translated) for result in results)
    print(
        json.dumps(
            {
                "summary": {
                    "locales_requested": len(targets),
                    "translated": translated_count,
                    "failed_batches": failed,
                    "state": str(state_path),
                }
            },
            ensure_ascii=False,
        )
    )
    return int(failed > 0)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except TranslatorError as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1) from error
