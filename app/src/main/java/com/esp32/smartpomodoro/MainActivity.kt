package com.esp32.smartpomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.esp32.smartpomodoro.ui.screens.MainDashboardScreen
import com.esp32.smartpomodoro.ui.theme.SmartPomodoroTheme
import com.esp32.smartpomodoro.ui.viewmodel.ClockViewModel
import com.esp32.smartpomodoro.ui.viewmodel.ClockViewModelFactory
import com.esp32.smartpomodoro.ui.viewmodel.PomodoroViewModel
import com.esp32.smartpomodoro.ui.viewmodel.PomodoroViewModelFactory
import com.esp32.smartpomodoro.ui.viewmodel.StopwatchViewModel
import com.esp32.smartpomodoro.ui.viewmodel.StopwatchViewModelFactory

class MainActivity : ComponentActivity() {

    private val pomodoroViewModel: PomodoroViewModel by viewModels {
        val app = application as SmartPomodoroApp
        PomodoroViewModelFactory(app.repository, app.notificationHelper)
    }

    private val stopwatchViewModel: StopwatchViewModel by viewModels {
        val app = application as SmartPomodoroApp
        StopwatchViewModelFactory(app.stopwatchEngine, app.stopwatchFeedbackHelper, applicationContext)
    }

    private val clockViewModel: ClockViewModel by viewModels {
        val app = application as SmartPomodoroApp
        ClockViewModelFactory(app.clockEngine, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartPomodoroTheme {
                // Request POST_NOTIFICATIONS permission for Android 13+ (API 33)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Keep screen awake if configured in desk clock
                val clockUiState by clockViewModel.uiState.collectAsState()
                LaunchedEffect(clockUiState.keepScreenOn) {
                    if (clockUiState.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                MainDashboardScreen(
                    pomodoroViewModel = pomodoroViewModel,
                    stopwatchViewModel = stopwatchViewModel,
                    clockViewModel = clockViewModel
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Intercept volume keys to control stopwatch (Start/Stop, Lap)
        if (stopwatchViewModel.handleKeyEvent(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Consume volume key release so system volume slider does not flash
        if (stopwatchViewModel.handleKeyEvent(keyCode, event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
