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
    // Never emits on time ticks, preventing MainActivity from recomposing when seconds tick.
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
    private var isAppVisible: Boolean = true

    init {
        loadPreferences()
        syncSnapshot()
        if (autoStartTicker && (isClockVisible || engine.isFullScreen)) {
            startTicker()
        }
    }

    private fun loadPreferences() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("clock_preferences", Context.MODE_PRIVATE)
            val fontName = prefs.getString("font_family", ClockFontFamily.SEVEN_SEGMENT.name)
            val themeName = prefs.getString("theme", ClockTheme.GLACIER_OLED.name)
            val formatName = prefs.getString("time_format", ClockTimeFormat.TWELVE_HOUR.name)
            val secondsName = prefs.getString("seconds_display", ClockSecondsDisplay.SMALL.name)
            val scale = prefs.getFloat("font_size_scale", 1.0f)
            val showDate = prefs.getBoolean("show_date", false)
            val blinking = prefs.getBoolean("blinking_colon", true)
            val night = prefs.getBoolean("night_dimming", false)
            val keepOn = prefs.getBoolean("keep_screen_on", true)

            engine.fontFamily = ClockFontFamily.entries.find { it.name == fontName } ?: ClockFontFamily.SEVEN_SEGMENT
            engine.theme = ClockTheme.entries.find { it.name == themeName } ?: ClockTheme.GLACIER_OLED
            engine.timeFormat = ClockTimeFormat.entries.find { it.name == formatName } ?: ClockTimeFormat.TWELVE_HOUR
            engine.secondsDisplay = ClockSecondsDisplay.entries.find { it.name == secondsName } ?: ClockSecondsDisplay.SMALL
            engine.fontSizeScale = scale
            engine.showDate = showDate
            engine.blinkingColon = blinking
            engine.nightDimming = night
            engine.keepScreenOn = keepOn
        } catch (_: Exception) {}
    }

    private fun savePreferences() {
        val ctx = appContext ?: return
        try {
            ctx.getSharedPreferences("clock_preferences", Context.MODE_PRIVATE)
                .edit()
                .putString("font_family", engine.fontFamily.name)
                .putString("theme", engine.theme.name)
                .putString("time_format", engine.timeFormat.name)
                .putString("seconds_display", engine.secondsDisplay.name)
                .putFloat("font_size_scale", engine.fontSizeScale)
                .putBoolean("show_date", engine.showDate)
                .putBoolean("blinking_colon", engine.blinkingColon)
                .putBoolean("night_dimming", engine.nightDimming)
                .putBoolean("keep_screen_on", engine.keepScreenOn)
                .apply()
        } catch (_: Exception) {}
    }

    fun onAppBackgrounded() {
        isAppVisible = false
        stopTicker()
    }

    fun onAppForegrounded() {
        isAppVisible = true
        if (isClockVisible || _uiState.value.isFullScreen) {
            syncSnapshot()
            if (autoStartTicker) {
                startTicker()
            }
        }
    }

    fun setClockVisible(visible: Boolean) {
        if (isClockVisible == visible) return
        isClockVisible = visible
        if ((visible || _uiState.value.isFullScreen) && isAppVisible) {
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
                // Adaptive tick interval saves CPU and battery
                val interval = if (engine.blinkingColon) 250L else 1000L
                delay(interval)
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

    private fun updateStateAndSave() {
        syncSnapshot()
        savePreferences()
    }

    fun cycleTheme() {
        engine.cycleTheme()
        updateStateAndSave()
    }

    fun setTheme(theme: ClockTheme) {
        engine.theme = theme
        updateStateAndSave()
    }

    fun cycleFontFamily() {
        engine.cycleFontFamily()
        updateStateAndSave()
    }

    fun setFontFamily(fontFamily: ClockFontFamily) {
        engine.fontFamily = fontFamily
        updateStateAndSave()
    }

    fun setFontSizeScale(scale: Float) {
        engine.setFontSizeScaleFactor(scale)
        updateStateAndSave()
    }

    fun setFontSizeScalePreset(preset: ClockFontSizeScale) {
        engine.setFontSizeScaleFactor(preset.scaleFactor)
        updateStateAndSave()
    }

    fun cycleFontSizeScale() {
        engine.cycleFontSizeScale()
        updateStateAndSave()
    }

    fun cycleSecondsDisplay() {
        engine.cycleSecondsDisplay()
        updateStateAndSave()
    }

    fun setSecondsDisplay(display: ClockSecondsDisplay) {
        engine.secondsDisplay = display
        updateStateAndSave()
    }

    fun toggleTimeFormat() {
        engine.toggleTimeFormat()
        updateStateAndSave()
    }

    fun setTimeFormat(format: ClockTimeFormat) {
        engine.timeFormat = format
        updateStateAndSave()
    }

    fun toggleShowDate() {
        engine.showDate = !engine.showDate
        updateStateAndSave()
    }

    fun setShowDate(show: Boolean) {
        engine.showDate = show
        updateStateAndSave()
    }

    fun toggleBlinkingColon() {
        engine.blinkingColon = !engine.blinkingColon
        updateStateAndSave()
    }

    fun setBlinkingColon(blink: Boolean) {
        engine.blinkingColon = blink
        updateStateAndSave()
    }

    fun toggleNightDimming() {
        engine.nightDimming = !engine.nightDimming
        updateStateAndSave()
    }

    fun setNightDimming(enabled: Boolean) {
        engine.nightDimming = enabled
        updateStateAndSave()
    }

    fun setBrightnessFactor(factor: Float) {
        engine.brightnessFactor = factor.coerceIn(0.1f, 1.0f)
        syncSnapshot()
    }

    fun toggleKeepScreenOn() {
        engine.keepScreenOn = !engine.keepScreenOn
        updateStateAndSave()
    }

    fun setKeepScreenOn(keep: Boolean) {
        engine.keepScreenOn = keep
        updateStateAndSave()
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
