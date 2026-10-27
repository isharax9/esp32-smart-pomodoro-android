package com.esp32.smartpomodoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
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
    strobeMode: Int = 0,
    fadeSpeed: Int = 60,
    fadePalette: Int = 0,
    smoothSpeed: Int = 75,
    smoothBreath: Int = 7,
    smoothPalette: Int = 0,
    onSelectStrip: (StripTarget) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onSendCommand: (String) -> Unit,
    onSetFadeConfig: ((speed: Int?, palette: Int?) -> Unit)? = null,
    onSetSmoothConfig: ((speed: Int?, breath: Int?, palette: Int?) -> Unit)? = null,
    onSetStrobeMode: ((String) -> Unit)? = null
) {
    // Current Active Tab for Advanced Lighting Controls (Defaults to currentEffect if in 2..4, otherwise Fade=3)
    var selectedEffectTab by remember { mutableStateOf(if (currentEffect in 2..4) currentEffect else 3) }

    LaunchedEffect(currentEffect) {
        if (currentEffect in 2..4) {
            selectedEffectTab = currentEffect
        }
    }

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
                                        .clickable {
                                            when (button.commandVal) {
                                                "strobe" -> selectedEffectTab = 2
                                                "fade" -> selectedEffectTab = 3
                                                "smooth" -> selectedEffectTab = 4
                                            }
                                            onSendCommand(button.commandVal)
                                        },
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

            Spacer(modifier = Modifier.height(20.dp))

            // ADVANCED LIGHTING CONTROLS CONTAINER (Always visible and interactive)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Title
                    Text(
                        text = "ADVANCED EFFECT CONTROLS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Selector Pills: Rainbow Fade (3), Smooth Breath (4), Custom Strobe (2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf(
                            Triple(3, "🌈 Fade", Color(0xFFA855F7)),
                            Triple(4, "🌊 Smooth", AccentCyan),
                            Triple(2, "✨ Strobe", AccentAmber)
                        )
                        tabs.forEach { (tabId, label, themeColor) ->
                            val isTabSelected = (selectedEffectTab == tabId)
                            val isRunningOnEsp = (currentEffect == tabId)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedEffectTab = tabId },
                                color = if (isTabSelected) themeColor.copy(alpha = 0.25f) else SurfaceCard,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isTabSelected) 1.5.dp else 1.dp,
                                    if (isTabSelected) themeColor else SurfaceCardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isTabSelected) Color.White else TextSecondary
                                    )
                                    if (isRunningOnEsp) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(AccentGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab 3: Rainbow Fade Controls
                    if (selectedEffectTab == 3) {
                        var localFadeSpeed by remember(fadeSpeed) { mutableStateOf(fadeSpeed.toFloat()) }
                        var localFadePalette by remember(fadePalette) { mutableStateOf(fadePalette) }
                        val fadePalettes = listOf(
                            0 to "🌈 Rainbow",
                            1 to "🎨 Pastel",
                            2 to "🌅 Sunset",
                            3 to "🌊 Ocean"
                        )
                        val speedPresets = listOf(15 to "15s Fast", 45 to "45s Normal", 60 to "60s Calm", 120 to "120s Slow")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌈 FULL RAINBOW FADE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFC084FC)
                            )
                            if (currentEffect == 3) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = AccentGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AccentGreen
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onSendCommand("fade") },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply Fade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Palette Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fadePalettes.forEach { (id, label) ->
                                val isSelected = (localFadePalette == id)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            localFadePalette = id
                                            onSetFadeConfig?.invoke(null, id)
                                        },
                                    color = if (isSelected) Color(0xFFA855F7).copy(alpha = 0.35f) else SurfaceCard,
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

                        // Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            speedPresets.forEach { (sec, label) ->
                                val isPreset = (localFadeSpeed.toInt() == sec)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            localFadeSpeed = sec.toFloat()
                                            onSetFadeConfig?.invoke(sec, null)
                                        },
                                    color = if (isPreset) Color(0xFFA855F7).copy(alpha = 0.25f) else SurfaceCard,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isPreset) Color(0xFFA855F7) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isPreset) Color.White else TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tab 4: Smooth Breath Controls
                    if (selectedEffectTab == 4) {
                        var localSmoothSpeed by remember(smoothSpeed) { mutableStateOf(smoothSpeed.toFloat()) }
                        var localSmoothBreath by remember(smoothBreath) { mutableStateOf(smoothBreath.toFloat()) }
                        var localSmoothPalette by remember(smoothPalette) { mutableStateOf(smoothPalette) }
                        val smoothPalettes = listOf(
                            0 to "🌈 Rainbow",
                            1 to "🎨 Pastel",
                            2 to "🌅 Sunset",
                            3 to "🌊 Ocean"
                        )
                        val breathPresets = listOf(3 to "3s Brisk", 5 to "5s Active", 7 to "7s Calm", 10 to "10s Zen")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌊 SMOOTH BREATH & FLOW",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = AccentCyan
                            )
                            if (currentEffect == 4) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = AccentGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AccentGreen
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onSendCommand("smooth") },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply Smooth", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BackgroundDark)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Palette Selector Chips (Row 1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            smoothPalettes.forEach { (id, label) ->
                                val isSelected = (localSmoothPalette == id)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            localSmoothPalette = id
                                            onSetSmoothConfig?.invoke(null, null, id)
                                        },
                                    color = if (isSelected) AccentCyan.copy(alpha = 0.35f) else SurfaceCard,
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

                        // Selected Color Breath Chip (Row 2)
                        val isColorBreath = (localSmoothPalette == 4)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    localSmoothPalette = 4
                                    onSetSmoothConfig?.invoke(null, null, 4)
                                },
                            color = if (isColorBreath) AccentCyan.copy(alpha = 0.35f) else SurfaceCard,
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

                        // Quick Breath Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            breathPresets.forEach { (sec, label) ->
                                val isPreset = (localSmoothBreath.toInt() == sec)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            localSmoothBreath = sec.toFloat()
                                            onSetSmoothConfig?.invoke(null, sec, null)
                                        },
                                    color = if (isPreset) AccentCyan.copy(alpha = 0.25f) else SurfaceCard,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isPreset) AccentCyan else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isPreset) Color.White else TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tab 2: Strobe Effect Controls
                    if (selectedEffectTab == 2) {
                        var localStrobeMode by remember(strobeMode) { mutableStateOf(strobeMode) }
                        val strobeModes = listOf(
                            0 to "🎯 Selected Color (Active Tone)",
                            1 to "🔄 RGB Fast Cycle (R-G-B)",
                            2 to "🌈 Rainbow Cycle (Spectrum)",
                            3 to "⚡ Pure White (High Intensity)"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ STROBE PATTERN MODE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = AccentAmber
                            )
                            if (currentEffect == 2) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = AccentGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AccentGreen
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onSendCommand("strobe") },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply Strobe", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BackgroundDark)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            strobeModes.forEach { (modeId, label) ->
                                val isSelected = (localStrobeMode == modeId)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            localStrobeMode = modeId
                                            onSetStrobeMode?.invoke(modeId.toString())
                                        },
                                    color = if (isSelected) AccentAmber.copy(alpha = 0.35f) else SurfaceCard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentAmber else SurfaceCardBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = AccentAmber,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
