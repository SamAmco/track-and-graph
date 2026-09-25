#!/usr/bin/env python3
"""Render or frame one locale's Play Store screenshots."""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TRANSLATIONS_DIR = PROJECT_ROOT / "scripts/translations"
sys.path.insert(0, str(TRANSLATIONS_DIR))

from languages import ALL_LANGUAGES, TranslationTarget, play_locale  # noqa: E402


GENERATOR = TRANSLATIONS_DIR / "generate_playstore_screenshot_tests.py"
REFERENCE_DIR = PROJECT_ROOT / "app/app/src/screenshotTestPlayStoreDebug/reference"
FRAMEIT_ROOT = PROJECT_ROOT / "fastlane/frameit"
FRAMEIT_SCREENSHOTS = FRAMEIT_ROOT / "screenshots"
METADATA_ROOT = PROJECT_ROOT / "fastlane/metadata/android"
SCREENSHOT_COUNT = 8


def target_for_locale(locale: str) -> TranslationTarget:
    aliases = {
        value: target
        for target in ALL_LANGUAGES
        for value in (target.locale, play_locale(target.locale))
    }
    try:
        return aliases[locale]
    except KeyError as error:
        supported = ", ".join(target.locale for target in ALL_LANGUAGES)
        raise ValueError(f"Unsupported locale {locale!r}; expected one of: {supported}") from error


def find_rendered_screenshots(reference_dir: Path, locale: str) -> list[Path]:
    paths: list[Path] = []
    for number in range(1, SCREENSHOT_COUNT + 1):
        matches = sorted(
            reference_dir.glob(
                f"**/*PlayStoreScreenshot{number:02d}_{locale}_*.png"
            )
        )
        if len(matches) != 1:
            raise FileNotFoundError(
                f"Expected exactly one rendered screenshot {number} for {locale}, "
                f"found {len(matches)} under {reference_dir}"
            )
        paths.append(matches[0])
    return paths


def generate_tests(locale: str) -> None:
    subprocess.run(
        [sys.executable, "-B", str(GENERATOR), "--locale", locale],
        cwd=PROJECT_ROOT,
        check=True,
    )


def render_screenshots(target: TranslationTarget, output_dir: Path) -> list[Path]:
    generate_tests(target.locale)
    shutil.rmtree(REFERENCE_DIR, ignore_errors=True)
    subprocess.run(
        [
            "./gradlew",
            ":app:updatePlayStoreDebugScreenshotTest",
            "--rerun-tasks",
        ],
        cwd=PROJECT_ROOT / "app",
        check=True,
    )
    rendered = find_rendered_screenshots(REFERENCE_DIR, target.locale)
    output_dir.mkdir(parents=True, exist_ok=True)
    outputs: list[Path] = []
    for number, source in enumerate(rendered, 1):
        destination = output_dir / f"{number}.png"
        shutil.copy2(source, destination)
        outputs.append(destination)
    return outputs


def raw_screenshots(
    locale: str, screenshot_root: Path = FRAMEIT_SCREENSHOTS
) -> list[Path]:
    raw_dir = screenshot_root / locale
    paths = [
        raw_dir / f"{number}.png" for number in range(1, SCREENSHOT_COUNT + 1)
    ]
    missing = [path for path in paths if not path.is_file()]
    if missing:
        raise FileNotFoundError(
            f"Raw screenshots for {locale} are incomplete; run the snapshot stage first. "
            f"Missing: {', '.join(str(path) for path in missing)}"
        )
    return paths


def frame_screenshots(raw_images: list[Path], locale: str, destination: Path) -> list[Path]:
    raw_dir = raw_images[0].parent
    captions = raw_dir / "title.strings"
    if not captions.is_file():
        raise FileNotFoundError(f"Frameit captions not found for {locale}: {captions}")
    framed_sources = [
        raw_dir / f"{number}_framed.png"
        for number in range(1, SCREENSHOT_COUNT + 1)
    ]
    for framed_source in framed_sources:
        framed_source.unlink(missing_ok=True)
    subprocess.run(
        [
            "bundle",
            "exec",
            "fastlane",
            "run",
            "frameit",
            f"path:{raw_dir}",
        ],
        cwd=PROJECT_ROOT,
        check=True,
    )
    destination.mkdir(parents=True, exist_ok=True)
    outputs: list[Path] = []
    for number, source in enumerate(framed_sources, 1):
        if not source.is_file():
            raise FileNotFoundError(f"Frameit did not produce {source}")
        target = destination / f"{number}_{locale}.png"
        shutil.copy2(source, target)
        outputs.append(target)
    return outputs


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "stage",
        choices=("snapshot", "frame"),
        help="snapshot renders raw images; frame consumes existing raw images",
    )
    parser.add_argument(
        "locale",
        help="canonical app locale (en, de, zh-Hans, ...) or matching Play locale",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    target = target_for_locale(args.locale)
    store_locale = play_locale(target.locale)
    raw_dir = FRAMEIT_SCREENSHOTS / store_locale
    if args.stage == "snapshot":
        raw_images = render_screenshots(target, raw_dir)
        print(f"Rendered {len(raw_images)} {store_locale} screenshots in {raw_dir}")
    else:
        raw_images = raw_screenshots(store_locale)
        destination = METADATA_ROOT / store_locale / "images/phoneScreenshots"
        framed = frame_screenshots(raw_images, store_locale, destination)
        print(f"Framed {len(framed)} {store_locale} screenshots in {destination}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
