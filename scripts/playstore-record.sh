#!/bin/bash

# Record high-res Play Store screenshots from Compose preview screenshot tests.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REFERENCE_DIR="$ROOT_DIR/app/app/src/screenshotTestPlayStoreDebug/reference"

source "$SCRIPT_DIR/lib/playstore-screenshot-languages.sh"
load_playstore_screenshot_languages

cd "$ROOT_DIR"

cleanup() {
    rm -rf "$REFERENCE_DIR"
}
trap cleanup EXIT

echo "==> Starting Play Store screenshot recording process"

# Fast-fail prerequisite checks
if ! bundle exec fastlane --version &>/dev/null; then
    echo "Error: fastlane not available via bundler. Run: bundle install"
    exit 1
fi

echo "==> Rendering Compose previews"
rm -rf "$REFERENCE_DIR"
(cd "$ROOT_DIR/app" && ./gradlew :app:updatePlayStoreDebugScreenshotTest --rerun-tasks)

# Create frameit directories for all languages
for lang in "${SCREENSHOT_PLAY_LOCALES[@]}"; do
    mkdir -p "fastlane/frameit/screenshots/$lang"
    mkdir -p "fastlane/metadata/android/$lang/images/phoneScreenshots"
done

# Copy raw screenshots to frameit directories for all languages
for index in "${!SCREENSHOT_LOCALES[@]}"; do
    locale="${SCREENSHOT_LOCALES[$index]}"
    play_locale="${SCREENSHOT_PLAY_LOCALES[$index]}"
    echo "Copying screenshots for language: $play_locale"
    for i in {1..8}; do
        screenshot_number="$(printf "%02d" "$i")"
        source_file="$(find "$REFERENCE_DIR" -type f -name "*PlayStoreScreenshot${screenshot_number}_${locale}_*.png" | sort | head -n 1)"

        if [ -z "$source_file" ]; then
            echo "ERROR: Could not find rendered screenshot $i in $REFERENCE_DIR"
            exit 1
        fi

        cp "$source_file" "fastlane/frameit/screenshots/$play_locale/$i.png"
    done
done

# Process screenshots with frameit
"$SCRIPT_DIR/frameit-process.sh"

echo "==> Play Store screenshot recording completed successfully"
