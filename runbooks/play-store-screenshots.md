# Update Play Store screenshots

- Finalize the screenshot fixtures and English captions in `fastlane/frameit/screenshots/en-GB/title.strings`.
- Render English with `make playstore-screenshots-english`, review the raw images in `fastlane/frameit/screenshots/en-GB/`
- Frame them with `make playstore-screenshots-frame LANGUAGE=en`. review in `fastlane/metadata/android/en-GB/images/phoneScreenshots/`. 
- If captions changed, run `make frameit-translations-generate`. (paid API calls)
- If visible app strings changed, run `make translations-generate`. (paid API calls)
- Run `make translations-test translations-validate`.
- Generate and frame every remaining locale sequentially with `make playstore-screenshots-generate`. 
- If needed retry broken locales with `make playstore-screenshots-snapshot LANGUAGE=<locale>` and `make playstore-screenshots-frame LANGUAGE=<locale>`.
- Get an agent review: Please review the latest screenshots at `fastlane/metadata/android/*/images/phoneScreenshots/` for anything that looks incorrect or broken.
- Upload every complete locale in retryable ten-locale batches with `make playstore-screenshots-upload`. Fastlane skips screenshots whose remote checksums already match.
- If a batch fails, use the printed `make playstore-screenshots-upload FROM_LANGUAGE=<locale>` command to resume at that batch without revisiting committed batches. Language order is as listed in `configuration/translation-languages.tsv``
