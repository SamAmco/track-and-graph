#!/bin/bash

# Record tutorial images for app with ImageMagick processing

set -e

# Source common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/emulator-common.sh"

AVD_NAME="shot-api35-hi"

echo "==> Starting tutorial screenshot recording process"

# Check prerequisites and setup
check_no_devices
ensure_avd "$AVD_NAME"
boot_and_prep "$AVD_NAME"

# Set high-res display for tutorial capture (1080x2340 @ 420dpi)
set_display 1080 2340 420

# Clean up any old tutorial screenshots
cleanup_device_screenshots "/sdcard/Pictures/TutorialScreenshots/"

echo "==> Running tutorial captures"
./gradlew :app:connectedPromoAndroidTest -PusePromoTests=true -Pandroid.testInstrumentationRunnerArguments.class=com.samco.trackandgraph.tutorial.TutorialScreenshots

# Wait for files to be written
wait_for_files

echo "==> Pulling tutorial screenshots from device and processing with ImageMagick"
rm -rf tmp/tutorial_screenshots
mkdir -p tmp/tutorial_screenshots
mkdir -p app/src/main/res/drawable-mdpi
mkdir -p app/src/main/res/drawable-hdpi
mkdir -p app/src/main/res/drawable-xhdpi
mkdir -p app/src/main/res/drawable-xxhdpi
mkdir -p app/src/main/res/drawable-xxxhdpi

# Pull screenshots from device
adb pull /sdcard/Pictures/TutorialScreenshots tmp/tutorial_screenshots/

echo "==> Converting screenshots to optimal sizes for each density bucket"

# Function to process a single tutorial image
process_tutorial_image() {
    local image_num="$1"
    local source_path="tmp/tutorial_screenshots/TutorialScreenshots/tutorial_${image_num}.png"
    
    echo "Processing tutorial_${image_num}.png..."
    
    # Source: 1080x2340 emulator capture - optimized for API 24 memory constraints
    # mdpi (25% of emulator): ~270x585 pixels, 0.6MB memory - safe for low-end devices
    magick "$source_path" -resize 25% "app/src/main/res/drawable-mdpi/tutorial_image_${image_num}.png"
    # hdpi (35% of emulator): ~378x819 pixels, 1.2MB memory - balanced size/quality
    magick "$source_path" -resize 35% "app/src/main/res/drawable-hdpi/tutorial_image_${image_num}.png"
    # xhdpi (50% of emulator): ~540x1170 pixels, 2.5MB memory - good quality, API 24 safe
    magick "$source_path" -resize 50% "app/src/main/res/drawable-xhdpi/tutorial_image_${image_num}.png"
    # xxhdpi (75% of emulator): ~810x1755 pixels, 5.7MB memory - high-end devices
    magick "$source_path" -resize 75% "app/src/main/res/drawable-xxhdpi/tutorial_image_${image_num}.png"
    # xxxhdpi (100% of emulator): 1080x2340 pixels, 10.1MB memory - flagship devices with lots of RAM
    cp "$source_path" "app/src/main/res/drawable-xxxhdpi/tutorial_image_${image_num}.png"
}

# Process all tutorial images
process_tutorial_image 1
process_tutorial_image 2
process_tutorial_image 3

echo "==> Tutorial images generated successfully"

# Cleanup
kill_emulator

echo "==> Tutorial screenshot recording completed successfully"
