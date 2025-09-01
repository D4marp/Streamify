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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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
    
    private lateinit var vncConfig: VncConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vncConfig = VncConfig(this)
        
        setContent {
            ClientScreen(
                isConnected = isConnected,
                isReconnecting = isReconnecting,
                connectionAttempts = connectionAttempts,
                maxAttempts = maxReconnectAttempts,
                currentFrame = currentFrame,
                vncClient = vncClient,
                onConnect = { ip, port -> connectToServer(ip, port) },
                onDisconnect = { disconnectFromServer() },
                onBack = { finish() }
            )
        }
    }

    private fun connectToServer(ip: String, port: Int) {
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
                        }
                        
                        override fun onError(error: String) {
                            lifecycleScope.launch {
                                Toast.makeText(this@ClientActivity, "VNC Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                    
                    val success = vncClient?.connect() ?: false
                    
                    withContext(Dispatchers.Main) {
                        if (success) {
                            isConnected = true
                            Toast.makeText(this@ClientActivity, "Connected to $ip:$port", Toast.LENGTH_SHORT).show()
                            
                            // Start requesting frames
                            startFrameUpdates()
                        } else {
                            Toast.makeText(this@ClientActivity, "Failed to connect to $ip:$port", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ClientActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun startFrameUpdates() {
        lifecycleScope.launch {
            while (isConnected) {
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
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ClientActivity, "Frame update error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    break
                }
            }
        }
    }

    private fun disconnectFromServer() {
        lifecycleScope.launch {
            try {
                isConnected = false
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
    currentFrame: Bitmap?,
    vncClient: VncClient?,
    onConnect: (String, Int) -> Unit,
    onDisconnect: () -> Unit,
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
                text = "VNC Client",
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
                        text = "Connect to VNC Server",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
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
                    // Display received frame or placeholder
                    if (currentFrame != null) {
                        Image(
                            bitmap = currentFrame.asImageBitmap(),
                            contentDescription = "Remote Screen",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            // Calculate touch coordinates relative to frame
                                            val frameWidth = currentFrame.width
                                            val frameHeight = currentFrame.height
                                            
                                            // Scale touch coordinates to match remote screen
                                            val scaleX = frameWidth.toFloat() / size.width
                                            val scaleY = frameHeight.toFloat() / size.height
                                            
                                            val remoteX = (offset.x * scaleX).toInt()
                                            val remoteY = (offset.y * scaleY).toInt()
                                            
                                            Log.d("ClientActivity", "Touch detected at ($remoteX, $remoteY)")
                                            
                                            // Send tap as mouse click (button down + up) in background thread
                                            vncClient?.let { client ->
                                                Thread {
                                                    Log.d("ClientActivity", "Sending pointer down at ($remoteX, $remoteY)")
                                                    client.sendPointerEvent(remoteX, remoteY, 1) // Button down
                                                    Thread.sleep(50) // Short delay
                                                    Log.d("ClientActivity", "Sending pointer up at ($remoteX, $remoteY)")
                                                    client.sendPointerEvent(remoteX, remoteY, 0) // Button up
                                                }.start()
                                            }
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        // Handle drag as mouse move
                                        val frameWidth = currentFrame.width
                                        val frameHeight = currentFrame.height
                                        
                                        val scaleX = frameWidth.toFloat() / size.width
                                        val scaleY = frameHeight.toFloat() / size.height
                                        
                                        val remoteX = (change.position.x * scaleX).toInt()
                                        val remoteY = (change.position.y * scaleY).toInt()
                                        
                                        Log.d("ClientActivity", "Drag detected at ($remoteX, $remoteY)")
                                        
                                        // Send pointer move (no button pressed) in background thread
                                        vncClient?.let { client ->
                                            Thread {
                                                client.sendPointerEvent(remoteX, remoteY, 0)
                                            }.start()
                                        }
                                    }
                                }
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
