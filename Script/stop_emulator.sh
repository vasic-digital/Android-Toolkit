#!/bin/bash

# Bear Messenger - Android Emulator Stop Script
# This script stops the running Android emulator

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Bear Messenger - Stopping Emulator${NC}"
echo -e "${GREEN}========================================${NC}"

# Check for saved PID
if [ -f /tmp/bear_emulator.pid ]; then
    EMULATOR_PID=$(cat /tmp/bear_emulator.pid)
    echo -e "${YELLOW}Found emulator PID: $EMULATOR_PID${NC}"

    if ps -p $EMULATOR_PID > /dev/null; then
        echo -e "${YELLOW}Stopping emulator process...${NC}"
        kill $EMULATOR_PID
        sleep 2

        # Force kill if still running
        if ps -p $EMULATOR_PID > /dev/null; then
            echo -e "${YELLOW}Force killing emulator...${NC}"
            kill -9 $EMULATOR_PID
        fi

        echo -e "${GREEN}✓ Emulator stopped${NC}"
        rm /tmp/bear_emulator.pid
    else
        echo -e "${YELLOW}Emulator process not running${NC}"
        rm /tmp/bear_emulator.pid
    fi
else
    echo -e "${YELLOW}No saved PID found. Searching for running emulators...${NC}"

    # Find and kill all emulator processes
    PIDS=$(pgrep -f "emulator.*avd" || true)

    if [ -n "$PIDS" ]; then
        echo -e "${YELLOW}Found emulator processes: $PIDS${NC}"
        echo "$PIDS" | xargs kill
        sleep 2
        echo -e "${GREEN}✓ All emulators stopped${NC}"
    else
        echo -e "${YELLOW}No running emulators found${NC}"
    fi
fi

# Kill adb server for clean state
echo -e "${YELLOW}Killing ADB server...${NC}"
adb kill-server 2>/dev/null || true

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Emulator shutdown complete${NC}"
echo -e "${GREEN}========================================${NC}"
