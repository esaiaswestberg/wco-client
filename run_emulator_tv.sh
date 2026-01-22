#!/bin/bash

# Build and Install the Debug APK
echo "🏗️  Building and Installing APK..."
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo "🚀 Launching App on Emulator..."
    # Force stop first to ensure a fresh start (optional but helpful for development)
    adb shell am force-stop com.example.wco_tv
    
    # Start the Main Activity
    adb shell am start -n com.example.wco_tv/.MainActivity
    
    echo "✅ App Launched!"
    echo "   (Tip: Run 'adb logcat -s MainActivity HTMLOUT System.out *:E' to see logs)"
else
    echo "❌ Build Failed."
    exit 1
fi
