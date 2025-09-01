package com.app.streamify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vncConfig = remember { VncConfig(context) }
    
    var frameRate by remember { mutableIntStateOf(vncConfig.frameRate) }
    var imageQuality by remember { mutableIntStateOf(vncConfig.imageQuality) }
    var autoReconnect by remember { mutableStateOf(vncConfig.autoReconnect) }
    var serverPort by remember { mutableIntStateOf(vncConfig.serverPort) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Server Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Server Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Server Port
                Text(text = "Server Port", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = serverPort.toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { port ->
                            if (vncConfig.isValidPort(port)) {
                                serverPort = port
                                vncConfig.serverPort = port
                            }
                        }
                    },
                    label = { Text("Port (${VncConfig.MIN_PORT}-${VncConfig.MAX_PORT})") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Performance Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Performance Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Frame Rate
                Text(text = "Frame Rate: ${frameRate} FPS", fontWeight = FontWeight.Medium)
                Slider(
                    value = frameRate.toFloat(),
                    onValueChange = { 
                        frameRate = it.toInt()
                        vncConfig.frameRate = frameRate
                    },
                    valueRange = VncConfig.MIN_FRAME_RATE.toFloat()..VncConfig.MAX_FRAME_RATE.toFloat(),
                    steps = (VncConfig.MAX_FRAME_RATE - VncConfig.MIN_FRAME_RATE) / 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Lower = Better performance, Higher = Smoother video",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image Quality
                Text(text = "Image Quality: ${imageQuality}%", fontWeight = FontWeight.Medium)
                Slider(
                    value = imageQuality.toFloat(),
                    onValueChange = { 
                        imageQuality = it.toInt()
                        vncConfig.imageQuality = imageQuality
                    },
                    valueRange = VncConfig.MIN_IMAGE_QUALITY.toFloat()..VncConfig.MAX_IMAGE_QUALITY.toFloat(),
                    steps = 7,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Lower = Faster transmission, Higher = Better quality",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Client Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Client Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Auto Reconnect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Auto Reconnect", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Automatically reconnect if connection is lost",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoReconnect,
                        onCheckedChange = { 
                            autoReconnect = it
                            vncConfig.autoReconnect = autoReconnect
                        }
                    )
                }
            }
        }
        
        // Performance Tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Performance Tips",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Use same WiFi network for best performance\n" +
                          "• Lower frame rate for slower devices\n" +
                          "• Reduce quality for better network performance\n" +
                          "• Close other apps to free memory\n" +
                          "• Use 5GHz WiFi if available",
                    fontSize = 12.sp
                )
            }
        }
        
        // Reset to Defaults
        Button(
            onClick = {
                frameRate = VncConfig.DEFAULT_FRAME_RATE
                imageQuality = VncConfig.DEFAULT_IMAGE_QUALITY
                autoReconnect = true
                serverPort = VncConfig.DEFAULT_PORT
                
                vncConfig.frameRate = frameRate
                vncConfig.imageQuality = imageQuality
                vncConfig.autoReconnect = autoReconnect
                vncConfig.serverPort = serverPort
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset to Defaults")
        }
    }
}
