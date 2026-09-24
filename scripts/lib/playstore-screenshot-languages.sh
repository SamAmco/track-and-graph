#!/bin/bash

# Populates SCREENSHOT_LOCALES and SCREENSHOT_PLAY_LOCALES from the shared
# screenshot-language selection. The caller must define ROOT_DIR.
load_playstore_screenshot_languages() {
    local config_file="$ROOT_DIR/configuration/play-store-screenshot-languages.txt"
    local locale
    local play_locale

    SCREENSHOT_LOCALES=()
    SCREENSHOT_PLAY_LOCALES=()
    while IFS=$'\t' read -r locale play_locale; do
        if [ -z "$locale" ] || [[ "$locale" == \#* ]]; then
            continue
        fi
        if [ -z "$play_locale" ]; then
            echo "ERROR: Missing Google Play locale for $locale in $config_file" >&2
            return 1
        fi
        SCREENSHOT_LOCALES+=("$locale")
        SCREENSHOT_PLAY_LOCALES+=("$play_locale")
    done < "$config_file"

    if [ "${#SCREENSHOT_LOCALES[@]}" -eq 0 ]; then
        echo "ERROR: No screenshot languages configured in $config_file" >&2
        return 1
    fi
}
