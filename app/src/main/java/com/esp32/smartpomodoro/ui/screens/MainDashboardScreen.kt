package com.esp32.smartpomodoro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.ui.components.*
import com.esp32.smartpomodoro.ui.theme.*
import com.esp32.smartpomodoro.ui.viewmodel.ClockViewModel
import com.esp32.smartpomodoro.ui.viewmodel.PomodoroViewModel
import com.esp32.smartpomodoro.ui.viewmodel.StopwatchViewModel

enum class DashboardTab(val title: String) {
    POMODORO("Focus Sprint"),
    STOPWATCH("Giant Stopwatch"),
    CLOCK("Desk Clock")
}

@Composable
fun MainDashboardScreen(
    pomodoroViewModel: PomodoroViewModel,
    stopwatchViewModel: StopwatchViewModel? = null,
    clockViewModel: ClockViewModel? = null
) {
    val pomodoroUiState by pomodoroViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(DashboardTab.POMODORO) }

    // Synchronize active tab with StopwatchViewModel so hardware volume keys are only intercepted when Stopwatch is in focus
    LaunchedEffect(selectedTab) {
        stopwatchViewModel?.setActiveTab(selectedTab.name)
    }

    // Stopwatch and Clock state observation
    val stopwatchUiState = stopwatchViewModel?.uiState?.collectAsState()?.value
    val clockUiState = clockViewModel?.uiState?.collectAsState()?.value

    // If Giant Clock is in full screen mode, show full-screen immersive view with BackHandler
    if (clockUiState?.isFullScreen == true) {
        BackHandler {
            clockViewModel.setFullScreen(false)
        }
        GiantClockFullScreen(
            uiState = clockUiState,
            pomodoroStatus = pomodoroUiState.status,
            onCycleRotation = { clockViewModel.cycleRotation() },
            onSelectRotation = { clockViewModel.setRotationAngle(it) },
            onToggleOrientation = { clockViewModel.toggleOrientation() },
            onToggleAutoRotate = { clockViewModel.toggleAutoRotate() },
            onSwitchToStopwatch = {
                clockViewModel.setFullScreen(false)
                selectedTab = DashboardTab.STOPWATCH
            },
            onSwitchToPomodoro = {
                clockViewModel.setFullScreen(false)
                selectedTab = DashboardTab.POMODORO
            },
            onCycleTheme = { clockViewModel.cycleTheme() },
            onCycleFontFamily = { clockViewModel.cycleFontFamily() },
            onCycleFontSize = { clockViewModel.cycleFontSizeScale() },
            onToggleTimeFormat = { clockViewModel.toggleTimeFormat() },
            onCycleSeconds = { clockViewModel.cycleSecondsDisplay() },
            onToggleNightDimming = { clockViewModel.toggleNightDimming() },
            onBrightnessChange = { clockViewModel.setBrightnessFactor(it) },
            onExitFullScreen = { clockViewModel.setFullScreen(false) },
            onOpenSettings = { clockViewModel.setShowSettingsDialog(true) }
        )

        // Settings Dialog accessible from Fullscreen as well
        if (clockUiState.showSettingsDialog) {
            ClockSettingsDialog(
                uiState = clockUiState,
                onTimeFormatChange = { clockViewModel.setTimeFormat(it) },
                onSecondsDisplayChange = { clockViewModel.setSecondsDisplay(it) },
                onFontFamilyChange = { clockViewModel.setFontFamily(it) },
                onFontSizeChange = { clockViewModel.setFontSizeScale(it) },
                onToggleShowDate = { clockViewModel.toggleShowDate() },
                onToggleBlinkingColon = { clockViewModel.toggleBlinkingColon() },
                onToggleAutoRotate = { clockViewModel.toggleAutoRotate() },
                onSelectRotation = { clockViewModel.setRotationAngle(it) },
                onToggleOrientation = { clockViewModel.toggleOrientation() },
                onToggleNightDimming = { clockViewModel.toggleNightDimming() },
                onBrightnessChange = { clockViewModel.setBrightnessFactor(it) },
                onToggleKeepScreenOn = { clockViewModel.toggleKeepScreenOn() },
                onThemeSelect = { clockViewModel.setTheme(it) },
                onDismiss = { clockViewModel.setShowSettingsDialog(false) }
            )
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark,
        topBar = {
            HeaderBar(
                isConnected = pomodoroUiState.isConnected,
                hostAddress = pomodoroUiState.hostAddress,
                onOpenIpSettings = { pomodoroViewModel.setShowIpDialog(true) },
                onOpenCalibration = { pomodoroViewModel.setShowCalibrationDialog(true) },
                onOpenAbout = { pomodoroViewModel.setShowAboutDialog(true) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .background(BackgroundDark),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Navigation Tab Switcher (Pomodoro Focus vs Giant Stopwatch vs Desk Clock)
            if (stopwatchViewModel != null || clockViewModel != null) {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. Pomodoro Tab Pill
                        val isPomodoro = selectedTab == DashboardTab.POMODORO
                        Surface(
                            color = if (isPomodoro) AccentRed else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTab = DashboardTab.POMODORO }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = "Pomodoro",
                                    tint = if (isPomodoro) BackgroundDark else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Focus",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isPomodoro) BackgroundDark else TextSecondary
                                )
                            }
                        }

                        // 2. Stopwatch Tab Pill (with active running badge)
                        val isStopwatch = selectedTab == DashboardTab.STOPWATCH
                        val anyStopwatchRunning = stopwatchUiState?.slots?.any { it.isRunning } == true
                        Surface(
                            color = if (isStopwatch) AccentCyan else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTab = DashboardTab.STOPWATCH }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AvTimer,
                                    contentDescription = "Stopwatch",
                                    tint = if (isStopwatch) BackgroundDark else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Stopwatch",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isStopwatch) BackgroundDark else TextSecondary
                                )
                                if (anyStopwatchRunning) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isStopwatch) BackgroundDark else AccentGreen)
                                    )
                                }
                            }
                        }

                        // 3. Giant Desk Clock Tab Pill
                        val isClock = selectedTab == DashboardTab.CLOCK
                        val clockThemeColor = clockUiState?.theme?.primaryColor ?: AccentAmber
                        Surface(
                            color = if (isClock) clockThemeColor else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTab = DashboardTab.CLOCK }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Desk Clock",
                                    tint = if (isClock) BackgroundDark else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Clock",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isClock) BackgroundDark else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                DashboardTab.POMODORO -> {
                    // 1. Pomodoro Focus Countdown Dial & Controls
                    PomodoroTimerCard(
                        status = pomodoroUiState.status,
                        onStart = { pomodoroViewModel.triggerPomodoroAction("start") },
                        onPause = { pomodoroViewModel.triggerPomodoroAction("pause") },
                        onResume = { pomodoroViewModel.triggerPomodoroAction("resume") },
                        onSkip = { pomodoroViewModel.triggerPomodoroAction("skip") },
                        onReset = { pomodoroViewModel.triggerPomodoroAction("reset") }
                    )

                    // 2. Presence & Environmental Telemetry
                    TelemetryGrid(
                        status = pomodoroUiState.status,
                        onToggleAutoPause = { pomodoroViewModel.toggleAutoPause() }
                    )

                    // 3. 24-Key RGB Light Strip Controller
                    RgbRemoteCard(
                        selectedStrip = pomodoroUiState.selectedStrip,
                        brightness = pomodoroUiState.localBrightness,
                        currentEffect = pomodoroUiState.status.effect,
                        fadeSpeed = pomodoroUiState.status.fadeSpeed,
                        fadePalette = pomodoroUiState.status.fadePalette,
                        smoothSpeed = pomodoroUiState.status.smoothSpeed,
                        smoothBreath = pomodoroUiState.status.smoothBreath,
                        smoothPalette = pomodoroUiState.status.smoothPalette,
                        onSelectStrip = { pomodoroViewModel.selectStrip(it) },
                        onBrightnessChange = { pomodoroViewModel.setBrightness(it) },
                        onSendCommand = { pomodoroViewModel.sendColorCommand(it) },
                        onSetFadeConfig = { speed, palette -> pomodoroViewModel.setFadeConfig(speed, palette) },
                        onSetSmoothConfig = { speed, breath, palette -> pomodoroViewModel.setSmoothConfig(speed, breath, palette) }
                    )
                }

                DashboardTab.STOPWATCH -> {
                    if (stopwatchViewModel != null && stopwatchUiState != null) {
                        val (mainDigits, fractionDigits) = remember(
                            stopwatchUiState.displayedTimeMs,
                            stopwatchUiState.precision
                        ) {
                            stopwatchViewModel.engine.getFormattedComponents(
                                stopwatchUiState.displayedTimeMs,
                                stopwatchUiState.precision
                            )
                        }

                        StopwatchCard(
                            uiState = stopwatchUiState,
                            mainTimeDigits = mainDigits,
                            fractionDigits = fractionDigits,
                            onPrimaryAction = { stopwatchViewModel.onPrimaryAction() },
                            onPrimaryActionLong = { stopwatchViewModel.onPrimaryActionLong() },
                            onSecondaryAction = { stopwatchViewModel.onSecondaryAction() },
                            onSelectSlot = { stopwatchViewModel.selectSlot(it) },
                            onSetDelay = { stopwatchViewModel.setPreStartDelay(it) },
                            onToggleLock = { stopwatchViewModel.toggleTouchLock() },
                            onUnlock = { stopwatchViewModel.unlockTouch() },
                            onLockTouched = { stopwatchViewModel.onLockTouched() },
                            onOpenPaceDialog = { stopwatchViewModel.setShowPaceDialog(true) },
                            onCopyTime = { stopwatchViewModel.copyTimeToClipboard(context) },
                            onCopyLaps = { stopwatchViewModel.copyLapsToClipboard(context) },
                            onClearLaps = { stopwatchViewModel.clearLaps() },
                            onResetAll = { stopwatchViewModel.resetAllSlots() },
                            onToggleSound = { stopwatchViewModel.toggleSound() },
                            onToggleVolumeControl = { stopwatchViewModel.toggleVolumeControl() }
                        )

                        // Pace & Speed Calculator Dialog
                        if (stopwatchUiState.showPaceDialog) {
                            PaceCalculatorDialog(
                                elapsedTimeMs = stopwatchUiState.displayedTimeMs,
                                timeFormatted = stopwatchViewModel.engine.formatTimeFull(stopwatchUiState.displayedTimeMs),
                                onCalculate = { dist, unit -> stopwatchViewModel.calculatePaceAndSpeed(dist, unit) },
                                onCopyPace = { stopwatchViewModel.copyTextToClipboard("Pace", it, context) },
                                onCopySpeed = { stopwatchViewModel.copyTextToClipboard("Speed", it, context) },
                                onDismiss = { stopwatchViewModel.setShowPaceDialog(false) }
                            )
                        }
                    }
                }

                DashboardTab.CLOCK -> {
                    if (clockViewModel != null && clockUiState != null) {
                        ClockCard(
                            uiState = clockUiState,
                            pomodoroStatus = pomodoroUiState.status,
                            onCycleRotation = { clockViewModel.cycleRotation() },
                            onSelectRotation = { clockViewModel.setRotationAngle(it) },
                            onToggleOrientation = { clockViewModel.toggleOrientation() },
                            onToggleAutoRotate = { clockViewModel.toggleAutoRotate() },
                            onCycleTheme = { clockViewModel.cycleTheme() },
                            onCycleFontFamily = { clockViewModel.cycleFontFamily() },
                            onCycleFontSize = { clockViewModel.cycleFontSizeScale() },
                            onToggleTimeFormat = { clockViewModel.toggleTimeFormat() },
                            onCycleSeconds = { clockViewModel.cycleSecondsDisplay() },
                            onToggleNightDimming = { clockViewModel.toggleNightDimming() },
                            onToggleFullScreen = { clockViewModel.setFullScreen(true) },
                            onOpenSettings = { clockViewModel.setShowSettingsDialog(true) },
                            onCopyTime = { clockViewModel.copyCurrentTimeToClipboard(context) },
                            onSwitchToPomodoro = { selectedTab = DashboardTab.POMODORO }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // IP Connection Configuration Modal
        if (pomodoroUiState.showIpDialog) {
            IpSettingsDialog(
                currentHost = pomodoroUiState.hostAddress,
                onDismiss = { pomodoroViewModel.setShowIpDialog(false) },
                onSaveHost = { pomodoroViewModel.setHostAddress(it) },
                onTestConnection = { pomodoroViewModel.testConnection(it) }
            )
        }

        // Pin Swap Calibration Modal
        if (pomodoroUiState.showCalibrationDialog) {
            CalibrationDialog(
                onDismiss = { pomodoroViewModel.setShowCalibrationDialog(false) },
                onCalibrate = { strip, action -> pomodoroViewModel.calibrateStrip(strip, action) }
            )
        }

        // About & System Info Modal
        if (pomodoroUiState.showAboutDialog) {
            AboutDialog(
                hostAddress = pomodoroUiState.hostAddress,
                isConnected = pomodoroUiState.isConnected,
                onDismiss = { pomodoroViewModel.setShowAboutDialog(false) }
            )
        }

        // Clock Settings Configuration Modal
        if (clockUiState?.showSettingsDialog == true) {
            ClockSettingsDialog(
                uiState = clockUiState,
                onTimeFormatChange = { clockViewModel.setTimeFormat(it) },
                onSecondsDisplayChange = { clockViewModel.setSecondsDisplay(it) },
                onFontFamilyChange = { clockViewModel.setFontFamily(it) },
                onFontSizeChange = { clockViewModel.setFontSizeScale(it) },
                onToggleShowDate = { clockViewModel.toggleShowDate() },
                onToggleBlinkingColon = { clockViewModel.toggleBlinkingColon() },
                onToggleAutoRotate = { clockViewModel.toggleAutoRotate() },
                onSelectRotation = { clockViewModel.setRotationAngle(it) },
                onToggleOrientation = { clockViewModel.toggleOrientation() },
                onToggleNightDimming = { clockViewModel.toggleNightDimming() },
                onBrightnessChange = { clockViewModel.setBrightnessFactor(it) },
                onToggleKeepScreenOn = { clockViewModel.toggleKeepScreenOn() },
                onThemeSelect = { clockViewModel.setTheme(it) },
                onDismiss = { clockViewModel.setShowSettingsDialog(false) }
            )
        }
    }
}
