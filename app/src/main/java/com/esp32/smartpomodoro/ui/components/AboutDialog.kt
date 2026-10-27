package com.esp32.smartpomodoro.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.ui.theme.*

@Composable
fun AboutDialog(
    hostAddress: String,
    isConnected: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .border(1.dp, AccentCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🍅", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Smart Pomodoro",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Desk Environment System",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }

                // Version Tag Badge
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = AccentGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "v3.2",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        ),
                        color = AccentGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Version & Build Metadata Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Release Version", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("v3.2.0 (Build 32)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Hardware", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("ESP32-D0WD-V3 DevKit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Desk Host Link", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = if (isConnected) "$hostAddress (Online)" else "$hostAddress (Offline)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isConnected) AccentGreen else AccentRed
                            )
                        }
                    }
                }

                // Creator & Staff Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Author",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "PROJECT CREATOR & STAFF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = AccentCyan
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Author & Maintainer", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Ishara (mac_knight141)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GitHub Profile", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                text = "@isharax9",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentCyan,
                                modifier = Modifier.clickable { openUrl("https://github.com/isharax9") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("AI Pair Programmer", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("DeepMind Antigravity", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        }
                    }
                }

                // Bug Reporting & Issues Button
                Button(
                    onClick = { openUrl("https://github.com/isharax9/Esp32/issues/new") },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Report Bug",
                        tint = AccentRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Report a Bug / Request Feature",
                        color = AccentRed,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // GitHub Repositories Link Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { openUrl("https://github.com/isharax9/Esp32") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = "Firmware", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Firmware", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { openUrl("https://github.com/isharax9/esp32-smart-pomodoro-android") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = "App Repo", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Android App", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}
