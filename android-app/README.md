# Android WebRTC Streamer with GStreamer

Android application for streaming video using WebRTC with GStreamer support.

## 🚀 Quick Start (No Android Studio Required!)

```bash
# 1. Setup Android SDK (first time only)
./setup-sdk.sh

# 2. Build the app
./build.sh debug

# 3. Install on device
./build.sh install
```

**📖 See [QUICKSTART.md](QUICKSTART.md) for step-by-step guide**

## Prerequisites

**Option A: Without Android Studio (Recommended for CI/CD)**
- Java JDK 11+ (OpenJDK 17 recommended)
- Android SDK (auto-installed via `setup-sdk.sh`)
- Physical Android device or emulator

**Option B: With Android Studio**
- Android Studio (Latest version)
- Android SDK API 24 or higher
- Physical Android device (recommended for camera support)

## Setup Instructions

### Without Android Studio (Command Line)

1. **Setup Android SDK** (first time only):
   ```bash
   ./setup-sdk.sh
   ```

2. **Build the app**:
   ```bash
   ./build.sh debug
   ```

3. **Install on device**:
   ```bash
   ./build.sh install
   ```

📖 **Full guide**: [BUILD_WITHOUT_STUDIO.md](BUILD_WITHOUT_STUDIO.md)

### With Android Studio

1. Open Android Studio
2. Click "Open an Existing Project"
3. Navigate to this directory
4. Click "OK"
5. Click the "Run" button

### 2. Configure Signaling Server URL

In [MainActivity.kt](app/src/main/java/com/example/webrtcstreamer/MainActivity.kt), update the signaling server URL:

```kotlin
val signalingServerUrl = "ws://YOUR_SERVER_IP:3000"
```

**Important**: 
- For Android emulator: Use `ws://10.0.2.2:3000` (localhost)
- For physical device: Use your computer's IP address or public server URL

### 3. Build and Run

**Command Line:**
```bash
./build.sh debug    # Build
./build.sh install  # Build and install
```

**Android Studio:**
1. Connect your Android device via USB (enable USB debugging)
2. Click the "Run" button
3. Select your device
4. Grant camera and microphone permissions

## Features

- Real-time video streaming using WebRTC
- Camera preview
- Automatic ICE candidate exchange
- Stream ID generation for embedding
- Copy embed URL to clipboard

## GStreamer Integration (Advanced)

This version uses native Android WebRTC. To integrate GStreamer:

1. Download GStreamer Android binaries from https://gstreamer.freedesktop.org/download/
2. Extract to `app/src/main/jni/gstreamer/`
3. Update `build.gradle` to include native libraries
4. Modify `WebRTCClient.kt` to use GStreamer pipelines

## Permissions

The app requires:
- `CAMERA` - For video capture
- `RECORD_AUDIO` - For audio capture
- `INTERNET` - For WebRTC signaling
- `ACCESS_NETWORK_STATE` - For network monitoring

## Troubleshooting

### Camera not working
- Ensure permissions are granted
- Check if camera is in use by another app
- Try switching between front/back camera

### Connection issues
- Verify signaling server is running
- Check firewall settings
- Ensure correct server URL (use device IP for physical devices)

### No video on web client
- Check WebRTC peer connection state
- Verify ICE candidates are exchanged
- Check browser console for errors

## Architecture

```
MainActivity
    ├── WebRTCClient (handles video/audio capture and peer connections)
    └── SignalingClient (handles WebSocket communication)
```

## Next Steps

- Add GStreamer pipeline for advanced video processing
- Implement custom video effects
- Add recording functionality
- Support multiple simultaneous streams
