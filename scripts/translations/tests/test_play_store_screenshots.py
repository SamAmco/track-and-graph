from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[2] / "play_store_screenshots.py"
SPEC = importlib.util.spec_from_file_location("play_store_screenshots", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
play_store_screenshots = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(play_store_screenshots)


class PlayStoreScreenshotsTest(unittest.TestCase):
    def test_accepts_canonical_and_play_locale(self) -> None:
        self.assertEqual("de", play_store_screenshots.target_for_locale("de").locale)
        self.assertEqual("de", play_store_screenshots.target_for_locale("de-DE").locale)
        self.assertEqual(
            "zh-Hans", play_store_screenshots.target_for_locale("zh-CN").locale
        )

    def test_finds_one_render_for_each_screenshot_number(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for number in range(1, play_store_screenshots.SCREENSHOT_COUNT + 1):
                (root / f"PlayStoreScreenshot{number:02d}_en_test.png").touch()

            paths = play_store_screenshots.find_rendered_screenshots(root, "en")

            self.assertEqual(play_store_screenshots.SCREENSHOT_COUNT, len(paths))
            self.assertEqual("PlayStoreScreenshot01_en_test.png", paths[0].name)

    def test_rejects_ambiguous_render_output(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "PlayStoreScreenshot01_en_first.png").touch()
            (root / "PlayStoreScreenshot01_en_second.png").touch()

            with self.assertRaisesRegex(FileNotFoundError, "found 2"):
                play_store_screenshots.find_rendered_screenshots(root, "en")

    def test_frame_stage_requires_a_complete_existing_snapshot(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            locale_dir = root / "de-DE"
            locale_dir.mkdir()
            for number in range(1, play_store_screenshots.SCREENSHOT_COUNT):
                (locale_dir / f"{number}.png").touch()

            with self.assertRaisesRegex(
                FileNotFoundError, "run the snapshot stage first"
            ):
                play_store_screenshots.raw_screenshots("de-DE", root)

            (locale_dir / f"{play_store_screenshots.SCREENSHOT_COUNT}.png").touch()
            self.assertEqual(
                play_store_screenshots.SCREENSHOT_COUNT,
                len(play_store_screenshots.raw_screenshots("de-DE", root)),
            )


if __name__ == "__main__":
    unittest.main()
