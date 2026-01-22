#!/bin/bash

# Build and Install the Debug APK
echo "🏗️  Building and Installing APK for Mobile..."
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo "🚀 Launching App on Mobile Emulator/Device..."
    # Force stop first to ensure a fresh start
    adb shell am force-stop com.example.wco_tv
    
    # Start the Main Activity
    adb shell am start -n com.example.wco_tv/.MainActivity
    
    echo "✅ App Launched!"
    echo "   (Tip: Run 'adb logcat -s WCO_TV WCO_TV_JS WCO_TV_PLAYER' to see logs)"
else
    echo "❌ Build Failed."
    exit 1
fi
