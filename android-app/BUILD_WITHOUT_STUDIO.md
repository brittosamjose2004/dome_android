# Building Android App Without Android Studio

This guide explains how to build the Android app from the command line without Android Studio.

## Prerequisites

### 1. Install Java JDK

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

**MacOS:**
```bash
brew install openjdk@17
```

**Windows:**
Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)

Verify installation:
```bash
java -version
```

### 2. Install Android SDK Command Line Tools

**Option A: Download Directly**

1. Download from [Android Developer Site](https://developer.android.com/studio#command-tools)
2. Extract to a directory (e.g., `$HOME/Android/Sdk`)
3. Set environment variables:

**Linux/Mac:**
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

Add to `~/.bashrc` or `~/.zshrc` to make permanent.

**Windows:**
```cmd
set ANDROID_HOME=%USERPROFILE%\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\cmdline-tools\latest\bin
set PATH=%PATH%;%ANDROID_HOME%\platform-tools
```

**Option B: Use sdkmanager**

```bash
# Create SDK directory
mkdir -p $HOME/Android/Sdk
cd $HOME/Android/Sdk

# Download command line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip

# Install required components
./cmdline-tools/bin/sdkmanager --sdk_root=$HOME/Android/Sdk \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0" \
  "cmdline-tools;latest"
```

### 3. Verify Setup

```bash
# Check Android SDK
echo $ANDROID_HOME

# Check if sdkmanager works
sdkmanager --version

# Check if adb works
adb --version
```

## Building the App

### Method 1: Using the Build Script (Recommended)

The project includes a convenient build script:

```bash
cd android-app

# Make script executable
chmod +x build.sh

# Build debug APK
./build.sh debug

# Build and install on connected device
./build.sh install

# Build release APK
./build.sh release

# Clean build
./build.sh clean
```

### Method 2: Using Gradle Directly

```bash
cd android-app

# Make gradlew executable (Linux/Mac)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean

# View all tasks
./gradlew tasks
```

**Windows:**
```cmd
gradlew.bat assembleDebug
```

## Build Output Locations

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Installing the APK

### Option 1: Using ADB (Android Debug Bridge)

```bash
# List connected devices
adb devices

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Install and replace existing
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.example.webrtcstreamer/.MainActivity
```

### Option 2: Manual Installation

1. Transfer APK to Android device
2. Open file manager on device
3. Tap the APK file
4. Allow installation from unknown sources if prompted
5. Install

### Option 3: Using Gradle

```bash
./gradlew installDebug
```

## Signing Release APK

Release APKs must be signed before installation.

### Generate Keystore

```bash
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias my-key-alias
```

### Sign APK

```bash
# Using apksigner (preferred)
apksigner sign --ks my-release-key.jks \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

# Or using jarsigner (legacy)
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore my-release-key.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  my-key-alias

# Align the APK
zipalign -v 4 app-release-unaligned.apk app-release.apk
```

### Configure Signing in build.gradle

Add to `app/build.gradle`:

```gradle
android {
    signingConfigs {
        release {
            storeFile file("../my-release-key.jks")
            storePassword "your-keystore-password"
            keyAlias "my-key-alias"
            keyPassword "your-key-password"
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

Then build with:
```bash
./gradlew assembleRelease
```

## Troubleshooting

### "ANDROID_HOME not set"

Set environment variable:
```bash
export ANDROID_HOME=$HOME/Android/Sdk
```

### "SDK location not found"

Create `local.properties` file:
```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### "Build failed: license not accepted"

Accept licenses:
```bash
sdkmanager --licenses
```

### "No device found"

Enable USB debugging on Android:
1. Settings → About Phone
2. Tap "Build Number" 7 times
3. Settings → Developer Options
4. Enable "USB Debugging"
5. Connect via USB and accept prompt

### Gradle daemon issues

```bash
./gradlew --stop
./gradlew clean build
```

### Permission denied on gradlew

```bash
chmod +x gradlew
```

## Build Variants

```bash
# Debug build (with debug symbols)
./gradlew assembleDebug

# Release build (optimized)
./gradlew assembleRelease

# List all build variants
./gradlew tasks --all | grep assemble
```

## Running Tests

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device)
./gradlew connectedAndroidTest

# Generate test reports
./gradlew testDebugUnitTest
# Report at: app/build/reports/tests/testDebugUnitTest/index.html
```

## Useful Gradle Commands

```bash
# Check dependencies
./gradlew dependencies

# View project structure
./gradlew projects

# Build without running tests
./gradlew assembleDebug -x test

# Verbose output
./gradlew assembleDebug --info

# Full debug output
./gradlew assembleDebug --debug

# Offline build (uses cache)
./gradlew assembleDebug --offline

# Refresh dependencies
./gradlew build --refresh-dependencies
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Android Build

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'adopt'
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
    
    - name: Make gradlew executable
      run: chmod +x gradlew
      working-directory: android-app
    
    - name: Build Debug APK
      run: ./gradlew assembleDebug
      working-directory: android-app
    
    - name: Upload APK
      uses: actions/upload-artifact@v2
      with:
        name: app-debug
        path: android-app/app/build/outputs/apk/debug/app-debug.apk
```

## Docker Build (Advanced)

Create `Dockerfile` in android-app:

```dockerfile
FROM openjdk:17-jdk-slim

# Install Android SDK
RUN apt-get update && apt-get install -y wget unzip
RUN mkdir -p /opt/android-sdk
WORKDIR /opt/android-sdk

RUN wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
RUN unzip commandlinetools-linux-9477386_latest.zip

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

RUN yes | sdkmanager --licenses
RUN sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Copy project
WORKDIR /app
COPY . .

RUN chmod +x gradlew
RUN ./gradlew assembleDebug

CMD ["./gradlew", "assembleDebug"]
```

Build with Docker:
```bash
docker build -t android-builder .
docker run -v $(pwd)/app/build:/app/app/build android-builder
```

## Performance Tips

1. **Enable Gradle Daemon**: Add to `gradle.properties`
   ```properties
   org.gradle.daemon=true
   org.gradle.parallel=true
   org.gradle.caching=true
   ```

2. **Increase Memory**: Add to `gradle.properties`
   ```properties
   org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
   ```

3. **Use Build Cache**:
   ```bash
   ./gradlew assembleDebug --build-cache
   ```

That's it! You can now build the Android app without Android Studio. 🎉
