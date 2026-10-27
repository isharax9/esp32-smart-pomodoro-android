package com.esp32.smartpomodoro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.data.model.RemoteKeyMap
import com.esp32.smartpomodoro.data.model.StripTarget
import com.esp32.smartpomodoro.ui.theme.*

@Composable
fun RgbRemoteCard(
    selectedStrip: StripTarget,
    brightness: Float,
    currentEffect: Int,
    fadeSpeed: Int = 60,
    fadePalette: Int = 0,
    smoothSpeed: Int = 75,
    smoothBreath: Int = 7,
    smoothPalette: Int = 0,
    onSelectStrip: (StripTarget) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onSendCommand: (String) -> Unit,
    onSetFadeConfig: ((speed: Int?, palette: Int?) -> Unit)? = null,
    onSetSmoothConfig: ((speed: Int?, breath: Int?, palette: Int?) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header & Strip Target Selector
            Text(
                text = "RGB LIGHTING CONTROLLER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Strip Target Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StripTarget.values().forEach { target ->
                    val isSelected = (selectedStrip == target)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStrip(target) },
                        label = {
                            Text(
                                text = target.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan,
                            selectedLabelColor = BackgroundDark,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) AccentCyan else SurfaceCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Power Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Power ON Button
                Button(
                    onClick = { onSendCommand("on") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AccentGreen)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power On",
                        tint = AccentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ON", color = AccentGreen, fontWeight = FontWeight.Bold)
                }

                // Power OFF Button
                Button(
                    onClick = { onSendCommand("off") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AccentRed)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power Off",
                        tint = AccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OFF", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brightness Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessLow,
                    contentDescription = "Low Brightness",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = brightness,
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    valueRange = 10f..255f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = SurfaceDark
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.BrightnessHigh,
                    contentDescription = "High Brightness",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 24-Key Virtual Remote Palette Matrix (4 columns)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RemoteKeyMap.colorRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { button ->
                            if (button.isEffect) {
                                // Effect Button (Flash, Strobe, Fade, Smooth)
                                val isEffectActive = when (button.commandVal) {
                                    "flash" -> currentEffect == 1
                                    "strobe" -> currentEffect == 2
                                    "fade" -> currentEffect == 3
                                    "smooth" -> currentEffect == 4
                                    else -> false
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSendCommand(button.commandVal) },
                                    color = if (isEffectActive) AccentCyan.copy(alpha = 0.2f) else SurfaceDark,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isEffectActive) AccentCyan else SurfaceCardBorder
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = button.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isEffectActive) FontWeight.Black else FontWeight.Bold
                                            ),
                                            color = if (isEffectActive) AccentCyan else TextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // Solid Color Dot
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(button.color)
                                            .border(2.dp, SurfaceDark, CircleShape)
                                            .clickable { onSendCommand(button.commandVal) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Advanced Controls for FADE (Effect 3)
            AnimatedVisibility(
                visible = (currentEffect == 3),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                var localFadeSpeed by remember(fadeSpeed) { mutableStateOf(fadeSpeed.toFloat()) }
                val fadePalettes = listOf(
                    0 to "🌈 Rainbow",
                    1 to "🎨 Pastel",
                    2 to "🌅 Sunset",
                    3 to "🌊 Ocean"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌈 RAINBOW FADE CONTROLS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFC084FC)
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFFA855F7).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFC084FC)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Palette Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fadePalettes.forEach { (id, label) ->
                                val isSelected = (fadePalette == id)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSetFadeConfig?.invoke(null, id) },
                                    color = if (isSelected) Color(0xFFA855F7).copy(alpha = 0.3f) else SurfaceCard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFA855F7) else SurfaceCardBorder
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Circulation Speed Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Circulation Speed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${localFadeSpeed.toInt()}s", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFC084FC))
                        }

                        Slider(
                            value = localFadeSpeed,
                            onValueChange = { localFadeSpeed = it },
                            onValueChangeFinished = { onSetFadeConfig?.invoke(localFadeSpeed.toInt(), null) },
                            valueRange = 10f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFC084FC),
                                activeTrackColor = Color(0xFFA855F7),
                                inactiveTrackColor = SurfaceCard
                            )
                        )
                    }
                }
            }

            // Advanced Controls for SMOOTH (Effect 4)
            AnimatedVisibility(
                visible = (currentEffect == 4),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                var localSmoothSpeed by remember(smoothSpeed) { mutableStateOf(smoothSpeed.toFloat()) }
                var localSmoothBreath by remember(smoothBreath) { mutableStateOf(smoothBreath.toFloat()) }
                val smoothPalettes = listOf(
                    0 to "🌈 Rainbow",
                    1 to "🎨 Pastel",
                    2 to "🌅 Sunset",
                    3 to "🌊 Ocean"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌊 SMOOTH BREATH & RAINBOW FLOW",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = AccentCyan
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = AccentCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AccentCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Palette Selector Chips (Row 1: Rainbow, Pastel, Sunset, Ocean)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            smoothPalettes.forEach { (id, label) ->
                                val isSelected = (smoothPalette == id)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSetSmoothConfig?.invoke(null, null, id) },
                                    color = if (isSelected) AccentCyan.copy(alpha = 0.3f) else SurfaceCard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentCyan else SurfaceCardBorder
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Selected Color Breath Chip
                        val isColorBreath = (smoothPalette == 4)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetSmoothConfig?.invoke(null, null, 4) },
                            color = if (isColorBreath) AccentCyan.copy(alpha = 0.3f) else SurfaceCard,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isColorBreath) AccentCyan else SurfaceCardBorder
                            )
                        ) {
                            Text(
                                text = "🎯 Selected Color Breath (Breathe Chosen Color)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isColorBreath) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isColorBreath) Color.White else TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Rainbow Circulation Speed Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rainbow Cycle Speed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${localSmoothSpeed.toInt()}s", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AccentCyan)
                        }

                        Slider(
                            value = localSmoothSpeed,
                            onValueChange = { localSmoothSpeed = it },
                            onValueChangeFinished = { onSetSmoothConfig?.invoke(localSmoothSpeed.toInt(), null, null) },
                            valueRange = 15f..240f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentCyan,
                                activeTrackColor = AccentCyan,
                                inactiveTrackColor = SurfaceCard
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Breathing Rhythm Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Respiration Rhythm", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${localSmoothBreath.toInt()}s", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AccentCyan)
                        }

                        Slider(
                            value = localSmoothBreath,
                            onValueChange = { localSmoothBreath = it },
                            onValueChangeFinished = { onSetSmoothConfig?.invoke(null, localSmoothBreath.toInt(), null) },
                            valueRange = 3f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentCyan,
                                activeTrackColor = AccentCyan,
                                inactiveTrackColor = SurfaceCard
                            )
                        )
                    }
                }
            }
        }
    }
}
