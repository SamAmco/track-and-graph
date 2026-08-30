---
title: Build, test, and screenshot commands
description: Gradle commands for building and running tests; Play Store vs FOSS release flavor intent, including distribution-specific support payments; Compose screenshot-test setup for Play Store and tutorial image capture.
topics:
  - Build: cd app && ./gradlew assembleDebug
  - Test: cd app && ./gradlew :data:testDebugUnitTest
  - Release flavors: playStore uses Google Play Billing support; foss uses external Buy Me a Coffee support
  - F-Droid metadata should build the foss flavor explicitly, not a flavorless release
  - Release builds should run root Gradle clean, not only :app:clean, so dependent-module generated outputs cannot leak into APK/AAB artifacts
  - Release APKs keep native debug symbols in packaged .so files to avoid NDK stripping reproducibility differences
  - build-logic convention plugins tng.android.application and tng.android.library
  - Filter: --tests "fully.qualified.ClassName" to run a single test class
  - Test results: data/build/test-results/testDebugUnitTest/
  - Screenshots: make playstore-record, make tutorial-record
  - Play Store screenshots: Compose screenshot test previews, no emulator, fake status bar
  - Tutorial screenshots: Compose screenshot test previews, no emulator
  - AGP 9.3 fixes screenshot-test manifest generation that failed under AGP 9.1
keywords: [build, gradle, test, build-logic, convention-plugin, tng.android.application, tng.android.library, assembleDebug, commands, gradlew, testDebugUnitTest, release, variant, flavor, playStore, foss, F-Droid, fdroid, donation, bmc, screenshots, playstore, frameit, fastlane, tutorial, screenshotTest, compose-screenshot, mergedManifest, host-test, status-bar, showSystemUi]
---

# Build Commands

All commands run from `app/` directory (`gradlew` is at `app/gradlew`, not the project root).

```bash
cd app && ./gradlew assembleDebug              # Build debug APK
cd app && ./gradlew :data:testDebugUnitTest    # Run data unit tests
```

Release distribution variants:

```bash
cd app && ./gradlew clean :app:bundlePlayStoreRelease    # Google Play AAB, with Play Billing support
cd app && ./gradlew clean :app:assembleFossRelease       # F-Droid/GitHub APK, keeps donation UI/resources
cd app && ./gradlew clean :app:bundleFossRelease         # F-Droid/GitHub AAB if needed
```

## Release Distribution Flavors

The app has a `distribution` flavor dimension with `playStore` and `foss` flavors. The split exists for store-policy compliance: Play Store builds use Google Play Billing for voluntary support and must not compile in external payment links or Buy Me a Coffee assets. F-Droid/GitHub builds retain the external Buy Me a Coffee flow and do not include the Play Billing dependency or permission. See `play-billing.md` for the Play-specific lifecycle and Console setup.

Keep distribution-specific UI behind flavor source-set hooks in the app module. Shared UI such as the changelog/release-notes dialog should stay reusable and parameterized; the app flavor decides whether to provide support content and whether the dialog is dismissible by outside click/back press.

Buy Me a Coffee drawables belong in the app's `foss` resources, never `ui/main`: every app flavor depends on the shared UI module. The standalone changelog viewer may explicitly include the FOSS resource directory to preview that action without making the asset reachable from Play builds.

F-Droid's metadata should request the FOSS flavor explicitly for future releases:

```yaml
gradle:
  - foss
```

Do not rely on a generic/flavorless release build for F-Droid. Google Play release artifacts should continue to use the explicit Play Store tasks.

Release Make/Fastlane targets intentionally run root `clean` rather than `:app:clean`. The app depends on generated and compiled outputs from other modules, including `:data` and `:ui`; a root clean avoids stale module artifacts causing non-reproducible APK bytecode.

Release builds also keep native debug symbols in packaged JNI libraries via `packaging { jniLibs.keepDebugSymbols.add("**/*.so") }`. This trades a small APK size increase for avoiding differences caused by local NDK stripping behavior in reproducible-build verification.

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

Use AGP 9.3.1 or newer with Gradle 9.5.0 or newer for screenshot tests. AGP 9.1.1 created `GenerateTestConfig` without configuring its required merged-manifest input unless Android resources were manually enabled through the incubating host-test API. AGP 9.3.1 generates and processes the screenshot-test manifest correctly without that workaround. When changing this setup, verify both `generateFossDebugScreenshotTestConfig` and `generatePlayStoreDebugScreenshotTestConfig` because the tasks are flavor-specific.

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

`scripts/tutorial-record.sh` renders the screenshot-test previews, finds `TutorialScreenshot01..03`, and resizes those 1080x1920 PNGs into all `drawable-*dpi/tutorial_image_*.png` buckets with ImageMagick. These tutorial images intentionally skip the fake Play Store status bar.
