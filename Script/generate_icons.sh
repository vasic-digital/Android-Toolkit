#!/bin/bash

# Script to generate launcher icons and logo images from Assets/Logo/ sources
# Replaces all occurrences of old logos with new ones

set -e

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "ImageMagick is required. Please install it first."
    exit 1
fi

# Source directory
SOURCE_DIR="Assets/Logo"

# Target directories
APP_RES_DIR="Application/src/main/res"

# Source files (assuming Belarusian uses Russian logo if not present)
LATIN_SOURCE="$SOURCE_DIR/Main_Latin_Transparent_Full.png"
RUSSIAN_SOURCE="$SOURCE_DIR/Main_Russian_Transparent_Full.png"
BELARUSIAN_SOURCE="$SOURCE_DIR/Main_Belarusian_Transparent_Full.png"

if [ ! -f "$BELARUSIAN_SOURCE" ]; then
    BELARUSIAN_SOURCE="$RUSSIAN_SOURCE"
fi

# Function to generate launcher icons for a source
generate_launcher_icons() {
    local source=$1
    local base_name=$2

    if [ ! -f "$source" ]; then
        echo "Source file $source not found, skipping $base_name"
        return
    fi

    echo "Generating launcher icons from $source"

    # Densities and sizes for ic_launcher.png (square)
    declare -A densities=(
        ["mdpi"]="48"
        ["hdpi"]="72"
        ["xhdpi"]="96"
        ["xxhdpi"]="144"
        ["xxxhdpi"]="192"
    )

    for density in "${!densities[@]}"; do
        size=${densities[$density]}
        target_dir="$APP_RES_DIR/mipmap-$density"
        mkdir -p "$target_dir"

        # Generate ic_launcher.png
        convert "$source" -resize "${size}x${size}" -background transparent "$target_dir/ic_launcher.png"

        # Generate ic_launcher_round.png (with rounded corners if needed, but for now just copy)
        convert "$source" -resize "${size}x${size}" -background transparent "$target_dir/ic_launcher_round.png"

        # For adaptive icons, foreground
        convert "$source" -resize "${size}x${size}" -background transparent "$target_dir/ic_launcher_foreground.png"
    done

    # Playstore icon
    convert "$source" -resize "512x512" -background transparent "$APP_RES_DIR/ic_launcher-playstore.png"

    echo "Launcher icons generated for $base_name"
}

# Function to generate app logos
generate_app_logos() {
    local source=$1
    local locale=$2

    if [ ! -f "$source" ]; then
        echo "Source file $source not found, skipping $locale"
        return
    fi

    echo "Generating app logos from $source for $locale"

    if [ "$locale" = "default" ]; then
        target_dir="$APP_RES_DIR/drawable"
    else
        target_dir="$APP_RES_DIR/drawable-$locale"
        mkdir -p "$target_dir"
    fi

    # Generate logo.png (assume 512x512)
    convert "$source" -resize "512x512" -background transparent "$target_dir/logo.png"

    # Generate splash logo (bigger for better quality)
    convert "$source" -resize "1024x1024" -background transparent "$target_dir/ic_launcher_splash.png"

    # If there's ic_main_logo.xml, we might need to update it, but for now, assume it's vector and skip

    echo "App logos generated for $locale"
}

# Generate default (Latin) launcher icons
generate_launcher_icons "$LATIN_SOURCE" "default"

# Generate app logos for each locale
generate_app_logos "$LATIN_SOURCE" "default"
generate_app_logos "$RUSSIAN_SOURCE" "ru"
generate_app_logos "$BELARUSIAN_SOURCE" "be"

# For Chinese, if needed, but since no source, skip or use default
# generate_app_logos "$LATIN_SOURCE" "zh"

echo "All icons and logos generated successfully."
echo "Note: You may need to update vector drawables like ic_main_logo.xml manually if they reference old logos."