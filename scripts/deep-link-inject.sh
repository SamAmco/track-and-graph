#!/bin/bash

# Inject deep link for Lua file via ADB

set -e

# Check if FILE parameter is provided
if [ -z "$1" ]; then
    echo "Error: FILE parameter is required"
    echo "Usage: $0 <file_path>"
    exit 1
fi

FILE="$1"

echo "Injecting deep link for file: $FILE"

# Copy the file over adb to the devices tmp directory
adb push "$FILE" /data/local/tmp/tmp.lua

# Launch the app with the deep link intent
adb shell am start -a android.intent.action.VIEW -d "trackandgraph://lua_inject_file?path=/data/local/tmp/tmp.lua"

echo "Deep link injection completed successfully"
