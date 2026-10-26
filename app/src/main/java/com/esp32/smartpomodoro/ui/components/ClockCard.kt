package com.esp32.smartpomodoro.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.data.model.*
import com.esp32.smartpomodoro.ui.theme.*

@Composable
fun ClockCard(
    uiState: ClockUiState,
    onCycleRotation: () -> Unit,
    onSelectRotation: (ClockRotationAngle) -> Unit,
    onToggleOrientation: () -> Unit,
    onToggleAutoRotate: () -> Unit = {},
    onCycleTheme: () -> Unit,
    onToggleTimeFormat: () -> Unit,
    onCycleSeconds: () -> Unit,
    onToggleNightDimming: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyTime: () -> Unit
) {
    val snapshot = uiState.snapshot
    val isLandscape = uiState.rotationAngle.isLandscape

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = uiState.effectiveBackgroundColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP CONTROLS: Rotation Angle Selector, Theme, and Fullscreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multi-Orientation Angle Selector (Auto, 0°, 90°, 180°, 270°) and orientation toggles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Auto-Rotate pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (uiState.isAutoRotate) uiState.theme.primaryColor else SurfaceDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleAutoRotate() }
                    ) {
                        Text(
                            text = "Auto",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (uiState.isAutoRotate) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (uiState.isAutoRotate) BackgroundDark else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                        )
                    }

                    ClockRotationAngle.entries.forEach { angle ->
                        val isSelected = !uiState.isAutoRotate && uiState.rotationAngle == angle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) uiState.theme.primaryColor else SurfaceDark,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectRotation(angle) }
                        ) {
                            Text(
                                text = "${angle.degrees}°",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) BackgroundDark else TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Quick Rotate 90° button
                    IconButton(
                        onClick = onCycleRotation,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = uiState.theme.primaryColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Orientation toggle (Landscape / Portrait) button
                    IconButton(
                        onClick = onToggleOrientation,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Toggle Landscape/Portrait",
                            tint = if (uiState.rotationAngle.isLandscape) uiState.theme.primaryColor else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Action icons: Theme, Settings, Fullscreen
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Theme Swatch Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(uiState.theme.primaryColor)
                            .clickable { onCycleTheme() }
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme",
                            tint = BackgroundDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Settings Dialog Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Fullscreen Button
                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen Bedside Clock",
                            tint = uiState.theme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceCardBorder)

            // 2. GIANT TIME READOUT CONTAINER (Interactive: tap to expand fullscreen)
            val digitFontSize = when {
                isLandscape && uiState.secondsDisplay == ClockSecondsDisplay.BIG -> 22.sp
                isLandscape -> 30.sp
                uiState.secondsDisplay == ClockSecondsDisplay.BIG -> 40.sp
                else -> 52.sp
            }
            val smallSecFontSize = if (isLandscape) 14.sp else 22.sp
            val amPmFontSize = if (isLandscape) 9.sp else 11.sp
            val dateFontSize = if (isLandscape) 10.sp else 12.sp

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 220.dp else 160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onToggleFullScreen() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val boxW = maxWidth
                val boxH = maxHeight
                val contentW = if (isLandscape) boxH else boxW
                val contentH = if (isLandscape) boxW else boxH

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(width = contentW, height = contentH)
                        .graphicsLayer {
                            rotationZ = uiState.rotationAngle.degrees.toFloat()
                        }
                ) {
                    // Time Digits Row
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Hours
                        Text(
                            text = snapshot.hours,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = digitFontSize,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            ),
                            color = uiState.effectiveColor
                        )

                        // Blinking Colon
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = digitFontSize,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (snapshot.colonVisible) uiState.effectiveColor else Color.Transparent,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )

                        // Minutes
                        Text(
                            text = snapshot.minutes,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = digitFontSize,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            ),
                            color = uiState.effectiveColor
                        )

                        // Optional Seconds
                        when (uiState.secondsDisplay) {
                            ClockSecondsDisplay.BIG -> {
                                Text(
                                    text = ":",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = digitFontSize,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (snapshot.colonVisible) uiState.effectiveColor else Color.Transparent,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                                Text(
                                    text = snapshot.seconds,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = digitFontSize,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = uiState.effectiveSecondaryColor
                                )
                            }
                            ClockSecondsDisplay.SMALL -> {
                                Text(
                                    text = snapshot.seconds,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = smallSecFontSize,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = uiState.effectiveSecondaryColor,
                                    modifier = Modifier.padding(bottom = if (isLandscape) 4.dp else 8.dp, start = 4.dp)
                                )
                            }
                            ClockSecondsDisplay.NONE -> {
                                // Hidden
                            }
                        }

                        // AM/PM Indicator
                        if (snapshot.amPm.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = uiState.effectiveColor.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .padding(bottom = if (isLandscape) 6.dp else 10.dp, start = 6.dp)
                            ) {
                                Text(
                                    text = snapshot.amPm,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = amPmFontSize
                                    ),
                                    color = uiState.effectiveColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Optional Date & Day of Week
                    if (uiState.showDate && snapshot.dateFormatted.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${snapshot.dayOfWeek}, ${snapshot.dateFormatted}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = dateFontSize
                            ),
                            color = uiState.effectiveSecondaryColor
                        )
                    }
                }
            }

            // 3. BOTTOM QUICK CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Toggle Chips: 12/24H, Seconds Mode, Night Mode
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 12h / 24h Toggle Chip
                    AssistChip(
                        onClick = onToggleTimeFormat,
                        label = {
                            Text(
                                text = if (uiState.timeFormat == ClockTimeFormat.TWELVE_HOUR) "12H" else "24H",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SurfaceDark,
                            labelColor = TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    )

                    // Seconds Mode Chip
                    AssistChip(
                        onClick = onCycleSeconds,
                        label = {
                            Text(
                                text = "Sec: ${uiState.secondsDisplay.name.take(3)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SurfaceDark,
                            labelColor = TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    )

                    // Night Dimming Toggle Chip
                    AssistChip(
                        onClick = onToggleNightDimming,
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.nightDimming) Icons.Default.Bedtime else Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = if (uiState.nightDimming) AccentAmber else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (uiState.nightDimming) "Night ON" else "Day",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (uiState.nightDimming) AccentAmber.copy(alpha = 0.2f) else SurfaceDark,
                            labelColor = if (uiState.nightDimming) AccentAmber else TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.nightDimming) AccentAmber else SurfaceCardBorder
                        )
                    )
                }

                // Copy Time Button
                IconButton(
                    onClick = onCopyTime,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Time",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
