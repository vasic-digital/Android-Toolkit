#!/bin/sh

# Cleanup function to uninstall apps
cleanup() {
    adb uninstall russian.bear.messenger || true
    adb uninstall russian.bear.messenger.test || true
}

# Set trap to run cleanup on exit
trap cleanup EXIT

./gradlew test && \
timeout 600 ./gradlew connectedAndroidTest && \
echo "Tests executed with success"