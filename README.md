# WebRTC Android Streaming Platform

A complete WebRTC streaming solution for Android devices with Node.js signaling server and React web client. Stream video from your Android device to any web browser with embeddable player support.

## 🎯 Features

- **Android App**: Native Android application with WebRTC support
- **Node.js Signaling Server**: WebSocket-based signaling for WebRTC connections
- **React Web Client**: Beautiful web interface for viewing streams
- **Embeddable Player**: Get public URLs to embed streams in any website
- **GStreamer Ready**: Architecture supports GStreamer integration for advanced video processing

## 📁 Project Structure

```
dome_android/
├── android-app/          # Android application (Kotlin)
├── signaling-server/     # Node.js WebSocket signaling server
├── web-client/          # React web viewer application
└── README.md
```

## 🚀 Quick Start

### 1. Start the Signaling Server

```bash
cd signaling-server
npm install
npm start
```

Server will run on http://localhost:3000

### 2. Start the Web Client

```bash
cd web-client
npm install
npm start
```

Web client will open at http://localhost:3001

### 3. Build Android App

1. Open Android Studio
2. Open the `android-app` directory
3. Update signaling server URL in `MainActivity.kt`
4. Build and run on your Android device

## 📱 Android Application

### Requirements
- Android Studio (latest version)
- Android SDK API 24+
- Physical Android device (recommended)

### Configuration

Update the signaling server URL in [MainActivity.kt](android-app/app/src/main/java/com/example/webrtcstreamer/MainActivity.kt):

```kotlin
val signalingServerUrl = "ws://YOUR_SERVER_IP:3000"
```

**For Android Emulator**: Use `ws://10.0.2.2:3000`  
**For Physical Device**: Use your computer's IP or public server URL

### Permissions
- Camera
- Microphone
- Internet access
- Network state

## 🖥️ Signaling Server

WebSocket server handling WebRTC signaling between Android streamers and web viewers.

### API Endpoints

- `GET /api/health` - Server health check
- `GET /api/streams` - List active streams
- WebSocket: `ws://localhost:3000`

### Environment Variables

Create `.env` file:
```env
PORT=3000
PUBLIC_URL=http://localhost:3000
```

## 🌐 Web Client

React-based web application for viewing streams.

### Features
- Browse active streams
- Watch live streams
- Get embed codes
- Responsive design
- Real-time status updates

### Environment Variables

Create `.env` file in `web-client/`:
```env
REACT_APP_SIGNALING_SERVER=ws://localhost:3000
REACT_APP_API_URL=http://localhost:3000
```

## 🎥 How It Works

1. **Android App** captures video/audio using WebRTC
2. **Signaling Server** coordinates connections between streamers and viewers
3. **Web Client** receives and displays the stream
4. **ICE Candidates** are exchanged for NAT traversal
5. **Peer-to-peer** connection established via WebRTC

```
┌─────────────────┐      WebSocket      ┌──────────────────┐
│  Android App    │◄────────────────────►│ Signaling Server │
│  (Streamer)     │                      │   (Node.js)      │
└─────────────────┘                      └──────────────────┘
         │                                         ▲
         │                                         │
         │        WebRTC Data Channel              │
         │                                         │
         └────────────────────────────────────────►│
                                          ┌────────┴────────┐
                                          │   Web Client    │
                                          │    (React)      │
                                          └─────────────────┘
```

## 🔧 Development

### Testing Locally

1. Start signaling server
2. Start web client
3. Run Android app on emulator or device
4. Access web client to view the stream

### Network Configuration

**Same Network**:
- Find your computer's local IP: `ipconfig` (Windows) or `ifconfig` (Linux/Mac)
- Use `ws://YOUR_LOCAL_IP:3000` in Android app

**Public Access**:
- Deploy signaling server to cloud (Heroku, AWS, etc.)
- Update URLs in Android app and web client
- Ensure proper HTTPS/WSS configuration

## 🌍 Embedding Streams

Once streaming, get the embed code from the web client:

```html
<iframe 
  src="http://your-domain.com?streamId=YOUR_STREAM_ID" 
  width="640" 
  height="480" 
  frameborder="0" 
  allowfullscreen>
</iframe>
```

## 🔐 Security Considerations

- Use HTTPS/WSS in production
- Implement authentication for streamers
- Add CORS restrictions
- Use TURN servers for better connectivity
- Implement stream access controls

## 📦 Production Deployment

### Signaling Server
```bash
cd signaling-server
npm install --production
NODE_ENV=production npm start
```

### Web Client
```bash
cd web-client
npm run build
# Deploy build/ folder to static hosting
```

### Android App
1. Generate signed APK in Android Studio
2. Distribute via Google Play Store or direct APK

## 🛠️ Advanced: GStreamer Integration

To use GStreamer for advanced video processing:

1. Download [GStreamer Android binaries](https://gstreamer.freedesktop.org/download/)
2. Extract to `android-app/app/src/main/jni/gstreamer/`
3. Update `build.gradle` with native library paths
4. Modify `WebRTCClient.kt` to use GStreamer pipelines

Example GStreamer pipeline:
```
videotestsrc ! video/x-raw,width=1280,height=720 ! videoconvert ! webrtcbin
```

## 📚 Documentation

- [Android App README](android-app/README.md)
- [Signaling Server](signaling-server/)
- [Web Client README](web-client/README.md)

## 🐛 Troubleshooting

### Android app can't connect
- Check server URL is correct
- Verify server is running
- Check firewall settings
- Use device IP instead of localhost

### No video in web client
- Check browser console for errors
- Verify WebRTC is supported (Chrome, Firefox, Safari)
- Check camera permissions in Android
- Verify ICE candidates are exchanged

### Connection drops frequently
- Use TURN server for better NAT traversal
- Check network stability
- Increase WebSocket timeout
- Monitor peer connection state

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

MIT License

## 🙋 Support

For issues and questions:
- Create an issue in the repository
- Check existing documentation
- Review troubleshooting section

## 🔄 Next Steps

- [ ] Add recording functionality
- [ ] Implement authentication
- [ ] Add multiple stream support
- [ ] Create admin dashboard
- [ ] Add chat functionality
- [ ] Implement screen sharing
- [ ] Add video effects and filters
- [ ] Create mobile web client
- [ ] Add analytics and monitoring

---

Built with ❤️ using WebRTC, Node.js, React, and Android