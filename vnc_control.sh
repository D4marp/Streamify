#!/bin/bash

# Streamify VNC Control Script
# Memudahkan control VNC server di HP A melalui ADB

function show_help() {
    echo "Streamify VNC Control Script"
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  start [port]  - Start VNC server di HP A dengan port optional"
    echo "  stop          - Stop VNC server di HP A"
    echo "  status        - Check status VNC server"
    echo "  ip            - Get IP address HP A"
    echo "  help          - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start        - Start VNC server dengan port default (5901)"
    echo "  $0 start 5902   - Start VNC server dengan port 5902"
    echo "  $0 start 6001   - Start VNC server dengan port 6001"
    echo ""
    echo "Prerequisites:"
    echo "- HP A terhubung via USB"
    echo "- USB Debugging enabled"
    echo "- Streamify app installed"
}

function check_device() {
    if ! command -v adb &> /dev/null; then
        echo "Error: ADB not found. Please install Android SDK Platform Tools"
        exit 1
    fi
    
    devices=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    if [ $devices -eq 0 ]; then
        echo "Error: No Android device connected or USB debugging not enabled"
        echo "Please:"
        echo "1. Connect HP A via USB"
        echo "2. Enable USB Debugging in Developer Options"
        echo "3. Accept USB debugging prompt on device"
        exit 1
    fi
    
    if [ $devices -gt 1 ]; then
        echo "Warning: Multiple devices connected. Using first device."
    fi
}

function start_vnc() {
    local port=${1:-5901}  # Default port 5901 if not specified
    
    # Validate port number
    if ! [[ "$port" =~ ^[0-9]+$ ]] || [ "$port" -lt 1024 ] || [ "$port" -gt 65535 ]; then
        echo "Error: Invalid port number. Port must be between 1024 and 65535"
        exit 1
    fi
    
    echo "Starting VNC server on HP A with port $port..."
    check_device
    
    # Check if app is installed
    app_installed=$(adb shell pm list packages | grep "com.app.streamify")
    if [ -z "$app_installed" ]; then
        echo "Error: Streamify app not installed on device"
        echo "Please install the app first"
        exit 1
    fi
    
    # Start VNC server with custom port
    adb shell am broadcast -a com.app.streamify.START_VNC_ADB --ei vnc_port "$port"
    
    if [ $? -eq 0 ]; then
        echo "VNC server start command sent successfully with port $port"
        echo "Please grant screen capture permission on device if prompted"
        echo ""
        sleep 2
        get_ip "$port"
    else
        echo "Error: Failed to start VNC server"
        exit 1
    fi
}

function stop_vnc() {
    echo "Stopping VNC server on HP A..."
    check_device
    
    adb shell am broadcast -a com.app.streamify.STOP_VNC_ADB
    
    if [ $? -eq 0 ]; then
        echo "VNC server stopped successfully"
    else
        echo "Error: Failed to stop VNC server"
        exit 1
    fi
}

function get_status() {
    echo "Checking VNC server status..."
    check_device
    
    # Try to check if VNC service is running
    vnc_service=$(adb shell ps | grep "streamify" || echo "")
    
    if [ -n "$vnc_service" ]; then
        echo "Status: VNC server appears to be running"
        get_ip
    else
        echo "Status: VNC server not running"
    fi
}

function get_ip() {
    local port=${1:-5901}  # Default port 5901 if not specified
    
    echo "Getting HP A IP address..."
    check_device
    
    # Get WiFi IP address
    ip_address=$(adb shell ip route | grep wlan | head -1 | awk '{print $9}' 2>/dev/null)
    
    if [ -z "$ip_address" ]; then
        # Alternative method
        ip_address=$(adb shell "ip addr show wlan0 | grep 'inet ' | awk '{print \$2}' | cut -d/ -f1" 2>/dev/null)
    fi
    
    if [ -n "$ip_address" ]; then
        echo "HP A IP Address: $ip_address"
        echo "VNC Port: $port"
        echo ""
        echo "Use this information in HP B (Client):"
        echo "- Server IP: $ip_address"
        echo "- Port: $port"
        echo ""
        echo "📋 Connection command for testing:"
        echo "vncviewer $ip_address:$port"
    else
        echo "Could not determine IP address. Please check WiFi connection."
    fi
}

# Main script logic
case "$1" in
    start)
        start_vnc "$2"  # Pass port as second argument
        ;;
    stop)
        stop_vnc
        ;;
    status)
        get_status
        ;;
    ip)
        get_ip "$2"  # Pass port as second argument
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
