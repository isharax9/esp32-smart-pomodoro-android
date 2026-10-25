package com.esp32.smartpomodoro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esp32.smartpomodoro.ui.theme.*

@Composable
fun CalibrationDialog(
    onDismiss: () -> Unit,
    onCalibrate: (strip: String, action: String) -> Unit
) {
    var selectedStrip by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "RGB Pin Calibration & Test",
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
                    text = "If colors are inverted (e.g. Red lights up Green), swap channels below:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                // Strip Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStrip == "1",
                        onClick = { selectedStrip = "1" },
                        label = { Text("Strip 1 (Backlight)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan,
                            selectedLabelColor = BackgroundDark,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedStrip == "2",
                        onClick = { selectedStrip = "2" },
                        label = { Text("Strip 2 (Monitor)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan,
                            selectedLabelColor = BackgroundDark,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Channel Swapping",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCalibrate(selectedStrip, "swap_rg") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Swap R-G", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onCalibrate(selectedStrip, "swap_gb") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Swap G-B", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onCalibrate(selectedStrip, "swap_rb") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Swap R-B", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (selectedStrip == "2") {
                    Text(
                        text = "Strip 2 Green Pin Mapping",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onCalibrate("2", "set_green_d5") },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Use GPIO 5", color = AccentGreen, style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = { onCalibrate("2", "set_green_d17") },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Use GPIO 17", color = AccentGreen, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        }
    )
}
