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

echo "==> Pulling screenshots from device and processing with frameit"
mkdir -p tmp/device_screenshots
mkdir -p fastlane/frameit/screenshots/en-GB
mkdir -p fastlane/metadata/android/en-GB/images/phoneScreenshots

# Pull screenshots from device
adb pull /sdcard/Pictures/TrackAndGraphScreenshots/ tmp/device_screenshots/

# Copy raw screenshots to frameit directory with proper naming
cp tmp/device_screenshots/TrackAndGraphScreenshots/1.png fastlane/frameit/screenshots/en-GB/1.png
cp tmp/device_screenshots/TrackAndGraphScreenshots/2.png fastlane/frameit/screenshots/en-GB/2.png
cp tmp/device_screenshots/TrackAndGraphScreenshots/3.png fastlane/frameit/screenshots/en-GB/3.png
cp tmp/device_screenshots/TrackAndGraphScreenshots/4.png fastlane/frameit/screenshots/en-GB/4.png
cp tmp/device_screenshots/TrackAndGraphScreenshots/5.png fastlane/frameit/screenshots/en-GB/5.png
cp tmp/device_screenshots/TrackAndGraphScreenshots/6.png fastlane/frameit/screenshots/en-GB/6.png
cp tmp/device_screenshots/TrackAndGraphScreenshots/7.png fastlane/frameit/screenshots/en-GB/7.png
cp tmp/device_screenshots/TrackAndGraphScreenshots/8.png fastlane/frameit/screenshots/en-GB/8.png

echo "==> Running frameit to add frames and captions"

# Try to use bundler first, fallback to direct fastlane command
if command -v bundle >/dev/null 2>&1 && [ -f Gemfile ]; then
    echo "Using bundler to run frameit..."
    bundle exec fastlane run frameit path:"fastlane/frameit/screenshots" use_platform:"ANDROID"
elif command -v fastlane >/dev/null 2>&1; then
    echo "Using direct fastlane command..."
    fastlane run frameit path:"fastlane/frameit/screenshots" use_platform:"ANDROID"
else
    echo "ERROR: fastlane not found. Please install it first:"
    echo "  gem install fastlane"
    echo "  OR fix bundler setup and run: bundle install"
    exit 1
fi

echo "==> Copying framed screenshots to fastlane directory"
# Frameit typically creates files with device-specific names, let's check what was created
if ls fastlane/frameit/screenshots/en-GB/*_framed.png >/dev/null 2>&1; then
    echo "Found _framed.png files, copying them..."
    cp fastlane/frameit/screenshots/en-GB/1_framed.png fastlane/metadata/android/en-GB/images/phoneScreenshots/1_en-GB.png 2>/dev/null || echo "Warning: 1_framed.png not found"
    cp fastlane/frameit/screenshots/en-GB/2_framed.png fastlane/metadata/android/en-GB/images/phoneScreenshots/2_en-GB.png 2>/dev/null || echo "Warning: 2_framed.png not found"
    cp fastlane/frameit/screenshots/en-GB/3_framed.png fastlane/metadata/android/en-GB/images/phoneScreenshots/3_en-GB.png 2>/dev/null || echo "Warning: 3_framed.png not found"
    cp fastlane/frameit/screenshots/en-GB/4_framed.png fastlane/metadata/android/en-GB/images/phoneScreenshots/4_en-GB.png 2>/dev/null || echo "Warning: 4_framed.png not found"
    cp fastlane/frameit/screenshots/en-GB/5_framed.png fastlane/metadata/android/en-GB/images/phoneScreenshots/5_en-GB.png 2>/dev/null || echo "Warning: 5_framed.png not found"
    cp fastlane/frameit/screenshots/en-GB/6_framed.png fastlane/metadata/android/en-GB/images/phoneScreenshots/6_en-GB.png 2>/dev/null || echo "Warning: 6_framed.png not found"
    cp fastlane/frameit/screenshots/en-GB/7_framed.png fastlane/metadata/android/en-GB/images/phoneScreenshots/7_en-GB.png 2>/dev/null || echo "Warning: 7_framed.png not found"
    cp fastlane/frameit/screenshots/en-GB/8_framed.png fastlane/metadata/android/en-GB/images/phoneScreenshots/8_en-GB.png 2>/dev/null || echo "Warning: 8_framed.png not found"
else
    echo "No _framed.png files found. Checking for other frameit output patterns..."
    # List what files were actually created
    echo "Files in fastlane/frameit/screenshots/en-GB/ after frameit:"
    ls -la fastlane/frameit/screenshots/en-GB/
    
    # Try alternative naming patterns that frameit might use
    for i in {1..8}; do
        # Check for various possible output names
        if [ -f "fastlane/frameit/screenshots/en-GB/${i}.png" ] && [ "fastlane/frameit/screenshots/en-GB/${i}.png" -nt "tmp/device_screenshots/TrackAndGraphScreenshots/${i}.png" ]; then
            echo "Using updated ${i}.png (frameit processed the original)"
            cp "fastlane/frameit/screenshots/en-GB/${i}.png" "fastlane/metadata/android/en-GB/images/phoneScreenshots/${i}_en-GB.png"
        else
            echo "Warning: No framed version found for screenshot ${i}"
        fi
    done
fi

# Cleanup
kill_emulator

echo "==> Play Store screenshot recording completed successfully"

