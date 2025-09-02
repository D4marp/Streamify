# VNC Client Auto Reconnect Feature 🔄

## Overview
The VNC Client now includes an automatic reconnection feature that eliminates the need to manually restart connections when the VNC server goes down or becomes unavailable.

## Key Features

### ✅ **Smart Auto Reconnect**
- **Automatic Detection**: Detects connection loss in real-time
- **Configurable Attempts**: Tries up to 5 reconnection attempts
- **Progressive Delay**: 3-second delay between attempts
- **UI Feedback**: Shows "Reconnecting (X/5)" status

### ✅ **Multiple Trigger Points**
- **Frame Update Errors**: Reconnects when frame reception fails
- **Socket Disconnections**: Handles unexpected socket closures
- **VNC Protocol Errors**: Recovers from VNC-specific errors
- **Server Restarts**: Automatically reconnects when server comes back online

### ✅ **User Controls**
- **Toggle Switch**: Enable/disable auto reconnect
- **Manual Override**: Can disable during manual disconnect
- **Status Display**: Real-time connection status
- **Smart Resume**: Resumes frame updates after reconnection

## How It Works

### Connection Monitoring
```kotlin
// Monitors connection health in real-time
private fun startFrameUpdates() {
    while (isConnected && !isReconnecting) {
        try {
            vncClient?.requestFramebufferUpdate()
            val frame = vncClient?.receiveFrame()
            // Update UI...
        } catch (e: Exception) {
            // Trigger auto reconnect on any error
            if (autoReconnectEnabled) {
                triggerAutoReconnect()
            }
        }
    }
}
```

### Auto Reconnect Logic
```kotlin
// Attempts reconnection with exponential backoff
private suspend fun attemptReconnect() {
    while (connectionAttempts < maxReconnectAttempts) {
        connectionAttempts++
        
        // Clean up previous connection
        vncClient?.disconnect()
        
        // Wait before retry (3 seconds)
        delay(reconnectDelayMs)
        
        // Attempt new connection
        if (vncClient?.connect() == true) {
            // Success - resume normal operation
            isReconnecting = false
            startFrameUpdates()
            return
        }
    }
}
```

### UI Integration
```kotlin
// Auto reconnect toggle in the UI
Row {
    Text("Auto Reconnect")
    Switch(
        checked = autoReconnectEnabled,
        onCheckedChange = { toggleAutoReconnect() }
    )
}

// Status display
Text(
    text = when {
        isConnected -> "Connected"
        isReconnecting -> "Reconnecting ($connectionAttempts/$maxAttempts)"
        else -> "Disconnected"
    }
)
```

## Usage Instructions

### 1. **Enable Auto Reconnect**
- Open VNC Client
- Toggle the "Auto Reconnect" switch to ON
- Enter server IP and port
- Press "Connect"

### 2. **Monitor Status**
- Green "Connected" = Normal operation
- Yellow "Reconnecting (X/5)" = Auto reconnect in progress
- Red "Disconnected" = Connection failed after max attempts

### 3. **Manual Control**
- **Disable**: Turn off auto reconnect switch
- **Manual Disconnect**: Automatically disables auto reconnect
- **Re-enable**: Turn switch back on to resume auto reconnect

## Testing Scenarios

### Test 1: Server Restart
```bash
# Start server and client
# Stop VNC server
# Client should show "Reconnecting (1/5)"
# Restart VNC server
# Client should reconnect automatically
```

### Test 2: Network Interruption
```bash
# Establish connection
# Disconnect WiFi briefly
# Client attempts reconnection
# Reconnect WiFi
# Client should recover automatically
```

### Test 3: App Background/Foreground
```bash
# Connect to server
# Put client app in background
# Resume client app
# Connection should be maintained or auto-restored
```

## Configuration

### Customizable Parameters
```kotlin
private val maxReconnectAttempts = 5        // Max retry attempts
private var reconnectDelayMs = 3000L        // 3 seconds between attempts
private var autoReconnectEnabled = true     // Default state
```

### Performance Settings
- **Frame Rate**: Configurable (default: 25 FPS)
- **Connection Timeout**: 10 seconds
- **Reconnect Interval**: 3 seconds
- **Max Attempts**: 5 tries

## Benefits

### 🎯 **User Experience**
- **Zero Manual Intervention**: No need to manually reconnect
- **Seamless Operation**: Transparent to user during reconnection
- **Clear Feedback**: Always know connection status
- **Reliable Control**: Remote control resumes automatically

### 🛡️ **Robustness**
- **Network Resilience**: Handles WiFi drops and network issues
- **Server Resilience**: Survives server app restarts
- **Error Recovery**: Recovers from various VNC protocol errors
- **Resource Management**: Proper cleanup between attempts

### ⚡ **Performance**
- **Minimal Overhead**: Only activates when needed
- **Smart Timing**: Reasonable delays prevent spam
- **Efficient Cleanup**: Proper socket and resource management
- **Background Safe**: Works when app is backgrounded

## Troubleshooting

### Common Issues

#### Auto Reconnect Not Working
- ✅ Verify toggle is ON
- ✅ Check server is actually running
- ✅ Confirm IP/port are correct
- ✅ Check network connectivity

#### Too Many Reconnect Attempts
- ✅ Server might be permanently down
- ✅ Network issues might persist
- ✅ Try manual disconnect and reconnect
- ✅ Restart both apps if needed

#### Slow Reconnection
- ✅ Normal - 3 second delay between attempts
- ✅ Check network latency
- ✅ Ensure server is responsive
- ✅ Monitor logcat for specific errors

## Advanced Usage

### Manual Reconnect Control
```kotlin
// Disable auto reconnect for manual control
autoReconnectEnabled = false

// Manual disconnect
disconnectFromServer()

// Re-enable and trigger reconnect
autoReconnectEnabled = true
if (!isConnected && lastConnectionIp.isNotEmpty()) {
    triggerAutoReconnect()
}
```

### Custom Reconnect Logic
```kotlin
// Extend reconnection logic for specific scenarios
override fun onError(error: String) {
    when {
        error.contains("broken pipe") -> triggerAutoReconnect()
        error.contains("connection reset") -> triggerAutoReconnect()
        error.contains("timeout") -> triggerAutoReconnect()
        else -> logError(error)
    }
}
```

## Implementation Summary

The auto reconnect feature provides a **robust, user-friendly solution** for maintaining VNC connections without manual intervention. It handles various failure scenarios gracefully and provides clear feedback to users about connection status.

Key improvements:
- ✅ **Zero downtime** during server restarts
- ✅ **Automatic recovery** from network issues
- ✅ **Clear status indicators** for user awareness
- ✅ **Configurable behavior** for different use cases
- ✅ **Resource efficient** implementation

This makes the VNC remote control experience much more reliable and professional.
