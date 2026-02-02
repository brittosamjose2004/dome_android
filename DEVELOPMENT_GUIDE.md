# 🚀 Complete Project Setup Guide

Step-by-step guide to set up and develop the entire WebRTC Android Streaming Platform.

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Overview](#project-overview)
3. [Setup Steps](#setup-steps)
4. [Development Workflow](#development-workflow)
5. [Testing the Complete System](#testing-the-complete-system)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

#### 1. Node.js and npm
```bash
# Check if installed
node --version    # Should be v18 or higher
npm --version

# Install on Ubuntu/Debian
sudo apt update
sudo apt install nodejs npm

# Install on MacOS
brew install node

# Install on Windows
# Download from https://nodejs.org/
```

#### 2. Java JDK (for Android)
```bash
# Check if installed
java -version    # Should be 11 or higher

# Install on Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Install on MacOS
brew install openjdk@17

# Install on Windows
# Download from https://adoptium.net/
```

#### 3. Git
```bash
# Check if installed
git --version

# Install on Ubuntu/Debian
sudo apt install git

# Install on MacOS
brew install git
```

#### 4. Android Device or Emulator
- Physical Android device (recommended for camera testing)
- Or Android emulator (limited camera support)

### Optional Software

- **Android Studio** (if you prefer GUI development)
- **VS Code** or any code editor
- **Postman** (for API testing)

---

## Project Overview

The project consists of 3 main components:

```
dome_android/
├── signaling-server/    # Node.js WebSocket server (Port 3000)
├── web-client/          # React web application (Port 3001)
└── android-app/         # Android streaming application
```

**Communication Flow:**
```
Android App → WebSocket → Signaling Server ← WebSocket ← Web Client
            ↓                                            ↑
            └────────── WebRTC Data Channel ────────────┘
```

---

## Setup Steps

### Phase 1: Clone and Prepare

#### Step 1: Clone Repository (if not already done)

```bash
# If starting fresh
git clone https://github.com/brittosamjose2004/dome_android.git
cd dome_android
```

#### Step 2: Review Project Structure

```bash
# View the structure
tree -L 2

# Expected output:
# dome_android/
# ├── signaling-server/
# ├── web-client/
# ├── android-app/
# ├── README.md
# ├── SETUP.md
# └── docker-compose.yml
```

---

### Phase 2: Setup Signaling Server

#### Step 3: Install Signaling Server Dependencies

```bash
cd signaling-server

# Install dependencies
npm install

# Expected time: 1-2 minutes
```

#### Step 4: Configure Signaling Server

```bash
# Copy environment template
cp .env.example .env

# Edit configuration (optional)
nano .env
```

**Edit `.env`:**
```env
PORT=3000
PUBLIC_URL=http://localhost:3000
# For production, use your public domain:
# PUBLIC_URL=https://your-domain.com
```

#### Step 5: Test Signaling Server

```bash
# Start the server
npm start

# You should see:
# Signaling server running on port 3000
# WebSocket endpoint: ws://localhost:3000
# Health check: http://localhost:3000/api/health
```

**Keep this terminal open!**

#### Step 6: Verify Server is Running

Open another terminal:
```bash
# Test health endpoint
curl http://localhost:3000/api/health

# Expected response:
# {"status":"ok","activeClients":0,"activeStreams":0}
```

✅ **Signaling Server is now running!**

---

### Phase 3: Setup Web Client

#### Step 7: Install Web Client Dependencies

```bash
# Open new terminal
cd web-client

# Install dependencies
npm install

# Expected time: 2-3 minutes
```

#### Step 8: Configure Web Client (Optional)

```bash
# Create .env file (optional)
nano .env
```

**Edit `.env` (optional):**
```env
REACT_APP_SIGNALING_SERVER=ws://localhost:3000
REACT_APP_API_URL=http://localhost:3000
```

#### Step 9: Start Web Client

```bash
# Start development server
npm start

# Should automatically open: http://localhost:3001
```

**Keep this terminal open!**

#### Step 10: Verify Web Client

- Browser should open automatically to http://localhost:3001
- You should see "WebRTC Stream Viewer" page
- Should show "No Active Streams" (expected, no streams yet)

✅ **Web Client is now running!**

---

### Phase 4: Setup Android App

#### Step 11: Determine Your Computer's IP Address

You need your computer's IP address for the Android app to connect.

**On Linux/Mac:**
```bash
# Get your local IP
ip addr show | grep "inet " | grep -v 127.0.0.1

# Or
ifconfig | grep "inet " | grep -v 127.0.0.1

# Example output: 192.168.1.100
```

**On Windows:**
```cmd
ipconfig

# Look for "IPv4 Address" under your active network adapter
# Example: 192.168.1.100
```

**Write down your IP address:** `________________`

#### Step 12: Update Android App Configuration

```bash
cd android-app/app/src/main/java/com/example/webrtcstreamer
nano MainActivity.kt
```

**Find and update line ~40:**
```kotlin
// BEFORE:
val signalingServerUrl = "ws://10.0.2.2:3000"

// AFTER (replace with YOUR IP):
val signalingServerUrl = "ws://192.168.1.100:3000"
```

**Important:**
- Use `ws://10.0.2.2:3000` if using Android Emulator
- Use `ws://YOUR_IP:3000` if using physical Android device
- Both computer and Android device must be on the same WiFi network

#### Step 13: Setup Android SDK

```bash
cd /workspaces/dome_android/android-app

# Install Android SDK (first time only)
./setup-sdk.sh

# Expected time: 2-5 minutes
# Will download and configure Android SDK
```

#### Step 14: Build Android App

```bash
# Build debug APK
./build.sh debug

# Expected time: 5-10 minutes (first build)
# Subsequent builds: 30 seconds - 2 minutes
```

**Expected output:**
```
✅ Build successful!
APK location: app/build/outputs/apk/debug/app-debug.apk
APK size: ~25M
```

✅ **Android App is built!**

---

### Phase 5: Install on Android Device

#### Step 15: Prepare Android Device

**Enable Developer Options:**
1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. You'll see "You are now a developer!"

**Enable USB Debugging:**
1. Go to **Settings** → **Developer Options**
2. Enable **USB Debugging**
3. Enable **Install via USB** (if available)

#### Step 16: Connect Device

```bash
# Connect device via USB cable

# Verify connection
adb devices

# Expected output:
# List of devices attached
# ABC123456789    device
```

If you see "unauthorized", accept the USB debugging prompt on your device.

#### Step 17: Install APK

**Option A: Automated Install**
```bash
./build.sh install
```

**Option B: Manual ADB Install**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Option C: Manual File Transfer**
```bash
# Copy APK to device
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/

# Then install via file manager on device
```

#### Step 18: Grant Permissions

1. Open the app on your device
2. Grant **Camera** permission
3. Grant **Microphone** permission
4. Grant **Internet** permission (auto-granted)

✅ **Android App is installed!**

---

## Testing the Complete System

### Step 19: Start Streaming from Android

1. **Open the app** on your Android device
2. **Check the Server URL** field shows: `ws://YOUR_IP:3000`
3. **Tap "Start Streaming"** button
4. **Allow camera/microphone** if prompted

**Expected:**
- Status changes to "Streaming"
- You see yourself in the camera preview
- Stream ID and Embed URL appear

### Step 20: View Stream on Web Client

1. **Open web browser** on your computer: http://localhost:3001
2. **You should see** your stream listed with "🔴 LIVE" badge
3. **Click "▶️ Watch Stream"** button
4. **Video should appear** in the browser

✅ **Complete system is working!**

### Step 21: Test Embed URL

1. **Copy the embed URL** from the Android app or web client
2. **Create a test HTML file:**

```html
<!DOCTYPE html>
<html>
<head>
    <title>Embedded Stream Test</title>
</head>
<body>
    <h1>My Embedded Stream</h1>
    <iframe 
        src="http://localhost:3001?streamId=YOUR_STREAM_ID" 
        width="640" 
        height="480" 
        frameborder="0" 
        allowfullscreen>
    </iframe>
</body>
</html>
```

3. **Open in browser** to verify embedding works

✅ **Embedding works!**

---

## Development Workflow

### Daily Development Routine

#### 1. Start Backend Services

```bash
# Terminal 1: Signaling Server
cd signaling-server
npm start

# Terminal 2: Web Client  
cd web-client
npm start
```

#### 2. Develop Android App

**Option A: Command Line**
```bash
cd android-app

# Make changes to code...

# Rebuild and install
./build.sh install
```

**Option B: Android Studio**
```bash
# Open Android Studio
# File → Open → Select android-app folder
# Click Run button
```

#### 3. Test Changes

1. Restart Android app
2. Start streaming
3. View in web client
4. Verify functionality

### Making Changes

#### Backend Changes (Signaling Server)

```bash
cd signaling-server

# Edit files
nano server.js

# Restart server (Ctrl+C then npm start)
npm start
```

#### Frontend Changes (Web Client)

```bash
cd web-client

# Edit files
nano src/App.js

# Changes auto-reload (React hot reload)
```

#### Android Changes

```bash
cd android-app

# Edit files
nano app/src/main/java/com/example/webrtcstreamer/MainActivity.kt

# Rebuild
./build.sh install
```

---

## Network Configuration

### For Local Network (Same WiFi)

**Signaling Server:** Keep as `http://localhost:3000` on your computer

**Android App:** Use `ws://YOUR_COMPUTER_IP:3000`

**Web Client:** Keep as `http://localhost:3001` on your computer

### For Public Access

#### 1. Deploy Signaling Server

**Using Heroku:**
```bash
cd signaling-server
heroku create your-app-name
git push heroku main
```

**Using your own server:**
```bash
# SSH to server
ssh user@your-server.com

# Clone and setup
git clone <repo>
cd dome_android/signaling-server
npm install
PORT=3000 npm start
```

#### 2. Update URLs

**Android App:**
```kotlin
val signalingServerUrl = "wss://your-domain.com"  // Note: wss not ws
```

**Web Client .env:**
```env
REACT_APP_SIGNALING_SERVER=wss://your-domain.com
REACT_APP_API_URL=https://your-domain.com
```

#### 3. Setup SSL/TLS

For production, you need HTTPS/WSS:
```bash
# Use Let's Encrypt
sudo certbot certonly --standalone -d your-domain.com
```

---

## Troubleshooting

### Signaling Server Issues

**Port already in use:**
```bash
# Find process
lsof -i :3000

# Kill process
kill -9 <PID>
```

**Cannot connect from Android:**
```bash
# Check firewall
sudo ufw status
sudo ufw allow 3000/tcp

# Verify server is running
curl http://localhost:3000/api/health
```

### Web Client Issues

**Port 3001 in use:**
```bash
# Kill process on port 3001
lsof -i :3001
kill -9 <PID>

# Or use different port
PORT=3002 npm start
```

**Cannot see streams:**
- Check browser console (F12) for errors
- Verify signaling server is running
- Check CORS settings

### Android App Issues

**Build fails:**
```bash
# Clean build
./gradlew clean

# Refresh dependencies
./gradlew build --refresh-dependencies
```

**Cannot connect to server:**
- Verify both devices on same WiFi
- Check firewall allows port 3000
- Use correct IP address (not 127.0.0.1)
- Try pinging: `ping YOUR_COMPUTER_IP`

**No camera:**
- Grant camera permission in Android settings
- Check if another app is using camera
- Restart device

**Connection drops:**
- Add TURN server configuration
- Check network stability
- Verify WebRTC peer connection state

### Common Errors

**"EADDRINUSE" error:**
- Port already in use
- Kill the process or use different port

**"Module not found" error:**
- Run `npm install` again
- Delete `node_modules` and reinstall

**"ANDROID_HOME not set":**
- Run `./setup-sdk.sh`
- Or manually set: `export ANDROID_HOME=$HOME/Android/Sdk`

---

## Quick Reference Commands

### Start Everything
```bash
# Terminal 1: Signaling Server
cd signaling-server && npm start

# Terminal 2: Web Client
cd web-client && npm start

# Terminal 3: Build Android
cd android-app && ./build.sh install
```

### Stop Everything
```bash
# Press Ctrl+C in each terminal
# Or kill all Node processes:
killall node
```

### Check Status
```bash
# Check signaling server
curl http://localhost:3000/api/health

# Check web client
curl http://localhost:3001

# Check Android device
adb devices
```

---

## Next Steps After Setup

1. ✅ **Customize UI** - Update colors, logos, branding
2. ✅ **Add Authentication** - Secure your streams
3. ✅ **Setup Analytics** - Track usage and performance
4. ✅ **Add Recording** - Save streams to storage
5. ✅ **Deploy to Production** - Use cloud hosting
6. ✅ **Add TURN Server** - Better connectivity
7. ✅ **Create Admin Panel** - Manage streams
8. ✅ **Mobile Web Client** - Responsive design
9. ✅ **Add Chat Feature** - Real-time messaging
10. ✅ **Multi-stream Support** - Multiple simultaneous streams

---

## Development Resources

- **WebRTC:** https://webrtc.org/
- **React Docs:** https://react.dev/
- **Node.js Docs:** https://nodejs.org/docs/
- **Android Docs:** https://developer.android.com/
- **GStreamer:** https://gstreamer.freedesktop.org/

---

## Summary Checklist

Before you start developing, ensure:

- [ ] Node.js installed (v18+)
- [ ] Java JDK installed (11+)
- [ ] Git installed
- [ ] Signaling server running on port 3000
- [ ] Web client running on port 3001
- [ ] Android SDK configured
- [ ] Android app built successfully
- [ ] Android app installed on device
- [ ] Camera and microphone permissions granted
- [ ] Computer and Android on same WiFi network
- [ ] Stream appears in web client
- [ ] Embed URL works

**If all checked ✅ - You're ready to develop!** 🎉

---

For detailed documentation:
- [README.md](../README.md) - Project overview
- [SETUP.md](../SETUP.md) - Detailed setup guide
- [android-app/QUICKSTART.md](android-app/QUICKSTART.md) - Android quick start
- [android-app/BUILD_WITHOUT_STUDIO.md](android-app/BUILD_WITHOUT_STUDIO.md) - Build guide
