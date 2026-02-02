# 🚀 Quick Start: Build Android App Without Android Studio

This guide will get you building the Android app in just a few minutes!

## ⚡ TL;DR (Quick Start)

```bash
# 1. Setup Android SDK (first time only)
cd android-app
chmod +x setup-sdk.sh
./setup-sdk.sh

# 2. Build the app
./build.sh debug

# 3. Install on device (optional)
./build.sh install
```

## 📋 Prerequisites Check

### ✅ Java (Required)

```bash
java -version
```

**Expected**: Java 11 or higher

**If not installed:**
- Ubuntu/Debian: `sudo apt install openjdk-17-jdk`
- MacOS: `brew install openjdk@17`
- Windows: Download from [Adoptium](https://adoptium.net/)

### ✅ Android SDK (Will be auto-installed)

The `setup-sdk.sh` script will automatically download and configure Android SDK.

**Or install manually:**
```bash
# Download from https://developer.android.com/studio#command-tools
# Extract to ~/Android/Sdk
# Run: ./setup-sdk.sh
```

## 🛠️ Step-by-Step Setup

### Step 1: Navigate to Android App Directory

```bash
cd /workspaces/dome_android/android-app
```

### Step 2: Setup Android SDK (First Time Only)

```bash
chmod +x setup-sdk.sh
./setup-sdk.sh
```

This will:
- Download Android Command Line Tools
- Install Android SDK Platform 34
- Install Build Tools 34.0.0
- Configure environment variables

**Expected time**: 2-5 minutes depending on internet speed

### Step 3: Build the App

```bash
./build.sh debug
```

This will create a debug APK at: `app/build/outputs/apk/debug/app-debug.apk`

**Expected time**: 2-10 minutes (first build downloads dependencies)

## 📱 Installing on Device

### Option 1: Automated Install

```bash
# Connect device via USB and enable USB debugging
./build.sh install
```

### Option 2: Manual Install

```bash
# Transfer APK to device
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/

# Or copy manually and install through file manager
```

### Option 3: Direct ADB Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🏃 Launch the App

```bash
# Launch from command line
adb shell am start -n com.example.webrtcstreamer/.MainActivity

# Or tap the app icon on your device
```

## 📦 Build Commands Reference

```bash
# Debug build (for testing)
./build.sh debug

# Release build (for production)
./build.sh release

# Build and install on connected device
./build.sh install

# Clean build artifacts
./build.sh clean
```

## 🔧 Manual Gradle Commands

If you prefer using Gradle directly:

```bash
# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Run tests
./gradlew test

# Clean
./gradlew clean
```

## 🐛 Common Issues

### "ANDROID_HOME not set"

**Solution:**
```bash
# Linux/Mac
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Add to ~/.bashrc or ~/.zshrc to make permanent
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc
```

### "No device found" when installing

**Solution:**
1. Connect Android device via USB
2. Enable Developer Options:
   - Settings → About Phone
   - Tap "Build Number" 7 times
3. Enable USB Debugging:
   - Settings → Developer Options
   - Enable "USB Debugging"
4. Accept USB debugging prompt on device
5. Verify: `adb devices`

### "Permission denied" on scripts

**Solution:**
```bash
chmod +x gradlew build.sh setup-sdk.sh
```

### Build fails with memory error

**Solution:**
```bash
# Edit gradle.properties
echo "org.gradle.jvmargs=-Xmx4096m" >> gradle.properties
```

### "SDK location not found"

**Solution:**
```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

## 🎯 What Gets Built

After running `./build.sh debug`, you'll find:

```
app/build/outputs/apk/debug/
└── app-debug.apk          ← Install this on your device
```

**APK Size**: ~20-40 MB (includes WebRTC libraries)

## 🔐 Building Release APK

For production release:

### 1. Generate Signing Key (First Time)

```bash
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias webrtc-streamer
```

### 2. Configure Signing

Edit `app/build.gradle` and add:

```gradle
android {
    signingConfigs {
        release {
            storeFile file("../my-release-key.jks")
            storePassword "your-password"
            keyAlias "webrtc-streamer"
            keyPassword "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            // ... rest of config
        }
    }
}
```

### 3. Build Signed Release

```bash
./build.sh release
```

Or sign manually:

```bash
# Build unsigned
./gradlew assembleRelease

# Sign
apksigner sign --ks my-release-key.jks \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

## 📊 Build Times (Approximate)

- **First build**: 5-10 minutes (downloads dependencies)
- **Subsequent builds**: 30 seconds - 2 minutes
- **Clean build**: 2-5 minutes

## ✨ Build Optimizations

### Enable Parallel Builds

Add to `gradle.properties`:
```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

### Increase Build Memory

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
```

## 🚀 Advanced: CI/CD Build

### Docker Build

```bash
cd android-app

# Build Docker image
docker build -t android-builder -f- . <<EOF
FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y wget unzip
COPY . /app
WORKDIR /app
RUN chmod +x gradlew && ./gradlew assembleDebug
EOF

# Extract APK
docker run --rm -v $(pwd)/build:/output android-builder \
  cp /app/app/build/outputs/apk/debug/app-debug.apk /output/
```

### GitHub Actions

The project is ready for CI/CD. See [BUILD_WITHOUT_STUDIO.md](BUILD_WITHOUT_STUDIO.md) for GitHub Actions example.

## 📚 Next Steps

After building:

1. ✅ Update signaling server URL in code
2. ✅ Test on physical device
3. ✅ Configure release signing
4. ✅ Setup CI/CD pipeline
5. ✅ Distribute APK or publish to Play Store

## 💡 Tips

- **Use build cache**: `./gradlew assembleDebug --build-cache`
- **Offline mode**: `./gradlew assembleDebug --offline` (after first build)
- **View dependencies**: `./gradlew dependencies`
- **Debug build issues**: `./gradlew assembleDebug --debug`

## 📖 Full Documentation

For comprehensive details, see [BUILD_WITHOUT_STUDIO.md](BUILD_WITHOUT_STUDIO.md)

## 🎉 Success!

You've successfully built the Android app without Android Studio! The APK is ready to install and stream video via WebRTC.

**To test the complete system:**

1. Start signaling server: `cd signaling-server && npm start`
2. Start web client: `cd web-client && npm start`
3. Install APK on Android device
4. Update server URL in app settings
5. Start streaming!

---

Need help? Check [BUILD_WITHOUT_STUDIO.md](BUILD_WITHOUT_STUDIO.md) for detailed troubleshooting.
