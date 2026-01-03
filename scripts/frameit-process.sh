#!/bin/bash

# Process screenshots with frameit for all languages
# This script assumes screenshots are already copied to frameit directories

set -e

# Define supported languages
LANGUAGES=("en-GB" "es-ES" "de-DE" "fr-FR")

echo "==> Running frameit to add frames and captions"

# Try to use bundler first, fallback to error
if command -v bundle >/dev/null 2>&1 && [ -f Gemfile ]; then
    echo "Using bundler to run frameit..."
    bundle exec fastlane run frameit path:"fastlane/frameit/screenshots"
else
    echo "ERROR: fastlane not found. Please install it first:"
    echo "  fix bundler setup and run: bundle install"
    exit 1
fi

echo "==> Copying framed screenshots to fastlane directories for all languages"

# Verify at least one language has framed screenshots
framed_found=false
for lang in "${LANGUAGES[@]}"; do
    if ls "fastlane/frameit/screenshots/$lang"/*_framed.png >/dev/null 2>&1; then
        framed_found=true
        break
    fi
done

if [ "$framed_found" = false ]; then
    echo "ERROR: No _framed.png files found for any language!"
    exit 1
fi

# Copy framed screenshots for each language
for lang in "${LANGUAGES[@]}"; do
    echo "Processing framed screenshots for language: $lang"
    
    # Check if framed files exist for this language
    if ls "fastlane/frameit/screenshots/$lang"/*_framed.png >/dev/null 2>&1; then
        echo "Found _framed.png files for $lang, copying them..."
        
        # Ensure the destination directory exists
        mkdir -p "fastlane/metadata/android/$lang/images/phoneScreenshots"
        
        # Copy each framed screenshot to the appropriate fastlane metadata directory
        for i in {1..8}; do
            src_file="fastlane/frameit/screenshots/$lang/${i}_framed.png"
            dest_file="fastlane/metadata/android/$lang/images/phoneScreenshots/${i}_$lang.png"
            
            if [ -f "$src_file" ]; then
                cp "$src_file" "$dest_file"
                echo "  Copied $i.png for $lang"
            else
                echo "  Warning: ${i}_framed.png not found for $lang"
            fi
        done
    else
        echo "Warning: No _framed.png files found for $lang"
    fi
done

echo "==> Frameit processing completed successfully"
