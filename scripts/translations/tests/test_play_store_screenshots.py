from __future__ import annotations

import importlib.util
import io
import json
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
from unittest.mock import patch


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

    def test_caption_font_uses_fontconfig_fallback_for_non_latin_text(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            captions = Path(temp) / "title.strings"
            captions.write_text('"1" = "日本語";\n', encoding="utf-8")
            fallback = Path(temp) / "NotoSansCJK-Bold.ttc"
            fallback.touch()
            completed = unittest.mock.Mock(stdout=f"Noto Sans CJK JP\t{fallback}\n")

            with patch.object(
                play_store_screenshots.subprocess,
                "run",
                return_value=completed,
            ) as run:
                selected = play_store_screenshots.caption_font(captions)

            self.assertEqual(fallback.resolve(), selected)
            pattern = run.call_args.args[0][-1]
            self.assertIn("65e5", pattern)
            self.assertIn("672c", pattern)

    def test_scoped_frameit_config_uses_relative_font_and_background(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            config_path = root / "Framefile.json"
            font = root / "font.ttf"
            font.touch()
            raw_dir = root / "ja-JP"
            raw_dir.mkdir()

            with (
                patch.object(play_store_screenshots, "FRAMEIT_CONFIG", config_path),
                patch.object(play_store_screenshots, "FRAMEIT_ROOT", root),
            ):
                config_path.write_text(
                    json.dumps(
                        {
                            "default": {
                                "keyword": {"font": "old"},
                                "title": {"font": "old"},
                                "background": "old",
                            }
                        }
                    ),
                    encoding="utf-8",
                )
                destination = play_store_screenshots.write_scoped_frameit_config(
                    raw_dir, font.resolve()
                )

            generated = json.loads(destination.read_text(encoding="utf-8"))
            self.assertEqual(
                "../font.ttf", generated["default"]["title"]["font"]
            )
            self.assertEqual(
                "../background.jpg",
                generated["default"]["background"],
            )

    def test_generate_localized_continues_after_a_locale_fails(self) -> None:
        targets = play_store_screenshots.SUPPORTED_TARGETS[:2]
        with (
            patch.object(play_store_screenshots, "SUPPORTED_TARGETS", targets),
            patch.object(
                play_store_screenshots,
                "snapshot_target",
                side_effect=(RuntimeError("render failed"), None),
            ) as snapshot,
            patch.object(play_store_screenshots, "frame_target") as frame,
            redirect_stdout(io.StringIO()),
            redirect_stderr(io.StringIO()),
        ):
            result = play_store_screenshots.generate_localized_screenshots()

        self.assertEqual(1, result)
        self.assertEqual(
            list(targets),
            [call.args[0] for call in snapshot.call_args_list],
        )
        frame.assert_called_once_with(targets[1])


if __name__ == "__main__":
    unittest.main()
