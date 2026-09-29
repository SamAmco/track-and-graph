#!/usr/bin/env python3
"""Update and validate the localized in-app changelog index."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TRANSLATIONS_DIR = PROJECT_ROOT / "scripts/translations"
sys.path.insert(0, str(TRANSLATIONS_DIR))

from languages import ALL_LANGUAGES  # noqa: E402


CHANGELOG_ROOT = PROJECT_ROOT / "changelogs"
INDEX_PATH = CHANGELOG_ROOT / "index.json"
DEFAULT_LOCALES = tuple(target.locale for target in ALL_LANGUAGES)
VERSION_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:-(.+))?$")
LOCALE_RE = re.compile(r"^[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*$")
REVIEW_ONLY_MARKER = "<!-- REVIEW-ONLY-COMMITS: remove before translation/publishing -->"


class IndexError(ValueError):
    pass


def version_key(version: str) -> tuple[int, int, int, int, str]:
    match = VERSION_RE.fullmatch(version)
    if not match:
        raise IndexError(f"Invalid changelog version: {version!r}")
    major, minor, patch, suffix = match.groups()
    return int(major), int(minor), int(patch), int(suffix is None), suffix or ""


def read_index(path: Path = INDEX_PATH) -> dict[str, object]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise IndexError(f"Could not read {path}: {error}") from error
    if not isinstance(value, dict) or set(value) != {"changelogs"}:
        raise IndexError(f"{path}: expected only a 'changelogs' object")
    if not isinstance(value["changelogs"], dict):
        raise IndexError(f"{path}: 'changelogs' must be an object")
    return value


def discover_version(
    version: str,
    *,
    changelog_root: Path = CHANGELOG_ROOT,
    locales: tuple[str, ...] | None = None,
) -> dict[str, str]:
    version_key(version)
    supported = locales or DEFAULT_LOCALES
    directory = changelog_root / version
    if not directory.is_dir():
        raise IndexError(f"Missing changelog directory: {directory}")
    discovered = {path.stem: path for path in directory.glob("*.md") if path.is_file()}
    unsupported = sorted(set(discovered) - set(supported))
    if unsupported:
        raise IndexError(
            f"{directory}: unsupported locale file(s): {', '.join(unsupported)}"
        )
    if "en" not in discovered:
        raise IndexError(f"Missing English changelog: {directory / 'en.md'}")
    return {
        locale: f"{version}/{locale}.md"
        for locale in supported
        if locale in discovered
    }


def ordered_changelogs(changelogs: dict[str, object]) -> dict[str, object]:
    return {
        version: changelogs[version]
        for version in sorted(changelogs, key=version_key, reverse=True)
    }


def update_version(
    version: str,
    *,
    changelog_root: Path = CHANGELOG_ROOT,
    index_path: Path = INDEX_PATH,
    locales: tuple[str, ...] | None = None,
) -> dict[str, object]:
    index = read_index(index_path)
    changelogs = dict(index["changelogs"])
    changelogs[version] = discover_version(
        version, changelog_root=changelog_root, locales=locales
    )
    updated = {"changelogs": ordered_changelogs(changelogs)}
    index_path.write_text(
        json.dumps(updated, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return updated


def validation_issues(
    *,
    changelog_root: Path = CHANGELOG_ROOT,
    index_path: Path = INDEX_PATH,
    locales: tuple[str, ...] | None = None,
) -> tuple[str, ...]:
    supported = locales or DEFAULT_LOCALES
    supported_set = set(supported)
    try:
        index = read_index(index_path)
    except IndexError as error:
        return (str(error),)
    changelogs = index["changelogs"]
    assert isinstance(changelogs, dict)
    issues: list[str] = []
    versions = list(changelogs)
    try:
        expected_versions = sorted(versions, key=version_key, reverse=True)
    except IndexError as error:
        issues.append(str(error))
        expected_versions = versions
    if versions != expected_versions:
        issues.append("changelog versions are not ordered newest-first")

    indexed_versions: set[str] = set()
    for version, raw_entries in changelogs.items():
        if not VERSION_RE.fullmatch(version):
            continue
        indexed_versions.add(version)
        if not isinstance(raw_entries, dict):
            issues.append(f"{version}: locale entries must be an object")
            continue
        entries = raw_entries
        if "en" not in entries:
            issues.append(f"{version}: English locale 'en' is required")
        unknown = sorted(set(entries) - supported_set)
        if unknown:
            issues.append(f"{version}: unsupported locale(s): {', '.join(unknown)}")
        expected_locale_order = [locale for locale in supported if locale in entries]
        if list(entries) != expected_locale_order:
            issues.append(f"{version}: locales are not in language-manifest order")
        for locale, path_value in entries.items():
            if not LOCALE_RE.fullmatch(locale):
                issues.append(f"{version}: invalid locale {locale!r}")
            expected_path = f"{version}/{locale}.md"
            if path_value != expected_path:
                issues.append(
                    f"{version}/{locale}: expected path {expected_path!r}, got {path_value!r}"
                )
                continue
            if not (changelog_root / expected_path).is_file():
                issues.append(f"{version}/{locale}: missing {changelog_root / expected_path}")
                continue
            if REVIEW_ONLY_MARKER in (changelog_root / expected_path).read_text(
                encoding="utf-8"
            ):
                issues.append(
                    f"{version}/{locale}: remove the review-only commit inventory before publishing"
                )

        directory = changelog_root / version
        if directory.is_dir():
            disk_locales = {
                path.stem for path in directory.glob("*.md") if path.is_file()
            }
            missing_entries = sorted(disk_locales - set(entries))
            if missing_entries:
                issues.append(
                    f"{version}: Markdown locale file(s) missing from index: "
                    + ", ".join(missing_entries)
                )

    disk_versions = {
        path.parent.name
        for path in changelog_root.glob("*/en.md")
        if VERSION_RE.fullmatch(path.parent.name)
    }
    for version in sorted(disk_versions - indexed_versions, key=version_key, reverse=True):
        issues.append(f"{version}: English changelog exists but version is missing from index")
    return tuple(issues)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    update = subparsers.add_parser("update")
    update.add_argument("version")
    subparsers.add_parser("validate")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        if args.command == "update":
            updated = update_version(args.version)
            locales = updated["changelogs"][args.version]
            print(f"Updated {INDEX_PATH} with {len(locales)} locale(s) for {args.version}")
            return 0
        issues = validation_issues()
        if issues:
            for issue in issues:
                print(f"error: {issue}", file=sys.stderr)
            return 1
        print(f"Validated {INDEX_PATH}")
        return 0
    except IndexError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
