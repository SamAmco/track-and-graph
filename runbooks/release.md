# Publish a release

- Run `make validate-all`.
- Update `versionCode` and `versionName` in `app/app/build.gradle.kts`.
- Run `make release-notes` to draft and translate the concise Play Store/GitHub
  release notes. This makes paid API calls.
- Review every locale reported as invalid and edit the written Fastlane `.txt`
  file directly; over-limit output is deliberately retained for repair.
- Run `make release-notes-validate` until every locale is present, structurally
  valid, and within the 500-character Play limit. This step is offline.
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

Long in-app changelogs are independent of this release checklist. Publish one
later using the in-app changelog runbook when the release warrants it.
