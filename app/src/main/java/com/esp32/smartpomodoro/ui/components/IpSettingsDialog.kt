package com.esp32.smartpomodoro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esp32.smartpomodoro.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun IpSettingsDialog(
    currentHost: String,
    onDismiss: () -> Unit,
    onSaveHost: (String) -> Unit,
    onTestConnection: suspend (String) -> Boolean
) {
    var hostInput by remember { mutableStateOf(currentHost) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "ESP32 Connection Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure the IP address or mDNS hostname of your ESP32 controller.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { hostInput = "192.168.4.1" },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hotspot AP", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { hostInput = "pomodoro.local" },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("mDNS Local", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = {
                        hostInput = it
                        testStatus = null
                    },
                    label = { Text("IP Address or Hostname") },
                    placeholder = { Text("e.g. 192.168.1.120 or 192.168.4.1") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                testStatus = null
                                val ok = onTestConnection(hostInput)
                                testStatus = if (ok) "Connected Successfully! ✓" else "Connection Failed ✗"
                                isTesting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isTesting && hostInput.isNotBlank()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test Ping", color = AccentCyan)
                        }
                    }

                    testStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (status.contains("Successfully")) AccentGreen else AccentRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveHost(hostInput) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save & Connect", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
