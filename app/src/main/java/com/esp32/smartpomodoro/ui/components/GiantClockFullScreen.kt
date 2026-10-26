package com.esp32.smartpomodoro.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.data.model.*
import com.esp32.smartpomodoro.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GiantClockFullScreen(
    uiState: ClockUiState,
    pomodoroStatus: PomodoroStatus? = null,
    onCycleRotation: () -> Unit,
    onSelectRotation: (ClockRotationAngle) -> Unit,
    onToggleOrientation: () -> Unit,
    onToggleAutoRotate: () -> Unit = {},
    onSwitchToStopwatch: (() -> Unit)? = null,
    onSwitchToPomodoro: (() -> Unit)? = null,
    onCycleTheme: () -> Unit,
    onToggleTimeFormat: () -> Unit,
    onCycleSeconds: () -> Unit,
    onToggleNightDimming: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onExitFullScreen: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val snapshot = uiState.snapshot
    val isLandscape = uiState.rotationAngle.isLandscape

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

        // When rotated 90° or 270°, the content width is the screen height and vice versa
        val contentWidth = if (isLandscape) screenH else screenW
        val contentHeight = if (isLandscape) screenW else screenH

        // Responsive auto-scaling typography calculation (constrained by width and height to prevent clipping)
        val digitScale = when {
            uiState.secondsDisplay == ClockSecondsDisplay.BIG -> 0.14f
            else -> 0.22f
        }
        val maxFontFromWidth = contentWidth.value * digitScale
        val maxFontFromHeight = contentHeight.value * 0.52f
        val computedFontSize = minOf(maxFontFromWidth, maxFontFromHeight).coerceIn(36f, 220f).sp
        val smallSecFontSize = (computedFontSize.value * 0.42f).coerceIn(18f, 90f).sp
        val amPmFontSize = (computedFontSize.value * 0.25f).coerceIn(12f, 40f).sp
        val dateFontSize = minOf(contentWidth.value * 0.045f, contentHeight.value * 0.08f).coerceIn(12f, 32f).sp

        // Central Giant Digits with Rotation
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .size(width = contentWidth, height = contentHeight)
                    .safeDrawingPadding()
                    .graphicsLayer {
                        rotationZ = uiState.rotationAngle.degrees.toFloat()
                    }
            ) {
                // Digits Row
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hours
                    Text(
                        text = snapshot.hours,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = computedFontSize,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-2).sp
                        ),
                        color = uiState.effectiveColor
                    )

                    // Blinking Colon
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = computedFontSize,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (snapshot.colonVisible) uiState.effectiveColor else Color.Transparent,
                        modifier = Modifier.padding(horizontal = (computedFontSize.value * 0.02f).dp)
                    )

                    // Minutes
                    Text(
                        text = snapshot.minutes,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = computedFontSize,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-2).sp
                        ),
                        color = uiState.effectiveColor
                    )

                    // Seconds Display
                    when (uiState.secondsDisplay) {
                        ClockSecondsDisplay.BIG -> {
                            Text(
                                text = ":",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = computedFontSize,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (snapshot.colonVisible) uiState.effectiveColor else Color.Transparent,
                                modifier = Modifier.padding(horizontal = (computedFontSize.value * 0.02f).dp)
                            )
                            Text(
                                text = snapshot.seconds,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = computedFontSize,
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
                                modifier = Modifier.padding(
                                    bottom = (computedFontSize.value * 0.12f).dp,
                                    start = 6.dp
                                )
                            )
                        }
                        ClockSecondsDisplay.NONE -> {}
                    }

                    // AM/PM Indicator
                    if (snapshot.amPm.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = uiState.effectiveColor.copy(alpha = 0.18f),
                            modifier = Modifier
                                .padding(
                                    bottom = (computedFontSize.value * 0.14f).dp,
                                    start = 8.dp
                                )
                        ) {
                            Text(
                                text = snapshot.amPm,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = amPmFontSize
                                ),
                                color = uiState.effectiveColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Date Display beneath giant numerals
                if (uiState.showDate && snapshot.dateFormatted.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${snapshot.dayOfWeek}, ${snapshot.dateFormatted}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = dateFontSize,
                            letterSpacing = 0.5.sp
                        ),
                        color = uiState.effectiveSecondaryColor
                    )
                }

                // Running Pomodoro Timer Indicator
                if (pomodoroStatus != null && pomodoroStatus.parsedState != PomodoroState.IDLE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val pState = pomodoroStatus.parsedState
                    val isWork = pState == PomodoroState.WORK
                    val pColor = if (pomodoroStatus.isPaused) AccentAmber else if (isWork) AccentRed else AccentGreen
                    val min = pomodoroStatus.remainingSec / 60
                    val sec = pomodoroStatus.remainingSec % 60
                    val timeStr = String.format("%02d:%02d", min, sec)
                    val stateLabel = if (isWork) "FOCUS" else "BREAK"
                    val statusText = if (pomodoroStatus.isPaused) "$stateLabel (PAUSED)  $timeStr" else "$stateLabel  $timeStr"

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = pColor.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onSwitchToPomodoro?.invoke()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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
                                    fontSize = (computedFontSize.value * 0.16f).coerceIn(12f, 22f).sp,
                                    letterSpacing = 1.sp
                                ),
                                color = pColor
                            )
                        }
                    }
                }
            }
        }

        // Animated Overlays (Top Bar & Bottom Controls)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // TOP BAR: Exit, Rotation Angle Pills, Theme Swatch, Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back / Exit Fullscreen Button
                    IconButton(
                        onClick = onExitFullScreen,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }

                    // Multi-Orientation Controls (Auto, 0°, 90°, 180°, 270°, Rotate, Toggle)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Auto-Rotate pill
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
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (uiState.isAutoRotate) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (uiState.isAutoRotate) BackgroundDark else TextSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                            )
                        }

                        ClockRotationAngle.entries.forEach { angle ->
                            val isSelected = !uiState.isAutoRotate && uiState.rotationAngle == angle
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) uiState.theme.primaryColor else SurfaceDark,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        userInteractionTimestamp = System.currentTimeMillis()
                                        onSelectRotation(angle)
                                    }
                            ) {
                                Text(
                                    text = "${angle.degrees}°",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) BackgroundDark else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Rotate 90° button
                        IconButton(
                            onClick = {
                                userInteractionTimestamp = System.currentTimeMillis()
                                onCycleRotation()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate 90°",
                                tint = uiState.theme.primaryColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Orientation toggle (Portrait / Landscape) button
                        IconButton(
                            onClick = {
                                userInteractionTimestamp = System.currentTimeMillis()
                                onToggleOrientation()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Toggle Landscape/Portrait",
                                tint = if (uiState.rotationAngle.isLandscape) uiState.theme.primaryColor else TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Actions: Stopwatch switch, Theme & Settings
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick switch to Giant Stopwatch
                        if (onSwitchToStopwatch != null) {
                            IconButton(
                                onClick = {
                                    userInteractionTimestamp = System.currentTimeMillis()
                                    onSwitchToStopwatch()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AvTimer,
                                    contentDescription = "Switch to Stopwatch",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Quick Theme Cycle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(uiState.theme.primaryColor)
                                .border(1.dp, Color.White, CircleShape)
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
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Settings Dialog
                        IconButton(
                            onClick = {
                                userInteractionTimestamp = System.currentTimeMillis()
                                onOpenSettings()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // BOTTOM BAR: Quick Mode Toggles (12/24H, Sec, Night Mode, Brightness)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Format & Mode Pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.nightDimming) Icons.Default.Bedtime else Icons.Default.BrightnessMedium,
                                    contentDescription = null,
                                    tint = if (uiState.nightDimming) AccentAmber else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (uiState.nightDimming) "Night" else "Day",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (uiState.nightDimming) AccentAmber else TextSecondary
                                )
                            }
                        }
                    }

                    // Brightness Level Indicator / Presets
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
