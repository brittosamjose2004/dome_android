# Setup and Installation Guide

## Prerequisites

- Node.js 18+ and npm
- Android Studio (latest version)
- Git
- (Optional) Docker and Docker Compose

## Installation Steps

### 1. Clone the Repository

```bash
git clone <repository-url>
cd dome_android
```

### 2. Setup Signaling Server

```bash
cd signaling-server
npm install
cp .env.example .env
# Edit .env with your configuration
npm start
```

The server will start on http://localhost:3000

### 3. Setup Web Client

```bash
cd web-client
npm install
npm start
```

The web client will open at http://localhost:3001

### 4. Setup Android App

#### Option A: Android Studio
1. Open Android Studio
2. File → Open → Select `android-app` directory
3. Wait for Gradle sync to complete
4. Update signaling server URL in `MainActivity.kt`:
   ```kotlin
   val signalingServerUrl = "ws://YOUR_IP:3000"
   ```
5. Connect Android device via USB
6. Click Run button

#### Option B: Command Line
```bash
cd android-app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

### Network Configuration

#### For Local Development

1. Find your computer's IP address:
   - Windows: `ipconfig`
   - Mac/Linux: `ifconfig` or `ip addr`

2. Update Android app:
   ```kotlin
   val signalingServerUrl = "ws://192.168.1.XXX:3000"
   ```

3. Update web client `.env`:
   ```env
   REACT_APP_SIGNALING_SERVER=ws://192.168.1.XXX:3000
   REACT_APP_API_URL=http://192.168.1.XXX:3000
   ```

#### For Android Emulator

Use special IP `10.0.2.2` to access host machine:
```kotlin
val signalingServerUrl = "ws://10.0.2.2:3000"
```

### Production Deployment

#### Deploy Signaling Server

**Heroku**:
```bash
cd signaling-server
heroku create your-app-name
git push heroku main
```

**AWS/DigitalOcean/etc**:
```bash
# SSH to server
git clone <repo>
cd dome_android/signaling-server
npm install --production
PORT=3000 npm start
# Use PM2 or systemd for process management
```

#### Deploy Web Client

**Netlify/Vercel**:
```bash
cd web-client
npm run build
# Upload build/ folder
```

**Static Hosting**:
```bash
npm run build
# Copy build/ folder to web server
```

## Docker Deployment (Recommended)

### Quick Start with Docker Compose

```bash
# From project root
docker-compose up -d
```

This starts:
- Signaling server on port 3000
- Web client on port 3001

### Individual Docker Builds

**Signaling Server**:
```bash
cd signaling-server
docker build -t webrtc-signaling .
docker run -p 3000:3000 webrtc-signaling
```

**Web Client**:
```bash
cd web-client
docker build -t webrtc-client .
docker run -p 3001:80 webrtc-client
```

## Testing the Setup

### 1. Test Signaling Server

```bash
curl http://localhost:3000/api/health
```

Expected response:
```json
{"status":"ok","activeClients":0,"activeStreams":0}
```

### 2. Test Web Client

Open browser: http://localhost:3001

You should see the stream list page.

### 3. Test Android App

1. Open app on device
2. Enter server URL
3. Tap "Start Streaming"
4. Grant camera/mic permissions
5. Check web client for new stream

### 4. Test End-to-End

1. Start streaming from Android
2. Open web client
3. You should see your stream listed
4. Click "Watch Stream"
5. Video should appear in browser

## Firewall Configuration

### Allow Incoming Connections

**Linux (ufw)**:
```bash
sudo ufw allow 3000/tcp
sudo ufw allow 3001/tcp
```

**Windows Firewall**:
1. Open Windows Defender Firewall
2. Advanced Settings → Inbound Rules
3. New Rule → Port → TCP → 3000, 3001
4. Allow the connection

**Mac**:
```bash
# Usually no configuration needed for local network
```

## SSL/TLS Setup (Production)

### Using Let's Encrypt

```bash
# Install certbot
sudo apt-get install certbot

# Get certificate
sudo certbot certonly --standalone -d your-domain.com

# Update signaling server to use HTTPS
# Update web client URLs to wss:// and https://
```

### Update Android App for HTTPS

```kotlin
val signalingServerUrl = "wss://your-domain.com"
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port
lsof -i :3000  # Mac/Linux
netstat -ano | findstr :3000  # Windows

# Kill process
kill -9 <PID>  # Mac/Linux
taskkill /PID <PID> /F  # Windows
```

### Cannot Connect from Android

1. Verify server is running: `curl http://YOUR_IP:3000/api/health`
2. Check firewall allows port 3000
3. Ensure Android and computer on same network
4. Try using IP address instead of hostname
5. Check Android app logs: `adb logcat`

### WebRTC Connection Fails

1. Check ICE candidates in browser console
2. May need TURN server for restrictive networks
3. Verify STUN server is accessible
4. Check NAT/firewall settings

### Build Errors in Android

```bash
# Clean build
cd android-app
./gradlew clean

# Sync Gradle
./gradlew --refresh-dependencies
```

## Next Steps

After successful setup:

1. ✅ Configure production URLs
2. ✅ Setup authentication
3. ✅ Add TURN server for better connectivity
4. ✅ Implement analytics
5. ✅ Add recording feature
6. ✅ Setup monitoring (PM2, logs)
7. ✅ Configure CDN for web client
8. ✅ Add error tracking (Sentry)

## Support

If you encounter issues:

1. Check logs:
   - Signaling server: `npm start` output
   - Web client: Browser DevTools Console
   - Android: `adb logcat`

2. Verify versions:
   ```bash
   node --version  # Should be 18+
   npm --version
   ```

3. Clear caches:
   ```bash
   npm cache clean --force
   ```

4. Review documentation in each subdirectory

## Resources

- [WebRTC Documentation](https://webrtc.org/)
- [React Documentation](https://react.dev/)
- [Android WebRTC](https://developer.android.com/)
- [Node.js WebSocket](https://www.npmjs.com/package/ws)
