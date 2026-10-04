from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[2] / "upload_play_store_screenshots.py"
SPEC = importlib.util.spec_from_file_location("upload_play_store_screenshots", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
upload = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = upload
SPEC.loader.exec_module(upload)


class PlayStoreScreenshotUploadTest(unittest.TestCase):
    def create_complete_set(self, root: Path, locale: str) -> tuple[Path, ...]:
        target = upload.target_for_locale(locale)
        images = upload.expected_images(root, target)
        for image in images:
            image.parent.mkdir(parents=True, exist_ok=True)
            image.touch()
        return images

    def test_accepts_canonical_and_play_locale(self) -> None:
        self.assertEqual("de", upload.target_for_locale("de").locale)
        self.assertEqual("de", upload.target_for_locale("de-DE").locale)
        self.assertEqual("zh-Hans", upload.target_for_locale("zh-CN").locale)

    def test_discovers_only_complete_sets_in_manifest_order(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.create_complete_set(root, "de")
            self.create_complete_set(root, "en")

            selected = upload.select_screenshot_sets(root, None)

            self.assertEqual(["en", "de"], [item.target.locale for item in selected])

    def test_resumes_from_requested_locale_in_manifest_order(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.create_complete_set(root, "en")
            self.create_complete_set(root, "af")
            self.create_complete_set(root, "sq")

            selected = upload.select_screenshot_sets(root, None, "af")

            self.assertEqual(
                ["af", "sq"], [item.target.locale for item in selected]
            )

    def test_rejects_a_partial_set_instead_of_silently_skipping_it(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            images = upload.expected_images(root, upload.target_for_locale("de"))
            images[0].parent.mkdir(parents=True)
            images[0].touch()

            with self.assertRaisesRegex(FileNotFoundError, "Partial screenshot set"):
                upload.select_screenshot_sets(root, None)

    def test_batches_preserve_order_and_limit(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            selected = tuple(
                upload.ScreenshotSet(
                    upload.target_for_locale(locale),
                    self.create_complete_set(root, locale),
                )
                for locale in ("en", "af", "sq")
            )

            batches = upload.screenshot_batches(selected, 2)

            self.assertEqual(
                [["en", "af"], ["sq"]],
                [[item.target.locale for item in batch] for batch in batches],
            )

    def test_staging_view_contains_only_batch_locales_and_uses_symlinks(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            screenshots = tuple(
                upload.ScreenshotSet(
                    upload.target_for_locale(locale),
                    self.create_complete_set(root / "metadata", locale),
                )
                for locale in ("de", "fr")
            )

            scoped = upload.stage_screenshot_batch(screenshots, root / "staging")

            staged = sorted(scoped.rglob("*.png"))
            self.assertEqual(upload.SCREENSHOT_COUNT * 2, len(staged))
            self.assertTrue(all(path.is_symlink() for path in staged))
            self.assertEqual(
                {"de-DE", "fr-FR"},
                {path.relative_to(scoped).parts[0] for path in staged},
            )

    def test_supply_command_uploads_only_screenshots(self) -> None:
        command = upload.supply_command(Path("/tmp/scoped-metadata"))
        for flag in (
            "--skip_upload_apk",
            "--skip_upload_aab",
            "--skip_upload_metadata",
            "--skip_upload_changelogs",
            "--skip_upload_images",
        ):
            self.assertIn(flag, command)
        self.assertNotIn("--skip_upload_screenshots", command)
        self.assertNotIn("--track", command)
        self.assertNotIn("--version_code", command)


if __name__ == "__main__":
    unittest.main()
