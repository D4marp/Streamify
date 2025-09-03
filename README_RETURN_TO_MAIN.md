# Streamify VNC - Return to Main v4.4 FINAL 

**Mirror-Only Screen Sharing dengan Auto Return to Main**

## 📱 Overview
Streamify VNC adalah aplikasi Android untuk **screen mirroring** (view-only) antar perangkat Android menggunakan protokol VNC. Aplikasi ini telah di-optimize untuk **mirror-only mode** tanpa fitur remote control, dengan **auto return to main** jika reconnect gagal.

## ✨ Features

### 🖥️ Mirror-Only Mode
- **View-only screen sharing** - tidak ada remote control
- **Auto reconnect** yang persistent dan reliable
- **Full screen mirror** tanpa UI elements
- **Server-side control** - semua kontrol hanya di server (HP A)
- **Auto return to main** - kembali ke halaman awal jika reconnect gagal

### 🔄 Smart Reconnect System
- **Auto Reconnect**: Default ON dan persistent
- **Unlimited Attempts** (FullScreen): Reconnect tanpa batas
- **Time-based Timeout** (FullScreen): 60 detik timeout
- **Attempt-based Limit** (Regular): 50 attempts maximum
- **Return to Main**: Otomatis kembali ke halaman awal jika gagal total

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
4. **Auto Return**: Jika reconnect gagal total, otomatis kembali ke main menu

### 🖥️ Full Screen Mirror Mode
- **No UI Elements**: Layar murni tanpa tombol atau kontrol
- **Persistent Full Screen**: Tetap full screen setelah reconnect
- **60 Second Timeout**: Jika tidak bisa reconnect dalam 60 detik, kembali ke main
- **Server-Only Control**: Disconnect hanya bisa dilakukan dari server

### 📋 Regular Client Mode
- **50 Attempt Limit**: Maksimal 50 percobaan reconnect
- **Auto Return**: Setelah 50 attempts gagal, kembali ke main menu
- **Toast Notification**: Notifikasi ketika reconnect gagal

## 📋 System Requirements
- Android 7.0+ (API level 24+)
- Permissions: INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE
- Screen capture permission (untuk server mode)

## 🛠️ Installation
1. Download `Streamify-VNC-ReturnToMain-FINAL-v4.4.apk`
2. Enable "Install from Unknown Sources"
3. Install APK di kedua perangkat
4. Grant semua permissions yang diminta

## 🔧 Configuration
- **Default Port**: 5900 (dapat diubah di settings)
- **Auto Reconnect**: Default ON dan persistent
- **Connection Timeout**: 10 detik
- **Reconnect Interval**: 3 detik
- **Full Screen Timeout**: 60 detik
- **Regular Client Limit**: 50 attempts

## 📖 Usage Instructions

### First Time Setup
1. Install APK di kedua HP (A dan B)
2. HP A: Pilih "Server Config", grant screen permission, start server
3. HP B: Pilih "Full Screen Mirror", masukkan IP HP A, connect

### Daily Usage
1. HP A: Start server
2. HP B: Connect menggunakan "Full Screen Mirror"
3. **Auto Reconnect**: Jika HP A restart, HP B akan auto reconnect
4. **Auto Return**: Jika reconnect gagal total, HP B kembali ke main menu
5. HP A: Kontrol disconnect menggunakan "Force Disconnect" button

## 🔄 Reconnect Scenarios

### Scenario 1: HP A Restart (Sukses)
1. HP A disconnect/restart
2. HP B auto detect disconnect
3. HP B mulai auto reconnect
4. HP A start server lagi
5. HP B auto connect kembali (tetap full screen)

### Scenario 2: HP A Mati Total (Gagal)
1. HP A mati/tidak available
2. HP B auto detect disconnect
3. HP B mencoba reconnect (60 detik untuk full screen / 50 attempts untuk regular)
4. Setelah timeout/limit, HP B auto return ke main menu
5. Toast notification: "Reconnect failed for [IP]:[Port] - Returned to main menu"

## 🔒 Security Notes
- Tidak ada authentication (untuk jaringan lokal yang aman)
- Data tidak encrypted (VNC standard)
- Gunakan hanya di jaringan yang terpercaya

## 🐛 Troubleshooting

### Connection Issues
- Pastikan kedua HP di jaringan WiFi yang sama
- Check firewall/security software
- Restart aplikasi jika perlu

### Reconnect Issues
- HP B akan otomatis return to main jika reconnect gagal total
- Check Toast notification untuk detail error
- Reconnect manual dari main menu jika diperlukan

### Permission Issues
- Grant semua permissions manual di Settings > Apps
- Untuk screen capture: Settings > Special Access > Display over apps

## 📝 Version History

### v4.4 FINAL - Return to Main
- ✅ **Auto return to main** - kembali ke halaman awal jika reconnect gagal
- ✅ **60 second timeout** untuk full screen mode
- ✅ **50 attempt limit** untuk regular client mode
- ✅ **Toast notifications** untuk reconnect failure
- ✅ **Improved UX** - tidak stuck di reconnect loop

### v4.3 - Auto Reconnect Enhanced
- Auto reconnect unlimited untuk full screen
- Enhanced reconnect logic

### v4.2 - Freeze Mode
- Mirror-only mode implementation
- All control features disabled

### v4.1 - Full Screen
- Full screen mirror mode
- Server-side control only

## 🔗 Development Info
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose  
- **Build System**: Gradle
- **Target SDK**: 34
- **Min SDK**: 24

---

**Note**: Aplikasi ini adalah untuk **mirror-only** screen sharing dengan **smart reconnect system**. Jika auto reconnect gagal total, aplikasi akan otomatis kembali ke halaman awal untuk user experience yang lebih baik.
