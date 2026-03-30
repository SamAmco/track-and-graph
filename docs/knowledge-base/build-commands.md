---
title: Build, test, and screenshot commands
description: Gradle commands for building and running tests; screenshot test setup (promo build type, prerequisites, Compose test timing pitfalls with AndroidView content); TestDataInteractor raw SQL must match current groups_table schema.
topics:
  - Build: cd app && ./gradlew assembleDebug
  - Test: cd app && ./gradlew :data:testDebugUnitTest
  - Filter: --tests "fully.qualified.ClassName" to run a single test class
  - Test results: data/build/test-results/testDebugUnitTest/
  - Screenshots: make playstore-record, make snapshots-record, make tutorial-record
  - Promo build type: -PusePromoTests=true switches testBuildType to "promo"
  - Compose test timing: Thread.sleep blocks UI; use waitUntil with conditions instead
keywords: [build, gradle, test, assembleDebug, commands, gradlew, testDebugUnitTest, screenshots, promo, playstore, frameit, fastlane, waitUntil, AndroidView, TestDataInteractor, groups_table]
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

## Screenshot Tests

Play Store screenshots and snapshot tests use Makefile targets that delegate to scripts in `scripts/`:

```bash
make playstore-record    # High-res Play Store screenshots (scripts/playstore-record.sh)
make snapshots-record    # Low-res snapshot baselines (scripts/snapshots-record.sh)
make snapshots-verify    # Verify snapshots against baselines
make tutorial-record     # Tutorial images for in-app use
```

### Prerequisites
- Android cmdline-tools installed (`avdmanager`, `sdkmanager`)
- Ruby + bundler + fastlane (`bundle install` from project root)
- No other emulators/devices running

### Promo Build Type
The `promo` build type (`app/app/build.gradle.kts`) is used for Play Store screenshot tests. AGP only generates `connectedXxxAndroidTest` tasks for the configured `testBuildType` (default: `debug`). The `-PusePromoTests=true` Gradle property switches `testBuildType` to `"promo"`, enabling `connectedPromoAndroidTest`. This flag is passed automatically by `scripts/playstore-record.sh`.

### Compose Test Timing with AndroidView Content
Graphs use `AndroidViewBinding` (MPAndroidChart) which renders outside Compose's layout system. Key pitfalls:
- **`Thread.sleep` blocks UI rendering**: Compose test framework synchronizes the test thread with the main thread, so `Thread.sleep` prevents AndroidView content from rendering during the wait.
- **`waitForIdle` returns too early**: Compose considers itself idle before AndroidView content finishes rendering.
- **`waitUntilDoesNotExist` race condition**: If checked before loading indicators are composed (e.g. in a lazy list), it passes immediately because the nodes don't exist yet.
- **Working pattern**: Use `composeRule.waitUntil(timeoutMillis) { condition }` which polls with yields between checks, allowing the UI thread to render. Combine positive checks (e.g. expected card count) with negative checks (no loading indicators) for reliability.

### Test Demo Data
Demo data for screenshots is in `data/src/testFixtures/` (shared between promo and snapshot tests). `TestDataInteractor` creates the Room database with a raw SQL insert for the root group — this must match the current `groups_table` schema (currently: `id`, `name`, `color_index` only — no `display_index` or `parent_group_id`, which moved to `group_items_table`).

**Display order pitfall**: `insertGroup` always inserts at display_index 0, shifting existing items down. Sequential inserts end up in reverse order. If tests navigate by index (e.g. `onAllNodes(hasTestTag("groupCard"))[1]`), call `updateGroupChildOrder` after creation to set explicit ordering — see `PlaystoreScreenshotsDemoData.kt` and `FirstOpenTutorialDemoData.kt` for examples.
