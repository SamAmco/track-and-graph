#!/usr/bin/env python3

from __future__ import annotations

import sys
import io
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch


TRANSLATIONS_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TRANSLATIONS_DIR))

from markdown_validation import validate_markdown  # noqa: E402
from languages import SUPPORTED_TARGETS  # noqa: E402
from providers.base import TranslationResult  # noqa: E402
import translate_release_notes as release_notes  # noqa: E402
from translate_release_notes import _instructions  # noqa: E402


FIXTURE = Path(__file__).parent / "fixtures" / "complex_release_note.md"
DOMAIN_BRIEF = TRANSLATIONS_DIR / "domain_brief.md"


class MarkdownValidationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.source = FIXTURE.read_text(encoding="utf-8")

    def assert_rejected(self, before: str, after: str, check: str) -> None:
        result = validate_markdown(self.source, self.source.replace(before, after, 1))
        self.assertIn(check, result.failures)

    def test_source_validates_against_itself(self) -> None:
        self.assertTrue(validate_markdown(self.source, self.source).is_valid)

    def test_domain_brief_keeps_core_context_with_generous_safety_limit(self) -> None:
        brief = DOMAIN_BRIEF.read_text(encoding="utf-8").strip()
        self.assertLessEqual(len(brief), 8000)
        self.assertIn("Core context — do not remove", brief)
        self.assertIn("A Tracker stores", brief)
        self.assertIn("A Function is a derived data source", brief)

    def test_supported_language_list_is_complete_unique_and_ltr(self) -> None:
        locales = [target.locale for target in SUPPORTED_TARGETS]
        self.assertEqual(65, len(locales))
        self.assertEqual(len(locales), len(set(locales)))
        self.assertTrue({"de", "es", "fr", "pt", "zh"}.issubset(locales))
        self.assertTrue({"ar", "fa", "he", "ur"}.isdisjoint(locales))

    def test_retry_prompt_includes_failed_structure_details(self) -> None:
        instructions = _instructions(
            "Finnish",
            "Domain context",
            self.source,
            ("indented_code_exact", "markdown_marker_counts_exact"),
        )
        self.assertIn("    Indented code is also immutable", instructions)
        self.assertIn("'**'=16", instructions)

    def test_invalid_translation_is_written_and_reported_without_retry(self) -> None:
        invalid = self.source.replace("do **not** translate", "do not translate")

        class FakeTranslator:
            calls = 0

            def translate(self, **_kwargs: object) -> TranslationResult:
                self.calls += 1
                return TranslationResult(text=invalid, usage={})

        fake = FakeTranslator()
        with tempfile.TemporaryDirectory() as output_dir:
            arguments = [
                "translate_release_notes.py",
                str(FIXTURE),
                "--target", "xx=Test",
                "--output-dir", output_dir,
            ]
            stdout = io.StringIO()
            with (
                patch.object(release_notes, "_translator", return_value=fake),
                patch.object(sys, "argv", arguments),
                patch.dict(release_notes.os.environ, {"OPENAI_API_KEY": "test"}),
                redirect_stdout(stdout),
            ):
                self.assertEqual(1, release_notes.main())

            self.assertEqual(1, fake.calls)
            self.assertEqual(invalid, (Path(output_dir) / "xx.md").read_text())
            summary = json.loads(stdout.getvalue().splitlines()[-1])["summary"]
            self.assertEqual("xx", summary["invalid"][0]["locale"])
            self.assertIn(
                "markdown_marker_counts_exact",
                summary["invalid"][0]["issues"],
            )

    def test_rejects_changed_url(self) -> None:
        self.assert_rejected("https://example.com/releases", "https://invalid.example/releases", "urls_exact")

    def test_rejects_changed_link_destination(self) -> None:
        self.assert_rejected("functions.md#examples", "functions.md#wrong", "link_destinations_exact")

    def test_rejects_changed_fenced_code(self) -> None:
        self.assert_rejected("local value", "local translated", "fenced_code_exact")

    def test_rejects_changed_indented_code(self) -> None:
        self.assert_rejected("Indented code is also immutable", "Translated code is also immutable", "indented_code_exact")

    def test_rejects_changed_inline_code(self) -> None:
        self.assert_rejected("featureId", "translatedId", "inline_code_exact")

    def test_rejects_changed_reference_identifier(self) -> None:
        self.assert_rejected("[docs]", "[documentation]", "reference_ids_exact")

    def test_rejects_changed_task_marker(self) -> None:
        self.assert_rejected("- [x]", "- [ ]", "task_markers_exact")

    def test_ignores_a_missing_trailing_blank_line(self) -> None:
        self.assertTrue(validate_markdown(self.source, self.source.rstrip() + "\n").is_valid)


if __name__ == "__main__":
    unittest.main()
