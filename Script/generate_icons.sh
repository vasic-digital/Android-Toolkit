#!/bin/bash

# Script to generate launcher icons and logo images from Assets/Logo/ sources
# Replaces all occurrences of old logos with new ones
# Usage: ./generate_icons.sh [background_color_hex]
# Default background color: #00373d

set -e

# Default background color
BACKGROUND_COLOR="#00373d"

# Parse arguments
if [ $# -gt 0 ]; then
    BACKGROUND_COLOR="$1"
fi

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "ImageMagick is required. Please install it first."
    exit 1
fi

# Source directory
SOURCE_DIR="../../Assets/Logo"

# Target directories
APP_RES_DIR="../../Application/src/main/res"
WEB_PUBLIC_DIR="../../web-app/public"
ADMIN_PUBLIC_DIR="../../admin-panel/public"
DESKTOP_ICON_DIR="../../desktop-app/build/icons"

# Source file
SOURCE="$SOURCE_DIR/Logo.png"

# Function to generate launcher icons
generate_launcher_icons() {
    local source=$1

    if [ ! -f "$source" ]; then
        echo "Source file $source not found"
        exit 1
    fi

    echo "Generating launcher icons from $source"

    # Generate legacy launcher icons with background
    for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
        target_dir="$APP_RES_DIR/mipmap-$density"
        mkdir -p "$target_dir"

        # Generate ic_launcher.png with background
        convert "$source" -resize "${size}x${size}" -background "$BACKGROUND_COLOR" -gravity center -extent ${size}x${size} "$target_dir/ic_launcher.png"

        # Generate ic_launcher_round.png with background (same as launcher for now)
        convert "$source" -resize "${size}x${size}" -background "$BACKGROUND_COLOR" -gravity center -extent ${size}x${size} "$target_dir/ic_launcher_round.png"
    done

    # Densities and sizes for ic_launcher_foreground.png
    declare -A densities=(
        ["mdpi"]="108"
        ["hdpi"]="162"
        ["xhdpi"]="216"
        ["xxhdpi"]="324"
        ["xxxhdpi"]="432"
    )

    for density in "${!densities[@]}"; do
        size=${densities[$density]}
        target_dir="$APP_RES_DIR/mipmap-$density"
        mkdir -p "$target_dir"

        # Generate ic_launcher_foreground.png (transparent background)
        convert "$source" -resize "${size}x${size}" -background transparent "$target_dir/ic_launcher_foreground.png"
    done

    # Create adaptive icon XML for anydpi
    anydpi_dir="$APP_RES_DIR/mipmap-anydpi-v26"
    mkdir -p "$anydpi_dir"
    cat > "$anydpi_dir/ic_launcher.xml" << EOF
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
EOF

    # Playstore icon with background
    convert "$source" -resize "512x512" -background "#00373d" -gravity center -extent 512x512 "$APP_RES_DIR/ic_launcher-playstore.png"

    echo "Adaptive launcher icons generated"
}

# Function to generate app logos
generate_app_logos() {
    local source=$1

    if [ ! -f "$source" ]; then
        echo "Source file $source not found"
        exit 1
    fi

    echo "Generating app logos from $source"

    # Default drawable
    target_dir="$APP_RES_DIR/drawable"
    mkdir -p "$target_dir"

    # Generate logo.png (512x512, transparent)
    convert "$source" -resize "512x512" -background transparent "$target_dir/logo.png"

    # Generate ic_launcher_foreground.png (512x512, transparent)
    convert "$source" -resize "512x512" -background transparent "$target_dir/ic_launcher_foreground.png"

    # Generate splash logo (1024x1024, transparent)
    convert "$source" -resize "1024x1024" -background transparent "$target_dir/ic_launcher_splash.png"

    # For locales, copy to drawable-ru and drawable-be
    for locale in ru be; do
        locale_dir="$APP_RES_DIR/drawable-$locale"
        mkdir -p "$locale_dir"
        cp "$target_dir/logo.png" "$locale_dir/logo.png"
        cp "$target_dir/ic_launcher_foreground.png" "$locale_dir/ic_launcher_foreground.png"
        cp "$target_dir/ic_launcher_splash.png" "$locale_dir/ic_launcher_splash.png"
    done

    echo "App logos generated"
}

# Function to generate web favicon
generate_web_favicon() {
    local source=$1
    local target_dir=$2

    mkdir -p "$target_dir"

    # Generate favicon.ico with background
    convert "$source" -resize "32x32" -background "#00373d" -gravity center -extent 32x32 "$target_dir/favicon.ico"

    echo "Web favicon generated in $target_dir"
}

# Function to generate desktop icon
generate_desktop_icon() {
    local source=$1
    local target_dir=$2

    mkdir -p "$target_dir"

    # Generate icon.png with background
    convert "$source" -resize "512x512" -background "#00373d" -gravity center -extent 512x512 "$target_dir/icon.png"

    echo "Desktop icon generated in $target_dir"
}

# Generate launcher icons
generate_launcher_icons "$SOURCE"

# Generate app logos
generate_app_logos "$SOURCE"

# Generate web favicon
generate_web_favicon "$SOURCE" "$WEB_PUBLIC_DIR"

# Generate admin favicon
generate_web_favicon "$SOURCE" "$ADMIN_PUBLIC_DIR"

# Generate desktop icon
generate_desktop_icon "$SOURCE" "$DESKTOP_ICON_DIR"

# Verify generated icons
echo "Verifying generated icons..."

# Check Android icons
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    if [ ! -f "$APP_RES_DIR/mipmap-$density/ic_launcher_foreground.png" ]; then
        echo "Error: Missing ic_launcher_foreground.png for $density"
        exit 1
    fi
    if [ ! -f "$APP_RES_DIR/mipmap-$density/ic_launcher.png" ]; then
        echo "Error: Missing ic_launcher.png for $density"
        exit 1
    fi
    if [ ! -f "$APP_RES_DIR/mipmap-$density/ic_launcher_round.png" ]; then
        echo "Error: Missing ic_launcher_round.png for $density"
        exit 1
    fi
done
if [ ! -f "$APP_RES_DIR/mipmap-anydpi-v26/ic_launcher.xml" ]; then
    echo "Error: Missing ic_launcher.xml"
    exit 1
fi
if [ ! -f "$APP_RES_DIR/ic_launcher-playstore.png" ]; then
    echo "Error: Missing ic_launcher-playstore.png"
    exit 1
fi

# Check app logos
if [ ! -f "$APP_RES_DIR/drawable/logo.png" ]; then
    echo "Error: Missing logo.png"
    exit 1
fi
if [ ! -f "$APP_RES_DIR/drawable/ic_launcher_foreground.png" ]; then
    echo "Error: Missing ic_launcher_foreground.png"
    exit 1
fi

# Check web favicon
if [ ! -f "$WEB_PUBLIC_DIR/favicon.ico" ]; then
    echo "Error: Missing web favicon.ico"
    exit 1
fi

# Check admin favicon
if [ ! -f "$ADMIN_PUBLIC_DIR/favicon.ico" ]; then
    echo "Error: Missing admin favicon.ico"
    exit 1
fi

# Check desktop icon
if [ ! -f "$DESKTOP_ICON_DIR/icon.png" ]; then
    echo "Error: Missing desktop icon.png"
    exit 1
fi

echo "All icons verified successfully."
echo "All icons and logos generated successfully for Android, Web, Admin, and Desktop apps."
echo "Note: You may need to update vector drawables like ic_main_logo.xml manually if they reference old logos."