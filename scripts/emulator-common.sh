#!/bin/bash

# Common emulator management functions for screenshot capture

set -e

# Configuration
HOST_ARCH=$(uname -m)
if [ "$HOST_ARCH" = "arm64" ]; then
    AVD_PACKAGE="system-images;android-35;google_apis;arm64-v8a"
else
    AVD_PACKAGE="system-images;android-35;google_apis;x86_64"
fi

EMULATOR="$ANDROID_HOME/emulator/emulator"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

# Check if any devices are already connected
check_no_devices() {
    if [ "$(adb devices | grep -w 'device' | grep -v 'List')" != "" ]; then
        echo "Error: another emulator or device is already running. Close it first."
        exit 1
    fi
}

# Ensure AVD exists
ensure_avd() {
    local avd_name="$1"
    
    if [ -z "$ANDROID_HOME" ]; then
        echo "Error: ANDROID_HOME is not set."
        exit 1
    fi
    
    if ! "$EMULATOR" -list-avds | grep -Fx "$avd_name" >/dev/null; then
        if [ -x "$AVDMANAGER" ]; then
            echo "==> Installing system image: $AVD_PACKAGE"
            yes | "$SDKMANAGER" "$AVD_PACKAGE"
            echo no | "$AVDMANAGER" create avd -n "$avd_name" -k "$AVD_PACKAGE" -d "pixel_6"
        else
            echo "Error: avdmanager missing. Install Android cmdline-tools."
            exit 1
        fi
    fi
}

# Boot and prepare device
boot_and_prep() {
    local avd_name="$1"
    
    echo "==> Booting emulator: $avd_name"
    "$EMULATOR" -avd "$avd_name" -gpu swiftshader_indirect -no-snapshot -no-boot-anim -no-audio &
    
    echo "==> Waiting for device to boot..."
    adb wait-for-device
    adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done'
    
    echo "==> Configuring device settings..."
    adb shell settings put global hidden_api_policy_pre_p_apps 1
    adb shell settings put global hidden_api_policy_p_apps 1
    adb shell settings put global window_animation_scale 0
    adb shell settings put global transition_animation_scale 0
    adb shell settings put global animator_duration_scale 0
    adb shell settings put system user_rotation 0  # portrait
    adb shell settings put secure show_ime_with_hard_keyboard 1
    adb shell cmd clipboard set text ''
}

# Set display configuration
set_display() {
    local width="$1"
    local height="$2"
    local density="$3"
    
    echo "==> Setting display to ${width}x${height} @ ${density}dpi"
    adb shell wm size "${width}x${height}"
    adb shell wm density "$density"
}

# Kill emulator
kill_emulator() {
    echo "==> Shutting down emulator..."
    adb emu kill || true
}

# Clean up screenshots directory on device
cleanup_device_screenshots() {
    local screenshot_dir="$1"
    
    echo "==> Cleaning up old screenshots: $screenshot_dir"
    # On the emulator, restart adbd as root otherwise the rm can fail
    adb root >/dev/null 2>&1 || true
    adb wait-for-device
    # Now raw deletes are reliable
    adb shell rm -rf "$screenshot_dir" || true
}

# Wait for files to be written
wait_for_files() {
    echo "==> Waiting for files to be written..."
    sleep 0.3
}
