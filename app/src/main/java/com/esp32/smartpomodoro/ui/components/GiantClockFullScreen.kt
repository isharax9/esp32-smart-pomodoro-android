package com.esp32.smartpomodoro.ui.components

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.data.model.*
import com.esp32.smartpomodoro.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GiantClockFullScreen(
    uiState: ClockUiState,
    pomodoroStatus: PomodoroStatus? = null,
    onCycleRotation: () -> Unit = {},
    onSelectRotation: (ClockRotationAngle) -> Unit = {},
    onToggleOrientation: () -> Unit,
    onToggleAutoRotate: () -> Unit = {},
    onSwitchToStopwatch: (() -> Unit)? = null,
    onSwitchToPomodoro: (() -> Unit)? = null,
    onCycleTheme: () -> Unit,
    onCycleFontFamily: () -> Unit = {},
    onCycleFontSize: () -> Unit = {},
    onToggleTimeFormat: () -> Unit,
    onCycleSeconds: () -> Unit,
    onToggleNightDimming: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onExitFullScreen: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val snapshot = uiState.snapshot
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Controls visibility auto-hides after 4 seconds of inactivity
    var controlsVisible by remember { mutableStateOf(true) }
    var userInteractionTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(userInteractionTimestamp, controlsVisible) {
        if (controlsVisible) {
            delay(4000L)
            controlsVisible = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        userInteractionTimestamp = System.currentTimeMillis()
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    userInteractionTimestamp = System.currentTimeMillis()
                    val delta = -dragAmount / 400f
                    val newBrightness = (uiState.brightnessFactor + delta).coerceIn(0.1f, 1.0f)
                    onBrightnessChange(newBrightness)
                }
            }
    ) {
        val screenW = maxWidth
        val screenH = maxHeight

        val clockFont = uiState.fontFamily.toComposeFontFamily()
        val clockLetterSpacing = if (uiState.fontFamily == ClockFontFamily.MONOSPACE) (-1).sp else 0.sp

        // LAYER 1: Subtle Ambient Backlight Glow centered behind digits
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(
                    width = (screenW * 0.75f).coerceAtLeast(200.dp),
                    height = (screenH * 0.50f).coerceAtLeast(150.dp)
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

        // LAYER 2: Main Clock Display
        if (isLandscape) {
            // ==========================================
            // LANDSCAPE MODE: Edge-to-edge widescreen
            // ==========================================
            val digitSlotUnit = when (uiState.fontFamily) {
                ClockFontFamily.CURSIVE -> 1.32f
                ClockFontFamily.MONOSPACE -> 1.25f
                ClockFontFamily.SERIF -> 1.22f
                ClockFontFamily.SANS_SERIF -> 1.18f
            }
            val colonSlotUnit = 0.28f

            val hasAmPm = snapshot.amPm.isNotEmpty()
            val totalSlotUnits: Float
            val fixedHorizontalPaddings: Float

            when (uiState.secondsDisplay) {
                ClockSecondsDisplay.BIG -> {
                    // HH : MM : SS [+ AM/PM]
                    val amPmUnit = if (hasAmPm) 0.45f else 0f
                    totalSlotUnits = (digitSlotUnit * 3) + (colonSlotUnit * 2) + amPmUnit
                    fixedHorizontalPaddings = if (hasAmPm) 24f else 8f
                }
                ClockSecondsDisplay.SMALL -> {
                    // HH : MM [+ stacked SS & AM/PM]
                    val secColUnit = if (hasAmPm || uiState.secondsDisplay != ClockSecondsDisplay.NONE) 0.55f else 0f
                    totalSlotUnits = (digitSlotUnit * 2) + colonSlotUnit + secColUnit
                    fixedHorizontalPaddings = 20f
                }
                ClockSecondsDisplay.NONE -> {
                    // HH : MM [+ AM/PM]
                    val amPmUnit = if (hasAmPm) 0.45f else 0f
                    totalSlotUnits = (digitSlotUnit * 2) + colonSlotUnit + amPmUnit
                    fixedHorizontalPaddings = if (hasAmPm) 20f else 0f
                }
            }

            val safeW = (screenW.value - 36f).coerceAtLeast(100f)
            val maxFontFromWidth = ((safeW - fixedHorizontalPaddings) / totalSlotUnits).coerceAtLeast(20f)

            val extraHeight = (if (uiState.showDate) 32f else 0f) +
                    (if (pomodoroStatus != null && pomodoroStatus.parsedState != PomodoroState.IDLE) 42f else 0f) + 24f
            val availableClockH = (screenH.value - extraHeight).coerceAtLeast(70f)
            val maxFontFromHeight = availableClockH * 0.82f

            // Maximum safe font size guaranteed to fit both width and height without any clipping
            val maxFittingFontSize = minOf(maxFontFromWidth, maxFontFromHeight)
            val baseStandardFontSize = maxFittingFontSize * 0.76f
            val computedFontSize = (baseStandardFontSize * uiState.fontSizeScale)
                .coerceIn(22f, maxFittingFontSize).sp

            val smallSecFontSize = (computedFontSize.value * 0.38f).coerceIn(12f, 44f).sp
            val amPmFontSize = (computedFontSize.value * 0.24f).coerceIn(11f, 32f).sp
            val dateFontSize = (screenH.value * 0.055f * uiState.fontSizeScale.coerceIn(0.85f, 1.15f)).coerceIn(11f, 18f).sp

            val digitSlotWidth = (computedFontSize.value * digitSlotUnit).dp
            val colonSlotWidth = (computedFontSize.value * colonSlotUnit).dp

            val digitTextStyle = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = computedFontSize,
                fontFamily = clockFont,
                letterSpacing = clockLetterSpacing,
                fontFeatureSettings = "tnum"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Giant Digits Row (Symmetrically and vertically centered colon)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hours Slot
                    Box(modifier = Modifier.width(digitSlotWidth), contentAlignment = Alignment.Center) {
                        Text(
                            text = snapshot.hours,
                            style = digitTextStyle,
                            color = uiState.effectiveColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Symmetrically Centered Colon (Never sunken, perfectly centered between digits)
                    CenteredClockColon(
                        slotWidth = colonSlotWidth,
                        fontSize = computedFontSize.value,
                        color = uiState.effectiveColor,
                        colonVisible = snapshot.colonVisible,
                        blinkingColon = uiState.blinkingColon
                    )

                    // Minutes Slot
                    Box(modifier = Modifier.width(digitSlotWidth), contentAlignment = Alignment.Center) {
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
                                fontSize = computedFontSize.value,
                                color = uiState.effectiveColor,
                                colonVisible = snapshot.colonVisible,
                                blinkingColon = uiState.blinkingColon
                            )
                            Box(modifier = Modifier.width(digitSlotWidth), contentAlignment = Alignment.Center) {
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = uiState.effectiveColor.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = snapshot.amPm,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = amPmFontSize,
                                            fontFamily = clockFont
                                        ),
                                        color = uiState.effectiveColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        ClockSecondsDisplay.SMALL -> {
                            Column(
                                modifier = Modifier.padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = uiState.effectiveColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${snapshot.seconds}s",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = smallSecFontSize,
                                            fontFamily = clockFont,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = uiState.effectiveSecondaryColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (snapshot.amPm.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = uiState.effectiveColor.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = snapshot.amPm,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = amPmFontSize,
                                                fontFamily = clockFont
                                            ),
                                            color = uiState.effectiveColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = uiState.effectiveColor.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = snapshot.amPm,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = amPmFontSize,
                                            fontFamily = clockFont
                                        ),
                                        color = uiState.effectiveColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Date Display (Clean, natural text without any frame or border)
                if (uiState.showDate && snapshot.dateFormatted.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${snapshot.dayOfWeek}, ${snapshot.dateFormatted}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = dateFontSize,
                            fontFamily = clockFont,
                            letterSpacing = 0.5.sp
                        ),
                        color = uiState.effectiveSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                }

                // Running Pomodoro status widget
                if (pomodoroStatus != null && pomodoroStatus.parsedState != PomodoroState.IDLE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val pState = pomodoroStatus.parsedState
                    val isWork = pState == PomodoroState.WORK
                    val pColor = if (pomodoroStatus.isPaused) AccentAmber else if (isWork) AccentRed else AccentGreen
                    val min = pomodoroStatus.remainingSec / 60
                    val sec = pomodoroStatus.remainingSec % 60
                    val timeStr = String.format("%02d:%02d", min, sec)
                    val stateLabel = if (isWork) "FOCUS" else "BREAK"
                    val statusText = if (pomodoroStatus.isPaused) "$stateLabel (PAUSED)  $timeStr" else "$stateLabel  $timeStr"

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = pColor.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSwitchToPomodoro?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (pomodoroStatus.isPaused) Icons.Default.Pause else Icons.Default.HourglassTop,
                                contentDescription = "Pomodoro",
                                tint = pColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = pColor
                            )
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // PORTRAIT MODE: Robust, professional layout
            // ==========================================
            val digitSlotUnit = when (uiState.fontFamily) {
                ClockFontFamily.CURSIVE -> 1.32f
                ClockFontFamily.MONOSPACE -> 1.25f
                ClockFontFamily.SERIF -> 1.22f
                ClockFontFamily.SANS_SERIF -> 1.18f
            }
            val colonSlotUnit = 0.28f

            val safeW = (screenW.value - 32f).coerceAtLeast(100f)
            val totalSlotUnits = (digitSlotUnit * 2) + colonSlotUnit
            val maxFontFromWidth = (safeW / totalSlotUnits).coerceAtLeast(24f)

            val extraHeight = (if (uiState.showDate) 40f else 0f) +
                    (if (pomodoroStatus != null && pomodoroStatus.parsedState != PomodoroState.IDLE) 54f else 0f) + 120f
            val availableClockH = (screenH.value - extraHeight).coerceAtLeast(120f)
            val maxFontFromHeight = availableClockH * 0.45f

            val maxFittingFontSize = minOf(maxFontFromWidth, maxFontFromHeight)
            val baseStandardFontSize = maxFittingFontSize * 0.78f
            val computedFontSize = (baseStandardFontSize * uiState.fontSizeScale)
                .coerceIn(26f, maxFittingFontSize).sp

            val secFontSize = (computedFontSize.value * 0.32f).coerceIn(13f, 32f).sp
            val amPmFontSize = (computedFontSize.value * 0.26f).coerceIn(12f, 28f).sp
            val dateFontSize = (screenW.value * 0.042f * uiState.fontSizeScale.coerceIn(0.85f, 1.15f)).coerceIn(12f, 18f).sp

            val digitSlotWidth = (computedFontSize.value * digitSlotUnit).dp
            val colonSlotWidth = (computedFontSize.value * colonSlotUnit).dp

            val digitTextStyle = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = computedFontSize,
                fontFamily = clockFont,
                letterSpacing = clockLetterSpacing,
                fontFeatureSettings = "tnum"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Date Display (Clean, natural text without any frame or border)
                if (uiState.showDate && snapshot.dateFormatted.isNotEmpty()) {
                    Text(
                        text = "${snapshot.dayOfWeek}, ${snapshot.dateFormatted}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = dateFontSize,
                            fontFamily = clockFont,
                            letterSpacing = 0.5.sp
                        ),
                        color = uiState.effectiveSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Main Hours : Minutes Row (Symmetrically and vertically centered colon)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.width(digitSlotWidth), contentAlignment = Alignment.Center) {
                        Text(
                            text = snapshot.hours,
                            style = digitTextStyle,
                            color = uiState.effectiveColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    CenteredClockColon(
                        slotWidth = colonSlotWidth,
                        fontSize = computedFontSize.value,
                        color = uiState.effectiveColor,
                        colonVisible = snapshot.colonVisible,
                        blinkingColon = uiState.blinkingColon
                    )

                    Box(modifier = Modifier.width(digitSlotWidth), contentAlignment = Alignment.Center) {
                        Text(
                            text = snapshot.minutes,
                            style = digitTextStyle,
                            color = uiState.effectiveColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Secondary row: Seconds badge & AM/PM indicator
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.secondsDisplay != ClockSecondsDisplay.NONE) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = uiState.effectiveColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, uiState.effectiveColor.copy(alpha = 0.22f))
                        ) {
                            Text(
                                text = "${snapshot.seconds} SEC",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = secFontSize,
                                    fontFamily = clockFont,
                                    fontFeatureSettings = "tnum",
                                    letterSpacing = 1.sp
                                ),
                                color = uiState.effectiveSecondaryColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                maxLines = 1
                            )
                        }
                    }

                    if (snapshot.amPm.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = uiState.effectiveColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = snapshot.amPm,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = amPmFontSize,
                                    fontFamily = clockFont
                                ),
                                color = uiState.effectiveColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Running Pomodoro Status Capsule
                if (pomodoroStatus != null && pomodoroStatus.parsedState != PomodoroState.IDLE) {
                    Spacer(modifier = Modifier.height(24.dp))
                    val pState = pomodoroStatus.parsedState
                    val isWork = pState == PomodoroState.WORK
                    val pColor = if (pomodoroStatus.isPaused) AccentAmber else if (isWork) AccentRed else AccentGreen
                    val min = pomodoroStatus.remainingSec / 60
                    val sec = pomodoroStatus.remainingSec % 60
                    val timeStr = String.format("%02d:%02d", min, sec)
                    val stateLabel = if (isWork) "FOCUS" else "BREAK"
                    val statusText = if (pomodoroStatus.isPaused) "$stateLabel (PAUSED)  $timeStr" else "$stateLabel  $timeStr"

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = pColor.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSwitchToPomodoro?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (pomodoroStatus.isPaused) Icons.Default.Pause else Icons.Default.HourglassTop,
                                contentDescription = "Pomodoro",
                                tint = pColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = pColor
                            )
                        }
                    }
                }
            }
        }

        // LAYER 3: Animated Overlays (Top Bar & Bottom Controls)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(350)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (isLandscape) 20.dp else 14.dp,
                        vertical = if (isLandscape) 12.dp else 18.dp
                    )
            ) {
                // TOP BAR: Exit, Orientation, Theme Swatch, Font, Size, Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, SurfaceCardBorder.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back / Exit Fullscreen Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                userInteractionTimestamp = System.currentTimeMillis()
                                onExitFullScreen()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Exit",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Orientation Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Auto-Rotate Toggle Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.isAutoRotate) uiState.theme.primaryColor else SurfaceDark,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onToggleAutoRotate()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoMode,
                                    contentDescription = null,
                                    tint = if (uiState.isAutoRotate) BackgroundDark else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Auto",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (uiState.isAutoRotate) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (uiState.isAutoRotate) BackgroundDark else TextSecondary
                                )
                            }
                        }

                        // Toggle Portrait <-> Landscape button
                        IconButton(
                            onClick = {
                                userInteractionTimestamp = System.currentTimeMillis()
                                onToggleOrientation()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Toggle Orientation",
                                tint = if (isLandscape) uiState.theme.primaryColor else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Action Icons: Font Size, Font Family, Theme Swatch, Settings
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick switch to Stopwatch
                        if (onSwitchToStopwatch != null) {
                            IconButton(
                                onClick = {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onSwitchToStopwatch()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AvTimer,
                                    contentDescription = "Stopwatch",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Size Scale Cycle Pill (Clear visual feedback on current scale)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onCycleFontSize()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = "Size: ${(uiState.fontSizeScale * 100).toInt()}%",
                                    tint = uiState.theme.primaryColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${(uiState.fontSizeScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = uiState.theme.primaryColor
                                )
                            }
                        }

                        // Font Family Cycle Pill (Shows font name abbreviation)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onCycleFontFamily()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = "Font: ${uiState.fontFamily.label}",
                                    tint = uiState.theme.primaryColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = uiState.fontFamily.label.take(4),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }

                        // Theme Swatch
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(uiState.theme.primaryColor)
                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onCycleTheme()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Theme",
                                tint = BackgroundDark,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        // Settings
                        IconButton(
                            onClick = {
                                userInteractionTimestamp = System.currentTimeMillis()
                                onOpenSettings()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // BOTTOM BAR: Format, Seconds, Night Dimming, Brightness Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, SurfaceCardBorder.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Format & Mode Pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 12H / 24H Toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onToggleTimeFormat()
                                }
                        ) {
                            Text(
                                text = if (uiState.timeFormat == ClockTimeFormat.TWELVE_HOUR) "12H" else "24H",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }

                        // Seconds Toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onCycleSeconds()
                                }
                        ) {
                            Text(
                                text = "Sec: ${uiState.secondsDisplay.name.take(3)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }

                        // Night Mode Toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.nightDimming) AccentAmber.copy(alpha = 0.25f) else SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.nightDimming) AccentAmber else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onToggleNightDimming()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.nightDimming) Icons.Default.Bedtime else Icons.Default.BrightnessMedium,
                                    contentDescription = null,
                                    tint = if (uiState.nightDimming) AccentAmber else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (uiState.nightDimming) "Night" else "Day",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (uiState.nightDimming) AccentAmber else TextSecondary
                                )
                            }
                        }
                    }

                    // Brightness Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { preset ->
                            val isClose = kotlin.math.abs(uiState.brightnessFactor - preset) < 0.15f
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isClose) uiState.theme.primaryColor else SurfaceDark,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        userInteractionTimestamp = System.currentTimeMillis()
                                        onBrightnessChange(preset)
                                    }
                            ) {
                                Text(
                                    text = "${(preset * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isClose) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isClose) BackgroundDark else TextMuted,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mathematically and geometrically centered two-dot colon.
 * Centered vertically with numbers, eliminating sunken colon font artifacts.
 */
@Composable
fun CenteredClockColon(
    slotWidth: Dp,
    fontSize: Float,
    color: Color,
    colonVisible: Boolean,
    blinkingColon: Boolean
) {
    val dotSize = (fontSize * 0.10f).coerceIn(4f, 20f).dp
    val dotSpacing = (fontSize * 0.22f).coerceIn(7f, 44f).dp
    val colonAlpha = if (colonVisible) 1.0f else if (blinkingColon) 0.12f else 1.0f

    Box(
        modifier = Modifier
            .width(slotWidth)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dotSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color.copy(alpha = colonAlpha))
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color.copy(alpha = colonAlpha))
            )
        }
    }
}
