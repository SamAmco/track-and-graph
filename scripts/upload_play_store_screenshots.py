#!/usr/bin/env python3
"""Upload existing Play Store listing screenshots in retryable locale batches."""

from __future__ import annotations

import argparse
import shlex
import shutil
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
SCREENSHOT_COUNT = 8
DEFAULT_BATCH_SIZE = 10


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
    metadata_root: Path,
    requested_locale: str | None,
    start_locale: str | None = None,
) -> tuple[ScreenshotSet, ...]:
    if requested_locale is not None and start_locale is not None:
        raise ValueError("--language and --from-language cannot be used together")
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
    if start_locale is not None:
        start_target = target_for_locale(start_locale)
        try:
            start_index = next(
                index
                for index, value in enumerate(complete)
                if value.target == start_target
            )
        except StopIteration as error:
            raise FileNotFoundError(
                f"No upload-ready screenshots found for {play_locale(start_target.locale)}"
            ) from error
        complete = complete[start_index:]
    return complete


def screenshot_batches(
    screenshots: tuple[ScreenshotSet, ...], batch_size: int
) -> tuple[tuple[ScreenshotSet, ...], ...]:
    if batch_size < 1:
        raise ValueError("batch size must be at least 1")
    return tuple(
        screenshots[index : index + batch_size]
        for index in range(0, len(screenshots), batch_size)
    )


def stage_screenshot_batch(
    screenshots: tuple[ScreenshotSet, ...], staging_root: Path
) -> Path:
    if not screenshots:
        raise ValueError("cannot stage an empty screenshot batch")
    batch_name = f"{screenshots[0].store_locale}--{screenshots[-1].store_locale}"
    metadata_root = staging_root / batch_name
    expected_locales = {item.store_locale for item in screenshots}
    if metadata_root.is_dir():
        for existing in metadata_root.iterdir():
            if existing.name not in expected_locales:
                if existing.is_dir() and not existing.is_symlink():
                    shutil.rmtree(existing)
                else:
                    existing.unlink()

    for item in screenshots:
        destination = (
            metadata_root / item.store_locale / "images/phoneScreenshots"
        )
        destination.mkdir(parents=True, exist_ok=True)
        expected_names = {image.name for image in item.images}
        for existing in destination.iterdir():
            if existing.name not in expected_names:
                existing.unlink()
        for source in item.images:
            link = destination / source.name
            if link.is_symlink() and link.resolve() == source.resolve():
                continue
            link.unlink(missing_ok=True)
            link.symlink_to(source.resolve())
    return metadata_root


def supply_command(metadata_root: Path) -> list[str]:
    return [
        "bundle",
        "exec",
        "fastlane",
        "supply",
        "--metadata_path",
        str(metadata_root),
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
    parser.add_argument(
        "--from-language",
        help="resume the full upload at this canonical app locale or Play locale",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=DEFAULT_BATCH_SIZE,
        help=f"locales committed per Play edit (default: {DEFAULT_BATCH_SIZE})",
    )
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
    screenshots = select_screenshot_sets(
        args.metadata_root, args.language, args.from_language
    )
    batches = screenshot_batches(screenshots, args.batch_size)
    for index, batch in enumerate(batches, 1):
        scoped_metadata = stage_screenshot_batch(batch, args.staging_root)
        command = supply_command(scoped_metadata)
        locale_summary = ", ".join(item.store_locale for item in batch)
        print(
            f"==> Batch [{index}/{len(batches)}] Uploading: {locale_summary}",
            flush=True,
        )
        if args.dry_run:
            print(shlex.join(command), flush=True)
        else:
            try:
                subprocess.run(command, cwd=PROJECT_ROOT, check=True)
            except subprocess.CalledProcessError as error:
                resume_locale = batch[0].target.locale
                print(
                    "Screenshot batch failed. Retry this batch and everything after "
                    "with:\n"
                    "  make playstore-screenshots-upload "
                    f"FROM_LANGUAGE={shlex.quote(resume_locale)}",
                    file=sys.stderr,
                )
                return error.returncode
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
