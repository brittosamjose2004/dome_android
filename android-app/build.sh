#!/bin/bash

# Android Build Script (without Android Studio)
# Usage: ./build.sh [debug|release|install|clean]

set -e

echo "🔨 Android Build Script"
echo "======================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed!${NC}"
    echo "Please install Java JDK 11 or higher"
    exit 1
fi

echo -e "${GREEN}✓ Java found:${NC} $(java -version 2>&1 | head -n 1)"

# Check if Android SDK is set
if [ -z "$ANDROID_HOME" ]; then
    echo -e "${YELLOW}⚠️  ANDROID_HOME not set${NC}"
    
    # Try to find Android SDK in common locations
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
        echo -e "${GREEN}✓ Found Android SDK:${NC} $ANDROID_HOME"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        echo -e "${GREEN}✓ Found Android SDK:${NC} $ANDROID_HOME"
    else
        echo -e "${RED}❌ Android SDK not found!${NC}"
        echo ""
        echo "Please install Android SDK:"
        echo "1. Download command line tools from https://developer.android.com/studio"
        echo "2. Extract and run: ./sdkmanager --sdk_root=\$HOME/Android/Sdk \"platform-tools\" \"platforms;android-34\" \"build-tools;34.0.0\""
        echo "3. Set ANDROID_HOME: export ANDROID_HOME=\$HOME/Android/Sdk"
        echo "4. Add to PATH: export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
        exit 1
    fi
fi

export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin

# Make gradlew executable
chmod +x gradlew

# Default to debug build
BUILD_TYPE=${1:-debug}

case $BUILD_TYPE in
    debug)
        echo -e "\n${GREEN}📦 Building DEBUG APK...${NC}\n"
        ./gradlew assembleDebug
        
        if [ $? -eq 0 ]; then
            echo -e "\n${GREEN}✅ Build successful!${NC}"
            echo -e "APK location: ${YELLOW}app/build/outputs/apk/debug/app-debug.apk${NC}"
            
            # Show APK info
            APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
            if [ -f "$APK_PATH" ]; then
                APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
                echo -e "APK size: ${YELLOW}$APK_SIZE${NC}"
            fi
        else
            echo -e "\n${RED}❌ Build failed!${NC}"
            exit 1
        fi
        ;;
        
    release)
        echo -e "\n${GREEN}📦 Building RELEASE APK...${NC}\n"
        ./gradlew assembleRelease
        
        if [ $? -eq 0 ]; then
            echo -e "\n${GREEN}✅ Build successful!${NC}"
            echo -e "APK location: ${YELLOW}app/build/outputs/apk/release/app-release-unsigned.apk${NC}"
            echo -e "\n${YELLOW}⚠️  Note: Release APK needs to be signed before installation${NC}"
        else
            echo -e "\n${RED}❌ Build failed!${NC}"
            exit 1
        fi
        ;;
        
    install)
        echo -e "\n${GREEN}📦 Building and Installing APK...${NC}\n"
        
        # Check if device is connected
        if ! command -v adb &> /dev/null; then
            echo -e "${RED}❌ adb not found!${NC}"
            echo "Make sure ANDROID_HOME/platform-tools is in your PATH"
            exit 1
        fi
        
        DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)
        if [ $DEVICES -eq 0 ]; then
            echo -e "${RED}❌ No Android device connected!${NC}"
            echo "Connect a device via USB or start an emulator"
            exit 1
        fi
        
        echo -e "${GREEN}✓ Device connected${NC}"
        
        ./gradlew installDebug
        
        if [ $? -eq 0 ]; then
            echo -e "\n${GREEN}✅ App installed successfully!${NC}"
            echo -e "\n${YELLOW}To launch the app:${NC}"
            echo "adb shell am start -n com.example.webrtcstreamer/.MainActivity"
        else
            echo -e "\n${RED}❌ Installation failed!${NC}"
            exit 1
        fi
        ;;
        
    clean)
        echo -e "\n${GREEN}🧹 Cleaning build...${NC}\n"
        ./gradlew clean
        echo -e "${GREEN}✅ Clean complete!${NC}"
        ;;
        
    *)
        echo -e "${RED}❌ Unknown build type: $BUILD_TYPE${NC}"
        echo ""
        echo "Usage: ./build.sh [debug|release|install|clean]"
        echo ""
        echo "  debug   - Build debug APK (default)"
        echo "  release - Build release APK (unsigned)"
        echo "  install - Build and install debug APK to connected device"
        echo "  clean   - Clean build artifacts"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}🎉 Done!${NC}"
