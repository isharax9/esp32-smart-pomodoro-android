package com.esp32.smartpomodoro.data.model

/**
 * Represents a single lap or split captured during timing.
 * Modeled after omegacentauri.mobi.simplestopwatch's lap recording mechanics.
 */
data class LapRecord(
    val lapNumber: Int,
    val splitTimeMs: Long,
    val deltaTimeMs: Long,
    val splitFormatted: String,
    val deltaFormatted: String,
    val isFastest: Boolean = false,
    val isSlowest: Boolean = false
)

/**
 * Configurable display and ticker precision.
 * - TENTHS: 100ms update resolution (.0)
 * - HUNDREDTHS: 10ms update resolution (.00)
 * - MILLISECONDS: 1ms update resolution (.000)
 */
enum class StopwatchPrecision(val intervalMs: Long, val decimalDigits: Int, val label: String) {
    TENTHS(100L, 1, "0.1s (Tenths)"),
    HUNDREDTHS(10L, 2, "0.01s (Hundredths)"),
    MILLISECONDS(1L, 3, "0.001s (Millis)")
}

/**
 * Control interaction schemes supported by omegacentauri.mobi.simplestopwatch:
 * - STANDARD: Primary Start/Stop, Secondary Lap/Reset (with Continue when paused)
 * - RESTART: Primary Start/Restart directly without pausing
 * - START_STOP_RESTART: Primary Start/Stop, Secondary Restart/Reset
 */
enum class StopwatchControlScheme(val label: String) {
    STANDARD("Standard (Start / Stop / Lap / Reset)"),
    RESTART("Fast Restart (Start / Restart)"),
    START_STOP_RESTART("Start / Stop / Restart")
}

/**
 * Represents one of up to 6 independent stopwatch instances.
 */
data class StopwatchSlot(
    val id: Int, // 1 through 6
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val baseTimeMs: Long = 0L,
    val pauseTimeMs: Long = 0L,
    val delayTimeMs: Long = 0L, // pre-countdown delay in milliseconds
    val lastLapTimeMs: Long = 0L,
    val lastAnnouncedSec: Long = 0L,
    val laps: List<LapRecord> = emptyList()
) {
    val hasLaps: Boolean get() = laps.isNotEmpty()
    val isRunning: Boolean get() = isActive && !isPaused
}

/**
 * Calculation results from the Pace & Speed calculator.
 */
data class PaceCalculation(
    val distance: Double,
    val distanceUnit: String, // "km" or "mi"
    val elapsedTimeMs: Long,
    val timeFormatted: String,
    val paceFormatted: String,  // e.g. "05:23 / km"
    val speedFormatted: String  // e.g. "11.15 km/h"
)

/**
 * Overall UI State for the Stopwatch feature.
 */
data class StopwatchUiState(
    val currentSlotIndex: Int = 0, // 0..5 corresponding to slots 1..6
    val slots: List<StopwatchSlot> = (1..6).map { StopwatchSlot(id = it) },
    val displayedTimeMs: Long = 0L,
    val isCountingDown: Boolean = false,
    val countdownRemainingSec: Int = 0,
    val precision: StopwatchPrecision = StopwatchPrecision.HUNDREDTHS,
    val controlScheme: StopwatchControlScheme = StopwatchControlScheme.STANDARD,
    val selectedDelaySec: Int = 0, // 0, 3, 5, 10, or custom
    val isTouchLockEnabled: Boolean = false,
    val touchWarningVisible: Boolean = false,
    val showPaceDialog: Boolean = false,
    val paceCalculation: PaceCalculation? = null,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val volumeControlEnabled: Boolean = true,
    val activeTab: String = "POMODORO"
) {
    val currentSlot: StopwatchSlot get() = slots.getOrElse(currentSlotIndex) { slots[0] }
    val isRunning: Boolean get() = currentSlot.isRunning
    val isPaused: Boolean get() = currentSlot.isPaused
    val isActive: Boolean get() = currentSlot.isActive
    val hasLaps: Boolean get() = currentSlot.hasLaps
    val currentLaps: List<LapRecord> get() = currentSlot.laps
    // Touch is locked when lock mode is armed AND the stopwatch is actively running (matches simplestopwatch noTouch())
    val isTouchLocked: Boolean get() = isTouchLockEnabled && isRunning
}
