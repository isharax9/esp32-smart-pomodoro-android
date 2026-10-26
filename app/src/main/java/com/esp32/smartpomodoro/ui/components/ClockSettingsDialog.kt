package com.esp32.smartpomodoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.esp32.smartpomodoro.data.model.*
import com.esp32.smartpomodoro.ui.theme.*

@Composable
fun ClockSettingsDialog(
    uiState: ClockUiState,
    onTimeFormatChange: (ClockTimeFormat) -> Unit,
    onSecondsDisplayChange: (ClockSecondsDisplay) -> Unit,
    onFontFamilyChange: (ClockFontFamily) -> Unit = {},
    onFontSizeChange: (Float) -> Unit = {},
    onToggleShowDate: () -> Unit,
    onToggleBlinkingColon: () -> Unit,
    onToggleAutoRotate: () -> Unit = {},
    onSelectRotation: (ClockRotationAngle) -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    onToggleNightDimming: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onThemeSelect: (ClockTheme) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Clock Settings",
                            tint = uiState.theme.primaryColor
                        )
                        Text(
                            text = "Clock Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = SurfaceCardBorder)

                // 1. Time Format: 12-Hour vs 24-Hour
                Text(
                    text = "TIME FORMAT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockTimeFormat.entries.forEach { format ->
                        val isSelected = uiState.timeFormat == format
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) uiState.theme.primaryColor else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTimeFormatChange(format) }
                        ) {
                            Text(
                                text = format.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) BackgroundDark else TextSecondary,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // 2. Seconds Display
                Text(
                    text = "SECONDS DISPLAY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockSecondsDisplay.entries.forEach { mode ->
                        val isSelected = uiState.secondsDisplay == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) uiState.theme.primaryColor else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSecondsDisplayChange(mode) }
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) BackgroundDark else TextSecondary,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // 3. Font Family Selector
                HorizontalDivider(color = SurfaceCardBorder)
                Text(
                    text = "FONT FAMILY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockFontFamily.entries.forEach { font ->
                        val isSelected = uiState.fontFamily == font
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) uiState.theme.primaryColor else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onFontFamilyChange(font) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "12:00",
                                    fontFamily = font.toComposeFontFamily(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) BackgroundDark else uiState.theme.primaryColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = font.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) BackgroundDark else TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // 4. Text & Numbers Size Selector
                HorizontalDivider(color = SurfaceCardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEXT & NUMBERS SIZE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                    Text(
                        text = "${(uiState.fontSizeScale * 100).toInt()}%",
                        color = uiState.theme.primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Size Scale Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockFontSizeScale.entries.forEach { preset ->
                        val isSelected = kotlin.math.abs(uiState.fontSizeScale - preset.scaleFactor) < 0.05f
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) uiState.theme.primaryColor else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onFontSizeChange(preset.scaleFactor) }
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) BackgroundDark else TextSecondary,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Size Slider
                Slider(
                    value = uiState.fontSizeScale,
                    onValueChange = onFontSizeChange,
                    valueRange = 0.75f..1.40f,
                    colors = SliderDefaults.colors(
                        thumbColor = uiState.theme.primaryColor,
                        activeTrackColor = uiState.theme.primaryColor,
                        inactiveTrackColor = SurfaceDark
                    )
                )

                // 3. Toggles: Blinking Colon, Date, Screen On
                HorizontalDivider(color = SurfaceCardBorder)
                Text(
                    text = "DISPLAY BEHAVIOR",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )

                // Blinking Colon Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Blinking Colon", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Pulse colon separators every second", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.blinkingColon,
                        onCheckedChange = { onToggleBlinkingColon() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = uiState.theme.primaryColor
                        )
                    )
                }

                // Show Date Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Show Date & Day", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Display calendar date beneath digits", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.showDate,
                        onCheckedChange = { onToggleShowDate() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = uiState.theme.primaryColor
                        )
                    )
                }

                // Keep Screen On Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Keep Screen On (Desk Mode)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Prevent sleep while clock is active", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.keepScreenOn,
                        onCheckedChange = { onToggleKeepScreenOn() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = uiState.theme.primaryColor
                        )
                    )
                }

                // 4. Orientation & Multi-Angle Rotation
                HorizontalDivider(color = SurfaceCardBorder)
                Text(
                    text = "ORIENTATION & ROTATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )

                // Auto-Rotate Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Rotate (Follow Sensor)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Automatically rotate clock when device turns", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.isAutoRotate,
                        onCheckedChange = { onToggleAutoRotate() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = uiState.theme.primaryColor
                        )
                    )
                }

                // Screen Angle Selection Pills & Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClockRotationAngle.entries.forEach { angle ->
                        val isSelected = !uiState.isAutoRotate && uiState.rotationAngle == angle
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) uiState.theme.primaryColor else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectRotation(angle) }
                        ) {
                            Text(
                                text = "${angle.degrees}°",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) BackgroundDark else TextSecondary,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Quick Toggle Orientation Button
                    IconButton(
                        onClick = onToggleOrientation,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Toggle Orientation",
                            tint = uiState.theme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 5. Night Mode Dimming & Brightness
                HorizontalDivider(color = SurfaceCardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Night Mode Bedside Dimming", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Deep OLED black with eye-comfort dimming", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = uiState.nightDimming,
                        onCheckedChange = { onToggleNightDimming() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = uiState.theme.primaryColor
                        )
                    )
                }

                // Brightness Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Brightness", color = TextSecondary, fontSize = 12.sp)
                        Text("${(uiState.brightnessFactor * 100).toInt()}%", color = uiState.theme.primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.brightnessFactor,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = uiState.theme.primaryColor,
                            activeTrackColor = uiState.theme.primaryColor,
                            inactiveTrackColor = SurfaceDark
                        )
                    )
                }

                // 5. Themes Selection
                HorizontalDivider(color = SurfaceCardBorder)
                Text(
                    text = "COLOR PALETTE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    items(ClockTheme.entries) { theme ->
                        val isSelected = uiState.theme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onThemeSelect(theme) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) TextPrimary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BackgroundDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.title.substringBefore(" "),
                                fontSize = 10.sp,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = uiState.theme.primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Done",
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
