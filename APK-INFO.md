# 📱 Streamify VNC Remote Control APK

## Download APK
**Latest Version:** `Streamify-VNC-Remote-Control-v1.0.apk` (12.7 MB)

## What's New in v1.0 ✨
- ✅ **FIXED:** App crash issues completely resolved
- ✅ **FIXED:** IP address detection now works without special permissions
- ✅ **ADDED:** Full VNC remote control functionality
- ✅ **ADDED:** Touch input simulation via Accessibility Service
- ✅ **ADDED:** Security measures and local network restrictions
- ✅ **ADDED:** Privacy policy and transparency documentation

## Features 🚀
- **VNC Server:** Share your screen over network
- **Remote Control:** Allow other devices to control your screen
- **Touch Simulation:** Real touch events via Accessibility Service
- **Security:** Restricted to local network only (192.168.x.x, 10.x.x.x)
- **Cross-Platform:** Works with any VNC client
- **No Root Required:** Uses accessibility service for touch simulation

## Installation Instructions 📋

### Method 1: Direct Install
1. Download `Streamify-VNC-Remote-Control-v1.0.apk`
2. Enable "Unknown Sources" in Android Settings
3. Install the APK
4. Grant required permissions when prompted

### Method 2: ADB Install
```bash
adb install Streamify-VNC-Remote-Control-v1.0.apk
```

## Setup Guide 🔧

### 1. Enable Accessibility Service
- Go to Settings > Accessibility
- Find "Streamify Touch Input" service
- Enable it to allow remote touch control

### 2. Grant Permissions
The app will request these permissions:
- **Internet:** For VNC server communication
- **Network State:** To detect connection status
- **Notifications:** For server status updates

### 3. Start VNC Server
- Open Streamify app
- Your device IP will be displayed automatically
- Tap "Start VNC Server"
- Server runs on port 5901

### 4. Connect from Another Device
Use any VNC client with:
- **IP:** Your device's IP address (shown in app)
- **Port:** 5901
- **Password:** (none - local network only)

## VNC Clients Recommended 📺
- **Windows:** TightVNC, RealVNC
- **Mac:** Screen Sharing (built-in), RealVNC
- **Linux:** Remmina, TigerVNC
- **Android:** VNC Viewer by RealVNC
- **iOS:** VNC Viewer by RealVNC

## Security 🛡️
- Connections restricted to local network only
- No internet exposure
- Privacy policy included in app
- No data collection or tracking

## Troubleshooting 🔧

### App Won't Start VNC Server
1. Check if accessibility service is enabled
2. Ensure you're connected to WiFi
3. Try restarting the app

### Can't Connect from Another Device
1. Make sure both devices are on same WiFi network
2. Check the IP address shown in the app
3. Ensure port 5901 is not blocked
4. Try disabling firewall temporarily

### Touch Control Not Working
1. Enable Accessibility Service for Streamify
2. Grant all requested permissions
3. Restart the app after enabling accessibility

## Technical Details 📊
- **Target Android:** API 34 (Android 14)
- **Minimum Android:** API 24 (Android 7.0)
- **VNC Protocol:** RFB 3.8
- **Encoding:** RAW (uncompressed)
- **Port:** 5901 (default VNC port + 1)
- **Architecture:** Universal APK

## Support 💬
For issues or questions:
1. Check the troubleshooting section above
2. Review the SECURITY.md file in the repository
3. Create an issue on GitHub repository

---
**Built with ❤️ for remote control enthusiasts**
