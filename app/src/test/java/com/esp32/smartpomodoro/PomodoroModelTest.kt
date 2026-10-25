package com.esp32.smartpomodoro

import com.esp32.smartpomodoro.data.model.PomodoroState
import com.esp32.smartpomodoro.data.model.PomodoroStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroModelTest {
    @Test
    fun testParsedState() {
        val workStatus = PomodoroStatus(state = "WORK")
        assertEquals(PomodoroState.WORK, workStatus.parsedState)

        val breakStatus = PomodoroStatus(state = "BREAK")
        assertEquals(PomodoroState.BREAK, breakStatus.parsedState)

        val idleStatus = PomodoroStatus(state = "IDLE")
        assertEquals(PomodoroState.IDLE, idleStatus.parsedState)
    }

    @Test
    fun testProgressFraction() {
        val halfWay = PomodoroStatus(totalSec = 1000, remainingSec = 500)
        assertEquals(0.5f, halfWay.progressFraction, 0.01f)
    }
}
