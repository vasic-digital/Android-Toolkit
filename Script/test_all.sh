#!/bin/sh

./gradlew test && \
timeout 600 ./gradlew connectedAndroidTest && \
echo "Tests executed with success"