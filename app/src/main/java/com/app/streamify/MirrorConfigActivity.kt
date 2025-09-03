package com.app.streamify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.colorResource
import com.app.streamify.ui.theme.StreamifyTheme

class MirrorConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StreamifyTheme {
                MirrorConfigScreen(
                    onConnect = { ip, port ->
                        if (isValidIp(ip) && port in 1..65535) {
                            val intent = Intent(this@MirrorConfigActivity, ClientActivity::class.java)
                            intent.putExtra("SERVER_IP", ip)
                            intent.putExtra("SERVER_PORT", port)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@MirrorConfigActivity, "Invalid IP or Port", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun isValidIp(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            parts.all { part ->
                val num = part.toIntOrNull()
                num != null && num in 0..255
            }
        } catch (e: Exception) {
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirrorConfigScreen(
    onConnect: (String, Int) -> Unit,
    onCancel: () -> Unit
) {
    var serverIp by remember { mutableStateOf("192.168.1.100") }
    var serverPort by remember { mutableStateOf("5901") }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.card_dark)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mirror Configuration",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.light)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Enter the IP address and port of the mirror server",
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.light),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = { serverIp = it },
                        label = { Text("Server IP Address", color = colorResource(id = R.color.light)) },
                        placeholder = { Text("192.168.1.100", color = colorResource(id = R.color.light).copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorResource(id = R.color.light),
                            unfocusedTextColor = colorResource(id = R.color.light),
                            focusedBorderColor = colorResource(id = R.color.blue),
                            unfocusedBorderColor = colorResource(id = R.color.light)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text("Server Port", color = colorResource(id = R.color.light)) },
                        placeholder = { Text("5901", color = colorResource(id = R.color.light).copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorResource(id = R.color.light),
                            unfocusedTextColor = colorResource(id = R.color.light),
                            focusedBorderColor = colorResource(id = R.color.blue),
                            unfocusedBorderColor = colorResource(id = R.color.light)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.dark)
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                color = colorResource(id = R.color.light),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                val port = serverPort.toIntOrNull() ?: 5901
                                onConnect(serverIp, port)
                            },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.blue)
                            )
                        ) {
                            Text(
                                text = "Connect",
                                color = colorResource(id = R.color.light),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Make sure the mirror server is running on the target device",
                fontSize = 12.sp,
                color = colorResource(id = R.color.light).copy(alpha = 0.7f)
            )
        }
    }
}
