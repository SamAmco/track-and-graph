#!/usr/bin/env python3
"""Generate Compose preview screenshot tests for one supported language."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from string import Template

from android_resources import android_resource_qualifier
from languages import ALL_LANGUAGES, TranslationTarget


PROJECT_ROOT = Path(__file__).resolve().parents[2]
TEMPLATE_PATH = Path(__file__).with_name("templates") / "PlayStoreScreenshotTests.kt.template"
OUTPUT_PATH = (
    PROJECT_ROOT
    / "app/app/src/screenshotTest/kotlin/com/samco/trackandgraph/playstore/PlayStoreScreenshotTests.kt"
)

@dataclass(frozen=True)
class Screenshot:
    number: int
    content_function: str


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


def target_for_locale(locale: str) -> TranslationTarget:
    try:
        return next(target for target in ALL_LANGUAGES if target.locale == locale)
    except StopIteration as error:
        supported = ", ".join(target.locale for target in ALL_LANGUAGES)
        raise ValueError(f"Unsupported locale {locale!r}; expected one of: {supported}") from error


def preview_locale(target: TranslationTarget) -> str:
    if target.locale == "en":
        return "en"
    return android_resource_qualifier(target.locale)


def render(target: TranslationTarget) -> str:
    locale_preview = (
        f'@Preview(name = "{target.locale}", '
        f'locale = "{preview_locale(target)}", '
        "device = PLAY_STORE_DEVICE)"
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
        locale_previews=locale_preview,
        screenshot_tests=screenshot_tests,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--locale",
        default="en",
        help="canonical locale from configuration/translation-languages.tsv (default: en)",
    )
    args = parser.parse_args()

    target = target_for_locale(args.locale)
    generated = render(target)
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(generated, encoding="utf-8")
    print(f"Generated {OUTPUT_PATH.relative_to(PROJECT_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
