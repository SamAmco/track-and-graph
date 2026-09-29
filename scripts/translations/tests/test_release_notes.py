#!/usr/bin/env python3

from __future__ import annotations

import tempfile
import sys
import unittest
from pathlib import Path


TRANSLATIONS_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TRANSLATIONS_DIR))

from languages import ALL_LANGUAGES
from providers.base import TranslationResult
from prepare_release_notes import (
    END_MARKER,
    MAX_CHARACTERS,
    START_MARKER,
    _release_tag_key,
    destination_for,
    editor_document,
    extract_note,
    note_issues,
    text_issues,
    translate_all,
    translation_instructions,
    validate_outputs,
    write_note,
)


class FakeTranslator:
    def __init__(self, text: str) -> None:
        self.text = text

    def translate(self, *, instructions: str, source_text: str) -> TranslationResult:
        return TranslationResult(self.text, {"input_tokens": 1, "output_tokens": 1})


class ReleaseNotesTest(unittest.TestCase):
    def test_editor_document_keeps_commit_reference_outside_note(self) -> None:
        document = editor_document("v10.4.0", "abc123 Fixed a thing")
        edited = document.replace(
            f"{START_MARKER}\n\n{END_MARKER}",
            f"{START_MARKER}\n- Fixed a thing\n{END_MARKER}",
        )

        self.assertEqual("- Fixed a thing", extract_note(edited))
        self.assertIn("abc123 Fixed a thing", document)
        self.assertNotIn("abc123", extract_note(edited))

    def test_extract_note_requires_both_unique_markers_and_copy(self) -> None:
        with self.assertRaisesRegex(ValueError, "exactly one"):
            extract_note("No markers")
        with self.assertRaisesRegex(ValueError, "empty"):
            extract_note(f"{START_MARKER}\n{END_MARKER}")

    def test_release_tag_key_ignores_non_release_tags(self) -> None:
        self.assertEqual(((10, 4, 0), 2), _release_tag_key("v10.4.0"))
        self.assertEqual(((10, 4, 0), 0), _release_tag_key("rc-v10.4.0"))
        self.assertIsNone(_release_tag_key("list"))

    def test_prompt_states_the_strict_character_limit(self) -> None:
        prompt = translation_instructions(ALL_LANGUAGES[1], "Context", "- Fixed")
        self.assertIn("at most 500 Unicode characters", prompt)
        self.assertIn("including spaces and line breaks", prompt)

    def test_note_issues_report_length_and_structure(self) -> None:
        issues = note_issues("- Fixed", "x" * (MAX_CHARACTERS + 1))
        self.assertIn("release notes are 501 characters; limit is 500", issues)
        self.assertIn("block_structure_exact", issues)

    def test_english_text_validation_does_not_compare_itself(self) -> None:
        self.assertEqual((), text_issues("- Fixed"))

    def test_invalid_translation_is_still_written(self) -> None:
        target = ALL_LANGUAGES[1]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            invalid, failures = translate_all(
                source="- Fixed",
                version_code="123",
                targets=(target,),
                translator=FakeTranslator("x" * (MAX_CHARACTERS + 1)),
                domain_context="Context",
                metadata_root=root,
            )

            path = destination_for("123", target, root)
            self.assertEqual("x" * (MAX_CHARACTERS + 1), path.read_text(encoding="utf-8"))
            self.assertEqual([], failures)
            self.assertEqual(target.locale, invalid[0]["locale"])
            self.assertEqual(MAX_CHARACTERS + 1, invalid[0]["characters"])

    def test_offline_validation_checks_every_requested_locale(self) -> None:
        english, target = ALL_LANGUAGES[:2]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_note("123", english, "- Fixed", root)
            missing = validate_outputs(
                version_code="123", targets=(english, target), metadata_root=root
            )
            self.assertEqual(target.locale, missing[0]["locale"])
            self.assertIn("missing", missing[0]["issues"][0])

            write_note("123", target, "- " + "x" * MAX_CHARACTERS, root)
            invalid = validate_outputs(
                version_code="123", targets=(english, target), metadata_root=root
            )
            self.assertEqual(target.locale, invalid[0]["locale"])
            self.assertIn("limit is 500", invalid[0]["issues"][0])

    def test_offline_validation_lists_every_locale_when_english_is_missing(self) -> None:
        targets = ALL_LANGUAGES[:3]
        with tempfile.TemporaryDirectory() as temp:
            invalid = validate_outputs(
                version_code="123", targets=targets, metadata_root=Path(temp)
            )

        self.assertEqual(
            [target.locale for target in targets],
            [item["locale"] for item in invalid],
        )


if __name__ == "__main__":
    unittest.main()
