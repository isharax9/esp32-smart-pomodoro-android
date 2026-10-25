package com.esp32.smartpomodoro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esp32.smartpomodoro.ui.components.*
import com.esp32.smartpomodoro.ui.theme.BackgroundDark
import com.esp32.smartpomodoro.ui.viewmodel.PomodoroViewModel

@Composable
fun MainDashboardScreen(
    viewModel: PomodoroViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark,
        topBar = {
            HeaderBar(
                isConnected = uiState.isConnected,
                hostAddress = uiState.hostAddress,
                onOpenIpSettings = { viewModel.setShowIpDialog(true) },
                onOpenCalibration = { viewModel.setShowCalibrationDialog(true) }
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
            // 1. Pomodoro Focus Countdown Dial & Controls
            PomodoroTimerCard(
                status = uiState.status,
                onStart = { viewModel.triggerPomodoroAction("start") },
                onPause = { viewModel.triggerPomodoroAction("pause") },
                onResume = { viewModel.triggerPomodoroAction("resume") },
                onSkip = { viewModel.triggerPomodoroAction("skip") },
                onReset = { viewModel.triggerPomodoroAction("reset") }
            )

            // 2. Presence & Environmental Telemetry
            TelemetryGrid(
                status = uiState.status,
                onToggleAutoPause = { viewModel.toggleAutoPause() }
            )

            // 3. 24-Key RGB Light Strip Controller
            RgbRemoteCard(
                selectedStrip = uiState.selectedStrip,
                brightness = uiState.localBrightness,
                currentEffect = uiState.status.effect,
                onSelectStrip = { viewModel.selectStrip(it) },
                onBrightnessChange = { viewModel.setBrightness(it) },
                onSendCommand = { viewModel.sendColorCommand(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // IP Connection Configuration Modal
        if (uiState.showIpDialog) {
            IpSettingsDialog(
                currentHost = uiState.hostAddress,
                onDismiss = { viewModel.setShowIpDialog(false) },
                onSaveHost = { viewModel.setHostAddress(it) },
                onTestConnection = { viewModel.testConnection(it) }
            )
        }

        // Pin Swap Calibration Modal
        if (uiState.showCalibrationDialog) {
            CalibrationDialog(
                onDismiss = { viewModel.setShowCalibrationDialog(false) },
                onCalibrate = { strip, action -> viewModel.calibrateStrip(strip, action) }
            )
        }
    }
}
