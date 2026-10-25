package com.esp32.smartpomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.esp32.smartpomodoro.ui.screens.MainDashboardScreen
import com.esp32.smartpomodoro.ui.theme.SmartPomodoroTheme
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

                MainDashboardScreen(
                    pomodoroViewModel = pomodoroViewModel,
                    stopwatchViewModel = stopwatchViewModel
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
