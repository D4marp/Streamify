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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrivacyPolicyScreen(
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
                text = "Privacy Policy",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(56.dp)) // Balance layout
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Streamify - Screen Sharing App",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last updated: September 2, 2025",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Privacy sections
        PrivacySection(
            title = "What is Streamify?",
            content = "Streamify is a local screen sharing application that allows you to share your Android device screen with another Android device on the same Wi-Fi network. It also supports remote control functionality."
        )
        
        PrivacySection(
            title = "Data Collection",
            content = "• NO personal data is collected\n• NO data is sent to external servers\n• NO user tracking or analytics\n• All communication happens locally on your Wi-Fi network"
        )
        
        PrivacySection(
            title = "Permissions Used",
            content = "• Screen Recording: To capture and share your screen\n• Accessibility Service: To enable remote control functionality\n• Network Access: To communicate between devices on local network\n• Foreground Service: To keep screen sharing active in background"
        )
        
        PrivacySection(
            title = "Security",
            content = "• All data transmission happens locally on your Wi-Fi network\n• No internet connection required for core functionality\n• No cloud storage or external servers involved\n• You control when to start and stop sharing"
        )
        
        PrivacySection(
            title = "Remote Control",
            content = "• Remote control only works when YOU explicitly enable it\n• Accessibility service is used ONLY for touch simulation\n• No sensitive data is accessed or transmitted\n• You can disable remote control anytime in Android Settings"
        )
        
        PrivacySection(
            title = "Contact",
            content = "This is an educational/development project. For questions or concerns, please refer to the project documentation or repository."
        )
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 14.sp
            )
        }
    }
}
