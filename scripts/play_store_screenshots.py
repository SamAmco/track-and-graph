#!/usr/bin/env python3
"""Render or frame one locale's Play Store screenshots."""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TRANSLATIONS_DIR = PROJECT_ROOT / "scripts/translations"
sys.path.insert(0, str(TRANSLATIONS_DIR))

from languages import (  # noqa: E402
    ALL_LANGUAGES,
    SUPPORTED_TARGETS,
    TranslationTarget,
    play_locale,
)


GENERATOR = TRANSLATIONS_DIR / "generate_playstore_screenshot_tests.py"
REFERENCE_DIR = PROJECT_ROOT / "app/app/src/screenshotTestPlayStoreDebug/reference"
FRAMEIT_ROOT = PROJECT_ROOT / "fastlane/frameit"
FRAMEIT_SCREENSHOTS = FRAMEIT_ROOT / "screenshots"
FRAMEIT_CONFIG = FRAMEIT_SCREENSHOTS / "Framefile.json"
FRAMEIT_BUNDLED_FONT = FRAMEIT_SCREENSHOTS / "fonts/Roboto-Bold.ttf"
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


def caption_font(captions: Path) -> Path:
    caption_text = captions.read_text(encoding="utf-8")
    characters = sorted(
        {character for character in caption_text if not character.isspace()},
        key=ord,
    )
    charset = " ".join(f"{ord(character):04x}" for character in characters)
    try:
        matches = subprocess.run(
            [
                "fc-match",
                "-f",
                "%{family}\t%{file}\n",
                f"Roboto:charset={charset}:weight=bold",
            ],
            check=True,
            capture_output=True,
            text=True,
        ).stdout.splitlines()
        family, filename = matches[0].split("\t", 1)
    except (
        FileNotFoundError,
        IndexError,
        subprocess.CalledProcessError,
        ValueError,
    ) as error:
        raise RuntimeError(
            "Could not select a font for the translated Frameit captions. "
            "Install fontconfig and the Google Noto fonts."
        ) from error
    font = FRAMEIT_BUNDLED_FONT if "Roboto" in family else Path(filename)
    if not font.is_file():
        raise FileNotFoundError(f"Matched caption font does not exist: {font}")
    return font.resolve()


def write_scoped_frameit_config(raw_dir: Path, font: Path) -> Path:
    config = json.loads(FRAMEIT_CONFIG.read_text(encoding="utf-8"))
    for text_style in ("keyword", "title"):
        config["default"][text_style]["font"] = os.path.relpath(font, raw_dir)
    config["default"]["background"] = os.path.relpath(
        (FRAMEIT_ROOT / "background.jpg").resolve(), raw_dir
    )
    destination = raw_dir / "Framefile.json"
    destination.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
    return destination


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
    font = caption_font(captions)
    scoped_config = write_scoped_frameit_config(raw_dir, font)
    print(f"Using {font.name} for {locale} captions", flush=True)
    try:
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
    finally:
        scoped_config.unlink(missing_ok=True)
    destination.mkdir(parents=True, exist_ok=True)
    outputs: list[Path] = []
    for number, source in enumerate(framed_sources, 1):
        if not source.is_file():
            raise FileNotFoundError(f"Frameit did not produce {source}")
        target = destination / f"{number}_{locale}.png"
        shutil.copy2(source, target)
        outputs.append(target)
    return outputs


def snapshot_target(target: TranslationTarget) -> None:
    store_locale = play_locale(target.locale)
    raw_dir = FRAMEIT_SCREENSHOTS / store_locale
    raw_images = render_screenshots(target, raw_dir)
    print(f"Rendered {len(raw_images)} {store_locale} screenshots in {raw_dir}")


def frame_target(target: TranslationTarget) -> None:
    store_locale = play_locale(target.locale)
    raw_images = raw_screenshots(store_locale)
    destination = METADATA_ROOT / store_locale / "images/phoneScreenshots"
    framed = frame_screenshots(raw_images, store_locale, destination)
    print(f"Framed {len(framed)} {store_locale} screenshots in {destination}")


def generate_localized_screenshots() -> int:
    failures: list[tuple[str, Exception]] = []
    for index, target in enumerate(SUPPORTED_TARGETS, 1):
        store_locale = play_locale(target.locale)
        print(
            f"==> [{index}/{len(SUPPORTED_TARGETS)}] Generating {store_locale}",
            flush=True,
        )
        try:
            snapshot_target(target)
            frame_target(target)
        except Exception as error:  # Continue so one locale does not lose the rest.
            failures.append((store_locale, error))
            print(f"FAILED {store_locale}: {error}", file=sys.stderr, flush=True)

    if failures:
        print("\nFailed locales:", file=sys.stderr)
        for locale, error in failures:
            print(f"- {locale}: {error}", file=sys.stderr)
        return 1
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "stage",
        choices=("snapshot", "frame", "generate-localized"),
        help=(
            "snapshot renders one locale; frame consumes one locale's raw images; "
            "generate-localized renders and frames every non-English locale"
        ),
    )
    parser.add_argument(
        "locale",
        nargs="?",
        help="canonical app locale (en, de, zh-Hans, ...) or matching Play locale",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.stage == "generate-localized":
        if args.locale is not None:
            raise SystemExit("generate-localized does not accept a locale")
        return generate_localized_screenshots()
    if args.locale is None:
        raise SystemExit(f"{args.stage} requires a locale")
    target = target_for_locale(args.locale)
    if args.stage == "snapshot":
        snapshot_target(target)
    else:
        frame_target(target)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
