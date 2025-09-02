#!/bin/bash

echo "🔄 Testing VNC Client Auto Reconnect Feature"
echo "============================================="

# Function to check if VNC server is running
check_vnc_server() {
    adb shell "ps | grep streamify" | grep -v grep > /dev/null
    return $?
}

# Function to start VNC server
start_vnc_server() {
    echo "📱 Starting VNC Server..."
    adb shell "am start -n com.app.streamify/.MainActivity"
    sleep 3
    # Simulate pressing Start VNC Server button
    adb shell "input tap 540 1000"  # Adjust coordinates as needed
    sleep 2
}

# Function to stop VNC server
stop_vnc_server() {
    echo "🛑 Stopping VNC Server..."
    # Simulate pressing Stop VNC Server button
    adb shell "input tap 540 1200"  # Adjust coordinates as needed
    sleep 2
}

# Function to start VNC client
start_vnc_client() {
    echo "💻 Starting VNC Client..."
    adb shell "am start -n com.app.streamify/.ClientActivity"
    sleep 3
    # Enable auto reconnect toggle
    adb shell "input tap 700 400"  # Toggle auto reconnect
    sleep 1
    # Press connect button
    adb shell "input tap 540 350"  # Connect button
    sleep 3
}

# Test sequence
echo "1️⃣ Starting VNC Server first..."
start_vnc_server

echo "2️⃣ Starting VNC Client with auto reconnect..."
start_vnc_client

echo "3️⃣ Waiting for initial connection..."
sleep 5

echo "4️⃣ Testing auto reconnect by stopping and restarting server..."
for i in {1..3}; do
    echo "   Test $i/3: Stop server..."
    stop_vnc_server
    sleep 5
    
    echo "   Test $i/3: Restart server..."
    start_vnc_server
    sleep 8
    
    echo "   Test $i/3: Client should auto reconnect..."
    sleep 5
done

echo "✅ Auto reconnect test completed!"
echo "Check the VNC Client screen to see reconnection status."
echo ""
echo "📋 What to verify:"
echo "   - Auto reconnect toggle is ON"
echo "   - Connection status shows 'Reconnecting (X/5)'"
echo "   - Client automatically reconnects after server restart"
echo "   - Frame updates resume after reconnection"
