#!/usr/bin/env python3
"""Generate localized Compose preview screenshot tests for every supported language."""

from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path
from string import Template

from android_resources import android_resource_qualifier
from languages import ALL_LANGUAGES, TranslationTarget


PROJECT_ROOT = Path(__file__).resolve().parents[2]
TEMPLATE_PATH = Path(__file__).with_name("templates") / "PlayStoreScreenshotTests.kt.template"
SCREENSHOT_LANGUAGES_PATH = PROJECT_ROOT / "configuration/play-store-screenshot-languages.txt"
OUTPUT_PATH = (
    PROJECT_ROOT
    / "app/app/src/screenshotTest/kotlin/com/samco/trackandgraph/playstore/PlayStoreScreenshotTests.kt"
)


@dataclass(frozen=True)
class Screenshot:
    number: int
    content_function: str


@dataclass(frozen=True)
class ScreenshotLanguage:
    target: TranslationTarget
    play_locale: str


SCREENSHOTS = (
    Screenshot(1, "PlayStoreDailyGroupScreenshotContent"),
    Screenshot(2, "PlayStoreExerciseScreenshotContent"),
    Screenshot(3, "PlayStoreDailyAddStressScreenshotContent"),
    Screenshot(4, "PlayStoreDailyDarkScreenshotContent"),
    Screenshot(5, "PlayStoreRestDayStatisticsScreenshotContent"),
    Screenshot(6, "PlayStoreFlexibleOrganisationScreenshotContent"),
    Screenshot(7, "PlayStoreRemindersScreenshotContent"),
    Screenshot(8, "PlayStoreFunctionEditorScreenshotContent"),
)


def load_screenshot_languages() -> tuple[ScreenshotLanguage, ...]:
    supported = {target.locale: target for target in ALL_LANGUAGES}
    selected: list[ScreenshotLanguage] = []
    seen: set[str] = set()
    for line_number, raw_line in enumerate(
        SCREENSHOT_LANGUAGES_PATH.read_text(encoding="utf-8").splitlines(), 1
    ):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        try:
            locale, play_locale = line.split("\t", 1)
        except ValueError as error:
            raise ValueError(
                f"{SCREENSHOT_LANGUAGES_PATH}:{line_number}: expected "
                "canonical locale<TAB>Google Play locale"
            ) from error
        locale = locale.strip()
        play_locale = play_locale.strip()
        if locale in seen:
            raise ValueError(
                f"{SCREENSHOT_LANGUAGES_PATH}:{line_number}: duplicate locale {locale}"
            )
        if locale not in supported:
            raise ValueError(
                f"{SCREENSHOT_LANGUAGES_PATH}:{line_number}: unsupported locale {locale}"
            )
        seen.add(locale)
        if not play_locale:
            raise ValueError(
                f"{SCREENSHOT_LANGUAGES_PATH}:{line_number}: Google Play locale is empty"
            )
        selected.append(ScreenshotLanguage(supported[locale], play_locale))
    if not selected or selected[0].target.locale != "en":
        raise ValueError(f"{SCREENSHOT_LANGUAGES_PATH}: first locale must be en")
    return tuple(selected)


def preview_locale(target: TranslationTarget) -> str:
    if target.locale == "en":
        return "en"
    return android_resource_qualifier(target.locale)


def render() -> str:
    locale_previews = "\n".join(
        f'@Preview(name = "{selection.target.locale}", '
        f'locale = "{preview_locale(selection.target)}", '
        "device = PLAY_STORE_DEVICE)"
        for selection in load_screenshot_languages()
    )
    screenshot_tests = "\n\n".join(
        "\n".join(
            (
                "@PreviewTest",
                "@PlayStoreLocalesPreview",
                "@Composable",
                f"fun PlayStoreScreenshot{screenshot.number:02d}() {{",
                f"    {screenshot.content_function}()",
                "}",
            )
        )
        for screenshot in SCREENSHOTS
    )
    template = Template(TEMPLATE_PATH.read_text(encoding="utf-8"))
    return template.substitute(
        locale_previews=locale_previews,
        screenshot_tests=screenshot_tests,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail instead of writing if the generated Kotlin file is stale",
    )
    args = parser.parse_args()

    generated = render()
    if args.check:
        current = OUTPUT_PATH.read_text(encoding="utf-8") if OUTPUT_PATH.exists() else ""
        if current != generated:
            print(
                f"{OUTPUT_PATH.relative_to(PROJECT_ROOT)} is stale; run "
                "make playstore-screenshot-tests-generate",
                file=sys.stderr,
            )
            return 1
        return 0

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(generated, encoding="utf-8")
    print(f"Generated {OUTPUT_PATH.relative_to(PROJECT_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
