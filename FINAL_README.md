# Streamify VNC - Full Screen Mirror v4.1 FINAL 

**Mirror-Only Screen Sharing Application**

## 📱 Overview
Streamify VNC adalah aplikasi Android untuk **screen mirroring** (view-only) antar perangkat Android menggunakan protokol VNC. Aplikasi ini telah di-optimize untuk **mirror-only mode** tanpa fitur remote control.

## ✨ Features

### 🖥️ Mirror-Only Mode
- **View-only screen sharing** - tidak ada remote control
- **Auto reconnect** yang persistent dan reliable
- **Full screen mirror** tanpa UI elements
- **Server-side control** - semua kontrol hanya di server (HP A)

### 🔧 Technical Features
- VNC Protocol (RFB 3.8) dengan RAW encoding
- TCP socket communication yang robust
- Error handling dan connection recovery
- Android MediaProjection API untuk screen capture
- Jetpack Compose UI yang modern

## 🚀 How to Use

### Server Mode (HP A - Share Screen)
1. **Grant Permission**: Berikan izin screen capture
2. **Start Server**: Klik "Start Mirror Server"
3. **Share Connection**: Bagikan IP address dan port ke HP B
4. **Force Disconnect**: Gunakan button "Force Disconnect All Mirror Clients" untuk memutus semua koneksi

### Client Mode (HP B - View Screen)
1. **Regular Client**: Klik "Mirror Client" untuk mode dengan UI
2. **Full Screen Mirror**: Klik "Full Screen Mirror" untuk mode tanpa UI
3. **Auto Reconnect**: Sistem akan otomatis reconnect jika koneksi terputus
4. **Pure Viewer**: Tidak ada kontrol apapun di client, hanya menampilkan layar

### 🖥️ Full Screen Mirror Mode
- **No UI Elements**: Layar murni tanpa tombol atau kontrol
- **Persistent Full Screen**: Tetap full screen setelah reconnect
- **Auto Reconnect**: Reconnect otomatis tanpa user intervention
- **Server-Only Control**: Disconnect hanya bisa dilakukan dari server

## 📋 System Requirements
- Android 7.0+ (API level 24+)
- Permissions: INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE
- Screen capture permission (untuk server mode)

## 🛠️ Installation
1. Download `Streamify-VNC-FullScreen-Mirror-v4.1-FINAL.apk`
2. Enable "Install from Unknown Sources"
3. Install APK di kedua perangkat
4. Grant semua permissions yang diminta

## 🔧 Configuration
- **Default Port**: 5900 (dapat diubah di settings)
- **Auto Reconnect**: Default ON dan persistent
- **Connection Timeout**: 10 detik
- **Reconnect Interval**: 3 detik

## 📖 Usage Instructions

### First Time Setup
1. Install APK di kedua HP (A dan B)
2. HP A: Pilih "Server Config", grant screen permission, start server
3. HP B: Pilih "Full Screen Mirror", masukkan IP HP A, connect

### Daily Usage
1. HP A: Start server
2. HP B: Connect menggunakan "Full Screen Mirror"
3. HP A: Kontrol disconnect menggunakan "Force Disconnect" button

## 🔒 Security Notes
- Tidak ada authentication (untuk jaringan lokal yang aman)
- Data tidak encrypted (VNC standard)
- Gunakan hanya di jaringan yang terpercaya

## 🐛 Troubleshooting

### Connection Issues
- Pastikan kedua HP di jaringan WiFi yang sama
- Check firewall/security software
- Restart aplikasi jika perlu

### Permission Issues
- Grant semua permissions manual di Settings > Apps
- Untuk screen capture: Settings > Special Access > Display over apps

### Performance Issues
- Tutup aplikasi lain untuk menghemat RAM
- Gunakan WiFi 5GHz untuk koneksi yang lebih stabil

## 📝 Version History

### v4.1 FINAL
- ✅ **Mirror-only mode** - semua fitur remote control dihapus
- ✅ **Full screen mirror** tanpa UI elements
- ✅ **Server-side control** - disconnect hanya dari server
- ✅ **Persistent auto reconnect** - default ON
- ✅ **Improved error handling** dan connection stability
- ✅ **Syntax fixes** dan code optimization

### v4.0 
- Full screen mirror mode implementation
- Auto reconnect default ON
- UI improvements

### v3.0
- Auto reconnect feature
- Error handling improvements

### v2.0
- Basic VNC functionality
- Client-server architecture

## 🔗 Development Info
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose  
- **Build System**: Gradle
- **Target SDK**: 34
- **Min SDK**: 24

---

**Note**: Aplikasi ini adalah untuk **mirror-only** screen sharing. Tidak ada fitur remote control atau touch input. Semua kontrol sistem (start/stop/disconnect) hanya tersedia di server (HP A).
