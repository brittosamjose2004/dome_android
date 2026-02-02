# 📱 Building Android App - Complete Guide

This directory contains everything needed to build the Android WebRTC streaming app **without Android Studio**.

## 📂 Files Overview

| File | Purpose |
|------|---------|
| `setup-sdk.sh` | Automatically downloads and configures Android SDK |
| `build.sh` | Main build script (debug, release, install, clean) |
| `gradlew` | Gradle wrapper for Linux/Mac |
| `gradlew.bat` | Gradle wrapper for Windows |
| `QUICKSTART.md` | Quick start guide (5 minutes to build) |
| `BUILD_WITHOUT_STUDIO.md` | Comprehensive build documentation |
| `README.md` | This file |

## 🚀 Quick Commands

```bash
# First time setup
./setup-sdk.sh              # Install Android SDK (2-5 min)

# Build commands
./build.sh debug           # Build debug APK
./build.sh release         # Build release APK
./build.sh install         # Build and install to device
./build.sh clean           # Clean build files

# Direct Gradle commands
./gradlew assembleDebug    # Build debug
./gradlew installDebug     # Install to device
./gradlew test            # Run tests
./gradlew tasks           # List all tasks
```

## 📦 Build Output

After building, your APK will be at:

```
app/build/outputs/apk/debug/app-debug.apk        # Debug APK
app/build/outputs/apk/release/app-release.apk    # Release APK
```

## ✅ Prerequisites

### Required
- **Java JDK 11+** (we detected OpenJDK 25 ✓)
- **Android SDK** (installed via `setup-sdk.sh`)

### Optional
- **ADB** (Android Debug Bridge) - for installing to device
- **Physical Android device** - for testing camera/streaming

## 🏗️ Build Process

### Method 1: Automated (Recommended)

```bash
# One-time setup
./setup-sdk.sh

# Build
./build.sh debug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Method 2: Manual Gradle

```bash
# Make gradlew executable
chmod +x gradlew

# Build
./gradlew assembleDebug

# Install
./gradlew installDebug
```

### Method 3: Step-by-Step

```bash
# 1. Setup environment
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 2. Build
./gradlew clean assembleDebug

# 3. Sign (for release)
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore my-release-key.jks app/build/outputs/apk/release/app-release-unsigned.apk my-key

# 4. Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📱 Installing APK

### Option 1: ADB (Fastest)
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Option 2: Build Script
```bash
./build.sh install
```

### Option 3: Manual Transfer
1. Copy APK to device
2. Open file manager
3. Tap APK file
4. Allow installation from unknown sources
5. Install

## 🔧 Configuration

### 1. Set Signaling Server URL

Edit [app/src/main/java/com/example/webrtcstreamer/MainActivity.kt](app/src/main/java/com/example/webrtcstreamer/MainActivity.kt):

```kotlin
val signalingServerUrl = "ws://YOUR_SERVER_IP:3000"
```

**For Android Emulator**: `ws://10.0.2.2:3000`  
**For Physical Device**: `ws://192.168.1.XXX:3000` (your computer's IP)  
**For Production**: `wss://your-domain.com`

### 2. Update Dependencies (if needed)

Edit [app/build.gradle](app/build.gradle):

```gradle
dependencies {
    implementation 'org.webrtc:google-webrtc:1.0.32006'
    // Add more dependencies
}
```

Then run:
```bash
./gradlew build --refresh-dependencies
```

## 🐛 Troubleshooting

### "Command not found: ./gradlew"
```bash
chmod +x gradlew build.sh setup-sdk.sh
```

### "ANDROID_HOME not set"
```bash
./setup-sdk.sh
# Or manually:
export ANDROID_HOME=$HOME/Android/Sdk
```

### "No device found"
```bash
# Check connected devices
adb devices

# Enable USB debugging on device:
# Settings → About Phone → Tap "Build Number" 7 times
# Settings → Developer Options → Enable "USB Debugging"
```

### "Build failed: Out of memory"
```bash
echo "org.gradle.jvmargs=-Xmx4096m" >> gradle.properties
```

### "SDK license not accepted"
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

## 📊 Build Statistics

- **First build**: 5-10 minutes (downloads dependencies)
- **Incremental build**: 30 seconds - 2 minutes
- **APK size**: ~20-40 MB (includes WebRTC libraries)
- **Minimum Android**: API 24 (Android 7.0)
- **Target Android**: API 34 (Android 14)

## 🎯 Build Variants

```bash
# Debug (for development)
./gradlew assembleDebug
# - Includes debug symbols
# - Not optimized
# - Debuggable

# Release (for production)
./gradlew assembleRelease
# - Optimized code
# - ProGuard enabled
# - Requires signing
```

## 🔐 Signing for Release

### Generate Keystore
```bash
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias webrtc-streamer
```

### Configure in build.gradle
```gradle
android {
    signingConfigs {
        release {
            storeFile file("../release-key.jks")
            storePassword "your-password"
            keyAlias "webrtc-streamer"
            keyPassword "your-password"
        }
    }
}
```

### Build Signed APK
```bash
./gradlew assembleRelease
```

## 🚀 CI/CD Integration

### GitHub Actions

Create `.github/workflows/android.yml`:

```yaml
name: Android Build
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Build
        run: |
          cd android-app
          chmod +x gradlew
          ./gradlew assembleDebug
      - uses: actions/upload-artifact@v2
        with:
          name: app-debug
          path: android-app/app/build/outputs/apk/debug/app-debug.apk
```

### Docker Build

```bash
docker run --rm -v "$PWD":/project -w /project \
  mingc/android-build-box:latest \
  bash -c "cd android-app && ./gradlew assembleDebug"
```

## 📚 Documentation

- **[QUICKSTART.md](QUICKSTART.md)** - Get started in 5 minutes
- **[BUILD_WITHOUT_STUDIO.md](BUILD_WITHOUT_STUDIO.md)** - Comprehensive guide
- **[README.md](README.md)** - Project overview
- **[/README.md](../README.md)** - Full project documentation

## 🎓 Learning Resources

- [Gradle Build Tool](https://gradle.org/)
- [Android SDK Command Line Tools](https://developer.android.com/studio/command-line)
- [WebRTC Android](https://webrtc.googlesource.com/src/+/main/docs/native-code/android/)
- [GStreamer Android](https://gstreamer.freedesktop.org/documentation/installing/for-android-development.html)

## ✨ What's Next?

After building successfully:

1. ✅ Install APK on device
2. ✅ Start signaling server (`cd signaling-server && npm start`)
3. ✅ Start web client (`cd web-client && npm start`)
4. ✅ Configure server URL in app
5. ✅ Test streaming!

## 🆘 Need Help?

- **Quick issues**: Check [QUICKSTART.md](QUICKSTART.md) troubleshooting
- **Build problems**: See [BUILD_WITHOUT_STUDIO.md](BUILD_WITHOUT_STUDIO.md)
- **App issues**: Check [README.md](README.md)
- **General**: See main [project README](../README.md)

## 🎉 Success Checklist

- [ ] Java installed (11+)
- [ ] Android SDK installed
- [ ] Build completes without errors
- [ ] APK file generated
- [ ] APK installs on device
- [ ] App launches successfully
- [ ] Camera permission granted
- [ ] Connects to signaling server
- [ ] Stream appears in web client

---

**Ready to build?** Run `./setup-sdk.sh` and then `./build.sh debug`! 🚀
