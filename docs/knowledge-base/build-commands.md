---
title: Build and test commands
description: Gradle commands for building and running tests; all run from the app/ directory. Includes test result locations.
topics:
  - Build: cd app && ./gradlew assembleDebug
  - Test: cd app && ./gradlew :data:testDebugUnitTest
  - Test results: data/build/test-results/testDebugUnitTest/
  - Note: --tests filter flag is NOT supported; run full task and inspect XML results
keywords: [build, gradle, test, assembleDebug, commands, gradlew, testDebugUnitTest]
---

# Build Commands

All commands run from `app/` directory (`gradlew` is at `app/gradlew`, not the project root).

```bash
cd app && ./gradlew assembleDebug              # Build debug APK
cd app && ./gradlew :data:testDebugUnitTest    # Run data unit tests
```

`--tests` filter flag is NOT supported. Run the full task and inspect XML results:
`data/build/test-results/testDebugUnitTest/`
