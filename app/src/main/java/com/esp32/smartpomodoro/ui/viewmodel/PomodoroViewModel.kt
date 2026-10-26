package com.esp32.smartpomodoro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.esp32.smartpomodoro.data.model.PomodoroState
import com.esp32.smartpomodoro.data.model.PomodoroStatus
import com.esp32.smartpomodoro.data.model.StripTarget
import com.esp32.smartpomodoro.data.repository.Esp32Repository
import com.esp32.smartpomodoro.notification.NotificationHelper
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
    val localBrightness: Float = 255f
)

class PomodoroViewModel(
    private val repository: Esp32Repository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var lastObservedState: PomodoroState = PomodoroState.IDLE

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchStatus()
                val delayMs = if (_uiState.value.isConnected) 1200L else 3000L
                delay(delayMs)
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
                    notificationHelper.sendPhaseAlert(
                        "Focus Session Complete! ☕",
                        "Great job! Time for a refreshing break. Desk lights set to recovery green."
                    )
                } else if (prev == PomodoroState.BREAK && current == PomodoroState.WORK) {
                    notificationHelper.sendPhaseAlert(
                        "Break Finished! 🎯",
                        "Ready to dive back in? Focus mode activated (lights red)."
                    )
                }
                lastObservedState = current

                _uiState.update {
                    it.copy(
                        status = status,
                        isConnected = true,
                        isConnecting = false,
                        errorMessage = null,
                        localBrightness = status.brightness.toFloat()
                    )
                }
            },
            onFailure = { error ->
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
        viewModelScope.launch {
            repository.sendPomodoroAction(action)
            fetchStatus()
        }
    }

    fun sendColorCommand(cmdVal: String) {
        viewModelScope.launch {
            repository.sendColorCommand(cmdVal, _uiState.value.selectedStrip)
            fetchStatus()
        }
    }

    fun setBrightness(level: Int) {
        _uiState.update { it.copy(localBrightness = level.toFloat()) }
        viewModelScope.launch {
            repository.setBrightness(level)
            fetchStatus()
        }
    }

    fun toggleAutoPause() {
        val currentSetting = _uiState.value.status.autoPauseEnabled
        viewModelScope.launch {
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
        _uiState.update {
            it.copy(
                hostAddress = sanitized,
                showIpDialog = false,
                isConnecting = true
            )
        }
        viewModelScope.launch {
            fetchStatus()
        }
    }

    fun setShowIpDialog(show: Boolean) {
        _uiState.update { it.copy(showIpDialog = show) }
    }

    fun setShowCalibrationDialog(show: Boolean) {
        _uiState.update { it.copy(showCalibrationDialog = show) }
    }

    fun calibrateStrip(strip: String, action: String) {
        viewModelScope.launch {
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
