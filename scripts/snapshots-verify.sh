#!/bin/bash

# Verify low-res snapshots

set -e

# Source common functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/emulator-common.sh"

AVD_NAME="shot-api35-low"

echo "==> Starting snapshot verification process"

# Check prerequisites and setup
check_no_devices
ensure_avd "$AVD_NAME"
boot_and_prep "$AVD_NAME"

# Set low-res display (tuned settings: 548x1280 @ 210dpi)
set_display 548 1280 210

echo "==> Verifying snapshots"
./gradlew :app:screenshotsExecuteScreenshotTests -PdirectorySuffix=api35-low

# Cleanup
kill_emulator

echo "==> Snapshot verification completed successfully"
