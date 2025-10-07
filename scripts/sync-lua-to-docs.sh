#!/bin/bash

# Sync Lua scripts from lua/src/community/graphs to docs/docs/lua/community
# This allows working in the lua directory and syncing changes back to docs

set -e

SOURCE_BASE="lua/src/community/graphs"
DEST_BASE="docs/docs/lua/community"

echo "==> Syncing Lua scripts from $SOURCE_BASE to $DEST_BASE"

# Find all .lua files except test files
find "$SOURCE_BASE" -name "*.lua" ! -name "test_*.lua" -type f | while read -r script_path; do
    # Get relative path from SOURCE_BASE
    rel_path="${script_path#$SOURCE_BASE/}"

    # Extract category and script name
    # e.g., "text/average-in-duration/average-in-duration.lua" -> category="text", name="average-in-duration"
    category=$(echo "$rel_path" | cut -d'/' -f1)
    script_name=$(echo "$rel_path" | cut -d'/' -f2)

    # Create destination directory
    dest_dir="$DEST_BASE/$category/$script_name"
    mkdir -p "$dest_dir"

    # Copy to script.lua
    cp "$script_path" "$dest_dir/script.lua"
    echo "Copied: $script_path -> $dest_dir/script.lua"
done

echo "==> Removing test files from docs"
find "$DEST_BASE" -name "test.lua" -type f -delete

echo "==> Sync complete"
