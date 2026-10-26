package com.esp32.smartpomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.OrientationEventListener
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.esp32.smartpomodoro.data.model.ClockRotationAngle
import com.esp32.smartpomodoro.ui.screens.MainDashboardScreen
import com.esp32.smartpomodoro.ui.theme.SmartPomodoroTheme
import com.esp32.smartpomodoro.ui.viewmodel.ClockViewModel
import com.esp32.smartpomodoro.ui.viewmodel.ClockViewModelFactory
import com.esp32.smartpomodoro.ui.viewmodel.PomodoroViewModel
import com.esp32.smartpomodoro.ui.viewmodel.PomodoroViewModelFactory
import com.esp32.smartpomodoro.ui.viewmodel.StopwatchViewModel
import com.esp32.smartpomodoro.ui.viewmodel.StopwatchViewModelFactory

class MainActivity : ComponentActivity() {

    private var orientationEventListener: OrientationEventListener? = null

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

        // Hardware orientation sensor listener for desk clock Auto-Rotate mode
        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                // Convert sensor degrees (0-359) to clockwise screen rotation angles
                val angle = when (orientation) {
                    in 45..134 -> ClockRotationAngle.ROTATION_270
                    in 135..224 -> ClockRotationAngle.ROTATION_180
                    in 225..314 -> ClockRotationAngle.ROTATION_90
                    else -> ClockRotationAngle.ROTATION_0
                }
                clockViewModel.onSensorOrientationChanged(angle)
            }
        }

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

                val clockUiState by clockViewModel.uiState.collectAsState()

                // Keep screen awake if configured in desk clock
                LaunchedEffect(clockUiState.keepScreenOn) {
                    if (clockUiState.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                // Dynamic orientation listener activation (saves battery when auto-rotate is disabled)
                LaunchedEffect(clockUiState.isAutoRotate) {
                    if (clockUiState.isAutoRotate && orientationEventListener?.canDetectOrientation() == true) {
                        orientationEventListener?.enable()
                    } else {
                        orientationEventListener?.disable()
                    }
                }

                // Fullscreen immersive mode (hides system bars and navigation bar on bedside desk clock)
                LaunchedEffect(clockUiState.isFullScreen) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    if (clockUiState.isFullScreen) {
                        insetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
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

    override fun onDestroy() {
        super.onDestroy()
        orientationEventListener?.disable()
        orientationEventListener = null
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
