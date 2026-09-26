from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path


TRANSLATIONS_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TRANSLATIONS_DIR))

from languages import ALL_LANGUAGES, play_locale  # noqa: E402
from translate_frameit_captions import (  # noqa: E402
    CAPTION_KEYS,
    DEFAULT_FRAMEIT_ROOT,
    SOURCE_LOCALE,
    parse_response,
    parse_title_strings,
    render_title_strings,
    validate_translation,
)


class FrameitCaptionsTest(unittest.TestCase):
    def test_repository_captions_cover_language_manifest(self) -> None:
        source = parse_title_strings(
            (DEFAULT_FRAMEIT_ROOT / SOURCE_LOCALE / "title.strings").read_text(
                encoding="utf-8"
            )
        )
        for target in ALL_LANGUAGES:
            path = DEFAULT_FRAMEIT_ROOT / play_locale(target.locale) / "title.strings"
            translated = parse_title_strings(path.read_text(encoding="utf-8"))
            self.assertEqual((), validate_translation(source, translated), target.locale)

    def test_title_strings_round_trip(self) -> None:
        source = parse_title_strings(
            (DEFAULT_FRAMEIT_ROOT / SOURCE_LOCALE / "title.strings").read_text(
                encoding="utf-8"
            )
        )
        self.assertEqual(CAPTION_KEYS, tuple(source))
        self.assertEqual(source, parse_title_strings(render_title_strings(source)))

    def test_response_requires_exact_non_empty_keys(self) -> None:
        valid = {key: f"Caption {key}" for key in CAPTION_KEYS}
        self.assertEqual(valid, parse_response(json.dumps(valid)))
        with self.assertRaisesRegex(ValueError, "exactly keys"):
            parse_response(json.dumps({**valid, "9": "Extra"}))

    def test_validation_preserves_lua(self) -> None:
        source = {key: f"Caption {key}" for key in CAPTION_KEYS}
        source["8"] = "Visual logic & Lua scripting"
        translated = dict(source)
        translated["8"] = "Visual logic and scripting"
        self.assertEqual(
            ("caption 8 changed the number of 'Lua' occurrences",),
            validate_translation(source, translated),
        )


if __name__ == "__main__":
    unittest.main()
