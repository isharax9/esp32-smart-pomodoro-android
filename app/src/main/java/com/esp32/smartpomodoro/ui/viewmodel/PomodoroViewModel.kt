package com.esp32.smartpomodoro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.esp32.smartpomodoro.data.model.PomodoroState
import com.esp32.smartpomodoro.data.model.PomodoroStatus
import com.esp32.smartpomodoro.data.model.StripTarget
import com.esp32.smartpomodoro.data.repository.Esp32Repository
import com.esp32.smartpomodoro.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PomodoroUiState(
    val status: PomodoroStatus = PomodoroStatus(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val selectedStrip: StripTarget = StripTarget.ALL,
    val hostAddress: String = "192.168.4.1",
    val showIpDialog: Boolean = false,
    val showCalibrationDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
    val localBrightness: Float = 255f
)

class PomodoroViewModel(
    private val repository: Esp32Repository,
    private val notificationHelper: NotificationHelper? = null,
    private val externalScope: CoroutineScope? = null,
    autoStartPolling: Boolean = true
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var lastObservedState: PomodoroState = PomodoroState.IDLE
    private var isAppVisible: Boolean = true
    private var activeTab: String = "FOCUS"
    private var consecutiveFailures: Int = 0

    // Standalone Pomodoro Engine state (operates independently of ESP32 and sensors)
    private var localTimerActive: Boolean = false
    private var localPhase: PomodoroState = PomodoroState.IDLE
    private var localRemainingSec: Long = 1500L
    private var localTotalSec: Long = 1500L
    private var localIsPaused: Boolean = false
    private var localCyclesCompleted: Long = 0L
    private var pomodoroTimerJob: Job? = null

    private var lastActionTimestamp: Long = 0L

    init {
        if (autoStartPolling) {
            startPolling()
        }
    }

    private fun updateLocalStatus() {
        _uiState.update { current ->
            current.copy(
                status = current.status.copy(
                    state = localPhase.name,
                    remainingSec = localRemainingSec,
                    totalSec = localTotalSec,
                    isPaused = localIsPaused,
                    cyclesCompleted = localCyclesCompleted
                )
            )
        }
    }

    private fun startLocalCountdown() {
        pomodoroTimerJob?.cancel()
        pomodoroTimerJob = scope.launch {
            while (isActive && localTimerActive) {
                delay(1000L)
                if (!localIsPaused && localPhase != PomodoroState.IDLE) {
                    if (localRemainingSec > 0) {
                        localRemainingSec--
                        updateLocalStatus()
                    }
                    if (localRemainingSec == 0L) {
                        // Phase complete
                        if (localPhase == PomodoroState.WORK) {
                            localCyclesCompleted++
                            localPhase = PomodoroState.BREAK
                            localTotalSec = 300L
                            localRemainingSec = 300L
                            updateLocalStatus()
                            notificationHelper?.sendPhaseAlert(
                                "Focus Session Complete! ☕",
                                "Great job! Time for a refreshing break."
                            )
                            launch {
                                try { repository.sendPomodoroAction("start") } catch (_: Exception) {}
                            }
                        } else if (localPhase == PomodoroState.BREAK) {
                            localPhase = PomodoroState.WORK
                            localTotalSec = 1500L
                            localRemainingSec = 1500L
                            updateLocalStatus()
                            notificationHelper?.sendPhaseAlert(
                                "Break Finished! 🎯",
                                "Ready to dive back in? Focus sprint activated."
                            )
                            launch {
                                try { repository.sendPomodoroAction("start") } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        }
    }

    fun startPolling() {
        if (!isAppVisible) return
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive && isAppVisible) {
                fetchStatus()
                val delayMs = if (_uiState.value.isConnected) {
                    if (activeTab == "FOCUS") 1500L else 4000L
                } else {
                    // Progressive backoff when ESP32 is offline to eliminate battery drain
                    when {
                        consecutiveFailures >= 5 -> 15000L
                        consecutiveFailures >= 3 -> 8000L
                        else -> 4000L
                    }
                }
                delay(delayMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun onAppForegrounded() {
        isAppVisible = true
        consecutiveFailures = 0
        startPolling()
    }

    fun onAppBackgrounded() {
        isAppVisible = false
        stopPolling()
    }

    fun setActiveTab(tab: String) {
        if (activeTab != tab) {
            activeTab = tab
            if (tab == "FOCUS" && isAppVisible) {
                // Instantly poll when returning to focus tab
                consecutiveFailures = 0
                scope.launch { fetchStatus() }
            }
        }
    }

    private suspend fun fetchStatus() {
        val result = repository.fetchStatus()
        result.fold(
            onSuccess = { status ->
                val prev = lastObservedState
                val current = status.parsedState

                // Detect state transitions to trigger sound & vibration notifications
                if (prev == PomodoroState.WORK && current == PomodoroState.BREAK) {
                    notificationHelper?.sendPhaseAlert(
                        "Focus Session Complete! ☕",
                        "Great job! Time for a refreshing break. Desk lights set to recovery green."
                    )
                } else if (prev == PomodoroState.BREAK && current == PomodoroState.WORK) {
                    notificationHelper?.sendPhaseAlert(
                        "Break Finished! 🎯",
                        "Ready to dive back in? Focus mode activated (lights red)."
                    )
                }
                lastObservedState = current

                val timeSinceAction = System.currentTimeMillis() - lastActionTimestamp
                val effectiveStatus = if (localTimerActive) {
                    if (timeSinceAction > 2500L) {
                        // Harmonize auto-pause state from ESP32 presence sensor once user action settles
                        if (status.parsedState == localPhase && status.isPaused != localIsPaused) {
                            localIsPaused = status.isPaused
                        }
                        // Harmonize time drift if ESP32 and phone drift by > 2s
                        if (status.parsedState == localPhase && kotlin.math.abs(localRemainingSec - status.remainingSec) > 2) {
                            localRemainingSec = status.remainingSec
                            localTotalSec = status.totalSec
                        }
                    }
                    status.copy(
                        state = localPhase.name,
                        remainingSec = localRemainingSec,
                        totalSec = localTotalSec,
                        isPaused = localIsPaused,
                        cyclesCompleted = localCyclesCompleted
                    )
                } else {
                    if (status.parsedState != PomodoroState.IDLE && timeSinceAction > 2500L) {
                        localPhase = status.parsedState
                        localRemainingSec = status.remainingSec
                        localTotalSec = status.totalSec
                        localIsPaused = status.isPaused
                        localCyclesCompleted = status.cyclesCompleted
                        localTimerActive = true
                        startLocalCountdown()
                    }
                    status
                }

                consecutiveFailures = 0
                _uiState.update {
                    it.copy(
                        status = effectiveStatus,
                        isConnected = true,
                        isConnecting = false,
                        errorMessage = null,
                        localBrightness = status.brightness.toFloat()
                    )
                }
            },
            onFailure = { error ->
                consecutiveFailures++
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = error.localizedMessage ?: "Connection error"
                    )
                }
            }
        )
    }

    fun triggerPomodoroAction(action: String) {
        lastActionTimestamp = System.currentTimeMillis()
        consecutiveFailures = 0
        when (action.lowercase()) {
            "start" -> {
                localTimerActive = true
                localPhase = PomodoroState.WORK
                localTotalSec = 1500L
                localRemainingSec = 1500L
                localIsPaused = false
                _uiState.update { it.copy(status = it.status.copy(isUserPresent = true)) }
                updateLocalStatus()
                startLocalCountdown()
            }
            "pause" -> {
                localIsPaused = true
                updateLocalStatus()
            }
            "resume" -> {
                localIsPaused = false
                if (localPhase == PomodoroState.IDLE) {
                    localPhase = PomodoroState.WORK
                    localTotalSec = 1500L
                    localRemainingSec = 1500L
                }
                localTimerActive = true
                updateLocalStatus()
                startLocalCountdown()
            }
            "skip" -> {
                if (localPhase == PomodoroState.WORK) {
                    localCyclesCompleted++
                    localPhase = PomodoroState.BREAK
                    localTotalSec = 300L
                    localRemainingSec = 300L
                } else {
                    localPhase = PomodoroState.WORK
                    localTotalSec = 1500L
                    localRemainingSec = 1500L
                }
                localIsPaused = false
                localTimerActive = true
                updateLocalStatus()
                startLocalCountdown()
            }
            "reset" -> {
                localTimerActive = false
                pomodoroTimerJob?.cancel()
                pomodoroTimerJob = null
                localPhase = PomodoroState.IDLE
                localTotalSec = 1500L
                localRemainingSec = 1500L
                localIsPaused = false
                updateLocalStatus()
            }
        }

        // Asynchronously mirror action to ESP32 without blocking local execution
        scope.launch {
            try {
                repository.sendPomodoroAction(action)
            } catch (_: Exception) {}
            fetchStatus()
        }
    }

    fun sendColorCommand(cmdVal: String) {
        scope.launch {
            repository.sendColorCommand(cmdVal, _uiState.value.selectedStrip)
            fetchStatus()
        }
    }

    fun setBrightness(level: Int) {
        _uiState.update { it.copy(localBrightness = level.toFloat()) }
        scope.launch {
            repository.setBrightness(level)
            fetchStatus()
        }
    }

    fun setFadeConfig(speed: Int? = null, palette: Int? = null) {
        scope.launch {
            repository.setFadeConfig(speed, palette, _uiState.value.selectedStrip)
            fetchStatus()
        }
    }

    fun setSmoothConfig(speed: Int? = null, breath: Int? = null, palette: Int? = null) {
        scope.launch {
            repository.setSmoothConfig(speed, breath, palette, _uiState.value.selectedStrip)
            fetchStatus()
        }
    }

    fun setStrobeMode(mode: String) {
        scope.launch {
            repository.setStrobeMode(mode, _uiState.value.selectedStrip)
            fetchStatus()
        }
    }

    fun toggleAutoPause() {
        val currentSetting = _uiState.value.status.autoPauseEnabled
        scope.launch {
            repository.setAutoPause(!currentSetting)
            fetchStatus()
        }
    }

    fun selectStrip(target: StripTarget) {
        _uiState.update { it.copy(selectedStrip = target) }
    }

    fun setHostAddress(address: String) {
        val sanitized = address.trim()
        repository.hostAddress = sanitized
        consecutiveFailures = 0
        _uiState.update {
            it.copy(
                hostAddress = sanitized,
                showIpDialog = false,
                isConnecting = true
            )
        }
        scope.launch {
            fetchStatus()
        }
    }

    fun setShowIpDialog(show: Boolean) {
        _uiState.update { it.copy(showIpDialog = show) }
    }

    fun setShowCalibrationDialog(show: Boolean) {
        _uiState.update { it.copy(showCalibrationDialog = show) }
    }

    fun setShowAboutDialog(show: Boolean) {
        _uiState.update { it.copy(showAboutDialog = show) }
    }

    fun calibrateStrip(strip: String, action: String) {
        scope.launch {
            repository.calibratePins(strip, action)
            fetchStatus()
        }
    }

    suspend fun testConnection(address: String): Boolean {
        return repository.testConnection(address).isSuccess
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        pomodoroTimerJob?.cancel()
    }
}

class PomodoroViewModelFactory(
    private val repository: Esp32Repository,
    private val notificationHelper: NotificationHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PomodoroViewModel(repository, notificationHelper) as T
    }
}
