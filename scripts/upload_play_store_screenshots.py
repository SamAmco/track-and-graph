#!/usr/bin/env python3
"""Upload existing Play Store screenshots one locale at a time."""

from __future__ import annotations

import argparse
import re
import shlex
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TRANSLATIONS_DIR = PROJECT_ROOT / "scripts/translations"
sys.path.insert(0, str(TRANSLATIONS_DIR))

from languages import ALL_LANGUAGES, TranslationTarget, play_locale  # noqa: E402


METADATA_ROOT = PROJECT_ROOT / "fastlane/metadata/android"
STAGING_ROOT = PROJECT_ROOT / "fastlane/generated/screenshot-upload"
BUILD_GRADLE = PROJECT_ROOT / "app/app/build.gradle.kts"
SCREENSHOT_COUNT = 8
VERSION_CODE_RE = re.compile(r"^\s*versionCode\s*=\s*(\d+)\s*$", re.MULTILINE)


@dataclass(frozen=True)
class ScreenshotSet:
    target: TranslationTarget
    images: tuple[Path, ...]

    @property
    def store_locale(self) -> str:
        return play_locale(self.target.locale)


def target_for_locale(locale: str) -> TranslationTarget:
    aliases = {
        alias: target
        for target in ALL_LANGUAGES
        for alias in (target.locale, play_locale(target.locale))
    }
    try:
        return aliases[locale]
    except KeyError as error:
        supported = ", ".join(target.locale for target in ALL_LANGUAGES)
        raise ValueError(
            f"Unsupported locale {locale!r}; expected one of: {supported}"
        ) from error


def expected_images(metadata_root: Path, target: TranslationTarget) -> tuple[Path, ...]:
    store_locale = play_locale(target.locale)
    directory = metadata_root / store_locale / "images/phoneScreenshots"
    return tuple(
        directory / f"{number}_{store_locale}.png"
        for number in range(1, SCREENSHOT_COUNT + 1)
    )


def screenshot_set(
    metadata_root: Path, target: TranslationTarget
) -> ScreenshotSet | None:
    images = expected_images(metadata_root, target)
    present = tuple(image for image in images if image.is_file())
    if not present:
        return None
    if len(present) != SCREENSHOT_COUNT:
        missing = ", ".join(str(image) for image in images if not image.is_file())
        raise FileNotFoundError(
            f"Partial screenshot set for {play_locale(target.locale)}; missing: {missing}"
        )
    return ScreenshotSet(target, images)


def select_screenshot_sets(
    metadata_root: Path, requested_locale: str | None
) -> tuple[ScreenshotSet, ...]:
    if requested_locale is not None:
        target = target_for_locale(requested_locale)
        selected = screenshot_set(metadata_root, target)
        if selected is None:
            raise FileNotFoundError(
                f"No upload-ready screenshots found for {play_locale(target.locale)}"
            )
        return (selected,)

    selected = tuple(
        screenshot_set(metadata_root, target) for target in ALL_LANGUAGES
    )
    complete = tuple(value for value in selected if value is not None)
    if not complete:
        raise FileNotFoundError(
            f"No complete screenshot sets found beneath {metadata_root}"
        )
    return complete


def current_version_code(build_gradle: Path = BUILD_GRADLE) -> str:
    matches = VERSION_CODE_RE.findall(build_gradle.read_text(encoding="utf-8"))
    if len(matches) != 1:
        raise ValueError(f"Expected exactly one versionCode in {build_gradle}")
    return matches[0]


def stage_screenshot_set(screenshots: ScreenshotSet, staging_root: Path) -> Path:
    metadata_root = staging_root / screenshots.store_locale
    destination = (
        metadata_root
        / screenshots.store_locale
        / "images/phoneScreenshots"
    )
    destination.mkdir(parents=True, exist_ok=True)
    expected_names = {image.name for image in screenshots.images}
    for existing in destination.iterdir():
        if existing.name not in expected_names:
            existing.unlink()
    for source in screenshots.images:
        link = destination / source.name
        if link.is_symlink() and link.resolve() == source.resolve():
            continue
        link.unlink(missing_ok=True)
        link.symlink_to(source.resolve())
    return metadata_root


def supply_command(
    metadata_root: Path, *, track: str, version_code: str
) -> list[str]:
    return [
        "bundle",
        "exec",
        "fastlane",
        "supply",
        "--metadata_path",
        str(metadata_root),
        "--track",
        track,
        "--version_code",
        version_code,
        "--skip_upload_apk",
        "--skip_upload_aab",
        "--skip_upload_metadata",
        "--skip_upload_changelogs",
        "--skip_upload_images",
        "--sync_image_upload",
    ]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--language",
        help="canonical app locale or Play locale; defaults to every complete set",
    )
    parser.add_argument("--track", default="production")
    parser.add_argument("--version-code", default=None)
    parser.add_argument("--metadata-root", type=Path, default=METADATA_ROOT)
    parser.add_argument("--staging-root", type=Path, default=STAGING_ROOT)
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="prepare the scoped metadata views and print commands without uploading",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    screenshots = select_screenshot_sets(args.metadata_root, args.language)
    version_code = args.version_code or current_version_code()
    for index, item in enumerate(screenshots, 1):
        scoped_metadata = stage_screenshot_set(item, args.staging_root)
        command = supply_command(
            scoped_metadata, track=args.track, version_code=version_code
        )
        print(
            f"==> [{index}/{len(screenshots)}] Uploading {item.store_locale}",
            flush=True,
        )
        if args.dry_run:
            print(shlex.join(command), flush=True)
        else:
            subprocess.run(command, cwd=PROJECT_ROOT, check=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
