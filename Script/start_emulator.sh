#!/bin/bash

# Bear Messenger - Android Emulator Startup Script
# This script starts an Android emulator for testing

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Bear Messenger - Emulator Startup${NC}"
echo -e "${GREEN}========================================${NC}"

# Check if Android SDK is installed
if [ -z "$ANDROID_HOME" ]; then
    echo -e "${RED}ERROR: ANDROID_HOME environment variable not set${NC}"
    echo "Please set ANDROID_HOME to your Android SDK location"
    echo "Example: export ANDROID_HOME=$HOME/Android/Sdk"
    exit 1
fi

EMULATOR="$ANDROID_HOME/emulator/emulator"
AVD_MANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"

# Check if emulator exists
if [ ! -f "$EMULATOR" ]; then
    echo -e "${RED}ERROR: Android emulator not found at $EMULATOR${NC}"
    exit 1
fi

# List available AVDs
echo -e "\n${YELLOW}Available Android Virtual Devices:${NC}"
$EMULATOR -list-avds

# Default AVD name
AVD_NAME="${1:-Pixel_6_API_34}"

echo -e "\n${YELLOW}Starting emulator: $AVD_NAME${NC}"

# Check if AVD exists
if ! $EMULATOR -list-avds | grep -q "^$AVD_NAME$"; then
    echo -e "${YELLOW}AVD '$AVD_NAME' not found. Creating it...${NC}"

    # Create AVD if it doesn't exist
    echo "no" | $AVD_MANAGER create avd \
        -n "$AVD_NAME" \
        -k "system-images;android-34;google_apis;x86_64" \
        -d "pixel_6"

    echo -e "${GREEN}AVD created successfully${NC}"
fi

# Start emulator in background
echo -e "${YELLOW}Launching emulator...${NC}"
$EMULATOR -avd "$AVD_NAME" \
    -no-snapshot-save \
    -no-boot-anim \
    -gpu swiftshader_indirect \
    -memory 4096 \
    -cores 4 \
    -wipe-data \
    > /dev/null 2>&1 &

EMULATOR_PID=$!
echo -e "${GREEN}Emulator started with PID: $EMULATOR_PID${NC}"

# Wait for emulator to boot
echo -e "${YELLOW}Waiting for emulator to boot...${NC}"
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'

echo -e "${GREEN}✓ Emulator is ready!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${YELLOW}Emulator Info:${NC}"
adb devices -l
echo -e "${GREEN}========================================${NC}"

# Save PID to file
echo $EMULATOR_PID > /tmp/bear_emulator.pid
echo -e "${YELLOW}Emulator PID saved to /tmp/bear_emulator.pid${NC}"
echo -e "${YELLOW}To stop: ./stop_emulator.sh or kill $EMULATOR_PID${NC}"
