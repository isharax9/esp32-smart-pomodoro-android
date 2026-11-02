package com.esp32.smartpomodoro.ui.components

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextGeometricTransform
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
    onFontSizeScaleChange: (Float) -> Unit = {},
    onToggleTimeFormat: () -> Unit,
    onCycleSeconds: () -> Unit,
    onToggleShowDate: () -> Unit = {},
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
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1.0f) {
                        userInteractionTimestamp = System.currentTimeMillis()
                        val updated = (uiState.fontSizeScale * zoom).coerceIn(0.50f, 1.00f)
                        onFontSizeScaleChange(updated)
                    }
                }
            }
    ) {
        val screenW = maxWidth
        val screenH = maxHeight

        val clockFont = uiState.fontFamily.toComposeFontFamily()
        val clockLetterSpacing = when (uiState.fontFamily) {
            ClockFontFamily.SEVEN_SEGMENT -> 0.sp
            ClockFontFamily.MONOSPACE -> (-1).sp
            else -> 0.sp
        }

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
            // =========================================================================
            // GIANT FULLSCREEN DESK CLOCK (100% DISPLAY-FILLED DIGITS)
            // Natural 1:1 font aspect ratio, giant centered digits filling the display,
            // with corner seconds & AM/PM aligned to bottom-right (matching reference).
            // =========================================================================
            val (digitUnit, colonUnit, fontHeightMultiplier, glyphSideMargin) = when (uiState.fontFamily) {
                ClockFontFamily.SEVEN_SEGMENT -> listOf(0.816f, 0.180f, 1.00f, 0.099f)
                ClockFontFamily.SANS_SERIF -> listOf(0.574f, 0.282f, 1.08f, 0.040f)
                ClockFontFamily.SERIF -> listOf(0.559f, 0.304f, 1.10f, 0.040f)
                ClockFontFamily.MONOSPACE -> listOf(0.600f, 0.250f, 1.08f, 0.050f)
                ClockFontFamily.CURSIVE -> listOf(0.620f, 0.251f, 1.18f, 0.040f)
            }

            val hourDigits = snapshot.displayHours.length
            val minuteDigits = 2
            val hasBigSeconds = uiState.secondsDisplay == ClockSecondsDisplay.BIG
            val secondDigits = if (hasBigSeconds) 2 else 0
            val colonsCount = if (hasBigSeconds) 2 else 1

            // In DSEG7, digit '1' uses only the right column (LSB = 593 out of 1000).
            // We calculate exact visual ink width and optical center offset so the clock is
            // perfectly centered with maximum edge-to-edge display fill.
            val is7Segment = uiState.fontFamily == ClockFontFamily.SEVEN_SEGMENT
            val firstLsb = if (is7Segment && snapshot.displayHours.startsWith("1")) 0.593f else glyphSideMargin
            val lastMargin = glyphSideMargin

            val totalAdvUnits = (hourDigits * digitUnit) + (colonsCount * colonUnit) +
                    (minuteDigits * digitUnit) + (secondDigits * digitUnit)
            val visualInkUnits = totalAdvUnits - lastMargin - firstLsb
            val inkCenterOffset = (firstLsb - lastMargin) / 2.0f

            val safeW = (screenW.value - 28f).coerceAtLeast(100f)
            val maxFontFromWidth = safeW / visualInkUnits

            val cornerSpaceNeeded = if (uiState.secondsDisplay == ClockSecondsDisplay.SMALL || snapshot.amPm.isNotEmpty()) 48f else 16f
            val safeH = (screenH.value - cornerSpaceNeeded).coerceAtLeast(60f)
            val maxFontFromHeight = safeH / fontHeightMultiplier

            val maxFittingFontSize = minOf(maxFontFromWidth, maxFontFromHeight)
            val computedFontSize = (maxFittingFontSize * uiState.fontSizeScale).sp

            val colonSlotWidth = (computedFontSize.value * colonUnit).dp
            val opticalOffset = (-(computedFontSize.value * inkCenterOffset)).dp

            val dateFontSize = (screenH.value * 0.045f * uiState.fontSizeScale.coerceIn(0.85f, 1.0f)).coerceIn(12f, 16f).sp
            val secFontSize = (screenH.value * 0.055f * uiState.fontSizeScale.coerceIn(0.85f, 1.0f)).coerceIn(15f, 22f).sp
            val amPmFontSize = (secFontSize.value * 0.75f).coerceIn(12f, 17f).sp

            val digitTextStyle = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = computedFontSize,
                lineHeight = computedFontSize,
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

            // Giant Clock Digits filling the display (Vertically and Horizontally Centered)
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .offset(x = opticalOffset),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = snapshot.displayHours,
                        style = digitTextStyle,
                        color = uiState.effectiveColor,
                        maxLines = 1,
                        softWrap = false
                    )

                    CenteredClockColon(
                        slotWidth = colonSlotWidth,
                        fontSize = computedFontSize.value,
                        color = uiState.effectiveColor,
                        colonVisible = snapshot.colonVisible,
                        blinkingColon = uiState.blinkingColon
                    )

                    Text(
                        text = snapshot.minutes,
                        style = digitTextStyle,
                        color = uiState.effectiveColor,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (hasBigSeconds) {
                        CenteredClockColon(
                            slotWidth = colonSlotWidth,
                            fontSize = computedFontSize.value,
                            color = uiState.effectiveColor,
                            colonVisible = snapshot.colonVisible,
                            blinkingColon = uiState.blinkingColon
                        )
                        Text(
                            text = snapshot.seconds,
                            style = digitTextStyle,
                            color = uiState.effectiveSecondaryColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Bottom-Right Corner: Small Live Seconds & AM/PM indicator (Matches reference design)
            if (uiState.secondsDisplay != ClockSecondsDisplay.BIG) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            end = 24.dp,
                            bottom = if (controlsVisible) 56.dp else 12.dp
                        ),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (uiState.secondsDisplay == ClockSecondsDisplay.SMALL) {
                            Text(
                                text = ": ${snapshot.seconds}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = secFontSize,
                                    fontFamily = clockFont,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = uiState.effectiveSecondaryColor
                            )
                        }
                        if (snapshot.amPm.isNotEmpty()) {
                            Text(
                                text = snapshot.amPm.lowercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = amPmFontSize,
                                    fontFamily = FontFamily.Default
                                ),
                                color = uiState.effectiveSecondaryColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Optional Date Display (Centered at bottom, subtle)
            if (uiState.showDate && snapshot.dateFormatted.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (controlsVisible) 56.dp else 16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "${snapshot.dayOfWeek}, ${snapshot.dateFormatted}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = dateFontSize,
                            fontFamily = FontFamily.Default,
                            letterSpacing = 0.5.sp
                        ),
                        color = uiState.effectiveSecondaryColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Optional Running Pomodoro Capsule (Bottom-Left Corner)
            if (pomodoroStatus != null && pomodoroStatus.parsedState != PomodoroState.IDLE) {
                val pState = pomodoroStatus.parsedState
                val isWork = pState == PomodoroState.WORK
                val pColor = if (pomodoroStatus.isPaused) AccentAmber else if (isWork) AccentRed else AccentGreen
                val min = pomodoroStatus.remainingSec / 60
                val sec = pomodoroStatus.remainingSec % 60
                val timeStr = String.format("%02d:%02d", min, sec)
                val stateLabel = if (isWork) "FOCUS" else "BREAK"
                val statusText = if (pomodoroStatus.isPaused) "$stateLabel (PAUSED)  $timeStr" else "$stateLabel  $timeStr"

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 24.dp,
                            bottom = if (controlsVisible) 56.dp else 14.dp
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = pColor.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, pColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSwitchToPomodoro?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (pomodoroStatus.isPaused) Icons.Default.Pause else Icons.Default.HourglassTop,
                                contentDescription = "Pomodoro",
                                tint = pColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
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
                ClockFontFamily.SEVEN_SEGMENT -> 1.70f
                ClockFontFamily.CURSIVE -> 1.35f
                ClockFontFamily.MONOSPACE -> 1.28f
                ClockFontFamily.SERIF -> 1.25f
                ClockFontFamily.SANS_SERIF -> 1.22f
            }
            val colonSlotUnit = 0.35f

            val fontHeightMultiplier = when (uiState.fontFamily) {
                ClockFontFamily.CURSIVE -> 1.25f
                ClockFontFamily.SERIF -> 1.18f
                ClockFontFamily.MONOSPACE -> 1.12f
                ClockFontFamily.SANS_SERIF -> 1.12f
                ClockFontFamily.SEVEN_SEGMENT -> 1.06f
            }

            val safeW = (screenW.value - 32f).coerceAtLeast(100f)
            val totalSlotUnits = (digitSlotUnit * 2) + colonSlotUnit
            val maxFontFromWidth = (safeW / totalSlotUnits).coerceAtLeast(24f)

            val extraHeight = (if (uiState.showDate) 44f else 0f) +
                    (if (pomodoroStatus != null && pomodoroStatus.parsedState != PomodoroState.IDLE) 54f else 0f) + 120f
            val availableClockH = (screenH.value - extraHeight).coerceAtLeast(120f)
            val maxFontFromHeight = (availableClockH / fontHeightMultiplier) * 0.45f

            val maxFittingFontSize = minOf(maxFontFromWidth, maxFontFromHeight)
            val computedFontSize = (maxFittingFontSize * uiState.fontSizeScale)
                .coerceIn(26f, maxFittingFontSize).sp

            val secFontSize = (computedFontSize.value * 0.32f).coerceIn(13f, 32f).sp
            val amPmFontSize = (computedFontSize.value * 0.26f).coerceIn(12f, 28f).sp
            val dateFontSize = (screenW.value * 0.040f * uiState.fontSizeScale.coerceIn(0.85f, 1.15f)).coerceIn(12f, 16f).sp

            val digitSlotWidth = (computedFontSize.value * digitSlotUnit).dp
            val colonSlotWidth = (computedFontSize.value * colonSlotUnit).dp

            val digitTextStyle = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = computedFontSize,
                lineHeight = computedFontSize,
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
                            fontFamily = FontFamily.Default,
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
                            text = snapshot.displayHours,
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
                                    fontFamily = FontFamily.Default
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
                                    text = when (uiState.fontFamily) {
                                        ClockFontFamily.SEVEN_SEGMENT -> "7-SEG"
                                        ClockFontFamily.MONOSPACE -> "MONO"
                                        ClockFontFamily.SANS_SERIF -> "SANS"
                                        ClockFontFamily.SERIF -> "SERIF"
                                        ClockFontFamily.CURSIVE -> "CURS"
                                    },
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

                        // Date Toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.showDate) uiState.theme.primaryColor.copy(alpha = 0.22f) else SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.showDate) uiState.theme.primaryColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onToggleShowDate()
                                }
                        ) {
                            Text(
                                text = if (uiState.showDate) "Date: ON" else "Date: OFF",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (uiState.showDate) uiState.theme.primaryColor else TextSecondary,
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
    val dotSize = (fontSize * 0.10f).coerceIn(4f, 48f).dp
    val dotSpacing = (fontSize * 0.28f).coerceIn(8f, 96f).dp
    val colonAlpha = if (colonVisible) 1.0f else if (blinkingColon) 0.12f else 1.0f

    Box(
        modifier = Modifier.width(slotWidth),
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
