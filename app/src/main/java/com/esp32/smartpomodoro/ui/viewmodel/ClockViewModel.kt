package com.esp32.smartpomodoro.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.esp32.smartpomodoro.data.clock.ClockEngine
import com.esp32.smartpomodoro.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ClockViewModel(
    val engine: ClockEngine = ClockEngine(),
    private val appContext: Context? = null,
    private val externalScope: CoroutineScope? = null,
    private val autoStartTicker: Boolean = true
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        ClockUiState(
            timeFormat = engine.timeFormat,
            secondsDisplay = engine.secondsDisplay,
            showDate = engine.showDate,
            blinkingColon = engine.blinkingColon,
            isAutoRotate = engine.isAutoRotate,
            rotationAngle = engine.rotationAngle,
            theme = engine.theme,
            fontFamily = engine.fontFamily,
            fontSizeScale = engine.fontSizeScale,
            nightDimming = engine.nightDimming,
            brightnessFactor = engine.brightnessFactor,
            keepScreenOn = engine.keepScreenOn,
            isFullScreen = engine.isFullScreen,
            snapshot = engine.createSnapshot()
        )
    )
    val uiState: StateFlow<ClockUiState> = _uiState.asStateFlow()

    // Decoupled window & orientation flow: only emits when window attributes change.
    // Never emits on 200ms time ticks, preventing MainActivity from recomposing when seconds tick.
    val windowConfig: kotlinx.coroutines.flow.Flow<ClockWindowConfig> = _uiState
        .map { state ->
            ClockWindowConfig(
                keepScreenOn = state.keepScreenOn,
                isAutoRotate = state.isAutoRotate,
                isFullScreen = state.isFullScreen,
                rotationAngle = state.rotationAngle
            )
        }
        .distinctUntilChanged()

    private var tickerJob: Job? = null
    private var isClockVisible: Boolean = false

    init {
        if (autoStartTicker) {
            startTicker()
        }
    }

    fun setClockVisible(visible: Boolean) {
        if (isClockVisible == visible) return
        isClockVisible = visible
        if (visible || _uiState.value.isFullScreen) {
            syncSnapshot()
            if (autoStartTicker) {
                startTicker()
            }
        } else {
            stopTicker()
        }
    }

    fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                syncSnapshot()
                // 200ms tick interval ensures accurate 500ms colon blink and crisp seconds update
                delay(200L)
            }
        }
    }

    fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun syncSnapshot() {
        val snapshot = engine.createSnapshot()
        _uiState.update { current ->
            current.copy(
                snapshot = snapshot,
                timeFormat = engine.timeFormat,
                secondsDisplay = engine.secondsDisplay,
                showDate = engine.showDate,
                blinkingColon = engine.blinkingColon,
                isAutoRotate = engine.isAutoRotate,
                rotationAngle = engine.rotationAngle,
                theme = engine.theme,
                fontFamily = engine.fontFamily,
                fontSizeScale = engine.fontSizeScale,
                nightDimming = engine.nightDimming,
                brightnessFactor = engine.brightnessFactor,
                keepScreenOn = engine.keepScreenOn,
                isFullScreen = engine.isFullScreen
            )
        }
    }

    fun cycleRotation() {
        engine.cycleRotation()
        syncSnapshot()
    }

    fun toggleOrientation() {
        engine.toggleOrientation()
        syncSnapshot()
    }

    fun toggleAutoRotate() {
        engine.toggleAutoRotate()
        syncSnapshot()
    }

    fun setAutoRotate(enabled: Boolean) {
        engine.isAutoRotate = enabled
        syncSnapshot()
    }

    fun onSensorOrientationChanged(angle: ClockRotationAngle) {
        if (engine.isAutoRotate && engine.rotationAngle != angle) {
            engine.rotationAngle = angle
            syncSnapshot()
        }
    }

    fun setRotationAngle(angle: ClockRotationAngle) {
        engine.isAutoRotate = false
        engine.rotationAngle = angle
        syncSnapshot()
    }

    fun cycleTheme() {
        engine.cycleTheme()
        syncSnapshot()
    }

    fun setTheme(theme: ClockTheme) {
        engine.theme = theme
        syncSnapshot()
    }

    fun cycleFontFamily() {
        engine.cycleFontFamily()
        syncSnapshot()
    }

    fun setFontFamily(fontFamily: ClockFontFamily) {
        engine.fontFamily = fontFamily
        syncSnapshot()
    }

    fun setFontSizeScale(scale: Float) {
        engine.setFontSizeScaleFactor(scale)
        syncSnapshot()
    }

    fun setFontSizeScalePreset(preset: ClockFontSizeScale) {
        engine.setFontSizeScaleFactor(preset.scaleFactor)
        syncSnapshot()
    }

    fun cycleFontSizeScale() {
        engine.cycleFontSizeScale()
        syncSnapshot()
    }

    fun cycleSecondsDisplay() {
        engine.cycleSecondsDisplay()
        syncSnapshot()
    }

    fun setSecondsDisplay(display: ClockSecondsDisplay) {
        engine.secondsDisplay = display
        syncSnapshot()
    }

    fun toggleTimeFormat() {
        engine.toggleTimeFormat()
        syncSnapshot()
    }

    fun setTimeFormat(format: ClockTimeFormat) {
        engine.timeFormat = format
        syncSnapshot()
    }

    fun toggleShowDate() {
        engine.showDate = !engine.showDate
        syncSnapshot()
    }

    fun setShowDate(show: Boolean) {
        engine.showDate = show
        syncSnapshot()
    }

    fun toggleBlinkingColon() {
        engine.blinkingColon = !engine.blinkingColon
        syncSnapshot()
    }

    fun setBlinkingColon(blink: Boolean) {
        engine.blinkingColon = blink
        syncSnapshot()
    }

    fun toggleNightDimming() {
        engine.nightDimming = !engine.nightDimming
        syncSnapshot()
    }

    fun setNightDimming(enabled: Boolean) {
        engine.nightDimming = enabled
        syncSnapshot()
    }

    fun setBrightnessFactor(factor: Float) {
        engine.brightnessFactor = factor.coerceIn(0.1f, 1.0f)
        syncSnapshot()
    }

    fun toggleKeepScreenOn() {
        engine.keepScreenOn = !engine.keepScreenOn
        syncSnapshot()
    }

    fun setKeepScreenOn(keep: Boolean) {
        engine.keepScreenOn = keep
        syncSnapshot()
    }

    fun setFullScreen(fullScreen: Boolean) {
        engine.isFullScreen = fullScreen
        if (fullScreen) {
            syncSnapshot()
            if (autoStartTicker) {
                startTicker()
            }
        } else if (!isClockVisible) {
            stopTicker()
        }
        syncSnapshot()
    }

    fun toggleFullScreen() {
        setFullScreen(!engine.isFullScreen)
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun copyCurrentTimeToClipboard(context: Context? = appContext) {
        val targetContext = context ?: appContext ?: return
        val text = engine.createSnapshot().fullTimeFormatted
        val clipboard = targetContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Clock Time", text)
        clipboard?.setPrimaryClip(clip)
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        tickerJob = null
    }
}

class ClockViewModelFactory(
    private val engine: ClockEngine,
    private val appContext: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClockViewModel::class.java)) {
            return ClockViewModel(engine, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
