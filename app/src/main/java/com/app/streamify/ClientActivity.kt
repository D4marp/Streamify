package com.app.streamify

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientActivity : ComponentActivity() {
    private var vncClient: VncClient? = null
    private var isConnected by mutableStateOf(false)
    private var isReconnecting by mutableStateOf(false)
    private var connectionAttempts by mutableIntStateOf(0)
    private val maxReconnectAttempts = 5
    private var currentFrame by mutableStateOf<Bitmap?>(null)
    
    // Auto reconnect configuration
    private var autoReconnectEnabled by mutableStateOf(true) // DEFAULT AKTIF
    private var lastConnectionIp = ""
    private var lastConnectionPort = 0
    private var reconnectDelayMs = 3000L // 3 seconds between attempts
    
    private lateinit var vncConfig: VncConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vncConfig = VncConfig(this)
        
        // Load auto reconnect preference - default TRUE
        val sharedPrefs = getSharedPreferences("vnc_client_settings", MODE_PRIVATE)
        autoReconnectEnabled = sharedPrefs.getBoolean("auto_reconnect_enabled", true) // Default TRUE
        
        setContent {
            ClientScreen(
                isConnected = isConnected,
                isReconnecting = isReconnecting,
                connectionAttempts = connectionAttempts,
                maxAttempts = maxReconnectAttempts,
                autoReconnectEnabled = autoReconnectEnabled,
                currentFrame = currentFrame,
                vncClient = vncClient,
                onConnect = { ip, port -> connectToServer(ip, port) },
                onDisconnect = { disconnectFromServer() },
                onToggleAutoReconnect = { toggleAutoReconnect() },
                onBack = { finish() }
            )
        }
    }

    private fun connectToServer(ip: String, port: Int) {
        // Store connection details for auto reconnect
        lastConnectionIp = ip
        lastConnectionPort = port
        connectionAttempts = 0
        
        // Ensure auto reconnect is enabled for new connections
        if (!autoReconnectEnabled) {
            autoReconnectEnabled = true
            val sharedPrefs = getSharedPreferences("vnc_client_settings", MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("auto_reconnect_enabled", true).apply()
        }
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vncClient = VncClient(ip, port)
                    vncClient?.setListener(object : VncClient.VncClientListener {
                        override fun onFrameReceived(bitmap: Bitmap) {
                            // Update UI with new frame
                            currentFrame = bitmap
                        }
                        
                        override fun onConnectionStateChanged(connected: Boolean) {
                            isConnected = connected
                            if (!connected && autoReconnectEnabled && !isReconnecting) {
                                // Connection lost - trigger auto reconnect
                                triggerAutoReconnect()
                            }
                        }
                        
                        override fun onError(error: String) {
                            if (autoReconnectEnabled && !isReconnecting) {
                                // Error occurred - trigger auto reconnect
                                triggerAutoReconnect()
                            }
                        }
                    })
                    
                    val success = vncClient?.connect() ?: false
                    
                    withContext(Dispatchers.Main) {
                        if (success) {
                            isConnected = true
                            isReconnecting = false
                            connectionAttempts = 0
                            Toast.makeText(this@ClientActivity, "Connected to $ip:$port", Toast.LENGTH_SHORT).show()
                            
                            // Start requesting frames
                            startFrameUpdates()
                        } else {
                            if (autoReconnectEnabled) {
                                triggerAutoReconnect()
                            } else {
                                Toast.makeText(this@ClientActivity, "Failed to connect to $ip:$port", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (autoReconnectEnabled && !isReconnecting) {
                        triggerAutoReconnect()
                    } else {
                        Toast.makeText(this@ClientActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun triggerAutoReconnect() {
        if (!autoReconnectEnabled || isReconnecting || lastConnectionIp.isEmpty()) {
            return
        }
        
        isReconnecting = true
        isConnected = false
        
        lifecycleScope.launch {
            attemptReconnect()
        }
    }
    
    private suspend fun attemptReconnect() {
        while (isReconnecting && connectionAttempts < maxReconnectAttempts && autoReconnectEnabled) {
            connectionAttempts++
            
            Log.d("VncClient", "Auto reconnect attempt $connectionAttempts of $maxReconnectAttempts")
            
            try {
                // Clean up previous connection
                withContext(Dispatchers.IO) {
                    vncClient?.disconnect()
                    vncClient = null
                }
                
                // Wait before reconnect attempt
                kotlinx.coroutines.delay(reconnectDelayMs)
                
                if (!isReconnecting || !autoReconnectEnabled) break
                
                // Attempt to reconnect
                withContext(Dispatchers.IO) {
                    vncClient = VncClient(lastConnectionIp, lastConnectionPort)
                    vncClient?.setListener(object : VncClient.VncClientListener {
                        override fun onFrameReceived(bitmap: Bitmap) {
                            currentFrame = bitmap
                        }
                        
                        override fun onConnectionStateChanged(connected: Boolean) {
                            isConnected = connected
                            if (!connected && autoReconnectEnabled && !isReconnecting) {
                                triggerAutoReconnect()
                            }
                        }
                        
                        override fun onError(error: String) {
                            if (autoReconnectEnabled && !isReconnecting) {
                                triggerAutoReconnect()
                            }
                        }
                    })
                    
                    val success = vncClient?.connect() ?: false
                    
                    withContext(Dispatchers.Main) {
                        if (success) {
                            isConnected = true
                            isReconnecting = false
                            connectionAttempts = 0
                            Toast.makeText(this@ClientActivity, "Reconnected to $lastConnectionIp:$lastConnectionPort", Toast.LENGTH_SHORT).show()
                            
                            // Start requesting frames
                            startFrameUpdates()
                            return@withContext
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VncClient", "Reconnect attempt $connectionAttempts failed: ${e.message}")
            }
        }
        
        // If we reach here, all reconnect attempts failed
        if (connectionAttempts >= maxReconnectAttempts) {
            withContext(Dispatchers.Main) {
                isReconnecting = false
                Toast.makeText(this@ClientActivity, "Auto reconnect failed after $maxReconnectAttempts attempts", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startFrameUpdates() {
        lifecycleScope.launch {
            while (isConnected && !isReconnecting) {
                try {
                    withContext(Dispatchers.IO) {
                        vncClient?.requestFramebufferUpdate()
                        val frame = vncClient?.receiveFrame()
                        frame?.let { 
                            withContext(Dispatchers.Main) {
                                currentFrame = it
                            }
                        }
                    }
                    // Wait before next frame request (based on frame rate)
                    kotlinx.coroutines.delay(1000 / vncConfig.frameRate.toLong())
                } catch (e: Exception) {
                    Log.e("VncClient", "Frame update error: ${e.message}")
                    
                    // Connection might be lost - trigger auto reconnect if enabled
                    if (autoReconnectEnabled && !isReconnecting) {
                        withContext(Dispatchers.Main) {
                            triggerAutoReconnect()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ClientActivity, "Frame update error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    break
                }
            }
        }
    }

    private fun disconnectFromServer() {
        lifecycleScope.launch {
            try {
                // Disable auto reconnect during manual disconnect
                autoReconnectEnabled = false
                isConnected = false
                isReconnecting = false
                currentFrame = null
                
                withContext(Dispatchers.IO) {
                    vncClient?.disconnect()
                    vncClient = null
                }
                Toast.makeText(this@ClientActivity, "Disconnected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ClientActivity, "Disconnect error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun toggleAutoReconnect() {
        autoReconnectEnabled = !autoReconnectEnabled
        
        // Save preference
        val sharedPrefs = getSharedPreferences("vnc_client_settings", MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("auto_reconnect_enabled", autoReconnectEnabled).apply()
        
        Toast.makeText(this, "Auto reconnect: ${if (autoReconnectEnabled) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        
        // If we just enabled auto reconnect and we're not connected, try to reconnect
        if (autoReconnectEnabled && !isConnected && !isReconnecting && lastConnectionIp.isNotEmpty()) {
            triggerAutoReconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vncClient?.disconnect()
    }
}

@Composable
fun ClientScreen(
    isConnected: Boolean,
    isReconnecting: Boolean,
    connectionAttempts: Int,
    maxAttempts: Int,
    autoReconnectEnabled: Boolean,
    currentFrame: Bitmap?,
    vncClient: VncClient?,
    onConnect: (String, Int) -> Unit,
    onDisconnect: () -> Unit,
    onToggleAutoReconnect: () -> Unit,
    onBack: () -> Unit
) {
    var serverIp by remember { mutableStateOf("192.168.1.100") }
    var serverPort by remember { mutableStateOf("5900") }
    var isConnecting by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) {
                Text("← Back")
            }
            
            Text(
                text = "VNC Mirror Client",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = if (isConnected) "Connected" 
                      else if (isReconnecting) "Reconnecting ($connectionAttempts/$maxAttempts)"
                      else "Disconnected",
                color = if (isConnected) Color.Green 
                       else if (isReconnecting) Color.Yellow
                       else Color.Red,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isConnected) {
            // Connection form
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Screen Mirror (View Only)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Connect to view remote screen without control",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = { serverIp = it },
                        label = { Text("Server IP Address") },
                        placeholder = { Text("e.g. 192.168.1.100") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isConnecting
                    )
                    
                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text("Server Port") },
                        placeholder = { Text("5900") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isConnecting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Button(
                        onClick = {
                            if (serverIp.isBlank()) {
                                Toast.makeText(context, "Please enter server IP", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val port = serverPort.toIntOrNull() ?: 5900
                            isConnecting = true
                            onConnect(serverIp, port)
                            isConnecting = false
                        },
                        enabled = !isConnecting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isConnecting) "Connecting..." else "Connect")
                    }
                    
                    // Auto Reconnect Toggle - DEFAULT ON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto Reconnect",
                                fontSize = 14.sp,
                                color = if (autoReconnectEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (autoReconnectEnabled) FontWeight.Medium else FontWeight.Normal
                            )
                            Text(
                                text = if (autoReconnectEnabled) "✓ Will auto reconnect if disconnected" else "Manual reconnect only",
                                fontSize = 12.sp,
                                color = if (autoReconnectEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoReconnectEnabled,
                            onCheckedChange = { onToggleAutoReconnect() },
                            enabled = !isConnecting
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instructions:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Make sure both devices are on the same WiFi network\n" +
                              "2. Get the server IP and port from the server device\n" +
                              "3. Enter the IP and port above\n" +
                              "4. Tap Connect to view the remote screen",
                        fontSize = 14.sp
                    )
                }
            }
            
        } else {
            // Connected view - Screen display area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Display received frame or placeholder - MIRROR ONLY (NO TOUCH INPUT)
                    if (currentFrame != null) {
                        Image(
                            bitmap = currentFrame.asImageBitmap(),
                            contentDescription = "Remote Screen Mirror",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                                        
                    } else {
                        // Placeholder while waiting for frames
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) { 
                            drawRect(
                                color = Color.DarkGray,
                                size = size
                            )
                        }
                        
                        Text(
                            text = "🔗 Connected\nWaiting for screen data...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Remote Control Info
            if (isConnected && currentFrame != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎮 Remote Control Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• Tap to click • Drag to move cursor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Disconnect button
            Button(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Disconnect", color = Color.White)
            }
        }
    }
}
