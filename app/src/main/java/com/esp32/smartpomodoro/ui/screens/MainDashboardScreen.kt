package com.esp32.smartpomodoro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Palette
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
import com.esp32.smartpomodoro.data.model.ClockTheme
import com.esp32.smartpomodoro.data.model.PomodoroStatus
import com.esp32.smartpomodoro.ui.components.*
import com.esp32.smartpomodoro.ui.theme.*
import com.esp32.smartpomodoro.ui.viewmodel.ClockViewModel
import com.esp32.smartpomodoro.ui.viewmodel.PomodoroViewModel
import com.esp32.smartpomodoro.ui.viewmodel.StopwatchViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class DashboardTab(val title: String) {
    FOCUS("Focus Sprint"),
    TIME("Desk Time"),
    AMBIENT("Ambient Aura")
}

enum class TimeSubTab(val title: String) {
    CLOCK("Desk Clock"),
    STOPWATCH("Giant Stopwatch")
}

@Composable
fun MainDashboardScreen(
    pomodoroViewModel: PomodoroViewModel,
    stopwatchViewModel: StopwatchViewModel? = null,
    clockViewModel: ClockViewModel? = null
) {
    val pomodoroUiState by pomodoroViewModel.uiState.collectAsState()
    val focusScrollState = rememberScrollState()
    val timeScrollState = rememberScrollState()
    val ambientScrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(DashboardTab.FOCUS) }
    var timeSubTab by remember { mutableStateOf(TimeSubTab.CLOCK) }

    // Synchronize active states:
    // 1. Clock ticker only ticks when Clock is actively visible (or in fullscreen)
    // 2. Hardware volume keys are only intercepted when Stopwatch is actively visible
    LaunchedEffect(selectedTab, timeSubTab) {
        val isClockActive = (selectedTab == DashboardTab.TIME && timeSubTab == TimeSubTab.CLOCK)
        clockViewModel?.setClockVisible(isClockActive)

        if (selectedTab == DashboardTab.TIME && timeSubTab == TimeSubTab.STOPWATCH) {
            stopwatchViewModel?.setActiveTab("STOPWATCH")
        } else {
            stopwatchViewModel?.setActiveTab("OTHER")
        }
    }

    // Window configuration (only emits when fullscreen, keepScreenOn, etc. change - never on 200ms time ticks)
    val windowConfig by (clockViewModel?.windowConfig?.collectAsState(
        initial = com.esp32.smartpomodoro.data.model.ClockWindowConfig(
            keepScreenOn = clockViewModel.engine.keepScreenOn,
            isAutoRotate = clockViewModel.engine.isAutoRotate,
            isFullScreen = clockViewModel.engine.isFullScreen,
            rotationAngle = clockViewModel.engine.rotationAngle
        )
    ) ?: remember { mutableStateOf(null) })

    // If Giant Clock is in full screen mode, show full-screen immersive view with BackHandler
    if (windowConfig?.isFullScreen == true && clockViewModel != null) {
        FullScreenClockHost(
            clockViewModel = clockViewModel,
            pomodoroStatus = pomodoroUiState.status,
            onSwitchToStopwatch = {
                clockViewModel.setFullScreen(false)
                selectedTab = DashboardTab.TIME
                timeSubTab = TimeSubTab.STOPWATCH
            },
            onSwitchToPomodoro = {
                clockViewModel.setFullScreen(false)
                selectedTab = DashboardTab.FOCUS
            }
        )
        return
    }

    // Distinct lightweight observations for Tab Row that NEVER emit on high-frequency ticks
    val clockTheme by remember(clockViewModel) {
        clockViewModel?.uiState?.map { it.theme }?.distinctUntilChanged()
            ?: flowOf(ClockTheme.AMBER_NIGHT)
    }.collectAsState(initial = ClockTheme.AMBER_NIGHT)
    val timeActiveColor = clockTheme.primaryColor

    val anyStopwatchRunning by (stopwatchViewModel?.isAnyRunning?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) })

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
                .background(BackgroundDark)
        ) {
            // Pinned Tab Bar: Rock-solid navigation anchored at top (never scrolls away or shifts vertically)
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Focus Sprint Tab Pill (Calm Mindful Coral)
                    val isFocus = selectedTab == DashboardTab.FOCUS
                    Surface(
                        color = if (isFocus) AccentCoral else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = DashboardTab.FOCUS }
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
                                contentDescription = "Focus",
                                tint = if (isFocus) BackgroundDark else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Focus",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isFocus) BackgroundDark else TextSecondary
                            )
                        }
                    }

                    // 2. Time Tab Pill (Clock + Stopwatch Unified, with active running badge)
                    val isTime = selectedTab == DashboardTab.TIME
                    Surface(
                        color = if (isTime) timeActiveColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = DashboardTab.TIME }
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
                                contentDescription = "Time",
                                tint = if (isTime) BackgroundDark else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Time",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isTime) BackgroundDark else TextSecondary
                            )
                            if (anyStopwatchRunning) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isTime) BackgroundDark else AccentGreen)
                                )
                            }
                        }
                    }

                    // 3. Ambient Lighting Tab Pill (Dedicated RGB Controller)
                    val isAmbient = selectedTab == DashboardTab.AMBIENT
                    Surface(
                        color = if (isAmbient) AccentPurple else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.05f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = DashboardTab.AMBIENT }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Ambient Lights",
                                tint = if (isAmbient) BackgroundDark else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Ambient",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isAmbient) BackgroundDark else TextSecondary
                            )
                        }
                    }
                }
            }

            // Tab Content with silky-smooth crossfade and independent scrolling per tab
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith
                    fadeOut(animationSpec = tween(120))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "dashboardTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    DashboardTab.FOCUS -> {
                        // Deep Work Sanctuary: Zero visual clutter from lighting controllers
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(focusScrollState)
                                .padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PomodoroTimerCard(
                                status = pomodoroUiState.status,
                                onStart = { pomodoroViewModel.triggerPomodoroAction("start") },
                                onPause = { pomodoroViewModel.triggerPomodoroAction("pause") },
                                onResume = { pomodoroViewModel.triggerPomodoroAction("resume") },
                                onSkip = { pomodoroViewModel.triggerPomodoroAction("skip") },
                                onReset = { pomodoroViewModel.triggerPomodoroAction("reset") }
                            )

                            TelemetryGrid(
                                status = pomodoroUiState.status,
                                onToggleAutoPause = { pomodoroViewModel.toggleAutoPause() }
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    DashboardTab.TIME -> {
                        // Unified Timekeeper Hub: Desk Clock and Giant Stopwatch
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(timeScrollState)
                                .padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Sub-tab switcher (Desk Clock vs Giant Stopwatch)
                            Surface(
                                color = SurfaceDark,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Sub-tab 1: Desk Clock
                                    val isClockSub = timeSubTab == TimeSubTab.CLOCK
                                    Surface(
                                        color = if (isClockSub) timeActiveColor else Color.Transparent,
                                        shape = RoundedCornerShape(11.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(11.dp))
                                            .clickable { timeSubTab = TimeSubTab.CLOCK }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Desk Clock",
                                                tint = if (isClockSub) BackgroundDark else TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Desk Clock",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isClockSub) BackgroundDark else TextSecondary
                                            )
                                        }
                                    }

                                    // Sub-tab 2: Giant Stopwatch
                                    val isStopwatchSub = timeSubTab == TimeSubTab.STOPWATCH
                                    Surface(
                                        color = if (isStopwatchSub) AccentCyan else Color.Transparent,
                                        shape = RoundedCornerShape(11.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(11.dp))
                                            .clickable { timeSubTab = TimeSubTab.STOPWATCH }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AvTimer,
                                                contentDescription = "Stopwatch",
                                                tint = if (isStopwatchSub) BackgroundDark else TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Stopwatch",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isStopwatchSub) BackgroundDark else TextSecondary
                                            )
                                            if (anyStopwatchRunning) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isStopwatchSub) BackgroundDark else AccentGreen)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Sub-tab Content with smooth transition
                            AnimatedContent(
                                targetState = timeSubTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(150)) togetherWith
                                    fadeOut(animationSpec = tween(90))
                                },
                                label = "timeSubTabTransition"
                            ) { subTab ->
                                when (subTab) {
                                    TimeSubTab.CLOCK -> {
                                        if (clockViewModel != null) {
                                            ClockCardHost(
                                                clockViewModel = clockViewModel,
                                                pomodoroStatus = pomodoroUiState.status,
                                                onSwitchToPomodoro = { selectedTab = DashboardTab.FOCUS }
                                            )
                                        }
                                    }
                                    TimeSubTab.STOPWATCH -> {
                                        if (stopwatchViewModel != null) {
                                            StopwatchCardHost(
                                                stopwatchViewModel = stopwatchViewModel
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    DashboardTab.AMBIENT -> {
                        // Dedicated 24-Key RGB Light Strip Controller & Atmosphere Studio
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(ambientScrollState)
                                .padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RgbRemoteCard(
                                selectedStrip = pomodoroUiState.selectedStrip,
                                brightness = pomodoroUiState.localBrightness,
                                currentEffect = pomodoroUiState.status.effect,
                                strobeMode = pomodoroUiState.status.strobeMode,
                                fadeSpeed = pomodoroUiState.status.fadeSpeed,
                                fadePalette = pomodoroUiState.status.fadePalette,
                                smoothSpeed = pomodoroUiState.status.smoothSpeed,
                                smoothBreath = pomodoroUiState.status.smoothBreath,
                                smoothPalette = pomodoroUiState.status.smoothPalette,
                                onSelectStrip = { pomodoroViewModel.selectStrip(it) },
                                onBrightnessChange = { pomodoroViewModel.setBrightness(it) },
                                onSendCommand = { pomodoroViewModel.sendColorCommand(it) },
                                onSetFadeConfig = { speed, palette -> pomodoroViewModel.setFadeConfig(speed, palette) },
                                onSetSmoothConfig = { speed, breath, palette -> pomodoroViewModel.setSmoothConfig(speed, breath, palette) },
                                onSetStrobeMode = { pomodoroViewModel.setStrobeMode(it) }
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
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
    }
}

/**
 * Isolated host for Desk Clock card: encapsulates 200ms snapshot updates
 * so they only recompose this specific card instead of the outer dashboard.
 */
@Composable
private fun ClockCardHost(
    clockViewModel: ClockViewModel,
    pomodoroStatus: PomodoroStatus,
    onSwitchToPomodoro: () -> Unit
) {
    val clockUiState by clockViewModel.uiState.collectAsState()
    val context = LocalContext.current

    ClockCard(
        uiState = clockUiState,
        pomodoroStatus = pomodoroStatus,
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
        onSwitchToPomodoro = onSwitchToPomodoro
    )

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
}

/**
 * Isolated host for Giant Stopwatch card: encapsulates 16ms snapshot updates
 * so they only recompose this specific card instead of the outer dashboard.
 */
@Composable
private fun StopwatchCardHost(
    stopwatchViewModel: StopwatchViewModel
) {
    val stopwatchUiState by stopwatchViewModel.uiState.collectAsState()
    val context = LocalContext.current

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

/**
 * Fullscreen host for Giant Clock: provides immersive full-screen display with back handling.
 */
@Composable
private fun FullScreenClockHost(
    clockViewModel: ClockViewModel,
    pomodoroStatus: PomodoroStatus,
    onSwitchToStopwatch: () -> Unit,
    onSwitchToPomodoro: () -> Unit
) {
    val clockUiState by clockViewModel.uiState.collectAsState()

    BackHandler {
        clockViewModel.setFullScreen(false)
    }

    GiantClockFullScreen(
        uiState = clockUiState,
        pomodoroStatus = pomodoroStatus,
        onCycleRotation = { clockViewModel.cycleRotation() },
        onSelectRotation = { clockViewModel.setRotationAngle(it) },
        onToggleOrientation = { clockViewModel.toggleOrientation() },
        onToggleAutoRotate = { clockViewModel.toggleAutoRotate() },
        onSwitchToStopwatch = onSwitchToStopwatch,
        onSwitchToPomodoro = onSwitchToPomodoro,
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
}
