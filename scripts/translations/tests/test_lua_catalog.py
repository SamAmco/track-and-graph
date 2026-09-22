#!/usr/bin/env python3

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path


TRANSLATIONS_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TRANSLATIONS_DIR))

from lua_catalog import (  # noqa: E402
    CopyField, LuaTranslationError, apply_function_translations,
    apply_shared_translations, batches, export_copy, parse_response,
    parse_response_with_repair, recover_items, render_failure, render_table,
)


class LuaCatalogTest(unittest.TestCase):
    def test_exports_executed_function_metadata(self) -> None:
        fields = export_copy("functions", "duration-in-units")
        self.assertEqual(
            ["duration-in-units/title", "duration-in-units/description", "duration-in-units/config/unit/name"],
            [field.id for field in fields],
        )
        self.assertTrue(fields[1].markdown)

    def test_shared_export_retains_existing_locale_ownership(self) -> None:
        field = export_copy("shared", "_all_fields")[0]
        self.assertIn("en", field.existing_locales)
        self.assertIn("fr", field.existing_locales)

    def test_response_requires_exact_ids(self) -> None:
        fields = [CopyField("title", "Title", False)]
        with self.assertRaisesRegex(LuaTranslationError, "unexpected translation id"):
            parse_response(fields, '{"translations":[{"id":"wrong","value":"Titre"}]}')

    def test_response_validates_markdown(self) -> None:
        fields = [CopyField("description", "See [docs](https://example.com).", True)]
        with self.assertRaisesRegex(LuaTranslationError, "changed Markdown"):
            parse_response(fields, '{"translations":[{"id":"description","value":"Voir [docs](https://wrong.example)."}]}')

    def test_unique_one_character_json_error_is_repaired(self) -> None:
        fields = [CopyField("title", "Title", False)]
        parsed, repaired = parse_response_with_repair(
            fields, '{"translations":[{"id":"title","value":"Titre"}]',
        )
        self.assertTrue(repaired)
        self.assertEqual("Titre", parsed["title"])

    def test_unrepairable_json_reports_validation_error(self) -> None:
        fields = [CopyField("title", "Title", False)]
        with self.assertRaisesRegex(LuaTranslationError, "automatic JSON repair"):
            parse_response_with_repair(fields, '{"translations":[')

    def test_batches_preserve_order_and_limit(self) -> None:
        fields = [CopyField(str(index), "x" * 20, False) for index in range(4)]
        result = batches(fields, 150)
        self.assertEqual(["0", "1", "2", "3"], [field.id for batch in result for field in batch])
        self.assertGreater(len(result), 1)

    def test_lua_draft_is_complete_and_preserves_multiline_copy(self) -> None:
        rendered = render_table("English\ncopy", {"fr": "Copie\nfrançaise"}, ["en", "fr"], "\t")
        self.assertIn('["en"] = [[', rendered)
        self.assertIn('["fr"] = [[', rendered)
        self.assertIn("Copie\nfrançaise", rendered)

    def test_failure_artifact_uses_recovered_values_and_marks_fallbacks(self) -> None:
        fields = [CopyField("one", "One", False), CopyField("two", "Two", False)]
        artifact = render_failure(fields, {"one": "Un"}, "fr", "bad JSON")
        self.assertIn('"Un"', artifact)
        self.assertIn("FIXME", artifact)
        self.assertEqual(
            {"one": "Un"},
            recover_items('{"translations":[{"id":"one","value":"Un"}, BROKEN]}', {"one", "two"}),
        )

    def test_applies_only_function_translation_tables(self) -> None:
        source = '''return {
\ttitle = { ["en"] = "Old", ["fr"] = "Ancien" },
\tdescription = { ["en"] = [[Old description]], ["fr"] = [[Ancienne]] },
\tconfig = {{
\t\tid = "amount",
\t\tname = { ["en"] = "Amount", ["fr"] = "Montant" },
\t}},
\tgenerator = function() return { untouched = true } end,
}'''
        fields = [
            CopyField("test/title", "New", False),
            CopyField("test/description", "New description", True),
            CopyField("test/config/amount/name", "Amount", False),
        ]
        translated = {"fr": {
            "test/title": "Nouveau", "test/description": "Nouvelle description",
            "test/config/amount/name": "Montant",
        }}
        updated = apply_function_translations(source, fields, translated, ["en", "fr"])
        self.assertIn('["en"] = "New"', updated)
        self.assertIn('["fr"] = "Nouveau"', updated)
        self.assertIn("untouched = true", updated)

    def test_applies_shared_record_by_id(self) -> None:
        source = 'return {\n\t{ _id = "_one", ["en"] = "One", ["fr"] = "Un" },\n}'
        fields = [CopyField("_one", "One", False)]
        updated = apply_shared_translations(source, fields, {"fr": {"_one": "Une"}}, ["en", "fr"])
        self.assertIn('["fr"] = "Une"', updated)


if __name__ == "__main__":
    unittest.main()
