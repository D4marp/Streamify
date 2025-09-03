package com.app.streamify

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullScreenMirrorActivity : ComponentActivity() {
    companion object {
        private const val TAG = "FullScreenMirror"
    }
    
    private var vncClient: VncClient? = null
    private var isConnected by mutableStateOf(false)
    private var isReconnecting by mutableStateOf(false)
    private var connectionAttempts by mutableIntStateOf(0)
    private val maxReconnectAttempts = Int.MAX_VALUE // Unlimited reconnect attempts in full screen
    private var currentFrame by mutableStateOf<Bitmap?>(null)
    
    // Auto reconnect configuration
    private var autoReconnectEnabled = true // Always enabled in full screen
    private var lastConnectionIp = ""
    private var lastConnectionPort = 0
    private var reconnectDelayMs = 3000L // 3 seconds between attempts
    private var reconnectStartTime = 0L // Track when reconnecting started
    private val maxReconnectTimeMs = 60000L // 60 seconds total reconnect time
    
    private lateinit var vncConfig: VncConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable full screen mode
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Hide system UI
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        vncConfig = VncConfig(this)
        
        // Get connection details from intent
        val serverIp = intent.getStringExtra("server_ip") ?: return
        val serverPort = intent.getIntExtra("server_port", 5900)
        
        setContent {
            FullScreenMirrorUI(
                currentFrame = currentFrame,
                isConnected = isConnected,
                isReconnecting = isReconnecting,
                connectionAttempts = connectionAttempts,
                maxAttempts = maxReconnectAttempts
            )
        }
        
        // Auto connect immediately
        connectToServer(serverIp, serverPort)
    }

    private fun connectToServer(ip: String, port: Int) {
        // Store connection details for auto reconnect
        lastConnectionIp = ip
        lastConnectionPort = port
        connectionAttempts = 0
        
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
                            
                            // Start requesting frames
                            startFrameUpdates()
                        } else {
                            triggerAutoReconnect()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (autoReconnectEnabled && !isReconnecting) {
                        triggerAutoReconnect()
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
        reconnectStartTime = System.currentTimeMillis()
        
        lifecycleScope.launch {
            // Try to reconnect with time-based timeout
            while (isReconnecting && autoReconnectEnabled) {
                connectionAttempts++
                
                // Check if we've exceeded max reconnect time
                val elapsedTime = System.currentTimeMillis() - reconnectStartTime
                if (elapsedTime > maxReconnectTimeMs) {
                    Log.w("FullScreenMirror", "Reconnect timeout after ${elapsedTime}ms, returning to main")
                    returnToMainActivity()
                    break
                }
                
                Log.d("FullScreenMirror", "Reconnect attempt $connectionAttempts (${elapsedTime}ms elapsed)")
                
                try {
                    withContext(Dispatchers.IO) {
                        vncClient?.disconnect()
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
                        
                        if (success) {
                            withContext(Dispatchers.Main) {
                                isConnected = true
                                isReconnecting = false
                                connectionAttempts = 0
                                
                                // Start requesting frames
                                startFrameUpdates()
                                return@withContext
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FullScreenMirror", "Reconnect attempt $connectionAttempts failed: ${e.message}")
                }
                
                // Wait before next attempt (exponential backoff, max 8 seconds)
                kotlinx.coroutines.delay(minOf(reconnectDelayMs * connectionAttempts, 8000))
                
                if (!isReconnecting || !autoReconnectEnabled) break
            }
            
            // If we reach here without successful connection, return to main
            if (isReconnecting) {
                Log.w("FullScreenMirror", "Reconnect failed, returning to main activity")
                returnToMainActivity()
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
                    Log.e("FullScreenMirror", "Frame update error: ${e.message}")
                    
                    // Connection might be lost - trigger auto reconnect if enabled
                    if (autoReconnectEnabled && !isReconnecting) {
                        withContext(Dispatchers.Main) {
                            triggerAutoReconnect()
                        }
                    } else {
                        break
                    }
                }
            }
        }
    }
    
    private fun returnToMainActivity() {
        isReconnecting = false
        autoReconnectEnabled = false
        
        // Disconnect VNC client
        vncClient?.disconnect()
        
        // Return to MainActivity with a flag indicating reconnect failure
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("reconnect_failed", true)
            putExtra("failed_ip", lastConnectionIp)
            putExtra("failed_port", lastConnectionPort)
        }
        startActivity(intent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // If not connected and not currently reconnecting, try to reconnect
        // This handles cases where HP A restarts and we need to reconnect
        if (!isConnected && !isReconnecting && lastConnectionIp.isNotEmpty()) {
            Log.d(TAG, "onResume: Attempting to reconnect to server")
            // Reset connection attempts to give fresh chances
            connectionAttempts = 0
            triggerAutoReconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't disconnect on destroy - let server control the disconnection
        // vncClient?.disconnect()
    }
    
    override fun onBackPressed() {
        // Disable back button - only server can control disconnect
        // Full screen should stay active until server disconnects
        Log.d(TAG, "Back button disabled - use server to disconnect")
    }
}

@Composable
fun FullScreenMirrorUI(
    currentFrame: Bitmap?,
    isConnected: Boolean,
    isReconnecting: Boolean,
    connectionAttempts: Int,
    maxAttempts: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentFrame != null && isConnected) {
            // Full screen mirror display - NO UI ELEMENTS
            Image(
                bitmap = currentFrame.asImageBitmap(),
                contentDescription = "Full Screen Mirror",
                contentScale = ContentScale.Crop, // Fill entire screen
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Loading/connection state with minimal UI in center
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isReconnecting) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Simple animated dots instead of CircularProgressIndicator
                        Text(
                            text = "●●●",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Reconnecting...\n($connectionAttempts/$maxAttempts)",
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (isConnected) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Simple animated dots instead of CircularProgressIndicator
                        Text(
                            text = "●●●",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connected\nWaiting for screen...",
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Simple animated dots instead of CircularProgressIndicator
                        Text(
                            text = "●●●",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting...",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
