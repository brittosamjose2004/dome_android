# WebRTC Streamer - Deployment Guide

## Build Summary

✅ **APK Successfully Built!**
- Location: `android-app/app/build/outputs/apk/debug/app-debug.apk`
- Size: 47MB
- Build Type: Debug
- Target SDK: API 34 (Android 14)
- Min SDK: API 24 (Android 7.0)

## Download APK

You can download the APK to your local machine using one of these methods:

### Method 1: Using GitHub Codespace UI
1. Navigate to `android-app/app/build/outputs/apk/debug/`
2. Right-click on `app-debug.apk`
3. Select "Download"

### Method 2: Using gh CLI
```bash
gh codespace cp remote:./android-app/app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk
```

## Installation on Android Device

### Enable Developer Options
1. Go to **Settings** → **About phone**
2. Tap **Build number** 7 times
3. Go back to **Settings** → **Developer options**
4. Enable **USB debugging**

### Install APK
```bash
# Connect your device via USB
adb install app-debug.apk
```

Or transfer the APK to your phone and install manually (you'll need to allow installation from unknown sources).

## Server Configuration

### For Testing from Android Device

Since your servers are running in a Codespace, you need to make them accessible:

#### Option 1: Port Forwarding (Recommended)
1. In VS Code, go to **Ports** tab
2. Make port 3000 (signaling server) **Public**
3. Note the forwarded URL (e.g., `https://xxxx-3000.app.github.dev`)

#### Option 2: Use Ngrok
```bash
# Install ngrok
npm install -g ngrok

# Forward port 3000
ngrok http 3000
```

### Update Android App Server URL

Currently, the app is configured to connect to `ws://10.0.2.2:3000` (Android emulator localhost).

To connect to your forwarded server:
1. Get your public URL from Ports tab
2. The app will need to be rebuilt with the new URL

**To change the server URL**, edit `MainActivity.kt` and rebuild:
```kotlin
private val SIGNALING_SERVER_URL = "wss://your-forwarded-url.app.github.dev"
```

## Testing the App

### 1. Start Servers
```bash
# Terminal 1: Start signaling server
cd signaling-server
npm start

# Terminal 2: Start web client
cd web-client
npm start
```

### 2. Launch Android App
- Open the app on your Android device
- It will request camera and microphone permissions
- Grant all permissions

### 3. View Stream on Web
- Open the web client URL in a browser
- You should see your Android stream listed
- Click to view the stream

## Architecture Overview

```
┌─────────────────┐         WebSocket          ┌──────────────────┐
│                 │◄─────────────────────────►│                  │
│  Android App    │      ICE Candidates       │ Signaling Server │
│  (Streamer)     │         SDP Offer         │   (Node.js)      │
│                 │        SDP Answer         │                  │
└─────────────────┘                           └──────────────────┘
        │                                             ▲
        │                                             │
        │         WebRTC Media Stream                 │
        │          (Direct P2P)                       │
        │                                             │
        ▼                                             │
┌─────────────────┐         WebSocket          ┌─────┴────────────┐
│                 │◄─────────────────────────►│                  │
│  Web Client     │      ICE Candidates       │                  │
│  (Viewer)       │         SDP Offer         │                  │
│  React App      │        SDP Answer         │                  │
└─────────────────┘                           └──────────────────┘
```

## Embedding Streams

Once a stream is active, you can embed it in any website:

```html
<iframe 
  src="https://your-web-client.com/stream/STREAM_ID"
  width="640" 
  height="480" 
  frameborder="0" 
  allowfullscreen>
</iframe>
```

## Troubleshooting

### App Won't Connect
- Check signaling server is running
- Verify the server URL in MainActivity.kt
- Ensure port 3000 is publicly accessible
- Check Android device has internet connection

### No Camera Permission
- Go to Settings → Apps → WebRTC Streamer → Permissions
- Enable Camera and Microphone

### Stream Not Showing in Web Client
- Check browser console for errors
- Verify WebSocket connection is established
- Check that ICE candidates are being exchanged

### Poor Video Quality
- Adjust resolution in WebRTCClient.kt:
  ```kotlin
  videoCapturer!!.startCapture(1280, 720, 30)  // width, height, fps
  ```

## Next Steps

### For Production
1. **Use Release Build**
   ```bash
   ./gradlew assembleRelease
   ```

2. **Sign the APK**
   - Generate a keystore
   - Configure signing in build.gradle
   - Build signed release APK

3. **Deploy Signaling Server**
   - Deploy to cloud (AWS, Heroku, etc.)
   - Use HTTPS/WSS for security
   - Add authentication

4. **Configure TURN Servers**
   - Add TURN servers for NAT traversal
   - Update RTCConfiguration in WebRTCClient.kt

### Improvements
- Add error handling and retry logic
- Implement stream quality selection
- Add chat/messaging features
- Support multiple cameras
- Add recording functionality
- Implement authentication/authorization

## Support

For issues or questions:
1. Check logs: `adb logcat | grep WebRTC`
2. Review signaling server logs
3. Check browser console in web client
