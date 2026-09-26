# Publish a release

- Run `make validate-all`.
- Update `versionCode` and `versionName` in `app/app/build.gradle.kts`.
- Run `make changelog` and review the release notes.
- Run `make commit-version`.
- Run `make assemble-bundle-release`.
- Install and sanity-check the Play Store APK:
   `adb install app/app/build/outputs/apk/playStore/release/app-playStore-release.apk`
- Upload the AAB and release notes to exactly one track:
   - Alpha: `make playstore-upload-alpha`
   - Beta: `make playstore-upload-beta`
   - Production: `make playstore-upload-production ROLLOUT=0.5` (use `1` for a
     complete rollout)
- Run `make assemble-foss-release` and optionally install the FOSS APK:
   `adb install app/app/build/outputs/apk/foss/release/app-foss-release.apk`
- Run `make github-release`.

Store copy and screenshots are not changed by the release upload targets; use
their separate runbooks when needed.
