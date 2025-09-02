#!/bin/bash

echo "🔧 Manual VNC Server Restart Test"
echo "=================================="
echo ""
echo "INSTRUCTIONS:"
echo "1. First, manually start VNC server in the app"
echo "2. Try connecting with VNC client"
echo "3. Stop VNC server in the app"  
echo "4. Start VNC server again in the app"
echo "5. Try connecting with VNC client again"
echo ""
echo "The issue was: After step 4, VNC client cannot connect"
echo "The fix includes:"
echo "  ✅ Proper state reset (isRunning, shouldStopGracefully, isClientConnected)"
echo "  ✅ Socket reuse enabled (serverSocket.reuseAddress = true)"
echo "  ✅ Additional cleanup delay (500ms for port release)"
echo "  ✅ Proper server socket cleanup"
echo ""

# Simple port check function
check_port() {
    local port=${1:-5900}
    echo "Checking if port $port is open..."
    
    if command -v nc >/dev/null 2>&1; then
        if nc -z localhost $port 2>/dev/null; then
            echo "✅ Port $port is OPEN (VNC server is running)"
        else
            echo "❌ Port $port is CLOSED (VNC server is not running)"
        fi
    elif command -v telnet >/dev/null 2>&1; then
        if echo "" | telnet localhost $port 2>/dev/null | grep -q "Connected"; then
            echo "✅ Port $port is OPEN (VNC server is running)"
        else
            echo "❌ Port $port is CLOSED (VNC server is not running)"
        fi
    else
        echo "⚠️  Cannot check port (nc or telnet not available)"
        echo "   Try connecting with VNC client to test manually"
    fi
}

echo "📱 Current status:"
check_port 5900

echo ""
echo "🎯 TO TEST THE FIX:"
echo "1. Open the Streamify app"
echo "2. Start VNC server"
echo "3. Run: ./test_vnc_restart.sh"
echo "4. Stop VNC server in app"
echo "5. Run: ./test_vnc_restart.sh"
echo "6. Start VNC server in app again"  
echo "7. Run: ./test_vnc_restart.sh"
echo "8. Try connecting with VNC client - should work now!"
