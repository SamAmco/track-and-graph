#!/bin/bash

# Record high-res Play Store screenshots

set -e

# Source common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/emulator-common.sh"

AVD_NAME="shot-api35-hi"

echo "==> Starting Play Store screenshot recording process"

# Check prerequisites and setup
check_no_devices
ensure_avd "$AVD_NAME"
boot_and_prep "$AVD_NAME"

# Set high-res display for Play Store (1080x2340 @ 420dpi)
set_display 1080 2340 420

# Clean up any old screenshots
cleanup_device_screenshots "/sdcard/Pictures/TrackAndGraphScreenshots/"

echo "==> Running promo captures"
(cd app && ./gradlew :app:connectedPromoAndroidTest -PusePromoTests=true -Pandroid.testInstrumentationRunnerArguments.class=com.samco.trackandgraph.promo.PromoScreenshots)

# Wait for files to be written
wait_for_files

echo "==> Pulling screenshots from device and processing with frameit"
mkdir -p tmp/device_screenshots

# Define supported languages
LANGUAGES=("en-GB" "es-ES" "de-DE" "fr-FR")

# Create frameit directories for all languages
for lang in "${LANGUAGES[@]}"; do
    mkdir -p "fastlane/frameit/screenshots/$lang"
    mkdir -p "fastlane/metadata/android/$lang/images/phoneScreenshots"
done

# Pull screenshots from device
adb pull /sdcard/Pictures/TrackAndGraphScreenshots/ tmp/device_screenshots/

# Cleanup
kill_emulator

# Copy raw screenshots to frameit directories for all languages
for lang in "${LANGUAGES[@]}"; do
    echo "Copying screenshots for language: $lang"
    for i in {1..8}; do
        cp "tmp/device_screenshots/TrackAndGraphScreenshots/$i.png" "fastlane/frameit/screenshots/$lang/$i.png"
    done
done

# Process screenshots with frameit
"$SCRIPT_DIR/frameit-process.sh"

echo "==> Play Store screenshot recording completed successfully"