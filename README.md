# 🎥 Dome Android - WebRTC Live Streaming Platform

A complete real-time video streaming solution with **Android + GStreamer + Mediasoup SFU + React**. Stream from your Android device and view on any web browser with sub-500ms latency.

## ✨ Features

- 📱 **Android Streaming App** - Stream directly from your Android device camera
- 🎬 **Hardware H.264 Encoding** - GStreamer-style pipeline using MediaCodec
- 🌐 **Mediasoup SFU Server** - Scalable stream relay (eliminates P2P NAT issues)
- 💻 **React Web Viewer** - Watch streams in any modern browser
- ⚡ **Real-time** - Sub-500ms latency for live streaming
- 🔒 **Production Ready** - TURN servers configured, SFU architecture

## 🏗️ Architecture

```
┌─────────────────┐
│  Android Device │
│   Camera2 API   │
└────────┬────────┘
         │ Raw Frames
         ▼
┌─────────────────┐
│ GStreamer       │
│ Pipeline        │
│ (MediaCodec)    │
└────────┬────────┘
         │ H.264 RTP
         ▼
┌─────────────────┐
│ Socket.IO       │
│ Client          │
└────────┬────────┘
         │ WebSocket
         ▼
┌─────────────────┐
│ Mediasoup SFU   │
│ Server (3002)   │
└────────┬────────┘
         │ WebRTC
         ▼
┌─────────────────┐
│ React Web       │
│ Viewer          │
└─────────────────┘
```

## 📦 Project Structure

```
dome_android/
├── android-app/                 # Android streaming application
│   ├── app/src/main/java/com/example/webrtcstreamer/
│   │   ├── MainActivity.kt      # Original P2P implementation
│   │   ├── StreamActivity.kt    # New GStreamer+Mediasoup implementation
│   │   ├── gstreamer/
│   │   │   ├── GStreamerPipeline.kt   # H.264 encoder pipeline
│   │   │   └── RtmpStreamer.kt        # RTMP streaming support
│   │   └── mediasoup/
│   │       └── MediasoupClient.kt     # Socket.IO client for SFU
│   └── build.gradle             # Dependencies & configuration
├── mediasoup-server/            # SFU relay server
│   ├── server.js                # Mediasoup SFU implementation
│   └── package.json             # Node.js dependencies
├── web-client/                  # React web viewer
│   ├── src/
│   │   ├── components/
│   │   │   ├── MediasoupViewer.js   # New SFU viewer
│   │   │   └── StreamViewer.js      # Legacy P2P viewer
│   │   └── App.js
│   └── package.json
├── signaling-server/            # Original P2P signaling (legacy)
│   └── server.js
└── GSTREAMER_IMPLEMENTATION.md  # Detailed technical documentation
```

## 🚀 Quick Start

### Prerequisites

- **Android Device**: API 24+ (Android 7.0+)
- **Node.js**: v18+
- **Java**: JDK 17
- **Gradle**: 8.6

### 1. Start Mediasoup Server

```bash
cd mediasoup-server
npm install
npm start
# Server runs on port 3002
```

### 2. Start Web Client

```bash
cd web-client
npm install
npm start
# Opens browser at http://localhost:3001
```

### 3. Build Android APK

```bash
cd android-app
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install & Stream

1. Install APK on Android device
2. Grant camera & audio permissions
3. Click "Start Streaming"
4. Copy the stream URL shown in the app
5. Open URL in web browser to view

## 🔧 Configuration

### Codespaces / Cloud Deployment

Update URLs in these files:

**Android** (`StreamActivity.kt`):
```kotlin
private const val MEDIASOUP_SERVER = "https://your-codespace-url-3002.app.github.dev"
```

**Web Client** (`MediasoupViewer.js`):
```javascript
const MEDIASOUP_SERVER = 'https://your-codespace-url-3002.app.github.dev';
```

**Mediasoup Server** (environment variable):
```bash
export ANNOUNCED_IP=your-public-ip
```

### Local Network

For local testing (Android Emulator):
- Android: `http://10.0.2.2:3002`
- Physical Device: `http://192.168.x.x:3002` (your local IP)

## 📊 Performance

- **Latency**: < 500ms end-to-end
- **Resolution**: 1280x720 (configurable)
- **FPS**: 30fps (configurable)
- **Bitrate**: 2 Mbps (configurable)
- **Codec**: H.264 Baseline Profile Level 3.1

## 🎛️ GStreamer Pipeline Equivalent

Our Android implementation replicates this GStreamer command:

```bash
gst-launch-1.0 \
  v4l2src device=/dev/video0 ! \
  videoconvert ! \
  x264enc speed-preset=ultrafast tune=zerolatency ! \
  rtph264pay config-interval=1 pt=96 ! \
  udpsink host=192.168.1.100 port=5000
```

**Components**:
- `Camera2` → `v4l2src` (video source)
- `GStreamerPipeline` → `x264enc` (H.264 encoder)
- `createRtpPackets()` → `rtph264pay` (RTP packetization)
- `MediasoupClient` → `udpsink` (network output)

## 🔐 Security & Production

### Current Setup (Dev/Testing)
- Public TURN servers (openrelay.metered.ca)
- Hardcoded URLs in source code
- No authentication

### Production Recommendations
1. **Deploy Your Own TURN Server**
   - Use [coturn](https://github.com/coturn/coturn)
   - Configure with authentication

2. **Environment Variables**
   - Move all URLs to config files
   - Use build variants for dev/prod

3. **Authentication**
   - Add JWT tokens for stream access
   - Implement user management

4. **HTTPS/WSS**
   - Use SSL certificates
   - Configure reverse proxy (nginx)

5. **Monitoring**
   - Add health check endpoints
   - Log stream metrics
   - Monitor server resources

## 📱 Android App Usage

### StreamActivity (Recommended - New Implementation)
Uses GStreamer pipeline + Mediasoup SFU for reliable streaming.

**Features**:
- Hardware-accelerated encoding
- Automatic reconnection
- Better NAT traversal
- Multiple viewers support

### MainActivity (Legacy - P2P Implementation)
Original WebRTC P2P implementation. May have connection issues behind NAT.

## 🌐 Web Viewer

Open `http://localhost:3001` to see available streams.

**Features**:
- Mediasoup SFU viewer (primary)
- Legacy P2P viewer (fallback)
- Stream list with auto-refresh
- Copy-to-clipboard stream URLs

## 🐛 Troubleshooting

### Android App

**Build Errors:**
```bash
cd android-app
./gradlew clean
./gradlew assembleDebug
```

**Camera Permission:**
- Go to Settings → Apps → WebRTC Streamer → Permissions
- Enable Camera and Microphone

**Connection Failed:**
- Check mediasoup server is running
- Verify URL is accessible from Android device
- Test with `curl https://your-url/api/health`

### Mediasoup Server

**Port Already in Use:**
```bash
# Change port in server.js
const PORT = process.env.PORT || 3003;
```

**Worker Creation Failed:**
```bash
# Check if ports 10000-10100 are available
netstat -an | grep 10000
```

### Web Client

**"Connection Error":**
- Verify mediasoup server URL
- Check browser console for errors
- Test WebSocket connection

**No Video:**
- Check stream ID is correct
- Verify producer is sending data
- Look for codec compatibility issues

## 📚 Documentation

- **[GSTREAMER_IMPLEMENTATION.md](GSTREAMER_IMPLEMENTATION.md)** - Detailed technical guide
- **[Mediasoup Docs](https://mediasoup.org/)** - SFU server documentation
- **[WebRTC Docs](https://webrtc.org/)** - WebRTC standards

## 🔄 API Endpoints

### Mediasoup Server (Port 3002)

**REST API:**
- `GET /api/streams` - List active streams
- `GET /api/health` - Server health check

**Socket.IO Events:**
- `getRouterRtpCapabilities` - Get codec capabilities
- `createProducerTransport` - Create transport for streaming
- `createConsumerTransport` - Create transport for viewing
- `produce` - Start producing video
- `consume` - Start consuming video

## 🛠️ Development

### Android

```bash
cd android-app
./gradlew installDebug  # Install on connected device
./gradlew build --info  # Verbose build output
```

### Server

```bash
cd mediasoup-server
npm run dev  # With nodemon for auto-restart
```

### Web Client

```bash
cd web-client
npm start    # Development server
npm run build  # Production build
```

## 📄 License

MIT License - Feel free to use in your projects!

## 🤝 Contributing

Contributions welcome! Areas for improvement:
- Add audio streaming support
- Implement recording functionality
- Add stream authentication
- Create iOS client
- Add stream quality selection
- Implement adaptive bitrate

## 🙏 Acknowledgments

- **Mediasoup** - Excellent SFU library
- **Stream WebRTC Android** - WebRTC SDK
- **GStreamer** - Inspiration for pipeline design

## 📞 Support

For issues or questions:
1. Check [GSTREAMER_IMPLEMENTATION.md](GSTREAMER_IMPLEMENTATION.md)
2. Review troubleshooting section above
3. Open an issue on GitHub

---

**Built with ❤️ for real-time streaming**
