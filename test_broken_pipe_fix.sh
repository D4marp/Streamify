#!/bin/bash

# VNC Disconnection & Broken Pipe Test Script
# Tests the robustness against broken pipe and null frame errors

echo "🔥 VNC Broken Pipe & Null Frame Test"
echo "====================================="

# Get device IP
DEVICE_IP=$(adb shell ip route | grep wlan0 | awk '{print $9}' | head -1)
if [ -z "$DEVICE_IP" ]; then
    echo "❌ Could not detect device IP address"
    exit 1
fi

echo "📱 Device IP: $DEVICE_IP"
echo "🔌 VNC Port: 5901"
echo ""

# Function to test broken pipe scenario
test_broken_pipe() {
    echo "🧪 Testing Broken Pipe Scenario..."
    echo "1. Starting VNC server"
    adb shell am broadcast -a com.app.streamify.START_VNC_ADB
    sleep 3
    
    echo "2. Connecting with nc to simulate client"
    timeout 5 nc $DEVICE_IP 5901 &
    NC_PID=$!
    sleep 2
    
    echo "3. Killing client connection abruptly (simulates broken pipe)"
    kill -9 $NC_PID 2>/dev/null
    
    echo "4. Checking if server is still responsive"
    sleep 2
    timeout 3 nc -zv $DEVICE_IP 5901 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ Server survived broken pipe"
    else
        echo "❌ Server crashed from broken pipe"
    fi
    
    echo "5. Stopping server cleanly"
    adb shell am broadcast -a com.app.streamify.STOP_VNC_ADB
    sleep 2
    echo ""
}

# Function to test rapid connect/disconnect
test_rapid_reconnect() {
    echo "🔄 Testing Rapid Connect/Disconnect..."
    
    for i in {1..5}; do
        echo "  Cycle $i:"
        echo "    Starting server..."
        adb shell am broadcast -a com.app.streamify.START_VNC_ADB
        sleep 1
        
        echo "    Quick connect/disconnect..."
        timeout 2 nc $DEVICE_IP 5901 &
        sleep 0.5
        pkill -f "nc $DEVICE_IP 5901" 2>/dev/null
        
        echo "    Stopping server..."
        adb shell am broadcast -a com.app.streamify.STOP_VNC_ADB
        sleep 1
        
        # Check if server is responsive for next cycle
        timeout 2 nc -zv $DEVICE_IP 5901 2>/dev/null
        if [ $? -eq 0 ]; then
            echo "    ❌ Server still running (cleanup failed)"
        else
            echo "    ✅ Server stopped cleanly"
        fi
    done
    echo ""
}

# Function to test null frame handling
test_null_frame() {
    echo "🖼️ Testing Null Frame Handling..."
    echo "1. Starting VNC server"
    adb shell am broadcast -a com.app.streamify.START_VNC_ADB
    sleep 2
    
    echo "2. Connecting and requesting multiple framebuffer updates"
    # Send VNC handshake and framebuffer requests rapidly
    (
        sleep 1
        printf "RFB 003.008\n"
        printf "\x01"  # Auth choice
        printf "\x01"  # Shared flag
        sleep 0.5
        # Send multiple framebuffer update requests rapidly
        for i in {1..10}; do
            printf "\x03\x00\x00\x00\x00\x00\x01\x40\x02\xbc"  # Framebuffer update request
            sleep 0.1
        done
    ) | timeout 10 nc $DEVICE_IP 5901 &
    
    sleep 3
    echo "3. Stopping server during active session"
    adb shell am broadcast -a com.app.streamify.STOP_VNC_ADB
    sleep 2
    
    echo "4. Checking for graceful shutdown"
    timeout 2 nc -zv $DEVICE_IP 5901 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "❌ Server still running after stop"
    else
        echo "✅ Server stopped gracefully"
    fi
    echo ""
}

# Function to monitor logs for errors
monitor_errors() {
    echo "📋 Monitoring for errors..."
    echo "Starting log monitor (will run for 30 seconds)..."
    
    # Start VNC server
    adb shell am broadcast -a com.app.streamify.START_VNC_ADB
    sleep 2
    
    # Monitor logs in background
    adb logcat -c
    timeout 30 adb logcat | grep -E "(VncServer|Broken pipe|Null|FATAL|AndroidRuntime)" > /tmp/vnc_test_logs.txt &
    LOG_PID=$!
    
    # Create some connection activity
    for i in {1..3}; do
        echo "  Creating connection activity $i/3..."
        timeout 3 nc $DEVICE_IP 5901 &
        sleep 1
        pkill -f "nc $DEVICE_IP 5901" 2>/dev/null
        sleep 1
    done
    
    # Stop server
    adb shell am broadcast -a com.app.streamify.STOP_VNC_ADB
    sleep 3
    
    # Stop log monitoring
    kill $LOG_PID 2>/dev/null
    
    echo "📊 Error Analysis:"
    if [ -f /tmp/vnc_test_logs.txt ]; then
        BROKEN_PIPE_COUNT=$(grep -c "Broken pipe" /tmp/vnc_test_logs.txt)
        NULL_ERROR_COUNT=$(grep -c "Null" /tmp/vnc_test_logs.txt)
        FATAL_COUNT=$(grep -c "FATAL" /tmp/vnc_test_logs.txt)
        
        echo "  - Broken pipe errors: $BROKEN_PIPE_COUNT"
        echo "  - Null errors: $NULL_ERROR_COUNT"
        echo "  - Fatal errors: $FATAL_COUNT"
        
        if [ $BROKEN_PIPE_COUNT -eq 0 ] && [ $NULL_ERROR_COUNT -eq 0 ] && [ $FATAL_COUNT -eq 0 ]; then
            echo "  ✅ No critical errors detected!"
        else
            echo "  ❌ Found error(s) - check logs for details"
            echo "  Recent errors:"
            tail -10 /tmp/vnc_test_logs.txt | head -5
        fi
        
        rm -f /tmp/vnc_test_logs.txt
    else
        echo "  ❌ Could not capture logs"
    fi
    echo ""
}

# Main test sequence
echo "🚀 Starting comprehensive VNC robustness tests..."
echo ""

# Run all tests
test_broken_pipe
test_rapid_reconnect
test_null_frame
monitor_errors

echo "✅ All tests completed!"
echo ""
echo "📋 Test Summary:"
echo "- Broken pipe handling: Tested connection drops"
echo "- Rapid reconnection: Tested server cleanup"
echo "- Null frame handling: Tested during active sessions"
echo "- Error monitoring: Checked for critical errors"
echo ""
echo "🔧 If issues found:"
echo "1. Check full logcat: adb logcat | grep VncServer"
echo "2. Restart Streamify app if needed"
echo "3. Ensure stable WiFi connection"
