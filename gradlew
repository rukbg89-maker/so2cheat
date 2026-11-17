#!/bin/sh
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
else
    echo "Downloading Gradle..."
    wget -q https://services.gradle.org/distributions/gradle-8.2-bin.zip
    unzip -q gradle-8.2-bin.zip
    exec ./gradle-8.2/bin/gradle "$@"
fi
