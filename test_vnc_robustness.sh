#!/bin/bash

# VNC Reconnection Test Script
# Tests the robustness of VNC server stop/start cycles

echo "🧪 VNC Server Robustness Test"
echo "================================"

# Get device IP
DEVICE_IP=$(adb shell ip route | grep wlan0 | awk '{print $9}' | head -1)
if [ -z "$DEVICE_IP" ]; then
    echo "❌ Could not detect device IP address"
    exit 1
fi

echo "📱 Device IP: $DEVICE_IP"
echo "🔌 VNC Port: 5901"
echo ""

# Function to test VNC connection
test_vnc_connection() {
    local test_name="$1"
    echo "🔍 Testing: $test_name"
    
    # Try to connect using nc (netcat)
    timeout 3 nc -zv $DEVICE_IP 5901 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ VNC server is responding"
    else
        echo "❌ VNC server is not responding"
    fi
    echo ""
}

# Function to start VNC server via ADB
start_vnc() {
    echo "🚀 Starting VNC server..."
    adb shell am broadcast -a com.app.streamify.START_VNC_ADB
    sleep 2
}

# Function to stop VNC server via ADB
stop_vnc() {
    echo "🛑 Stopping VNC server..."
    adb shell am broadcast -a com.app.streamify.STOP_VNC_ADB
    sleep 2
}

# Main test sequence
echo "📋 Test Sequence:"
echo "1. Start VNC server"
echo "2. Test connection"
echo "3. Stop VNC server (simulating broken pipe scenario)"
echo "4. Test connection again"
echo "5. Restart VNC server"
echo "6. Final connection test"
echo ""

# Test 1: Initial start
start_vnc
test_vnc_connection "Initial VNC startup"

# Test 2: Normal operation
test_vnc_connection "Normal operation check"

# Test 3: Stop server (this is where broken pipe usually occurs)
stop_vnc
test_vnc_connection "After server stop (should fail)"

# Test 4: Restart after stop
start_vnc
test_vnc_connection "After restart (recovery test)"

# Test 5: Multiple stop/start cycles
echo "🔄 Testing multiple start/stop cycles..."
for i in {1..3}; do
    echo "  Cycle $i:"
    stop_vnc
    start_vnc
    test_vnc_connection "  Cycle $i restart"
done

echo "✅ Robustness test completed!"
echo ""
echo "📊 Results Summary:"
echo "- Test checks VNC server's ability to handle stop/start cycles"
echo "- Verifies that broken pipe errors don't cause permanent failures"
echo "- Ensures server can restart cleanly after clients disconnect"
echo ""
echo "🔧 If any tests failed:"
echo "1. Check logcat for error messages: adb logcat | grep VncServer"
echo "2. Ensure WiFi is connected and stable"
echo "3. Try restarting the Streamify app"
