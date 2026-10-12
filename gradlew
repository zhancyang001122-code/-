#!/usr/bin/env sh
##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
APP_HOME=$(cd "$(dirname "$0")" && pwd)

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# If gradle-wrapper.jar is missing, fetch it automatically
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading gradle-wrapper.jar..."
    mkdir -p "$APP_HOME/gradle/wrapper"
    if command -v curl >/dev/null 2>&1; then
        curl -sLo "$WRAPPER_JAR" https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar || true
    elif command -v wget >/dev/null 2>&1; then
        wget -qO "$WRAPPER_JAR" https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar || true
    fi
fi

# Fallback to system gradle if jar still missing
if [ ! -f "$WRAPPER_JAR" ] && command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

# Run wrapper
exec java -jar "$WRAPPER_JAR" "$@"
