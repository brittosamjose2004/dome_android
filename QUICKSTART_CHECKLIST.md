# ⚡ Quick Start Checklist

Fast track to get your WebRTC streaming platform running in 15 minutes!

## 📦 Phase 1: Prerequisites (2 min)

```bash
# Check if you have everything
node --version    # Need v18+  ✓
java --version    # Need 11+   ✓
git --version     # Any version ✓
adb --version     # Optional    ✓
```

❌ **Missing something?** See [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#prerequisites)

---

## 🖥️ Phase 2: Backend Setup (5 min)

### ✅ Step 1: Start Signaling Server

```bash
cd signaling-server
npm install        # 1-2 min
npm start         # Keep running
```

**Test:** http://localhost:3000/api/health should return `{"status":"ok"}`

### ✅ Step 2: Start Web Client

```bash
# New terminal
cd web-client
npm install        # 2-3 min
npm start         # Keep running, opens browser
```

**Test:** http://localhost:3001 should show "No Active Streams"

---

## 📱 Phase 3: Android Setup (8 min)

### ✅ Step 3: Get Your IP Address

```bash
# Linux/Mac
ip addr show | grep "inet " | grep -v 127.0.0.1

# Result example: 192.168.1.100
# Write it down: _______________
```

### ✅ Step 4: Update Android App

Edit [android-app/app/src/main/java/com/example/webrtcstreamer/MainActivity.kt](android-app/app/src/main/java/com/example/webrtcstreamer/MainActivity.kt):

```kotlin
// Line ~40 - Change to YOUR IP:
val signalingServerUrl = "ws://192.168.1.100:3000"  // ← YOUR IP HERE
```

### ✅ Step 5: Setup SDK & Build

```bash
cd android-app
./setup-sdk.sh     # 2-5 min (first time only)
./build.sh debug   # 5-10 min (first time)
```

### ✅ Step 6: Install on Device

**Prepare device:**
- Settings → About → Tap "Build Number" 7 times
- Settings → Developer Options → Enable USB Debugging
- Connect via USB

**Install:**
```bash
./build.sh install
```

---

## 🎥 Phase 4: Test Streaming (2 min)

### ✅ Step 7: Start Stream

1. Open app on Android device
2. Tap "Start Streaming"
3. Grant camera/mic permissions
4. See "Stream ID" appear

### ✅ Step 8: View in Browser

1. Go to http://localhost:3001
2. See your stream listed (🔴 LIVE)
3. Click "Watch Stream"
4. See yourself! 🎉

---

## ✅ Success Checklist

- [ ] Signaling server running (Terminal 1)
- [ ] Web client running (Terminal 2)
- [ ] Android app installed on device
- [ ] App shows "Streaming" status
- [ ] Stream visible in web browser
- [ ] Video plays smoothly
- [ ] Can copy embed URL

**All done? 🎉 You're streaming!**

---

## 🐛 Quick Fixes

**Can't connect from Android?**
```bash
# Check same WiFi network
ping 192.168.1.100  # Your IP

# Check firewall
sudo ufw allow 3000/tcp
```

**Build failed?**
```bash
cd android-app
./gradlew clean
./build.sh debug
```

**Port in use?**
```bash
# Kill processes
killall node
lsof -i :3000
kill -9 <PID>
```

---

## 📚 Full Guides

- **Complete Setup:** [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
- **Android Build:** [android-app/QUICKSTART.md](android-app/QUICKSTART.md)
- **Project Overview:** [README.md](README.md)

---

## 🎯 What's Next?

After successful test:

1. **Customize** - Update branding, colors
2. **Secure** - Add authentication
3. **Deploy** - Move to production server
4. **Enhance** - Add features (chat, recording, etc.)

**Happy Streaming! 📹✨**
