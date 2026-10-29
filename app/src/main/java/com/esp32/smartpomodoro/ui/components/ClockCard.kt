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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.data.model.*
import com.esp32.smartpomodoro.ui.theme.*

@Composable
fun ClockCard(
    uiState: ClockUiState,
    pomodoroStatus: PomodoroStatus? = null,
    onCycleRotation: () -> Unit = {},
    onSelectRotation: (ClockRotationAngle) -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    onToggleAutoRotate: () -> Unit = {},
    onCycleTheme: () -> Unit,
    onCycleFontFamily: () -> Unit = {},
    onCycleFontSize: () -> Unit = {},
    onToggleTimeFormat: () -> Unit,
    onCycleSeconds: () -> Unit,
    onToggleShowDate: () -> Unit = {},
    onToggleNightDimming: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyTime: () -> Unit,
    onSwitchToPomodoro: (() -> Unit)? = null
) {
    val snapshot = uiState.snapshot

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
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. CLEAN BALANCED HEADER: Desk Clock title, format badge, theme, settings, and fullscreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and format badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(uiState.theme.primaryColor)
                    )
                    Text(
                        text = "DESK CLOCK",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceDark
                    ) {
                        Text(
                            text = if (uiState.timeFormat == ClockTimeFormat.TWELVE_HOUR) "12H" else "24H",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = uiState.theme.primaryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Compact action icons: Theme swatch, Settings, and prominent Fullscreen button
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
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = uiState.theme.primaryColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleFullScreen() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = BackgroundDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Full",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = BackgroundDark
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SurfaceCardBorder)

            // 2. TIME READOUT BEZEL CONTAINER (Tap anywhere to expand into Fullscreen mode)
            val scale = uiState.fontSizeScale

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp * scale.coerceIn(0.85f, 1.25f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, SurfaceCardBorder.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { onToggleFullScreen() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val boxW = maxWidth
                val boxH = maxHeight

                // Ambient glow behind digits
                Box(
                    modifier = Modifier
                        .size(
                            width = (boxW * 0.75f).coerceAtLeast(160.dp),
                            height = (boxH * 0.5f).coerceAtLeast(80.dp)
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    uiState.effectiveColor.copy(alpha = if (uiState.nightDimming) 0.04f else 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Responsively determine slot units required to fit card width without any clipping
                val (singleDigitUnit, colonSlotUnit) = when (uiState.fontFamily) {
                    ClockFontFamily.SEVEN_SEGMENT -> Pair(0.85f, 0.28f)
                    ClockFontFamily.CURSIVE -> Pair(0.68f, 0.32f)
                    ClockFontFamily.MONOSPACE -> Pair(0.64f, 0.32f)
                    ClockFontFamily.SERIF -> Pair(0.63f, 0.30f)
                    ClockFontFamily.SANS_SERIF -> Pair(0.61f, 0.30f)
                }
                val hasAmPm = snapshot.amPm.isNotEmpty()
                val hourDigits = snapshot.displayHours.length

                val totalSlotUnits: Float
                val fixedHorizontalPaddings: Float

                when (uiState.secondsDisplay) {
                    ClockSecondsDisplay.BIG -> {
                        val amPmUnit = if (hasAmPm) 0.90f else 0f
                        totalSlotUnits = ((hourDigits + 4) * singleDigitUnit) + (colonSlotUnit * 2) + amPmUnit
                        fixedHorizontalPaddings = if (hasAmPm) 28f else 12f
                    }
                    ClockSecondsDisplay.SMALL -> {
                        val secColUnit = if (hasAmPm || uiState.secondsDisplay != ClockSecondsDisplay.NONE) 0.85f else 0f
                        totalSlotUnits = ((hourDigits + 2) * singleDigitUnit) + colonSlotUnit + secColUnit
                        fixedHorizontalPaddings = 24f
                    }
                    ClockSecondsDisplay.NONE -> {
                        val amPmUnit = if (hasAmPm) 0.80f else 0f
                        totalSlotUnits = ((hourDigits + 2) * singleDigitUnit) + colonSlotUnit + amPmUnit
                        fixedHorizontalPaddings = if (hasAmPm) 24f else 8f
                    }
                }

                val fontHeightMultiplier = when (uiState.fontFamily) {
                    ClockFontFamily.CURSIVE -> 1.25f
                    ClockFontFamily.SERIF -> 1.18f
                    ClockFontFamily.MONOSPACE -> 1.12f
                    ClockFontFamily.SANS_SERIF -> 1.12f
                    ClockFontFamily.SEVEN_SEGMENT -> 1.06f
                }

                val safeW = (boxW.value - 20f).coerceAtLeast(80f)
                val maxFontFromW = ((safeW - fixedHorizontalPaddings) / totalSlotUnits).coerceAtLeast(16f)
                val availableCardH = (boxH.value - (if (uiState.showDate) 36f else 0f) - 16f).coerceAtLeast(60f)
                val maxFontFromH = (availableCardH / fontHeightMultiplier) * 0.65f

                val maxFittingFontSize = minOf(maxFontFromW, maxFontFromH)
                val baseResponsiveFont = maxFittingFontSize * 0.76f

                val digitFontSize = (baseResponsiveFont * scale)
                    .coerceIn(16f, maxFittingFontSize).sp
                val smallSecFontSize = (digitFontSize.value * 0.38f).coerceIn(10f, 26f).sp
                val amPmFontSize = (digitFontSize.value * 0.28f).coerceIn(9f, 20f).sp
                val dateFontSize = (boxW.value * 0.038f * scale.coerceIn(0.85f, 1.2f)).coerceIn(11f, 15f).sp

                val clockFont = uiState.fontFamily.toComposeFontFamily()
                val clockLetterSpacing = when (uiState.fontFamily) {
                    ClockFontFamily.SEVEN_SEGMENT -> 0.sp
                    ClockFontFamily.MONOSPACE -> (-1).sp
                    else -> 0.sp
                }

                val singleDigitWidth = (digitFontSize.value * singleDigitUnit).dp
                val hoursSlotWidth = singleDigitWidth * hourDigits
                val minutesSlotWidth = singleDigitWidth * 2
                val secondsSlotWidth = singleDigitWidth * 2
                val colonSlotWidth = (digitFontSize.value * colonSlotUnit).dp

                val digitTextStyle = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = digitFontSize,
                    lineHeight = digitFontSize,
                    fontFamily = clockFont,
                    letterSpacing = clockLetterSpacing,
                    fontFeatureSettings = "tnum",
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    ),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Date Display (Clean, natural text without any frame or border)
                    if (uiState.showDate && snapshot.dateFormatted.isNotEmpty()) {
                        Text(
                            text = "${snapshot.dayOfWeek}, ${snapshot.dateFormatted}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = dateFontSize,
                                fontFamily = FontFamily.Default,
                                letterSpacing = 0.5.sp
                            ),
                            color = uiState.effectiveSecondaryColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Time Digits Row (With Symmetrically Centered Colon)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Hours Slot
                        Box(
                            modifier = Modifier.width(hoursSlotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = snapshot.displayHours,
                                style = digitTextStyle,
                                color = uiState.effectiveColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Symmetrically Centered Colon
                        CenteredClockColon(
                            slotWidth = colonSlotWidth,
                            fontSize = digitFontSize.value,
                            color = uiState.effectiveColor,
                            colonVisible = snapshot.colonVisible,
                            blinkingColon = uiState.blinkingColon
                        )

                        // Minutes Slot
                        Box(
                            modifier = Modifier.width(minutesSlotWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = snapshot.minutes,
                                style = digitTextStyle,
                                color = uiState.effectiveColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Seconds & AM/PM Handling
                        when (uiState.secondsDisplay) {
                            ClockSecondsDisplay.BIG -> {
                                CenteredClockColon(
                                    slotWidth = colonSlotWidth,
                                    fontSize = digitFontSize.value,
                                    color = uiState.effectiveColor,
                                    colonVisible = snapshot.colonVisible,
                                    blinkingColon = uiState.blinkingColon
                                )
                                Box(
                                    modifier = Modifier.width(secondsSlotWidth),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = snapshot.seconds,
                                        style = digitTextStyle,
                                        color = uiState.effectiveSecondaryColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (snapshot.amPm.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = uiState.effectiveColor.copy(alpha = 0.16f),
                                        modifier = Modifier
                                            .padding(start = 10.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        Text(
                                            text = snapshot.amPm,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = amPmFontSize,
                                                fontFamily = FontFamily.Default
                                            ),
                                            color = uiState.effectiveColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            ClockSecondsDisplay.SMALL -> {
                                Column(
                                    modifier = Modifier.padding(start = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = uiState.effectiveColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${snapshot.seconds}s",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = smallSecFontSize,
                                                fontFamily = if (uiState.fontFamily == ClockFontFamily.SEVEN_SEGMENT) FontFamily.Default else clockFont,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = uiState.effectiveSecondaryColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    if (snapshot.amPm.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = uiState.effectiveColor.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = snapshot.amPm,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = amPmFontSize,
                                                    fontFamily = FontFamily.Default
                                                ),
                                                color = uiState.effectiveColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                            ClockSecondsDisplay.NONE -> {
                                if (snapshot.amPm.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = uiState.effectiveColor.copy(alpha = 0.16f),
                                        modifier = Modifier
                                            .padding(start = 10.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        Text(
                                            text = snapshot.amPm,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = amPmFontSize,
                                                fontFamily = FontFamily.Default
                                            ),
                                            color = uiState.effectiveColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Pomodoro Tracker if running
                    val isPomodoroRunning = pomodoroStatus != null && pomodoroStatus.parsedState != PomodoroState.IDLE
                    if (isPomodoroRunning && pomodoroStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val pState = pomodoroStatus.parsedState
                        val isWork = pState == PomodoroState.WORK
                        val pColor = if (pomodoroStatus.isPaused) AccentAmber else if (isWork) AccentRed else AccentGreen
                        val min = pomodoroStatus.remainingSec / 60
                        val sec = pomodoroStatus.remainingSec % 60
                        val timeStr = String.format("%02d:%02d", min, sec)
                        val stateLabel = if (isWork) "FOCUS" else "BREAK"
                        val statusText = if (pomodoroStatus.isPaused) "$stateLabel (PAUSED) $timeStr" else "$stateLabel $timeStr"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = pColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, pColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSwitchToPomodoro?.invoke() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (pomodoroStatus.isPaused) Icons.Default.Pause else Icons.Default.HourglassTop,
                                    contentDescription = "Pomodoro",
                                    tint = pColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = pColor
                                )
                            }
                        }
                    }
                }
            }

            // 3. BOTTOM QUICK CONTROLS: Segmented toggle chips and copy time
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
                                text = if (uiState.nightDimming) "Night" else "Day",
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
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Time",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
