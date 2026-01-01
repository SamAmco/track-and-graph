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
./gradlew :app:connectedPromoAndroidTest -PusePromoTests=true -Pandroid.testInstrumentationRunnerArguments.class=com.samco.trackandgraph.promo.PromoScreenshots

# Wait for files to be written
wait_for_files

echo "==> Pulling screenshots from device and copying to Fastlane phoneScreenshots"
mkdir -p tmp/device_screenshots
mkdir -p fastlane/metadata/android/en-GB/images/phoneScreenshots

# Pull screenshots from device
adb pull /sdcard/Pictures/TrackAndGraphScreenshots/ tmp/device_screenshots/

# Rename and copy to fastlane directory with language suffix
mv tmp/device_screenshots/TrackAndGraphScreenshots/1.png fastlane/metadata/android/en-GB/images/phoneScreenshots/1_en-GB.png
mv tmp/device_screenshots/TrackAndGraphScreenshots/2.png fastlane/metadata/android/en-GB/images/phoneScreenshots/2_en-GB.png
mv tmp/device_screenshots/TrackAndGraphScreenshots/3.png fastlane/metadata/android/en-GB/images/phoneScreenshots/3_en-GB.png
mv tmp/device_screenshots/TrackAndGraphScreenshots/4.png fastlane/metadata/android/en-GB/images/phoneScreenshots/4_en-GB.png
mv tmp/device_screenshots/TrackAndGraphScreenshots/5.png fastlane/metadata/android/en-GB/images/phoneScreenshots/5_en-GB.png
mv tmp/device_screenshots/TrackAndGraphScreenshots/6.png fastlane/metadata/android/en-GB/images/phoneScreenshots/6_en-GB.png
mv tmp/device_screenshots/TrackAndGraphScreenshots/7.png fastlane/metadata/android/en-GB/images/phoneScreenshots/7_en-GB.png
mv tmp/device_screenshots/TrackAndGraphScreenshots/8.png fastlane/metadata/android/en-GB/images/phoneScreenshots/8_en-GB.png

# Cleanup
kill_emulator

echo "==> Play Store screenshot recording completed successfully"
