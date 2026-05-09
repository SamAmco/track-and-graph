---
title: Build, test, and screenshot commands
description: Gradle commands for building and running tests; Compose screenshot-test setup for Play Store and tutorial image capture.
topics:
  - Build: cd app && ./gradlew assembleDebug
  - Test: cd app && ./gradlew :data:testDebugUnitTest
  - build-logic convention plugins tng.android.application and tng.android.library
  - Filter: --tests "fully.qualified.ClassName" to run a single test class
  - Test results: data/build/test-results/testDebugUnitTest/
  - Screenshots: make playstore-record, make tutorial-record
  - Play Store screenshots: Compose screenshot test previews, no emulator, fake status bar
  - Tutorial screenshots: Compose screenshot test previews, no emulator
keywords: [build, gradle, test, build-logic, convention-plugin, tng.android.application, tng.android.library, assembleDebug, commands, gradlew, testDebugUnitTest, screenshots, playstore, frameit, fastlane, tutorial, screenshotTest, compose-screenshot, status-bar, showSystemUi]
---

# Build Commands

All commands run from `app/` directory (`gradlew` is at `app/gradlew`, not the project root).

```bash
cd app && ./gradlew assembleDebug              # Build debug APK
cd app && ./gradlew :data:testDebugUnitTest    # Run data unit tests
```

Use `--tests` to filter by fully qualified class name:
```bash
cd app && ./gradlew :data:testDebugUnitTest --tests "com.samco.trackandgraph.data.interactor.TrackerHelperImplTest"
```

Test results XML: `data/build/test-results/testDebugUnitTest/`

## Build Logic

Shared Android build defaults live in the included build `app/build-logic`, not in `buildSrc`. It currently provides:

- `tng.android.application`
- `tng.android.library`

Use these for Android app/library modules so SDK versions, Java compatibility, Kotlin toolchain, JVM target, and common Kotlin compiler flags stay centralized. Keep module-specific behavior in the module build file: application IDs, versioning, signing, build types, Compose/Hilt/KSP/Room plugins, and dependencies.

## Play Store Screenshots

Play Store screenshots use Compose preview screenshot tests plus frameit:

```bash
make playstore-record    # Render Compose screenshot previews and process them through frameit
make tutorial-record     # Render Compose tutorial previews and resize app tutorial images
```

### Prerequisites
- Ruby + bundler + fastlane (`bundle install` from project root)

The Play Store path does not use an emulator or Shot. `make playstore-record` renders Compose previews via the `screenshotTest` source set, copies the generated reference PNGs into `fastlane/frameit/screenshots/`, then runs frameit. The screenshot content and fixtures live in `app/app/src/main/java/com/samco/trackandgraph/playstore/` so Android Studio previews can render them. Thin `@PreviewTest` wrappers live in `app/app/src/screenshotTest/kotlin/com/samco/trackandgraph/playstore/`.

Keep screenshot-only app data in the playstore package rather than reusing old emulator demo-data generators. The screenshot fixtures are deterministic and can call the real production composables directly, including graph cards and other `AndroidView`-backed content, as long as the fixture provides the state that a ViewModel would normally load from the database.

### Play Store System UI

Do not rely on `@Preview(showSystemUi = true)` for Play Store screenshot PNGs. Android Studio can show system UI for interactive previews, but the Compose screenshot-test renderer does not include the Android status bar in generated images. The Play Store frame therefore draws a small fake status bar in Compose and lays out content as if status/navigation bars exist.

Relevant implementation points:

- `PlayStorePreviewEnvironment` draws the fake status bar over the screenshot.
- App bars use an explicit status-bar height override so they draw behind the fake status bar while their content is padded down.
- Screens with top overlays/FABs pass explicit top padding instead of depending on `WindowInsets.systemBars`, which can be zero in the screenshot renderer.
- Group-screen screenshots pass explicit bottom inset padding so the track-all FAB respects the fake navigation area while scrollable content can still draw behind it.

If Play Store screenshots have a missing or misaligned status bar, adjust the playstore screenshot frame helpers/constants first. Avoid moving this behavior into frameit unless there is a strong reason; frameit only frames the already-rendered PNGs and cannot fix app content that was laid out without the expected insets.

## Tutorial Screenshots

Tutorial image capture also uses Compose screenshot tests now. The main-source tutorial screenshot content lives in `app/app/src/main/java/com/samco/trackandgraph/tutorial/TutorialScreenshotContent.kt`, with thin wrappers in `app/app/src/screenshotTest/kotlin/com/samco/trackandgraph/tutorial/`.

`scripts/tutorial-record.sh` renders the screenshot-test previews, finds `TutorialScreenshot01..03`, and resizes those 1080x2340 PNGs into all `drawable-*dpi/tutorial_image_*.png` buckets with ImageMagick. These tutorial images intentionally skip the fake Play Store status bar.
