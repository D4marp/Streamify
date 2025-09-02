#!/bin/bash

# Remote Control Testing Script for Streamify VNC
# This script helps test remote control functionality

function show_help() {
    echo "Streamify Remote Control Test Script"
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  check       - Check remote control setup status"
    echo "  test        - Perform basic remote control test"
    echo "  enable      - Help enable accessibility service"
    echo "  logs        - Show relevant logs for debugging"
    echo "  help        - Show this help message"
    echo ""
    echo "Prerequisites:"
    echo "- HP A connected via USB with debugging enabled"
    echo "- Streamify app installed and VNC server running"
    echo "- Accessibility service should be enabled"
}

function check_device() {
    if ! command -v adb &> /dev/null; then
        echo "Error: ADB not found. Please install Android SDK Platform Tools"
        exit 1
    fi
    
    devices=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    if [ $devices -eq 0 ]; then
        echo "Error: No Android device connected"
        exit 1
    fi
}

function check_remote_control() {
    echo "🔍 Checking Remote Control Setup..."
    check_device
    
    echo ""
    echo "1. Checking if Streamify app is installed..."
    app_installed=$(adb shell pm list packages | grep "com.app.streamify")
    if [ -n "$app_installed" ]; then
        echo "✅ Streamify app is installed"
    else
        echo "❌ Streamify app not found"
        exit 1
    fi
    
    echo ""
    echo "2. Checking accessibility service status..."
    accessibility_enabled=$(adb shell settings get secure accessibility_enabled)
    if [ "$accessibility_enabled" = "1" ]; then
        echo "✅ Accessibility services are enabled"
        
        # Check if our specific service is enabled
        enabled_services=$(adb shell settings get secure enabled_accessibility_services)
        if [[ "$enabled_services" == *"com.app.streamify/com.app.streamify.TouchAccessibilityService"* ]]; then
            echo "✅ Streamify Touch Service is enabled"
        else
            echo "❌ Streamify Touch Service is NOT enabled"
            echo "   Please enable it in Settings > Accessibility"
        fi
    else
        echo "❌ Accessibility services are disabled"
    fi
    
    echo ""
    echo "3. Checking VNC server status..."
    vnc_process=$(adb shell ps | grep streamify)
    if [ -n "$vnc_process" ]; then
        echo "✅ VNC server appears to be running"
    else
        echo "⚠️  VNC server may not be running"
    fi
    
    echo ""
    echo "4. Testing touch input capability..."
    echo "   Attempting to simulate a test touch..."
    adb shell input tap 500 1000 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✅ Touch input simulation works"
    else
        echo "❌ Touch input simulation failed"
    fi
}

function test_remote_control() {
    echo "🧪 Testing Remote Control..."
    check_device
    
    echo ""
    echo "This will perform a series of test touches on the device."
    echo "Watch the device screen for touch indicators."
    echo ""
    read -p "Press Enter to start test (or Ctrl+C to cancel)..."
    
    echo "Test 1: Center tap..."
    adb shell input tap 540 1200
    sleep 1
    
    echo "Test 2: Top-left corner tap..."
    adb shell input tap 100 300
    sleep 1
    
    echo "Test 3: Bottom-right corner tap..."
    adb shell input tap 980 2100
    sleep 1
    
    echo "Test 4: Swipe gesture (left to right)..."
    adb shell input swipe 200 1200 800 1200 500
    sleep 1
    
    echo ""
    echo "🎯 Test completed!"
    echo "If you saw touch indicators on the device, remote control is working."
    echo "If not, check accessibility service settings."
}

function enable_accessibility() {
    echo "🔧 Opening Accessibility Settings..."
    check_device
    
    echo ""
    echo "Opening accessibility settings on device..."
    adb shell am start -a android.settings.ACCESSIBILITY_SETTINGS
    
    echo ""
    echo "Please follow these steps on your device:"
    echo "1. Find 'Streamify Touch Service' in the list"
    echo "2. Tap on it"
    echo "3. Turn on the toggle switch"
    echo "4. Tap 'Allow' when prompted"
    echo "5. Return to Streamify app"
    echo ""
    echo "The service name should appear as:"
    echo "'Streamify Touch Service' or 'TouchAccessibilityService'"
}

function show_logs() {
    echo "📱 Showing Remote Control Logs..."
    check_device
    
    echo ""
    echo "Showing last 50 log entries related to remote control..."
    echo "Press Ctrl+C to stop..."
    echo ""
    
    adb logcat -s "TouchInputManager:*" "TouchAccessibilityService:*" "VncServer:*" "RemoteControlHelper:*" | tail -50
}

# Main script logic
case "$1" in
    check)
        check_remote_control
        ;;
    test)
        test_remote_control
        ;;
    enable)
        enable_accessibility
        ;;
    logs)
        show_logs
        ;;
    help|--help|-h)
        show_help
        ;;
    "")
        echo "No command specified. Use '$0 help' for usage information."
        exit 1
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac
