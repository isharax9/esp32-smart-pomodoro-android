package com.esp32.smartpomodoro.data.stopwatch

import com.esp32.smartpomodoro.data.model.*
import java.util.Locale
import kotlin.math.abs

/**
 * Pluggable time source allowing deterministic unit testing without Android SystemClock.
 */
fun interface TimeProvider {
    fun elapsedRealtime(): Long
}

/**
 * Event emitted during stopwatch updates to trigger audio or haptic feedback.
 */
sealed class StopwatchSignal {
    data class CountdownTick(val secondRemaining: Int) : StopwatchSignal()
    data object CountdownComplete : StopwatchSignal()
    data class PeriodicInterval(val intervalSec: Long) : StopwatchSignal()
    data class LapRecorded(val lapNumber: Int) : StopwatchSignal()
}

/**
 * High-performance stopwatch engine reverse-engineered from omegacentauri.mobi.simplestopwatch.
 * Features:
 * - Monotonic elapsed time arithmetic resilient to wall-clock time shifts
 * - Up to 6 simultaneous, fully independent stopwatch profiles
 * - Pre-start delay countdown (-3s, -5s, -10s, custom) with negative time phase
 * - Configurable precision (tenths, hundredths, millis)
 * - Flexible control schemes (Standard, Fast Restart, Start-Stop-Restart)
 * - Lap & split analysis (with fastest/slowest lap detection)
 * - Pace & Speed calculator
 * - Multi-format string outputs for giant UI readouts and clipboard sharing
 */
class StopwatchEngine(
    private val timeProvider: TimeProvider = DefaultTimeProvider
) {
    companion object {
        const val MAX_SLOTS = 6
        const val DEFAULT_PERIODIC_BEEP_INTERVAL_SEC = 0L // 0 = disabled

        val DefaultTimeProvider = TimeProvider {
            try {
                val clazz = Class.forName("android.os.SystemClock")
                val method = clazz.getMethod("elapsedRealtime")
                method.invoke(null) as Long
            } catch (e: Throwable) {
                System.currentTimeMillis()
            }
        }
    }

    private var _slots: MutableList<StopwatchSlot> = (1..MAX_SLOTS).map { StopwatchSlot(id = it) }.toMutableList()
    val slots: List<StopwatchSlot> get() = _slots.toList()

    var currentSlotIndex: Int = 0
        private set

    val currentSlot: StopwatchSlot
        get() = _slots.getOrElse(currentSlotIndex) { _slots[0] }

    var precision: StopwatchPrecision = StopwatchPrecision.HUNDREDTHS
    var controlScheme: StopwatchControlScheme = StopwatchControlScheme.STANDARD
    var periodicBeepIntervalSec: Long = DEFAULT_PERIODIC_BEEP_INTERVAL_SEC

    /**
     * Switch active stopwatch slot (0..5 for slots 1..6).
     */
    fun selectSlot(index: Int) {
        if (index in 0 until MAX_SLOTS) {
            currentSlotIndex = index
        }
    }

    /**
     * Set pre-start delay for current slot (e.g. 0, 3000, 5000, 10000 ms).
     */
    fun setPreStartDelay(delayMs: Long) {
        val slot = currentSlot
        if (!slot.isActive) {
            updateSlot(currentSlotIndex) {
                it.copy(delayTimeMs = delayMs.coerceAtLeast(0L))
            }
        }
    }

    /**
     * Calculates current elapsed time for a given slot.
     * When running with a pre-start delay, this returns a negative number during countdown.
     */
    fun getElapsedTimeMs(slot: StopwatchSlot = currentSlot): Long {
        if (!slot.isActive) {
            return if (slot.delayTimeMs > 0) -slot.delayTimeMs else 0L
        }
        val now = if (slot.isPaused) slot.pauseTimeMs else timeProvider.elapsedRealtime()
        return now - slot.baseTimeMs
    }

    /**
     * Handle primary action (Start / Stop / Continue / Restart depending on state & scheme).
     */
    fun onPrimaryButton(): List<StopwatchSignal> {
        val signals = mutableListOf<StopwatchSignal>()
        val slot = currentSlot

        when (controlScheme) {
            StopwatchControlScheme.RESTART -> {
                // Directly restarts timer from 0
                restartSlot(currentSlotIndex)
            }
            StopwatchControlScheme.STANDARD,
            StopwatchControlScheme.START_STOP_RESTART -> {
                if (!slot.isActive) {
                    // Start new timing session (with pre-start countdown if delay is set)
                    val now = timeProvider.elapsedRealtime()
                    val base = if (slot.delayTimeMs > 0) now + slot.delayTimeMs else now
                    updateSlot(currentSlotIndex) {
                        it.copy(
                            isActive = true,
                            isPaused = false,
                            baseTimeMs = base,
                            pauseTimeMs = 0L,
                            lastLapTimeMs = 0L,
                            lastAnnouncedSec = if (slot.delayTimeMs > 0) -1L else 0L,
                            laps = emptyList()
                        )
                    }
                } else if (slot.isPaused) {
                    // Continue from paused state
                    val now = timeProvider.elapsedRealtime()
                    val pauseDuration = now - slot.pauseTimeMs
                    updateSlot(currentSlotIndex) {
                        it.copy(
                            baseTimeMs = it.baseTimeMs + pauseDuration,
                            isPaused = false,
                            pauseTimeMs = 0L
                        )
                    }
                } else {
                    // Pause/Stop active timer
                    val now = timeProvider.elapsedRealtime()
                    updateSlot(currentSlotIndex) {
                        it.copy(
                            isPaused = true,
                            pauseTimeMs = now
                        )
                    }
                }
            }
        }
        return signals
    }

    /**
     * Handle primary button long-press.
     * Modeled after omegacentauri.mobi.simplestopwatch's firstButtonLong():
     * When using StopwatchControlScheme.RESTART, long-press stops and resets the stopwatch to idle.
     */
    fun onPrimaryButtonLong(): List<StopwatchSignal> {
        if (controlScheme == StopwatchControlScheme.RESTART) {
            val slot = currentSlot
            if (slot.isActive) {
                resetSlot(currentSlotIndex)
            }
        }
        return emptyList()
    }

    /**
     * Handle secondary action (Lap / Reset / Restart depending on state & scheme).
     */
    fun onSecondaryButton(): List<StopwatchSignal> {
        val signals = mutableListOf<StopwatchSignal>()
        val slot = currentSlot

        if (controlScheme == StopwatchControlScheme.RESTART) {
            return emptyList()
        }

        if (slot.isRunning) {
            if (controlScheme == StopwatchControlScheme.START_STOP_RESTART) {
                restartSlot(currentSlotIndex)
            } else {
                // Record a Lap
                val elapsed = getElapsedTimeMs(slot)
                if (elapsed >= 0) {
                    recordLap(currentSlotIndex, elapsed)?.let {
                        signals.add(StopwatchSignal.LapRecorded(it.lapNumber))
                    }
                }
            }
        } else if (slot.isPaused || slot.isActive) {
            // Reset to idle
            resetSlot(currentSlotIndex)
        } else {
            // Cycle delay if idle: 0s -> 3s -> 5s -> 10s -> 0s
            cycleDelay(currentSlotIndex)
        }
        return signals
    }

    /**
     * Records a lap at current elapsed time.
     */
    fun recordLap(slotIndex: Int = currentSlotIndex, currentTimeMs: Long = getElapsedTimeMs()): LapRecord? {
        val slot = _slots.getOrNull(slotIndex) ?: return null
        if (currentTimeMs < 0) return null

        val lapNumber = slot.laps.size + 1
        val delta = if (slot.laps.isEmpty()) currentTimeMs else (currentTimeMs - slot.lastLapTimeMs).coerceAtLeast(0L)

        val newRecord = LapRecord(
            lapNumber = lapNumber,
            splitTimeMs = currentTimeMs,
            deltaTimeMs = delta,
            splitFormatted = formatTimeFull(currentTimeMs, precision),
            deltaFormatted = "+${formatTimeFull(delta, precision)}"
        )

        val updatedLaps = slot.laps + newRecord
        val lapsWithHighlight = recomputeLapHighlights(updatedLaps)

        updateSlot(slotIndex) {
            it.copy(
                laps = lapsWithHighlight,
                lastLapTimeMs = currentTimeMs
            )
        }
        return newRecord
    }

    /**
     * Recompute fastest and slowest lap flags across the lap list.
     */
    private fun recomputeLapHighlights(laps: List<LapRecord>): List<LapRecord> {
        if (laps.size < 2) return laps.map { it.copy(isFastest = false, isSlowest = false) }

        val minDelta = laps.minOf { it.deltaTimeMs }
        val maxDelta = laps.maxOf { it.deltaTimeMs }

        // If all laps have equal delta, none is fastest or slowest
        if (minDelta == maxDelta) {
            return laps.map { it.copy(isFastest = false, isSlowest = false) }
        }

        return laps.map { lap ->
            lap.copy(
                isFastest = lap.deltaTimeMs == minDelta,
                isSlowest = lap.deltaTimeMs == maxDelta
            )
        }
    }

    /**
     * Resets specified slot to fresh zero state.
     */
    fun resetSlot(slotIndex: Int = currentSlotIndex) {
        updateSlot(slotIndex) {
            it.copy(
                isActive = false,
                isPaused = false,
                baseTimeMs = 0L,
                pauseTimeMs = 0L,
                lastLapTimeMs = 0L,
                lastAnnouncedSec = 0L,
                laps = emptyList()
            )
        }
    }

    /**
     * Resets all 6 stopwatch slots.
     */
    fun resetAllSlots() {
        for (i in 0 until MAX_SLOTS) {
            resetSlot(i)
        }
    }

    /**
     * Restarts timer directly from zero.
     */
    fun restartSlot(slotIndex: Int = currentSlotIndex) {
        val now = timeProvider.elapsedRealtime()
        updateSlot(slotIndex) {
            it.copy(
                isActive = true,
                isPaused = false,
                baseTimeMs = now,
                pauseTimeMs = 0L,
                delayTimeMs = 0L,
                lastLapTimeMs = 0L,
                lastAnnouncedSec = 0L,
                laps = emptyList()
            )
        }
    }

    /**
     * Clears recorded laps for current slot without stopping the stopwatch.
     */
    fun clearLaps(slotIndex: Int = currentSlotIndex) {
        updateSlot(slotIndex) {
            it.copy(laps = emptyList(), lastLapTimeMs = 0L)
        }
    }

    private fun cycleDelay(slotIndex: Int) {
        val slot = _slots[slotIndex]
        val nextDelay = when (slot.delayTimeMs) {
            0L -> 3_000L
            3_000L -> 5_000L
            5_000L -> 10_000L
            else -> 0L
        }
        updateSlot(slotIndex) { it.copy(delayTimeMs = nextDelay) }
    }

    /**
     * Check for audio/haptic events across all running stopwatch slots during timer tick
     * (countdown seconds, countdown completion, periodic chime).
     */
    fun checkSignals(): List<StopwatchSignal> {
        val signals = mutableListOf<StopwatchSignal>()

        for (index in _slots.indices) {
            val slot = _slots[index]
            if (!slot.isRunning) continue

            val elapsed = getElapsedTimeMs(slot)

            if (elapsed < 0) {
                // In countdown phase
                val secRemaining = ((-elapsed + 999) / 1000).toInt()
                if (secRemaining.toLong() != slot.lastAnnouncedSec) {
                    updateSlot(index) { it.copy(lastAnnouncedSec = secRemaining.toLong()) }
                    if (secRemaining in 1..3) {
                        signals.add(StopwatchSignal.CountdownTick(secRemaining))
                    }
                }
            } else {
                // Elapsed >= 0
                if (slot.lastAnnouncedSec != 0L && slot.delayTimeMs > 0) {
                    // Just completed countdown!
                    updateSlot(index) { it.copy(lastAnnouncedSec = 0L) }
                    signals.add(StopwatchSignal.CountdownComplete)
                } else if (periodicBeepIntervalSec > 0) {
                    val currentSec = elapsed / 1000
                    if (currentSec > 0 && currentSec % periodicBeepIntervalSec == 0L && currentSec != slot.lastAnnouncedSec) {
                        updateSlot(index) { it.copy(lastAnnouncedSec = currentSec) }
                        signals.add(StopwatchSignal.PeriodicInterval(currentSec))
                    }
                }
            }
        }
        return signals
    }

    private inline fun updateSlot(index: Int, transform: (StopwatchSlot) -> StopwatchSlot) {
        if (index in _slots.indices) {
            _slots[index] = transform(_slots[index])
        }
    }

    // ==========================================
    // PACE & SPEED CALCULATOR
    // ==========================================

    /**
     * Calculates running pace and speed based on elapsed time and user distance.
     * Replicates and enhances simplestopwatch's pace() dialog feature.
     */
    fun calculatePaceAndSpeed(
        distance: Double,
        unit: String = "km",
        timeMs: Long = getElapsedTimeMs()
    ): PaceCalculation? {
        if (distance <= 0.0 || distance.isNaN() || distance.isInfinite() || timeMs <= 0) return null

        val paceMsDouble = timeMs / distance
        if (paceMsDouble > 360_000_000_000.0 || paceMsDouble.isNaN() || paceMsDouble.isInfinite()) return null

        val paceMsPerUnit = paceMsDouble.toLong()
        val paceFormatted = formatTimeShort(paceMsPerUnit)

        val hours = timeMs / 3_600_000.0
        val speed = distance / hours
        if (speed.isNaN() || speed.isInfinite()) return null
        val speedFormatted = String.format(Locale.US, "%.2f %s/h", speed, unit)

        return PaceCalculation(
            distance = distance,
            distanceUnit = unit,
            elapsedTimeMs = timeMs,
            timeFormatted = formatTimeFull(timeMs, StopwatchPrecision.HUNDREDTHS),
            paceFormatted = "$paceFormatted /$unit",
            speedFormatted = speedFormatted
        )
    }

    // ==========================================
    // FORMATTING ENGINE
    // ==========================================

    /**
     * Splits time into main digits (HH:MM:SS or MM:SS) and sub-second fraction (.00).
     */
    fun getFormattedComponents(timeMs: Long, prec: StopwatchPrecision = precision): Pair<String, String> {
        if (timeMs < 0) {
            val totalSeconds = (abs(timeMs) + 999) / 1000
            val sign = "−"
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            val fraction = String.format(Locale.US, ".%0${prec.decimalDigits}d", 0)
            val formattedTime = if (h > 0) {
                String.format(Locale.US, "%s%02d:%02d:%02d", sign, h, m, s)
            } else {
                String.format(Locale.US, "%s%02d:%02d", sign, m, s)
            }
            return Pair(formattedTime, fraction)
        }

        val totalMs = timeMs
        val ms = totalMs % 1000
        val totalSec = totalMs / 1000
        val sec = totalSec % 60
        val totalMin = totalSec / 60
        val min = totalMin % 60
        val hours = totalMin / 60

        val mainPart = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, min, sec)
        } else {
            String.format(Locale.US, "%02d:%02d", min, sec)
        }

        val fracPart = when (prec) {
            StopwatchPrecision.TENTHS -> String.format(Locale.US, ".%01d", (ms / 100) % 10)
            StopwatchPrecision.HUNDREDTHS -> String.format(Locale.US, ".%02d", (ms / 10) % 100)
            StopwatchPrecision.MILLISECONDS -> String.format(Locale.US, ".%03d", ms)
        }

        return Pair(mainPart, fracPart)
    }

    /**
     * Formats full time string (e.g. "01:23:45.67").
     */
    fun formatTimeFull(timeMs: Long, prec: StopwatchPrecision = precision): String {
        val (main, frac) = getFormattedComponents(timeMs, prec)
        return main + frac
    }

    /**
     * Formats short time string without fraction (e.g. "05:23").
     */
    fun formatTimeShort(timeMs: Long): String {
        val totalSec = (timeMs / 1000).coerceAtLeast(0L)
        val sec = totalSec % 60
        val totalMin = totalSec / 60
        val min = totalMin % 60
        val hours = totalMin / 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, min, sec)
        } else {
            String.format(Locale.US, "%02d:%02d", min, sec)
        }
    }

    /**
     * Exports lap data into clean tab-delimited text for clipboard sharing.
     */
    fun formatLapsForClipboard(laps: List<LapRecord> = currentSlot.laps): String {
        if (laps.isEmpty()) return "No laps recorded"
        val sb = StringBuilder()
        sb.appendLine("Lap\tSplit Time\tInterval Time")
        for (lap in laps) {
            sb.appendLine("#${lap.lapNumber}\t${lap.splitFormatted}\t${lap.deltaFormatted}")
        }
        return sb.toString().trimEnd()
    }
}
