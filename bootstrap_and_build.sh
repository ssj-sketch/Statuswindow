#!/usr/bin/env bash
set -e
curl -L https://services.gradle.org/distributions/gradle-8.9-bin.zip -o gradle-8.9-bin.zip
unzip -q gradle-8.9-bin.zip -d .
rm -f gradle-8.9-bin.zip
./gradle-8.9/bin/gradle -p $(pwd) wrapper --gradle-version 8.9 --no-daemon
chmod +x ./gradlew
./gradlew clean assembleDebug --no-daemon
