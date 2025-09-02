# Streamify Security & Privacy Information

## Why might antivirus software flag this app?

Streamify uses several Android capabilities that are commonly found in legitimate remote access tools but can also be misused by malicious software:

### Capabilities that may trigger warnings:
1. **Screen Recording** - MediaProjection API
2. **Accessibility Service** - For touch simulation
3. **Network Communication** - TCP sockets for VNC protocol
4. **System-level Touch Injection** - For remote control

## Security Measures Implemented:

### 🔒 **Network Security**
- Only accepts connections from local network addresses (192.168.x.x, 10.x.x.x, etc.)
- No internet communication required
- All data stays on your local Wi-Fi network
- Clear network security configuration

### 🛡️ **Permission Transparency**
- Detailed permission explanations in app
- Privacy policy clearly states no data collection
- Accessibility service only for touch simulation
- User controls when to enable/disable features

### 📱 **App Integrity**
- Open source code (available for review)
- Debug build with full logging for transparency
- No obfuscation or hidden functionality
- Educational/development project

## To reduce false positives:

### For Android:
1. Add app to antivirus whitelist
2. Enable "Developer mode" to allow debug apps
3. Disable real-time scanning temporarily during installation

### For PC antivirus:
1. Add the APK file to exclusion list
2. Whitelist the entire project folder
3. Use "trust this application" option

## Verification:
- Check app permissions in Android Settings
- Review accessibility service description
- Monitor network traffic (stays local only)
- Examine source code for transparency

This app is designed for legitimate screen sharing between Android devices on the same network, similar to TeamViewer or VNC viewers, but specifically for Android-to-Android communication.
