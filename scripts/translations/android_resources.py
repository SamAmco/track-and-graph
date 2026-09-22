"""Parse, audit, validate, and update Android string-resource translations."""

from __future__ import annotations

import copy
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

from languages import TranslationTarget


RESOURCE_TAGS = frozenset(("string", "plurals", "string-array"))
PLURAL_QUANTITIES = frozenset(("zero", "one", "two", "few", "many", "other"))
PROTECTED_TOKEN_RE = re.compile(
    r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]|\\(?:n|t|u[0-9A-Fa-f]{4})"
)
XML_TAG_RE = re.compile(r"</?([A-Za-z][\w:.-]*)(?:\s[^>]*)?/?>")
UNESCAPED_ANDROID_QUOTE_RE = re.compile(r"(?<!\\)(['\"])")
FAILURE_ARTIFACT_PREFIX = "TRANSLATION_BATCH_FAILED\n"
FAILURE_ARTIFACT_GLOB = "translation_failure_*.xml"
TOOLS_NAMESPACE = "http://schemas.android.com/tools"
TRANSLATION_ID_ATTRIBUTE = f"{{{TOOLS_NAMESPACE}}}translationId"
UNRECOVERED_ATTRIBUTE = f"{{{TOOLS_NAMESPACE}}}unrecovered"
ET.register_namespace("tools", TOOLS_NAMESPACE)


class ResourceValidationError(ValueError):
    """Raised when a provider response cannot safely become Android resources."""


@dataclass(frozen=True)
class ResourceSet:
    identifier: str
    relative_res_dir: Path
    source_file_name: str = "strings.xml"

    def source_path(self, project_root: Path) -> Path:
        return project_root / self.relative_res_dir / "values" / self.source_file_name

    def target_path(self, project_root: Path, target: TranslationTarget) -> Path:
        qualifier = android_resource_qualifier(target.locale)
        return (
            project_root
            / self.relative_res_dir
            / f"values-{qualifier}"
            / self.source_file_name
        )


@dataclass(frozen=True)
class AndroidResource:
    source_set: str
    resource_type: str
    name: str
    attributes: dict[str, str]
    value: object
    element: ET.Element

    @property
    def key(self) -> str:
        return f"{self.source_set}/{self.resource_type}:{self.name}"

    @property
    def source_hash(self) -> str:
        canonical = json.dumps(
            {
                "type": self.resource_type,
                "attributes": self.attributes,
                "value": self.value,
            },
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        )
        return hashlib.sha256(canonical.encode("utf-8")).hexdigest()

    @property
    def is_translatable(self) -> bool:
        return self.attributes.get("translatable", "true").lower() != "false"


@dataclass(frozen=True)
class ResourceFile:
    resources: dict[str, AndroidResource]
    root: ET.Element


@dataclass(frozen=True)
class LocaleAudit:
    target: TranslationTarget
    pending: tuple[AndroidResource, ...]
    references: tuple[tuple[AndroidResource, object], ...]
    missing: tuple[str, ...]
    stale: tuple[str, ...]
    deletion_candidates: tuple[str, ...]
    failure_artifacts: tuple[str, ...]


def discover_resource_sets(project_root: Path) -> tuple[ResourceSet, ...]:
    """Discover Android source sets containing an English strings.xml file."""
    resource_sets: list[ResourceSet] = []
    pattern = "app/*/src/*/res/values/strings.xml"
    for strings_file in sorted(project_root.glob(pattern)):
        res_dir = strings_file.parents[1]
        relative_res_dir = res_dir.relative_to(project_root)
        identifier = relative_res_dir.parent.as_posix()
        resource_sets.append(ResourceSet(identifier, relative_res_dir))
    return tuple(resource_sets)


def android_resource_qualifier(locale: str) -> str:
    """Return an unambiguous Android resource qualifier for a BCP-47 tag.

    Existing human translations retain their historical directory names. New
    locales use Android's BCP-47 qualifier form so three-letter and modern
    language codes do not rely on legacy qualifier aliases.
    """
    existing = {"de": "de-rDE", "es": "es", "fr": "fr"}
    return existing.get(locale, "b+" + locale.replace("-", "+"))


def _parser() -> ET.XMLParser:
    return ET.XMLParser(target=ET.TreeBuilder(insert_comments=True))


def _scalar_value(element: ET.Element) -> dict[str, str]:
    if not list(element):
        return {"text": element.text or ""}
    inner = element.text or ""
    inner += "".join(ET.tostring(child, encoding="unicode") for child in element)
    return {"xml": inner}


def _element_value(element: ET.Element) -> object:
    if element.tag == "string":
        return _scalar_value(element)
    if element.tag == "plurals":
        return [
            {"quantity": item.attrib["quantity"], "value": _scalar_value(item)}
            for item in element
            if item.tag == "item"
        ]
    if element.tag == "string-array":
        return [
            _scalar_value(item)
            for item in element
            if item.tag == "item"
        ]
    raise ValueError(f"unsupported Android resource type: {element.tag}")


def read_resource_file(path: Path, source_set: str) -> ResourceFile:
    if not path.exists():
        return ResourceFile(resources={}, root=ET.Element("resources"))
    root = ET.parse(path, parser=_parser()).getroot()
    if root.tag != "resources":
        raise ValueError(f"expected <resources> root in {path}")
    resources: dict[str, AndroidResource] = {}
    for element in root:
        if element.tag not in RESOURCE_TAGS:
            continue
        name = element.attrib.get("name")
        if not name:
            raise ValueError(f"unnamed <{element.tag}> in {path}")
        resource = AndroidResource(
            source_set=source_set,
            resource_type=element.tag,
            name=name,
            attributes=dict(element.attrib),
            value=_element_value(element),
            element=copy.deepcopy(element),
        )
        if resource.key in resources:
            raise ValueError(f"duplicate resource {resource.key} in {path}")
        resources[resource.key] = resource
    return ResourceFile(resources=resources, root=root)


def load_state(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {"version": 2, "translations": {}}
    state = json.loads(path.read_text(encoding="utf-8"))
    if state.get("version") != 2 or not isinstance(state.get("translations"), dict):
        raise ValueError(f"unsupported app translation state in {path}")
    return state


def baseline_existing_translations(
    project_root: Path,
    targets: Iterable[TranslationTarget],
    resource_sets: Iterable[ResourceSet],
    state: dict[str, Any],
) -> int:
    """Record which current English value each existing translation corresponds to."""
    recorded = 0
    translations = state["translations"]
    resource_sets = tuple(resource_sets)
    for target in targets:
        locale_state: dict[str, str] = {}
        for resource_set in resource_sets:
            source_file = read_resource_file(
                resource_set.source_path(project_root), resource_set.identifier
            )
            target_file = read_resource_file(
                resource_set.target_path(project_root, target), resource_set.identifier
            )
            for key, source_resource in source_file.resources.items():
                if source_resource.is_translatable and key in target_file.resources:
                    locale_state[key] = source_resource.source_hash
                    recorded += 1
        if locale_state:
            translations[target.locale] = locale_state
        else:
            translations.pop(target.locale, None)
    return recorded


def audit_locale(
    project_root: Path,
    target: TranslationTarget,
    resource_sets: Iterable[ResourceSet],
    state: dict[str, Any],
) -> LocaleAudit:
    pending: list[AndroidResource] = []
    missing: list[str] = []
    stale: list[str] = []
    deletion_candidates: list[str] = []
    failure_artifacts: list[str] = []
    references: list[tuple[AndroidResource, object]] = []
    locale_state = state["translations"].get(target.locale, {})

    for resource_set in resource_sets:
        source_file = read_resource_file(
            resource_set.source_path(project_root), resource_set.identifier
        )
        target_file = read_resource_file(
            resource_set.target_path(project_root, target), resource_set.identifier
        )
        target_directory = resource_set.target_path(project_root, target).parent
        failure_artifacts.extend(
            str(path.relative_to(project_root))
            for path in sorted(target_directory.glob(FAILURE_ARTIFACT_GLOB))
        )
        source_all = source_file.resources
        source_translatable = {
            key: resource for key, resource in source_all.items() if resource.is_translatable
        }

        for key in sorted(target_file.resources.keys() - source_all.keys()):
            deletion_candidates.append(key)
        for key in sorted(target_file.resources.keys() & source_all.keys()):
            if not source_all[key].is_translatable:
                deletion_candidates.append(key)

        for key, source_resource in source_translatable.items():
            translated = target_file.resources.get(key)
            if translated is None:
                missing.append(key)
                pending.append(source_resource)
                continue
            if locale_state.get(key) == source_resource.source_hash:
                references.append((source_resource, translated.value))
            else:
                stale.append(key)
                pending.append(source_resource)

    return LocaleAudit(
        target=target,
        pending=tuple(pending),
        references=tuple(references),
        missing=tuple(sorted(missing)),
        stale=tuple(sorted(stale)),
        deletion_candidates=tuple(sorted(deletion_candidates)),
        failure_artifacts=tuple(sorted(set(failure_artifacts))),
    )


def _failure_artifact_path(
    project_root: Path,
    target: TranslationTarget,
    resource_set: ResourceSet,
    resources: Iterable[AndroidResource],
) -> Path:
    identifiers = "\n".join(sorted(resource.key for resource in resources))
    digest = hashlib.sha256(identifiers.encode("utf-8")).hexdigest()[:12]
    return (
        resource_set.target_path(project_root, target).parent
        / f"translation_failure_{digest}.xml"
    )


def write_failure_artifact(
    *,
    project_root: Path,
    target: TranslationTarget,
    resource_set: ResourceSet,
    resources: tuple[AndroidResource, ...],
    error: str,
    provider_response: str | None,
) -> Path:
    """Retain a failed response as readable XML that cannot enter a passing build."""
    path = _failure_artifact_path(project_root, target, resource_set, resources)
    path.parent.mkdir(parents=True, exist_ok=True)
    recovered: dict[str, object] = {}
    if provider_response is not None:
        try:
            recovered, _repair_applied = parse_batch_response_with_json_repair(
                resources, provider_response
            )
        except ResourceValidationError:
            try:
                raw = json.loads(provider_response)
            except json.JSONDecodeError:
                raw = None
            translations = raw.get("translations") if isinstance(raw, dict) else None
            if isinstance(translations, list):
                expected = {resource.key for resource in resources}
                for item in translations:
                    if (
                        isinstance(item, dict)
                        and isinstance(item.get("id"), str)
                        and item["id"] in expected
                        and "value" in item
                    ):
                        recovered[item["id"]] = _normalize_android_escapes(item["value"])

    root = ET.Element(
        "resources",
        {
            f"{{{TOOLS_NAMESPACE}}}translationFailureVersion": "2",
            f"{{{TOOLS_NAMESPACE}}}locale": target.locale,
            f"{{{TOOLS_NAMESPACE}}}language": target.language,
            f"{{{TOOLS_NAMESPACE}}}error": error,
        },
    )
    for resource in resources:
        value = recovered.get(resource.key)
        try:
            element = translated_element(resource, value) if value is not None else None
        except (AssertionError, KeyError, TypeError, ValueError, ResourceValidationError):
            element = None
        if element is None:
            root.append(ET.Comment(f" UNRECOVERED: replace the English source for {resource.key} "))
            element = copy.deepcopy(resource.element)
            element.set(UNRECOVERED_ATTRIBUTE, "true")
        element.set(TRANSLATION_ID_ATTRIBUTE, resource.key)
        root.append(element)
    if provider_response is not None and len(recovered) < len(resources):
        raw_response = ET.SubElement(
            root, f"{{{TOOLS_NAMESPACE}}}rawProviderResponse"
        )
        raw_response.text = provider_response
    ET.indent(root, space="    ")
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        FAILURE_ARTIFACT_PREFIX + ET.tostring(root, encoding="unicode") + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)
    return path


def read_failure_artifact(path: Path) -> dict[str, Any] | None:
    text = path.read_text(encoding="utf-8")
    if not text.startswith(FAILURE_ARTIFACT_PREFIX):
        return None
    body = text[len(FAILURE_ARTIFACT_PREFIX):].lstrip()
    if body.startswith("{"):  # Version 1 compatibility.
        payload = json.loads(body)
        return payload if isinstance(payload, dict) else None

    root = ET.fromstring(body, parser=_parser())
    if root.tag != "resources":
        return None
    resource_ids: list[str] = []
    translated_values: dict[str, object] = {}
    unrecovered_ids: list[str] = []
    for element in root:
        if element.tag not in RESOURCE_TAGS:
            continue
        identifier = element.attrib.get(TRANSLATION_ID_ATTRIBUTE)
        if not identifier:
            continue
        resource_ids.append(identifier)
        if element.attrib.get(UNRECOVERED_ATTRIBUTE) == "true":
            unrecovered_ids.append(identifier)
        translated_values[identifier] = _element_value(element)
    return {
        "version": 2,
        "locale": root.attrib.get(f"{{{TOOLS_NAMESPACE}}}locale"),
        "language": root.attrib.get(f"{{{TOOLS_NAMESPACE}}}language"),
        "error": root.attrib.get(f"{{{TOOLS_NAMESPACE}}}error"),
        "resource_ids": resource_ids,
        "translated_values": translated_values,
        "unrecovered_ids": unrecovered_ids,
    }


def remove_resolved_failure_artifacts(
    *,
    project_root: Path,
    target: TranslationTarget,
    resource_sets: Iterable[ResourceSet],
    pending_resource_ids: set[str],
) -> tuple[Path, ...]:
    """Remove generated failure files only after all their resources are resolved."""
    removed: list[Path] = []
    for resource_set in resource_sets:
        directory = resource_set.target_path(project_root, target).parent
        for path in directory.glob(FAILURE_ARTIFACT_GLOB):
            try:
                payload = read_failure_artifact(path)
            except (OSError, ValueError, json.JSONDecodeError):
                continue
            resource_ids = payload.get("resource_ids") if payload else None
            if not isinstance(resource_ids, list) or not all(
                isinstance(item, str) for item in resource_ids
            ):
                continue
            if pending_resource_ids.isdisjoint(resource_ids):
                path.unlink()
                removed.append(path)
    return tuple(removed)


def resource_request(resource: AndroidResource) -> dict[str, object]:
    return {"id": resource.key, "type": resource.resource_type, "value": resource.value}


def make_batches(
    resources: Iterable[AndroidResource], max_characters: int
) -> list[tuple[AndroidResource, ...]]:
    if max_characters < 1:
        raise ValueError("max_characters must be positive")
    batches: list[list[AndroidResource]] = []
    current: list[AndroidResource] = []
    current_size = 0
    for resource in resources:
        size = len(json.dumps(resource_request(resource), ensure_ascii=False))
        if current and current_size + size > max_characters:
            batches.append(current)
            current, current_size = [], 0
        current.append(resource)
        current_size += size
    if current:
        batches.append(current)
    return [tuple(batch) for batch in batches]


def _scalar_text(value: object) -> str:
    if not isinstance(value, dict) or set(value) not in ({"text"}, {"xml"}):
        raise ResourceValidationError("scalar value must contain exactly 'text' or 'xml'")
    text = next(iter(value.values()))
    if not isinstance(text, str):
        raise ResourceValidationError("translated scalar value must be a string")
    return text


def _normalize_android_escapes(value: object) -> object:
    """Escape ASCII quotes in plain Android string values.

    Models often return natural apostrophes without Android's required
    backslash. Inline-XML values are left alone because quotes may belong to tag
    attributes; their markup is validated separately.
    """
    if isinstance(value, dict):
        if set(value) == {"text"} and isinstance(value["text"], str):
            return {
                "text": UNESCAPED_ANDROID_QUOTE_RE.sub(r"\\\1", value["text"])
            }
        if set(value) == {"xml"}:
            return value
        if set(value) == {"quantity", "value"}:
            return {
                "quantity": value["quantity"],
                "value": _normalize_android_escapes(value["value"]),
            }
        return value
    if isinstance(value, list):
        return [_normalize_android_escapes(item) for item in value]
    return value


def _protected_tokens(value: object) -> Counter[str]:
    if isinstance(value, dict):
        text = _scalar_text(value)
        return Counter(PROTECTED_TOKEN_RE.findall(text))
    if isinstance(value, list):
        result: Counter[str] = Counter()
        for item in value:
            if isinstance(item, dict) and "quantity" in item:
                result.update(_protected_tokens(item.get("value")))
            else:
                result.update(_protected_tokens(item))
        return result
    raise ResourceValidationError("unsupported translated value shape")


def _validate_xml_scalar(source: object, translated: object) -> None:
    source_text = _scalar_text(source)
    translated_text = _scalar_text(translated)
    source_kind = next(iter(source))  # type: ignore[arg-type]
    translated_kind = next(iter(translated))  # type: ignore[arg-type]
    if source_kind != translated_kind:
        raise ResourceValidationError("text/XML value kind changed")
    if source_kind == "xml":
        if XML_TAG_RE.findall(source_text) != XML_TAG_RE.findall(translated_text):
            raise ResourceValidationError("inline XML tags changed")
        try:
            ET.fromstring(f"<wrapper>{translated_text}</wrapper>")
        except ET.ParseError as error:
            raise ResourceValidationError(f"invalid inline XML: {error}") from error


def _validate_value(resource: AndroidResource, translated: object) -> None:
    source = resource.value
    if resource.resource_type == "string":
        _validate_xml_scalar(source, translated)
        if _protected_tokens(source) != _protected_tokens(translated):
            raise ResourceValidationError("format placeholders or protected escapes changed")
    elif resource.resource_type == "string-array":
        if not isinstance(source, list) or not isinstance(translated, list):
            raise ResourceValidationError("string-array value must be a list")
        if len(source) != len(translated):
            raise ResourceValidationError("string-array length changed")
        for source_item, translated_item in zip(source, translated):
            _validate_xml_scalar(source_item, translated_item)
            if _protected_tokens(source_item) != _protected_tokens(translated_item):
                raise ResourceValidationError(
                    "format placeholders or protected escapes changed in string-array item"
                )
    elif resource.resource_type == "plurals":
        if not isinstance(source, list) or not isinstance(translated, list) or not translated:
            raise ResourceValidationError("plurals value must be a non-empty list")
        source_by_quantity = {
            str(item["quantity"]): item["value"]
            for item in source
            if isinstance(item, dict) and "quantity" in item and "value" in item
        }
        fallback_source = source_by_quantity.get("other")
        if fallback_source is None:
            raise ResourceValidationError("source plural has no 'other' quantity")
        quantities: list[str] = []
        for item in translated:
            if not isinstance(item, dict) or set(item) != {"quantity", "value"}:
                raise ResourceValidationError("invalid plural item shape")
            quantity = item["quantity"]
            if not isinstance(quantity, str) or quantity not in PLURAL_QUANTITIES:
                raise ResourceValidationError(f"invalid plural quantity: {quantity!r}")
            quantities.append(quantity)
            _scalar_text(item["value"])
            source_value = source_by_quantity.get(quantity, fallback_source)
            if _protected_tokens(source_value) != _protected_tokens(item["value"]):
                raise ResourceValidationError(
                    f"format placeholders changed in plural quantity {quantity}"
                )
        if len(quantities) != len(set(quantities)) or "other" not in quantities:
            raise ResourceValidationError("plural quantities must be unique and include 'other'")
    else:
        raise ResourceValidationError(f"unsupported resource type {resource.resource_type}")

def parse_batch_response(
    resources: Iterable[AndroidResource], response_text: str
) -> dict[str, object]:
    expected = {resource.key: resource for resource in resources}
    try:
        response = json.loads(response_text)
    except json.JSONDecodeError as error:
        raise ResourceValidationError(f"response is not valid JSON: {error}") from error
    if not isinstance(response, dict) or set(response) != {"translations"}:
        raise ResourceValidationError("response must contain only a 'translations' array")
    translations = response["translations"]
    if not isinstance(translations, list):
        raise ResourceValidationError("'translations' must be an array")

    parsed: dict[str, object] = {}
    for item in translations:
        if not isinstance(item, dict) or set(item) != {"id", "value"}:
            raise ResourceValidationError("each translation must contain only 'id' and 'value'")
        identifier = item["id"]
        if not isinstance(identifier, str) or identifier not in expected:
            raise ResourceValidationError(f"unexpected translation id: {identifier!r}")
        if identifier in parsed:
            raise ResourceValidationError(f"duplicate translation id: {identifier}")
        value = _normalize_android_escapes(item["value"])
        _validate_value(expected[identifier], value)
        parsed[identifier] = value

    missing = sorted(expected.keys() - parsed.keys())
    if missing:
        raise ResourceValidationError(f"response omitted translation ids: {', '.join(missing)}")
    return parsed


def parse_batch_response_with_json_repair(
    resources: Iterable[AndroidResource], response_text: str
) -> tuple[dict[str, object], bool]:
    """Apply a unique minimal JSON syntax repair, then run full resource validation."""
    resources = tuple(resources)
    original_error: ResourceValidationError | None = None
    try:
        return parse_batch_response(resources, response_text), False
    except ResourceValidationError as error:
        original_error = error
        try:
            json.loads(response_text)
        except json.JSONDecodeError as json_error:
            positions = range(
                max(0, json_error.pos - 2),
                min(len(response_text), json_error.pos + 2) + 1,
            )
        else:
            raise error

    candidates: set[str] = set()
    for position in positions:
        for character in ('}', ']', ',', '"', ':'):
            candidates.add(response_text[:position] + character + response_text[position:])
        if position < len(response_text):
            candidates.add(response_text[:position] + response_text[position + 1:])

    valid_repairs: dict[str, dict[str, object]] = {}
    for candidate in candidates:
        try:
            candidate_values = parse_batch_response(resources, candidate)
        except ResourceValidationError:
            continue
        canonical = json.dumps(
            candidate_values, ensure_ascii=False, sort_keys=True, separators=(",", ":")
        )
        valid_repairs[canonical] = candidate_values
    if len(valid_repairs) != 1:
        raise ResourceValidationError(
            "automatic JSON repair did not find exactly one validated translation result"
        ) from original_error
    return next(iter(valid_repairs.values())), True


def _set_scalar(element: ET.Element, value: object) -> None:
    text = _scalar_text(value)
    for child in list(element):
        element.remove(child)
    element.text = None
    if "text" in value:  # type: ignore[operator]
        element.text = text
        return
    wrapper = ET.fromstring(f"<wrapper>{text}</wrapper>")
    element.text = wrapper.text
    for child in list(wrapper):
        wrapper.remove(child)
        element.append(child)


def translated_element(resource: AndroidResource, value: object) -> ET.Element:
    element = copy.deepcopy(resource.element)
    if resource.resource_type == "string":
        _set_scalar(element, value)
    elif resource.resource_type == "string-array":
        for child in list(element):
            element.remove(child)
        assert isinstance(value, list)
        for item_value in value:
            item = ET.SubElement(element, "item")
            _set_scalar(item, item_value)
    elif resource.resource_type == "plurals":
        for child in list(element):
            element.remove(child)
        assert isinstance(value, list)
        for translated_item in value:
            assert isinstance(translated_item, dict)
            item = ET.SubElement(
                element, "item", {"quantity": str(translated_item["quantity"])}
            )
            _set_scalar(item, translated_item["value"])
    return element


def merge_translations(
    target_file: ResourceFile,
    translated: dict[str, tuple[AndroidResource, object]],
) -> ET.Element:
    root = copy.deepcopy(target_file.root)
    for _key, (resource, value) in translated.items():
        element = translated_element(resource, value)
        existing_index = next(
            (
                index
                for index, existing in enumerate(root)
                if existing.tag == resource.resource_type
                and existing.attrib.get("name") == resource.name
            ),
            None,
        )
        if existing_index is None:
            root.append(element)
        else:
            root.remove(root[existing_index])
            root.insert(existing_index, element)
    ET.indent(root, space="    ")
    return root


def write_resource_file(path: Path, root: ET.Element) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    ET.ElementTree(root).write(
        temporary, encoding="utf-8", xml_declaration=True, short_empty_elements=True
    )
    temporary.replace(path)


def update_state(
    state: dict[str, Any],
    target: TranslationTarget,
    translated: dict[str, tuple[AndroidResource, object]],
) -> None:
    locale_state = state["translations"].setdefault(target.locale, {})
    for key, (source, _translated_value) in translated.items():
        locale_state[key] = source.source_hash


def write_state(path: Path, state: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(state, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)
