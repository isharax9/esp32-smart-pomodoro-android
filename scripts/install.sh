#!/usr/bin/env bash
set -e

# ESP32 Smart Pomodoro Android App - Fast Local Dev & Install Script
# Builds the debug APK locally and installs it directly to a connected Android device or emulator.

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

# Locate Java 17
if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
    export PATH="$JAVA_HOME/bin:$PATH"
elif [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Locate Android SDK
if [ -z "$ANDROID_HOME" ] && [ -d "$HOME/Library/Android/sdk" ]; then
    export ANDROID_HOME="$HOME/Library/Android/sdk"
    export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
    export PATH="$ANDROID_HOME/platform-tools:$PATH"
fi

# Ensure local.properties exists if sdk is found
if [ ! -f "local.properties" ] && [ -d "$HOME/Library/Android/sdk" ]; then
    echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
fi

# Locate adb
ADB="adb"
if ! command -v adb &> /dev/null; then
    if [ -f "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
        ADB="$HOME/Library/Android/sdk/platform-tools/adb"
    else
        echo "❌ Warning: 'adb' not found in PATH or ~/Library/Android/sdk/platform-tools."
        echo "   Please enable USB Debugging on your device and ensure the Android SDK platform-tools are installed."
    fi
fi

echo "🚀 Building AuraFlow debug APK..."
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
mkdir -p build-output/debug
cp "$APK_PATH" build-output/debug/app-debug.apk

echo "✅ Build complete: $APK_PATH"

# Check if a device is connected
if [ -x "$(command -v "$ADB")" ]; then
    DEVICES=$($ADB devices | grep -v "List" | grep "device$" || true)
    UNAUTHORIZED=$($ADB devices | grep -v "List" | grep "unauthorized$" || true)

    if [ -n "$DEVICES" ]; then
        DEVICE_ID=$(echo "$DEVICES" | head -n1 | awk '{print $1}')
        echo "📲 Installing directly to connected device ($DEVICE_ID)..."
        $ADB -s "$DEVICE_ID" install -r "$APK_PATH"
        echo "🎉 Launching AuraFlow on device..."
        $ADB -s "$DEVICE_ID" shell am start -n com.esp32.smartpomodoro/.MainActivity
        echo "✨ Done! AuraFlow is running on your device."
    elif [ -n "$UNAUTHORIZED" ]; then
        echo ""
        echo "⚠️  Device detected but UNAUTHORIZED!"
        echo "   Please unlock your phone screen and tap 'Always allow from this computer' -> 'OK'."
        echo "   Then re-run this script."
    else
        echo ""
        echo "ℹ️  No authorized Android device detected via USB/Wireless adb."
        echo "   To install directly to your device:"
        echo "   1. Enable Developer Options -> USB Debugging on your phone."
        echo "   2. For Huawei / Honor (EMUI) phones:"
        echo "      - Set USB mode to 'Transfer files' (MTP) instead of 'Charge only'."
        echo "      - In Developer options, enable 'Allow ADB debugging in charge only mode'."
        echo "   3. Unlock your phone and accept the 'Allow USB debugging?' RSA prompt."
        echo "   4. Re-run './scripts/install.sh'."
        echo ""
        echo "   For now, the fresh APK is ready at: build-output/debug/app-debug.apk"
    fi
fi
