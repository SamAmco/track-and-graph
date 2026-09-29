#!/usr/bin/env python3

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPTS_DIR = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(SCRIPTS_DIR))

from in_app_changelog_index import update_version, validation_issues


class InAppChangelogIndexTest(unittest.TestCase):
    def make_repo(self, root: Path) -> tuple[Path, Path]:
        changelogs = root / "changelogs"
        changelogs.mkdir()
        index = changelogs / "index.json"
        index.write_text('{"changelogs": {}}\n', encoding="utf-8")
        return changelogs, index

    def test_update_discovers_files_and_uses_stable_order(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            changelogs, index = self.make_repo(Path(temp))
            for version in ("2.0.0", "10.0.0"):
                directory = changelogs / version
                directory.mkdir()
                (directory / "fr.md").write_text("French", encoding="utf-8")
                (directory / "en.md").write_text("English", encoding="utf-8")
                update_version(
                    version,
                    changelog_root=changelogs,
                    index_path=index,
                    locales=("en", "de", "fr"),
                )

            value = json.loads(index.read_text(encoding="utf-8"))
            self.assertEqual(["10.0.0", "2.0.0"], list(value["changelogs"]))
            self.assertEqual(
                {"en": "10.0.0/en.md", "fr": "10.0.0/fr.md"},
                value["changelogs"]["10.0.0"],
            )

    def test_validation_reports_unindexed_and_missing_files(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            changelogs, index = self.make_repo(Path(temp))
            directory = changelogs / "1.0.0"
            directory.mkdir()
            (directory / "en.md").write_text("English", encoding="utf-8")
            (directory / "fr.md").write_text("French", encoding="utf-8")
            index.write_text(
                json.dumps(
                    {
                        "changelogs": {
                            "1.0.0": {
                                "en": "1.0.0/en.md",
                                "de": "1.0.0/de.md",
                            }
                        }
                    }
                ),
                encoding="utf-8",
            )

            issues = validation_issues(
                changelog_root=changelogs,
                index_path=index,
                locales=("en", "de", "fr"),
            )

        self.assertTrue(any("missing" in issue and "de.md" in issue for issue in issues))
        self.assertTrue(any("missing from index: fr" in issue for issue in issues))

    def test_validation_reports_unindexed_english_version(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            changelogs, index = self.make_repo(Path(temp))
            directory = changelogs / "3.0.0"
            directory.mkdir()
            (directory / "en.md").write_text("English", encoding="utf-8")

            issues = validation_issues(
                changelog_root=changelogs,
                index_path=index,
                locales=("en", "de"),
            )

        self.assertIn(
            "3.0.0: English changelog exists but version is missing from index", issues
        )


if __name__ == "__main__":
    unittest.main()
