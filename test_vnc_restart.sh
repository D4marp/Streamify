#!/bin/bash

echo "🔧 Testing VNC Server Restart Capability"
echo "========================================"

# Function to test VNC connection
test_connection() {
    local port=${1:-5900}
    local timeout=${2:-3}
    
    echo "Testing connection to localhost:$port..."
    if timeout $timeout bash -c "</dev/tcp/localhost/$port"; then
        echo "✅ Connection successful"
        return 0
    else
        echo "❌ Connection failed"
        return 1
    fi
}

# Function to find VNC server process and send stop signal
stop_vnc_server() {
    echo "🛑 Stopping VNC server via app..."
    
    # Option 1: Send intent to stop server
    adb shell am broadcast -a com.app.streamify.STOP_VNC 2>/dev/null
    
    # Option 2: Kill the app process to force stop
    adb shell am force-stop com.app.streamify 2>/dev/null
    
    echo "Waiting for cleanup..."
    sleep 3
}

# Function to start VNC server
start_vnc_server() {
    echo "🚀 Starting VNC server via app..."
    
    # Launch the app
    adb shell am start -n com.app.streamify/.MainActivity 2>/dev/null
    
    echo "Waiting for server to start..."
    sleep 5
}

echo "📱 Testing VNC server restart sequence..."
echo ""

# Test 1: Initial connection
echo "Test 1: Initial VNC server start"
echo "--------------------------------"
start_vnc_server
if test_connection 5900 5; then
    echo "✅ Initial start successful"
else
    echo "❌ Initial start failed"
    exit 1
fi

echo ""

# Test 2: Stop server
echo "Test 2: Stop VNC server"
echo "----------------------"
stop_vnc_server
sleep 2

if test_connection 5900 2; then
    echo "❌ Server still running after stop (this is bad)"
else
    echo "✅ Server stopped successfully"
fi

echo ""

# Test 3: Restart server
echo "Test 3: Restart VNC server"
echo "--------------------------"
start_vnc_server

if test_connection 5900 5; then
    echo "✅ Server restarted successfully!"
    echo "🎉 VNC server restart capability is working!"
else
    echo "❌ Server failed to restart"
    echo "💡 This was the original problem - now should be fixed!"
fi

echo ""

# Test 4: Multiple restart cycles
echo "Test 4: Multiple restart cycles"
echo "-------------------------------"
for i in {1..3}; do
    echo "Cycle $i: Stop -> Start"
    stop_vnc_server
    sleep 1
    start_vnc_server
    
    if test_connection 5900 3; then
        echo "✅ Cycle $i successful"
    else
        echo "❌ Cycle $i failed"
        break
    fi
done

echo ""
echo "🏁 VNC restart test completed!"
echo "Try connecting with VNC client to test remote control capability."
