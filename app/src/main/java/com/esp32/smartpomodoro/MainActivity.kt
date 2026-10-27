package com.esp32.smartpomodoro

import android.Manifest
import android.content.pm.ActivityInfo
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
                // Convert physical sensor degrees (0-359) to compensating display rotation angles
                // When tilted clockwise 90° (left side up, 45..134°), graphics must rotate 270° to stay upright.
                // When tilted counter-clockwise 90° (right side up, 225..314°), graphics must rotate 90° to stay upright.
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

                val windowConfig by clockViewModel.windowConfig.collectAsState(
                    initial = com.esp32.smartpomodoro.data.model.ClockWindowConfig(
                        keepScreenOn = clockViewModel.engine.keepScreenOn,
                        isAutoRotate = clockViewModel.engine.isAutoRotate,
                        isFullScreen = clockViewModel.engine.isFullScreen,
                        rotationAngle = clockViewModel.engine.rotationAngle
                    )
                )

                // Keep screen awake if configured in desk clock
                LaunchedEffect(windowConfig.keepScreenOn) {
                    if (windowConfig.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                // Dynamic orientation listener activation (saves battery when auto-rotate is disabled)
                LaunchedEffect(windowConfig.isAutoRotate) {
                    if (windowConfig.isAutoRotate && orientationEventListener?.canDetectOrientation() == true) {
                        orientationEventListener?.enable()
                    } else {
                        orientationEventListener?.disable()
                    }
                }

                // Fullscreen immersive mode (hides system bars and navigation bar on bedside desk clock)
                LaunchedEffect(windowConfig.isFullScreen) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    if (windowConfig.isFullScreen) {
                        insetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                // Dynamic hardware screen orientation:
                // When in Fullscreen Desk Clock mode, rotate the entire physical display window
                // so the user gets 100% true full screen (never clamped or clipped with black bars).
                // When on Dashboard, keep portrait for normal vertical scrolling.
                LaunchedEffect(windowConfig.isFullScreen, windowConfig.isAutoRotate, windowConfig.rotationAngle) {
                    if (windowConfig.isFullScreen) {
                        requestedOrientation = if (windowConfig.isAutoRotate) {
                            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                        } else {
                            when (windowConfig.rotationAngle) {
                                ClockRotationAngle.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                ClockRotationAngle.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                ClockRotationAngle.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                                ClockRotationAngle.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            }
                        }
                    } else {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (clockViewModel.uiState.value.isFullScreen) {
            clockViewModel.setFullScreen(false)
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
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
