# Build Commands

All commands run from `app/` directory (`gradlew` is at `app/gradlew`, not the project root).

```bash
cd app && ./gradlew assembleDebug              # Build debug APK
cd app && ./gradlew :data:testDebugUnitTest    # Run data unit tests
```

`--tests` filter flag is NOT supported. Run the full task and inspect XML results:
`data/build/test-results/testDebugUnitTest/`
