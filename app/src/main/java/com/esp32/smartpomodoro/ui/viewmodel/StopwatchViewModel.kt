package com.esp32.smartpomodoro.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.esp32.smartpomodoro.data.model.*
import com.esp32.smartpomodoro.data.stopwatch.StopwatchEngine
import com.esp32.smartpomodoro.data.stopwatch.StopwatchSignal
import com.esp32.smartpomodoro.notification.StopwatchFeedbackHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StopwatchViewModel(
    val engine: StopwatchEngine = StopwatchEngine(),
    private val feedbackHelper: StopwatchFeedbackHelper? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StopwatchUiState(
            slots = engine.slots,
            displayedTimeMs = engine.getElapsedTimeMs(),
            precision = engine.precision,
            controlScheme = engine.controlScheme
        )
    )
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        syncUiState()
        ensureTickerRunningIfNeeded()
    }

    private fun syncUiState() {
        val elapsed = engine.getElapsedTimeMs()
        val isCountingDown = elapsed < 0 && engine.currentSlot.isRunning
        val secRemaining = if (isCountingDown) ((kotlin.math.abs(elapsed) + 999) / 1000).toInt() else 0

        _uiState.update { current ->
            current.copy(
                currentSlotIndex = engine.currentSlotIndex,
                slots = engine.slots,
                displayedTimeMs = elapsed,
                isCountingDown = isCountingDown,
                countdownRemainingSec = secRemaining,
                precision = engine.precision,
                controlScheme = engine.controlScheme,
                selectedDelaySec = (engine.currentSlot.delayTimeMs / 1000).toInt()
            )
        }
    }

    private fun ensureTickerRunningIfNeeded() {
        val anyRunning = engine.slots.any { it.isRunning }
        if (anyRunning && tickerJob == null) {
            tickerJob = viewModelScope.launch {
                while (isActive) {
                    val signals = engine.checkSignals()
                    dispatchSignals(signals)
                    syncUiState()
                    // Adaptive ticker delay based on precision
                    val tickInterval = when (engine.precision) {
                        StopwatchPrecision.MILLISECONDS -> 16L // ~60fps
                        StopwatchPrecision.HUNDREDTHS -> 25L   // ~40fps
                        StopwatchPrecision.TENTHS -> 50L       // ~20fps
                    }
                    delay(tickInterval)
                }
            }
        } else if (!anyRunning && tickerJob != null) {
            tickerJob?.cancel()
            tickerJob = null
            syncUiState()
        }
    }

    private fun dispatchSignals(signals: List<StopwatchSignal>) {
        if (!_uiState.value.soundEnabled && !_uiState.value.vibrationEnabled) return

        for (signal in signals) {
            when (signal) {
                is StopwatchSignal.CountdownTick -> {
                    if (_uiState.value.soundEnabled) feedbackHelper?.playCountdownTick()
                }
                is StopwatchSignal.CountdownComplete -> {
                    if (_uiState.value.soundEnabled) feedbackHelper?.playStartChime()
                }
                is StopwatchSignal.PeriodicInterval -> {
                    if (_uiState.value.soundEnabled) feedbackHelper?.playPeriodicChime()
                }
                is StopwatchSignal.LapRecorded -> {
                    if (_uiState.value.soundEnabled) feedbackHelper?.playLapChime()
                }
            }
        }
    }

    fun onPrimaryAction() {
        if (_uiState.value.vibrationEnabled) feedbackHelper?.performClickHaptic()
        val signals = engine.onPrimaryButton()
        dispatchSignals(signals)
        syncUiState()
        ensureTickerRunningIfNeeded()
    }

    fun onPrimaryActionLong() {
        if (_uiState.value.vibrationEnabled) feedbackHelper?.performClickHaptic()
        val signals = engine.onPrimaryButtonLong()
        dispatchSignals(signals)
        syncUiState()
        ensureTickerRunningIfNeeded()
    }

    fun onSecondaryAction() {
        if (_uiState.value.vibrationEnabled) feedbackHelper?.performClickHaptic()
        val signals = engine.onSecondaryButton()
        dispatchSignals(signals)
        syncUiState()
        ensureTickerRunningIfNeeded()
    }

    fun selectSlot(index: Int) {
        if (_uiState.value.isTouchLocked) return
        if (_uiState.value.vibrationEnabled) feedbackHelper?.performClickHaptic()
        engine.selectSlot(index)
        syncUiState()
        ensureTickerRunningIfNeeded()
    }

    fun setPreStartDelay(delayMs: Long) {
        if (_uiState.value.isTouchLocked) return
        engine.setPreStartDelay(delayMs)
        syncUiState()
    }

    fun setPrecision(precision: StopwatchPrecision) {
        engine.precision = precision
        syncUiState()
    }

    fun setControlScheme(scheme: StopwatchControlScheme) {
        engine.controlScheme = scheme
        syncUiState()
    }

    fun toggleTouchLock() {
        val newArmed = !_uiState.value.isTouchLockEnabled
        _uiState.update { it.copy(isTouchLockEnabled = newArmed) }
        feedbackHelper?.performLockWarningHaptic()
    }

    fun unlockTouch() {
        _uiState.update { it.copy(isTouchLockEnabled = false, touchWarningVisible = false) }
        feedbackHelper?.performClickHaptic()
    }

    fun onLockTouched() {
        feedbackHelper?.performLockWarningHaptic()
        _uiState.update { it.copy(touchWarningVisible = true) }
        viewModelScope.launch {
            delay(2500L)
            _uiState.update { it.copy(touchWarningVisible = false) }
        }
    }

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleVolumeControl() {
        _uiState.update { it.copy(volumeControlEnabled = !it.volumeControlEnabled) }
        feedbackHelper?.performClickHaptic()
    }

    fun toggleSound() {
        _uiState.update { it.copy(soundEnabled = !it.soundEnabled) }
        feedbackHelper?.performClickHaptic()
    }

    fun setShowPaceDialog(show: Boolean) {
        _uiState.update { it.copy(showPaceDialog = show) }
    }

    fun calculatePaceAndSpeed(distance: Double, unit: String): PaceCalculation? {
        val calc = engine.calculatePaceAndSpeed(distance, unit)
        _uiState.update { it.copy(paceCalculation = calc) }
        return calc
    }

    fun clearLaps() {
        if (_uiState.value.isTouchLocked) return
        engine.clearLaps()
        syncUiState()
    }

    fun resetAllSlots() {
        if (_uiState.value.isTouchLocked) return
        engine.resetAllSlots()
        syncUiState()
        ensureTickerRunningIfNeeded()
    }

    fun copyTimeToClipboard(context: Context = appContext ?: return) {
        val text = engine.formatTimeFull(engine.getElapsedTimeMs())
        copyToClipboard(context, "Stopwatch Time", text)
    }

    fun copyLapsToClipboard(context: Context = appContext ?: return) {
        val text = engine.formatLapsForClipboard()
        copyToClipboard(context, "Stopwatch Laps", text)
    }

    fun copyTextToClipboard(label: String, text: String, context: Context = appContext ?: return) {
        copyToClipboard(context, label, text)
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            feedbackHelper?.performClickHaptic()
        } catch (e: Exception) {
            // Ignore if clipboard fails
        }
    }

    /**
     * Intercepts hardware Volume keys to control stopwatch.
     * Modeled after omegacentauri.mobi.simplestopwatch's volume key support.
     * - KEYCODE_VOLUME_UP -> Start / Stop / Continue
     * - KEYCODE_VOLUME_DOWN -> Lap / Reset / Delay
     * Only intercepts when on the Stopwatch tab or when touch lock is active,
     * allowing normal system volume control during Pomodoro focus sessions.
     */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        val state = _uiState.value
        if (!state.volumeControlEnabled) return false

        val shouldIntercept = (state.activeTab == "STOPWATCH") || (state.isTouchLocked)
        if (!shouldIntercept) return false

        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> onPrimaryAction()
                KeyEvent.KEYCODE_VOLUME_DOWN -> onSecondaryAction()
            }
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            return true
        }

        return false
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        tickerJob = null
    }
}

class StopwatchViewModelFactory(
    private val engine: StopwatchEngine,
    private val feedbackHelper: StopwatchFeedbackHelper,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StopwatchViewModel(engine, feedbackHelper, context) as T
    }
}
