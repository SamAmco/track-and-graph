#!/bin/bash

# Record high-res Play Store screenshots from Compose preview screenshot tests.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REFERENCE_DIR="$ROOT_DIR/app/app/src/screenshotTestDebug/reference"

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
(cd "$ROOT_DIR/app" && ./gradlew :app:updateDebugScreenshotTest --rerun-tasks)

# Define supported languages
LANGUAGES=("en-GB" "es-ES" "de-DE" "fr-FR")

# Create frameit directories for all languages
for lang in "${LANGUAGES[@]}"; do
    mkdir -p "fastlane/frameit/screenshots/$lang"
    mkdir -p "fastlane/metadata/android/$lang/images/phoneScreenshots"
done

# Copy raw screenshots to frameit directories for all languages
for lang in "${LANGUAGES[@]}"; do
    echo "Copying screenshots for language: $lang"
    for i in {1..8}; do
        screenshot_number="$(printf "%02d" "$i")"
        source_file="$(find "$REFERENCE_DIR" -type f -name "*PlayStoreScreenshot${screenshot_number}_*.png" | sort | head -n 1)"

        if [ -z "$source_file" ]; then
            echo "ERROR: Could not find rendered screenshot $i in $REFERENCE_DIR"
            exit 1
        fi

        cp "$source_file" "fastlane/frameit/screenshots/$lang/$i.png"
    done
done

# Process screenshots with frameit
"$SCRIPT_DIR/frameit-process.sh"

echo "==> Play Store screenshot recording completed successfully"
