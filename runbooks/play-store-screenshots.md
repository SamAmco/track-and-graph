# Update Play Store screenshots

- Finalize the screenshot fixtures and English captions in
   `fastlane/frameit/screenshots/en-GB/title.strings`.
- If captions changed, run `make frameit-translations-generate`. If visible app
   strings changed, run `make translations-generate`. Both make paid API calls.
- Run `make translations-test translations-validate`.
- Generate and review English before processing the full suite:
   - Raw: `make playstore-screenshots-english`
   - Framed: `make playstore-screenshots-english-framed`
- Publish the release bundle to the intended Play track before uploading any
  screenshots. Fastlane requires the current version code to exist on that
  track; screenshot uploads default to the production track and current version.
- Upload the reviewed English screenshots:
   - Upload: `make playstore-screenshots-upload LANGUAGE=en`
- For each remaining locale, run and review each independently retryable stage:
   - `make playstore-screenshots-snapshot LANGUAGE=de`
   - `make playstore-screenshots-frame LANGUAGE=de`
   - `make playstore-screenshots-upload LANGUAGE=de`

After generating several locales, omit `LANGUAGE` from the upload command to
upload every complete set sequentially.
