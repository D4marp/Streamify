#!/bin/bash

echo "🧪 Quick Remote Control Test"
echo "============================="

# Check if device connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected. Connect HP A via USB first."
    exit 1
fi

echo "📱 Testing touch simulation on connected device..."
echo ""

echo "Test 1: Center tap (you should see a touch indicator)"
adb shell input tap 540 1200
sleep 2

echo "Test 2: Home button area tap"  
adb shell input tap 540 2100
sleep 2

echo "Test 3: Swipe up (recent apps gesture)"
adb shell input swipe 540 1800 540 800 300
sleep 2

echo ""
echo "✅ If you saw touch indicators/actions on your device, remote control will work!"
echo "📋 Now test via VNC:"
echo "   1. Start VNC server on HP A"
echo "   2. Connect from HP B" 
echo "   3. Touch HP B screen → HP A should respond automatically"
