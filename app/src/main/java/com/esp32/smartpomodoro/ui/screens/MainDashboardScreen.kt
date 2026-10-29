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
    val focusScrollState = rememberScrollState()
    val timeScrollState = rememberScrollState()
    val ambientScrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(DashboardTab.FOCUS) }
    var timeSubTab by remember { mutableStateOf(TimeSubTab.CLOCK) }

    // Synchronize active states:
    // 1. Clock ticker only ticks when Clock is actively visible (or in fullscreen)
    // 2. Hardware volume keys and 60fps ticker are only active when Stopwatch is actively visible
    // 3. Pomodoro polling interval is relaxed when on other tabs to save network & radio power
    LaunchedEffect(selectedTab, timeSubTab) {
        val isClockActive = (selectedTab == DashboardTab.TIME && timeSubTab == TimeSubTab.CLOCK)
        clockViewModel?.setClockVisible(isClockActive)

        if (selectedTab == DashboardTab.TIME && timeSubTab == TimeSubTab.STOPWATCH) {
            stopwatchViewModel?.setActiveTab("STOPWATCH")
        } else {
            stopwatchViewModel?.setActiveTab("OTHER")
        }

        pomodoroViewModel.setActiveTab(if (selectedTab == DashboardTab.FOCUS) "FOCUS" else "OTHER")
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
            pomodoroViewModel = pomodoroViewModel,
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
            HeaderBarHost(pomodoroViewModel = pomodoroViewModel)
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

            // Zero-overhead instantaneous Tab Content:
            // Direct conditional layout eliminates concurrent tree measurement, removing tab switching lag
            // and keeping CPU & battery footprint ultra-light for always-on desk operation.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    DashboardTab.FOCUS -> {
                        // Isolated Focus sanctuary: only recomposes on pomodoro timer ticks
                        FocusTabHost(
                            pomodoroViewModel = pomodoroViewModel,
                            scrollState = focusScrollState
                        )
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

                            // Sub-tab Content: direct zero-latency switching
                            when (timeSubTab) {
                                TimeSubTab.CLOCK -> {
                                    if (clockViewModel != null) {
                                        ClockCardHost(
                                            clockViewModel = clockViewModel,
                                            pomodoroViewModel = pomodoroViewModel,
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

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    DashboardTab.AMBIENT -> {
                        // Isolated Atmosphere Studio: Dedicated 24-Key RGB Light Strip Controller
                        AmbientTabHost(
                            pomodoroViewModel = pomodoroViewModel,
                            scrollState = ambientScrollState
                        )
                    }
                }
            }
        }

        // Isolated Modals Host: Zero recomposition of main layout when dialogs open or close
        PomodoroModalsHost(pomodoroViewModel = pomodoroViewModel)
    }
}

/**
 * Isolated host for HeaderBar: only recomposes when network connection state changes.
 * Completely immune to 1-second Pomodoro countdown or clock ticks.
 */
@Composable
private fun HeaderBarHost(pomodoroViewModel: PomodoroViewModel) {
    val connectionInfo by remember(pomodoroViewModel) {
        pomodoroViewModel.uiState.map { Pair(it.isConnected, it.hostAddress) }.distinctUntilChanged()
    }.collectAsState(initial = Pair(false, "192.168.4.1"))

    HeaderBar(
        isConnected = connectionInfo.first,
        hostAddress = connectionInfo.second,
        onOpenIpSettings = { pomodoroViewModel.setShowIpDialog(true) },
        onOpenCalibration = { pomodoroViewModel.setShowCalibrationDialog(true) },
        onOpenAbout = { pomodoroViewModel.setShowAboutDialog(true) }
    )
}

/**
 * Isolated host for Focus tab: contains PomodoroTimerCard and TelemetryGrid.
 * Only recomposes its own contents on Pomodoro countdown updates.
 */
@Composable
private fun FocusTabHost(
    pomodoroViewModel: PomodoroViewModel,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val uiState by pomodoroViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PomodoroTimerCard(
            status = uiState.status,
            onStart = { pomodoroViewModel.triggerPomodoroAction("start") },
            onPause = { pomodoroViewModel.triggerPomodoroAction("pause") },
            onResume = { pomodoroViewModel.triggerPomodoroAction("resume") },
            onSkip = { pomodoroViewModel.triggerPomodoroAction("skip") },
            onReset = { pomodoroViewModel.triggerPomodoroAction("reset") }
        )

        TelemetryGrid(
            status = uiState.status,
            onToggleAutoPause = { pomodoroViewModel.toggleAutoPause() }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Isolated host for Ambient lighting tab: contains 24-key remote card.
 * Only recomposes when RGB settings or strip targets change.
 */
@Composable
private fun AmbientTabHost(
    pomodoroViewModel: PomodoroViewModel,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val uiState by pomodoroViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RgbRemoteCard(
            selectedStrip = uiState.selectedStrip,
            brightness = uiState.localBrightness,
            currentEffect = uiState.status.effect,
            strobeMode = uiState.status.strobeMode,
            fadeSpeed = uiState.status.fadeSpeed,
            fadePalette = uiState.status.fadePalette,
            smoothSpeed = uiState.status.smoothSpeed,
            smoothBreath = uiState.status.smoothBreath,
            smoothPalette = uiState.status.smoothPalette,
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

/**
 * Isolated host for dialogs: only opens modal overlays when dialog booleans change.
 */
@Composable
private fun PomodoroModalsHost(pomodoroViewModel: PomodoroViewModel) {
    val showIpDialog by remember(pomodoroViewModel) {
        pomodoroViewModel.uiState.map { it.showIpDialog }.distinctUntilChanged()
    }.collectAsState(initial = false)

    val showCalibrationDialog by remember(pomodoroViewModel) {
        pomodoroViewModel.uiState.map { it.showCalibrationDialog }.distinctUntilChanged()
    }.collectAsState(initial = false)

    val showAboutDialog by remember(pomodoroViewModel) {
        pomodoroViewModel.uiState.map { it.showAboutDialog }.distinctUntilChanged()
    }.collectAsState(initial = false)

    val hostAddress by remember(pomodoroViewModel) {
        pomodoroViewModel.uiState.map { it.hostAddress }.distinctUntilChanged()
    }.collectAsState(initial = "192.168.4.1")

    val isConnected by remember(pomodoroViewModel) {
        pomodoroViewModel.uiState.map { it.isConnected }.distinctUntilChanged()
    }.collectAsState(initial = false)

    if (showIpDialog) {
        IpSettingsDialog(
            currentHost = hostAddress,
            onDismiss = { pomodoroViewModel.setShowIpDialog(false) },
            onSaveHost = { pomodoroViewModel.setHostAddress(it) },
            onTestConnection = { pomodoroViewModel.testConnection(it) }
        )
    }

    if (showCalibrationDialog) {
        CalibrationDialog(
            onDismiss = { pomodoroViewModel.setShowCalibrationDialog(false) },
            onCalibrate = { strip, action -> pomodoroViewModel.calibrateStrip(strip, action) }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            hostAddress = hostAddress,
            isConnected = isConnected,
            onDismiss = { pomodoroViewModel.setShowAboutDialog(false) }
        )
    }
}

/**
 * Isolated host for Desk Clock card: encapsulates 200ms snapshot updates
 * so they only recompose this specific card instead of the outer dashboard.
 */
@Composable
private fun ClockCardHost(
    clockViewModel: ClockViewModel,
    pomodoroViewModel: PomodoroViewModel,
    onSwitchToPomodoro: () -> Unit
) {
    val clockUiState by clockViewModel.uiState.collectAsState()
    val pomodoroStatus by remember(pomodoroViewModel) {
        pomodoroViewModel.uiState.map { it.status }.distinctUntilChanged()
    }.collectAsState(initial = PomodoroStatus())
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
        onToggleShowDate = { clockViewModel.toggleShowDate() },
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
    pomodoroViewModel: PomodoroViewModel,
    onSwitchToStopwatch: () -> Unit,
    onSwitchToPomodoro: () -> Unit
) {
    val clockUiState by clockViewModel.uiState.collectAsState()
    val pomodoroStatus by remember(pomodoroViewModel) {
        pomodoroViewModel.uiState.map { it.status }.distinctUntilChanged()
    }.collectAsState(initial = PomodoroStatus())

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
        onToggleShowDate = { clockViewModel.toggleShowDate() },
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
