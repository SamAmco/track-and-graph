# Update Play Store copy

- Finalize the English files in `fastlane/metadata/android/en-GB/`:
   `title.txt`, `short_description.txt`, and `full_description.txt`.
- Run `make fastlane-translations-generate` to regenerate all translations.
   This makes paid API calls.
- Review English and spot-check translated claims and terminology.
- Run `make translations-test`.
- Run `make playstore-upload-copy` to publish all localized listing copy.
