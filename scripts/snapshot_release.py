#!/usr/bin/env python3
"""Create the next snapshot-version revision with jj."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parent.parent
GRADLE_FILE = ROOT_DIR / "app" / "app" / "build.gradle.kts"

VERSION_CODE_RE = re.compile(r"^(?P<prefix>\s*versionCode\s*=\s*)(?P<value>\d+)(?P<suffix>\s*)$", re.MULTILINE)
VERSION_NAME_RE = re.compile(r'^(?P<prefix>\s*versionName\s*=\s*")(?P<value>[^"]+)(?P<suffix>"\s*)$', re.MULTILINE)
SNAPSHOT_VERSION_RE = re.compile(r"^(?P<base>\d+\.\d+\.\d+)-SNAPSHOT(?P<number>\d+)$")
STABLE_VERSION_RE = re.compile(r"^(?P<major>\d+)\.(?P<minor>\d+)\.(?P<patch>\d+)$")
RELEASE_TYPES = ("major", "minor", "patch")


class SnapshotReleaseError(Exception):
    """An expected validation failure."""


def run_jj(*args: str, capture_output: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["jj", *args],
        cwd=ROOT_DIR,
        check=True,
        text=True,
        capture_output=capture_output,
    )


def require_empty_working_copy() -> None:
    if run_jj("diff", "--summary").stdout.strip():
        raise SnapshotReleaseError(
            "The jj working copy is not empty. Commit or move those changes before creating a snapshot release."
        )


def replace_single(pattern: re.Pattern[str], content: str, value: str, label: str) -> str:
    matches = list(pattern.finditer(content))
    if len(matches) != 1:
        raise SnapshotReleaseError(
            f"Expected exactly one {label} in {GRADLE_FILE.relative_to(ROOT_DIR)}, found {len(matches)}."
        )

    match = matches[0]
    replacement = f"{match.group('prefix')}{value}{match.group('suffix')}"
    return content[: match.start()] + replacement + content[match.end() :]


def current_versions(content: str) -> tuple[int, str]:
    code_matches = list(VERSION_CODE_RE.finditer(content))
    name_matches = list(VERSION_NAME_RE.finditer(content))
    if len(code_matches) != 1 or len(name_matches) != 1:
        raise SnapshotReleaseError(
            "Expected exactly one versionCode and versionName in "
            f"{GRADLE_FILE.relative_to(ROOT_DIR)}."
        )

    return int(code_matches[0].group("value")), name_matches[0].group("value")


def bump_stable_version(version_name: str, release_type: str) -> str:
    match = STABLE_VERSION_RE.fullmatch(version_name)
    if match is None:
        raise SnapshotReleaseError(
            f'Current versionName "{version_name}" is neither a numbered snapshot '
            'nor a stable semantic version such as "10.4.0".'
        )

    major = int(match.group("major"))
    minor = int(match.group("minor"))
    patch = int(match.group("patch"))
    if release_type == "major":
        return f"{major + 1}.0.0-SNAPSHOT1"
    if release_type == "minor":
        return f"{major}.{minor + 1}.0-SNAPSHOT1"
    if release_type == "patch":
        return f"{major}.{minor}.{patch + 1}-SNAPSHOT1"
    raise SnapshotReleaseError(f"Unknown release type: {release_type}")


def next_versions(content: str, release_type: str | None = None) -> tuple[int, str, str]:
    current_code, current_name = current_versions(content)
    snapshot_match = SNAPSHOT_VERSION_RE.fullmatch(current_name)
    if snapshot_match is not None:
        next_name = (
            f"{snapshot_match.group('base')}-SNAPSHOT"
            f"{int(snapshot_match.group('number')) + 1}"
        )
    elif release_type is not None:
        next_name = bump_stable_version(current_name, release_type)
    else:
        raise SnapshotReleaseError(
            f'A release type is required to start snapshots after "{current_name}".'
        )

    return current_code + 1, current_name, next_name


def updated_gradle(content: str, next_code: int, next_name: str) -> str:
    content = replace_single(VERSION_CODE_RE, content, str(next_code), "versionCode")
    return replace_single(VERSION_NAME_RE, content, next_name, "versionName")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Increment versionCode and SNAPSHOT<n>, then commit the bump with jj."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="show the next versions and commit message without changing anything",
    )
    parser.add_argument(
        "--release-type",
        choices=RELEASE_TYPES,
        help="start a new major, minor, or patch snapshot without prompting",
    )
    return parser.parse_args()


def prompt_for_release_type(current_name: str) -> str:
    print(f'Current version "{current_name}" is not a snapshot.')
    print("What type will the next release be?")
    print("  1) Major")
    print("  2) Minor")
    print("  3) Patch")
    choices = {
        "1": "major",
        "major": "major",
        "2": "minor",
        "minor": "minor",
        "3": "patch",
        "patch": "patch",
    }

    while True:
        try:
            choice = input("Select [1/2/3]: ").strip().lower()
        except EOFError as error:
            raise SnapshotReleaseError("No release type was selected.") from error
        release_type = choices.get(choice)
        if release_type is not None:
            return release_type
        print("Please enter 1, 2, 3, major, minor, or patch.")


def main() -> int:
    args = parse_args()

    try:
        content = GRADLE_FILE.read_text()
        _, current_name = current_versions(content)
        release_type = args.release_type
        if SNAPSHOT_VERSION_RE.fullmatch(current_name) is None and release_type is None:
            release_type = prompt_for_release_type(current_name)

        next_code, current_name, next_name = next_versions(content, release_type)
        commit_message = f"Snapshot release {next_name}"

        print(f"versionCode: {next_code - 1} -> {next_code}")
        print(f"versionName: {current_name} -> {next_name}")
        print(f"commit: {commit_message}")

        if args.dry_run:
            return 0

        require_empty_working_copy()
        GRADLE_FILE.write_text(updated_gradle(content, next_code, next_name))
        run_jj("commit", "-m", commit_message, capture_output=False)
        print(f"Created snapshot revision for {next_name}.")
        return 0
    except SnapshotReleaseError as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1
    except (OSError, subprocess.CalledProcessError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
