# Quick Start Guide - Screen Sharing HP A ke HP B

## Step-by-Step Setup (5 menit)

### 📱 SETUP HP A (Server - Yang sharing layar)

1. **Install app Streamify** di HP A
2. **Aktifkan Developer Options**:
   - Settings → About Phone → Tap "Build Number" 7x
   - Settings → Developer Options → Enable "USB Debugging"

3. **Connect HP A ke PC** dengan kabel USB
4. **Install ADB** di PC (jika belum ada):
   - Download Platform Tools dari developer.android.com
   - Extract dan tambahkan ke PATH

5. **Configure Port (Optional)**:
   - Buka app → Tap "Server Config" 
   - Pilih port custom (5901, 5902, 5903, dll)
   - Atau gunakan port default 5901

6. **Start VNC Server**:
   ```bash
   # Default port 5901
   ./vnc_control.sh start
   
   # Custom port
   ./vnc_control.sh start 5902
   ./vnc_control.sh start 6001
   
   # Manual ADB command
   adb shell am broadcast -a com.app.streamify.START_VNC_ADB --ei vnc_port 5902
   ```

7. **Grant permission** screen capture di HP A
8. **Catat IP address dan port** yang muncul

---

### 📱 SETUP HP B (Client - Yang melihat stream)

1. **Install app Streamify** di HP B
2. **Pastikan HP B di WiFi yang sama** dengan HP A
3. **Buka app** → Pilih **"Client Mode"**
4. **Input connection details**:
   - Server IP: `192.168.1.100` (IP HP A)
   - Port: `5901` (atau port custom yang diset di HP A)
5. **Tap "Connect"**
6. **Layar HP A muncul di HP B** ✅

---

## Quick Commands

### Start Sharing (Default Port)
```bash
./vnc_control.sh start
```

### Start Sharing (Custom Port)
```bash
./vnc_control.sh start 5902
./vnc_control.sh start 6001
```

### Stop Sharing  
```bash
./vnc_control.sh stop
```

### Check Status
```bash
./vnc_control.sh status
```

### Get IP Address with Port
```bash
./vnc_control.sh ip 5902
```

---

## Troubleshooting Cepat

### ❌ "No device connected"
- Check kabel USB
- Enable USB Debugging
- Accept debugging prompt di HP A

### ❌ "Permission denied" 
- Grant screen capture permission di HP A
- Restart app jika perlu

### ❌ "Connection failed" di HP B
- Check IP address benar
- Pastikan HP A dan B di WiFi sama
- Check VNC server running di HP A

### ❌ Stream lag/putus
- Check kualitas WiFi
- Restart kedua app
- Pastikan tidak ada app lain pakai bandwidth besar

## Skenario Different Ports

### **Same WiFi, Different Ports:**

**Contoh 1: Multiple HP A sharing simultaneously**
```
HP A1: Port 5901 (192.168.1.100:5901)
HP A2: Port 5902 (192.168.1.101:5902) 
HP A3: Port 5903 (192.168.1.102:5903)

HP B connect ke salah satu:
- IP: 192.168.1.100, Port: 5901 → Stream dari HP A1
- IP: 192.168.1.101, Port: 5902 → Stream dari HP A2
- IP: 192.168.1.102, Port: 5903 → Stream dari HP A3
```

**Contoh 2: Port conflict resolution**
```
Jika default port 5901 sudah digunakan:
./vnc_control.sh start 5902  # Gunakan port alternatif
./vnc_control.sh start 6001  # Atau port lain yang available
```

**Contoh 3: Network testing**
```
Test multiple ports untuk cari yang stabil:
./vnc_control.sh start 5901  # Standard VNC
./vnc_control.sh start 5902  # VNC display 2
./vnc_control.sh start 8080  # HTTP alternative
./vnc_control.sh start 6969  # Custom port
```

### **Port Configuration dalam App:**

1. **HP A (Server)**: 
   - Buka app → "Server Config" → Set port custom
   - Port disimpan otomatis untuk next session

2. **HP B (Client)**:
   - Input IP dan port sesuai server
   - Connection details tersimpan otomatis

---

## Network Requirements

- **HP A dan HP B** harus di **WiFi yang sama**
- **Port yang dipilih** harus terbuka (1024-65535)
- **Bandwidth minimum** ~1 Mbps untuk streaming lancar
- **Multiple connections** bisa menggunakan port berbeda

---

## Expected Results

✅ **HP A**: Bisa share layar ke HP B  
✅ **HP B**: Bisa lihat layar HP A real-time  
✅ **Resolusi**: 320x700 (optimized untuk mobile)  
✅ **Frame rate**: ~10 FPS (smooth untuk viewing)  

---

**Total setup time**: ~5 menit  
**Works on**: Android 7.0+ 
**No root required** ✅
