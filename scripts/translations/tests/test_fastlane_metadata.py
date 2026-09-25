#!/usr/bin/env python3

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path


TRANSLATIONS_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TRANSLATIONS_DIR))

from languages import SUPPORTED_TARGETS, TranslationTarget  # noqa: E402
from translate_fastlane_metadata import (  # noqa: E402
    DEFAULT_METADATA_ROOT,
    FIELDS,
    instructions,
    load_source,
    parse_response,
    play_locale,
    validate_translation,
)


SOURCE = {
    "title": "Track & Graph: Habit Tracker",
    "short_description": "Dashboards for life!",
    "full_description": "Track & Graph: Heading\n\nTrack & Graph first.\n\nSecond.\n\nTrack & Graph uses Lua.\n\nTrack & Graph is private.",
}


class FastlaneMetadataTest(unittest.TestCase):
    def test_repository_metadata_covers_manifest_and_passes_validation(self) -> None:
        source = load_source(DEFAULT_METADATA_ROOT, "en-GB")
        for target in SUPPORTED_TARGETS:
            directory = DEFAULT_METADATA_ROOT / play_locale(target.locale)
            translated = {
                field: (directory / f"{field}.txt").read_text(encoding="utf-8").strip()
                for field in FIELDS
            }
            self.assertEqual((), validate_translation(source, translated), target.locale)

    def test_uses_explicit_play_locale_from_manifest(self) -> None:
        self.assertEqual("en-GB", play_locale("en"))
        self.assertEqual("de-DE", play_locale("de"))
        self.assertEqual("zh-CN", play_locale("zh-Hans"))
        self.assertEqual("es-ES", play_locale("es"))
        self.assertEqual("af", play_locale("af"))

    def test_parses_exact_json_fields(self) -> None:
        self.assertEqual(SOURCE, parse_response(json.dumps(SOURCE)))
        with self.assertRaisesRegex(ValueError, "exactly these fields"):
            parse_response(json.dumps({**SOURCE, "extra": "value"}))

    def test_rejects_changed_brand_and_block_structure(self) -> None:
        translated = dict(SOURCE)
        translated["full_description"] = translated["full_description"].replace(
            "Track & Graph first.\n\nSecond.", "Track and Graph first. Second."
        )
        issues = validate_translation(SOURCE, translated)
        self.assertIn("full_description changed the number of 'Track & Graph' occurrences", issues)
        self.assertIn("full_description changed the blank-line-separated block count", issues)

    def test_rejects_store_character_limit(self) -> None:
        translated = dict(SOURCE)
        translated["title"] = "Track & Graph: " + "x" * 16
        self.assertIn("title is 31 characters; limit is 30", validate_translation(SOURCE, translated))

    def test_prompt_preserves_legal_and_product_claims(self) -> None:
        prompt = instructions(TranslationTarget("de", "German"), "Domain context")
        for phrase in ("no ads", "no accounts", "no paywalled features", "only on the user's device"):
            self.assertIn(phrase, prompt)

    def test_albanian_prompt_guards_against_observed_language_confusion(self) -> None:
        prompt = instructions(TranslationTarget("sq", "Albanian"), "Domain context")
        self.assertIn("Latin alphabet", prompt)
        self.assertIn("do not confuse it with Macedonian", prompt)


if __name__ == "__main__":
    unittest.main()
