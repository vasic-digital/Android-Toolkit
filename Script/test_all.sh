#!/bin/sh

./gradlew test && \
./gradlew connectedAndroidTest && \
echo "Tests executed with success"