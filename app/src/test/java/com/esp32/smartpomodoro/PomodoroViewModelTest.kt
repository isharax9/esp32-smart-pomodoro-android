package com.esp32.smartpomodoro

import com.esp32.smartpomodoro.data.model.PomodoroState
import com.esp32.smartpomodoro.data.repository.Esp32Repository
import com.esp32.smartpomodoro.ui.viewmodel.PomodoroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PomodoroViewModelTest {

    private lateinit var viewModel: PomodoroViewModel

    @Before
    fun setUp() {
        val repo = Esp32Repository()
        // Provide external test scope and disable automatic polling during tests
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        viewModel = PomodoroViewModel(
            repository = repo,
            notificationHelper = null,
            externalScope = testScope,
            autoStartPolling = false
        )
    }

    @Test
    fun testInitialState() {
        val status = viewModel.uiState.value.status
        assertEquals(PomodoroState.IDLE, status.parsedState)
        assertEquals(1500L, status.totalSec)
        assertEquals(1500L, status.remainingSec)
        assertFalse(status.isPaused)
        assertEquals(0L, status.cyclesCompleted)
    }

    @Test
    fun testStartActionRunsLocallyWithoutEsp() {
        viewModel.triggerPomodoroAction("start")

        val status = viewModel.uiState.value.status
        assertEquals(PomodoroState.WORK, status.parsedState)
        assertEquals(1500L, status.totalSec)
        assertEquals(1500L, status.remainingSec)
        assertFalse(status.isPaused)
        assertTrue(status.isUserPresent)
    }

    @Test
    fun testPauseAndResumeLocally() {
        viewModel.triggerPomodoroAction("start")
        viewModel.triggerPomodoroAction("pause")

        var status = viewModel.uiState.value.status
        assertEquals(PomodoroState.WORK, status.parsedState)
        assertTrue(status.isPaused)

        viewModel.triggerPomodoroAction("resume")
        status = viewModel.uiState.value.status
        assertEquals(PomodoroState.WORK, status.parsedState)
        assertFalse(status.isPaused)
    }

    @Test
    fun testSkipWorkToBreakAndBack() {
        viewModel.triggerPomodoroAction("start")
        assertEquals(PomodoroState.WORK, viewModel.uiState.value.status.parsedState)

        viewModel.triggerPomodoroAction("skip")
        var status = viewModel.uiState.value.status
        assertEquals(PomodoroState.BREAK, status.parsedState)
        assertEquals(300L, status.remainingSec)
        assertEquals(300L, status.totalSec)
        assertEquals(1L, status.cyclesCompleted)

        viewModel.triggerPomodoroAction("skip")
        status = viewModel.uiState.value.status
        assertEquals(PomodoroState.WORK, status.parsedState)
        assertEquals(1500L, status.remainingSec)
        assertEquals(1500L, status.totalSec)
    }

    @Test
    fun testResetAction() {
        viewModel.triggerPomodoroAction("start")
        assertEquals(PomodoroState.WORK, viewModel.uiState.value.status.parsedState)

        viewModel.triggerPomodoroAction("reset")
        val status = viewModel.uiState.value.status
        assertEquals(PomodoroState.IDLE, status.parsedState)
        assertEquals(1500L, status.remainingSec)
        assertFalse(status.isPaused)
    }

    @Test
    fun testPausePreservesPresence() {
        viewModel.triggerPomodoroAction("start")
        assertTrue(viewModel.uiState.value.status.isUserPresent)

        viewModel.triggerPomodoroAction("pause")
        assertTrue(viewModel.uiState.value.status.isPaused)
        assertTrue(viewModel.uiState.value.status.isUserPresent)
    }
}
