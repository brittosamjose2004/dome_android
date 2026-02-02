# ⚡ Android Build - Command Reference

Quick reference for building the Android app without Android Studio.

## 🚀 First Time Setup

```bash
cd android-app
./setup-sdk.sh    # Downloads and configures Android SDK (2-5 min)
```

## 🔨 Build Commands

```bash
./build.sh debug      # Build debug APK
./build.sh release    # Build release APK  
./build.sh install    # Build + install to device
./build.sh clean      # Clean build files
```

## 📦 Direct Gradle Commands

```bash
./gradlew assembleDebug       # Build debug
./gradlew assembleRelease     # Build release
./gradlew installDebug        # Install to device
./gradlew clean               # Clean
./gradlew test                # Run tests
./gradlew tasks               # List all tasks
```

## 📱 Install APK

```bash
# Method 1: Automated
./build.sh install

# Method 2: ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Method 3: Manual
# Copy APK to device and install via file manager
```

## 🔧 Environment Setup

```bash
# Check Java
java -version

# Set Android SDK (if needed)
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

## 📂 Output Locations

```
app/build/outputs/apk/debug/app-debug.apk          # Debug APK
app/build/outputs/apk/release/app-release.apk      # Release APK
```

## 🐛 Quick Fixes

```bash
# Permission denied
chmod +x gradlew build.sh setup-sdk.sh

# Clean build
./gradlew clean build

# Refresh dependencies  
./gradlew build --refresh-dependencies

# Stop Gradle daemon
./gradlew --stop
```

## 📱 Device Setup

```bash
# Enable USB debugging:
# Settings → About Phone → Tap "Build Number" 7 times
# Settings → Developer Options → Enable "USB Debugging"

# Check devices
adb devices

# Launch app
adb shell am start -n com.example.webrtcstreamer/.MainActivity
```

## 🔐 Sign Release APK

```bash
# Generate keystore
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias webrtc-app

# Build signed
./gradlew assembleRelease
```

## 📊 Build Info

```bash
# View dependencies
./gradlew dependencies

# View build configuration
./gradlew properties

# Verbose build
./gradlew assembleDebug --info

# Debug build issues
./gradlew assembleDebug --debug --stacktrace
```

## 🚀 Optimization

```bash
# Enable build cache
./gradlew assembleDebug --build-cache

# Parallel execution  
./gradlew assembleDebug --parallel

# Offline mode (after first build)
./gradlew assembleDebug --offline
```

## 📚 Documentation

- [QUICKSTART.md](QUICKSTART.md) - 5-minute guide
- [BUILD_WITHOUT_STUDIO.md](BUILD_WITHOUT_STUDIO.md) - Full guide
- [BUILD_GUIDE.md](BUILD_GUIDE.md) - Complete reference

## 💡 Pro Tips

```bash
# Watch mode (auto-rebuild on changes)
./gradlew --continuous assembleDebug

# Generate APK + install + launch in one command
./gradlew installDebug && adb shell am start -n com.example.webrtcstreamer/.MainActivity

# Build with custom properties
./gradlew assembleDebug -PsigningServer=ws://192.168.1.100:3000

# Check APK size
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

---

**Need help?** Check [QUICKSTART.md](QUICKSTART.md) or [BUILD_WITHOUT_STUDIO.md](BUILD_WITHOUT_STUDIO.md)
