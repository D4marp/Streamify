# Streamify - Android Screen Sharing App

## Deskripsi
Streamify adalah aplikasi Android yang memungkinkan screen sharing atau streaming layar antara dua perangkat Android (HP A dan HP B) menggunakan protokol VNC (Virtual Network Computing).

## Fitur
- **Server Mode**: HP A dapat membagikan layarnya ke perangkat lain
- **Client Mode**: HP B dapat melihat dan menerima stream layar dari HP A
- Protokol VNC dengan encoding RAW
- Resolusi fixed 320x700 untuk konsistensi streaming
- Auto-scaling dan letterboxing untuk aspect ratio yang berbeda
- Background service untuk server VNC
- Real-time frame updates (~10 FPS)

## Cara Penggunaan

### Setup HP A (Server - Yang membagikan layar)

1. **Install aplikasi Streamify** di HP A
2. **Buka aplikasi** dan pilih "Server Mode"
3. **Aktifkan USB Debugging**:
   - Buka Settings > About Phone
   - Tap "Build Number" 7 kali untuk mengaktifkan Developer Options
   - Kembali ke Settings > Developer Options
   - Aktifkan "USB Debugging"

4. **Hubungkan HP A ke PC** dengan kabel USB
5. **Install ADB di PC** jika belum ada
6. **Jalankan command ADB** untuk memulai VNC server:
   ```bash
   adb shell am broadcast -a com.app.streamify.START_VNC_ADB
   ```

7. **Grant permission** screen capture saat diminta
8. **Catat IP address** yang ditampilkan di aplikasi (misal: 192.168.1.100)
9. **Port default**: 5901

### Setup HP B (Client - Yang melihat stream)

1. **Install aplikasi Streamify** di HP B
2. **Pastikan HP B terhubung** ke jaringan WiFi yang sama dengan HP A
3. **Buka aplikasi** dan pilih "Client Mode"
4. **Masukkan detail koneksi**:
   - Server IP: IP address HP A (misal: 192.168.1.100)
   - Port: 5901 (default)
5. **Tap "Connect"** untuk mulai menerima stream
6. **Layar HP A** akan muncul di HP B dalam waktu beberapa detik

### Menghentikan Sharing

**HP A (Server):**
```bash
adb shell am broadcast -a com.app.streamify.STOP_VNC_ADB
```

**HP B (Client):**
- Tap tombol "Disconnect" di aplikasi

## Arsitektur Aplikasi

### Server Side (HP A)
- `VncServer.kt`: Core VNC server implementation
- `VncServerService.kt`: Background service untuk VNC server
- `MainActivity.kt`: UI utama dengan status server

### Client Side (HP B)
- `VncClient.kt`: Core VNC client implementation  
- `ClientActivity.kt`: UI untuk koneksi dan menampilkan stream

### VNC Protocol Implementation
- Handshake dan authentication (no auth)
- Server initialization dengan pixel format 32-bit RGBA
- Framebuffer updates dengan RAW encoding
- Screen capture menggunakan MediaProjection API

## Technical Details

### Screen Capture (Server)
- Menggunakan `MediaProjectionManager` untuk screen capture
- `ImageReader` dengan format `RGBA_8888`
- Resolusi target: 320x700 pixels
- Letterboxing untuk aspect ratio yang berbeda
- Caching frame terakhir untuk performa

### Network Communication
- TCP Socket pada port 5901
- VNC Protocol 3.8
- RAW encoding (uncompressed)
- Non-blocking I/O dengan timeout handling

### Performance Optimizations
- Frame rate ~10 FPS untuk mengurangi bandwidth
- Buffer caching untuk frame berulang
- Concurrent handling dengan thread pool
- Graceful error handling dan reconnection

## Troubleshooting

### Server tidak bisa start
- Pastikan screen capture permission diberikan
- Check apakah port 5901 tidak digunakan aplikasi lain
- Restart aplikasi jika perlu

### Client tidak bisa connect
- Pastikan HP A dan HP B di jaringan WiFi yang sama
- Check IP address dan port benar
- Pastikan firewall tidak memblokir koneksi
- Check apakah VNC server running di HP A

### Stream lag atau terputus
- Check kualitas koneksi WiFi
- Pastikan tidak ada aplikasi lain yang menggunakan bandwidth besar
- Restart kedua aplikasi jika perlu

## Requirements
- Android 7.0+ (API level 24+)
- WiFi connection untuk kedua perangkat
- Screen recording permission untuk server
- ADB access untuk server control

## Limitations
- Saat ini hanya mendukung RAW encoding (uncompressed)
- Resolusi fixed untuk konsistensi
- Tidak ada audio streaming
- Tidak ada remote control (hanya viewing)

## Future Enhancements
- Compressed encoding (JPEG, PNG)
- Dynamic resolution adjustment
- Audio streaming support
- Remote input control
- Direct WiFi connection tanpa ADB
