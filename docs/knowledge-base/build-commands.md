---
title: Build and test commands
description: Gradle commands for building and running tests; all run from the app/ directory. Includes test result locations and --tests filtering.
topics:
  - Build: cd app && ./gradlew assembleDebug
  - Test: cd app && ./gradlew :data:testDebugUnitTest
  - Filter: --tests "fully.qualified.ClassName" to run a single test class
  - Test results: data/build/test-results/testDebugUnitTest/
keywords: [build, gradle, test, assembleDebug, commands, gradlew, testDebugUnitTest]
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
