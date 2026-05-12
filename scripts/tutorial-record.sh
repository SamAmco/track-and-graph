#!/bin/bash

# Record tutorial images from Compose preview screenshot tests.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REFERENCE_DIR="$ROOT_DIR/app/app/src/screenshotTestDebug/reference"

cd "$ROOT_DIR"

cleanup() {
    rm -rf "$REFERENCE_DIR"
}
trap cleanup EXIT

echo "==> Starting tutorial screenshot recording process"

if ! command -v magick &>/dev/null; then
    echo "Error: ImageMagick 'magick' command not found"
    exit 1
fi

echo "==> Rendering Compose tutorial previews"
rm -rf "$REFERENCE_DIR"
(cd "$ROOT_DIR/app" && ./gradlew :app:updateDebugScreenshotTest --rerun-tasks)

echo "==> Converting rendered tutorial screenshots to density buckets"
mkdir -p app/app/src/main/res/drawable-mdpi
mkdir -p app/app/src/main/res/drawable-hdpi
mkdir -p app/app/src/main/res/drawable-xhdpi
mkdir -p app/app/src/main/res/drawable-xxhdpi
mkdir -p app/app/src/main/res/drawable-xxxhdpi

# Function to process a single tutorial image
process_tutorial_image() {
    local image_num="$1"
    local screenshot_number
    screenshot_number="$(printf "%02d" "$image_num")"
    local source_path
    source_path="$(find "$REFERENCE_DIR" -type f -name "*TutorialScreenshot${screenshot_number}_*.png" | sort | head -n 1)"

    if [ -z "$source_path" ]; then
        echo "ERROR: Could not find rendered tutorial screenshot $image_num in $REFERENCE_DIR"
        exit 1
    fi
    
    echo "Processing tutorial_${image_num}.png..."
    
    # Source: 1080x1920 Compose preview - optimized for API 24 memory constraints
    # mdpi (25% of source): ~270x480 pixels, 0.5MB memory - safe for low-end devices
    magick "$source_path" -resize 25% "app/app/src/main/res/drawable-mdpi/tutorial_image_${image_num}.png"
    # hdpi (35% of source): ~378x672 pixels, 1.0MB memory - balanced size/quality
    magick "$source_path" -resize 35% "app/app/src/main/res/drawable-hdpi/tutorial_image_${image_num}.png"
    # xhdpi (50% of source): ~540x960 pixels, 2.0MB memory - good quality, API 24 safe
    magick "$source_path" -resize 50% "app/app/src/main/res/drawable-xhdpi/tutorial_image_${image_num}.png"
    # xxhdpi (75% of source): ~810x1440 pixels, 4.7MB memory - high-end devices
    magick "$source_path" -resize 75% "app/app/src/main/res/drawable-xxhdpi/tutorial_image_${image_num}.png"
    # xxxhdpi (100% of source): 1080x1920 pixels, 7.9MB memory - flagship devices with lots of RAM
    cp "$source_path" "app/app/src/main/res/drawable-xxxhdpi/tutorial_image_${image_num}.png"
}

# Process all tutorial images
process_tutorial_image 1
process_tutorial_image 2
process_tutorial_image 3

echo "==> Tutorial images generated successfully"

echo "==> Tutorial screenshot recording completed successfully"
