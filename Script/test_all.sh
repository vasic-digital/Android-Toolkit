#!/bin/bash

./gradlew test && \
./gradlew connectedAndroidTest && \
echo "Tests executed with success"