package com.esp32.smartpomodoro

import com.esp32.smartpomodoro.data.model.StopwatchControlScheme
import com.esp32.smartpomodoro.data.model.StopwatchPrecision
import com.esp32.smartpomodoro.data.stopwatch.StopwatchEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StopwatchEdgeCasesTest {

    private lateinit var timeProvider: MockTimeProvider
    private lateinit var engine: StopwatchEngine

    @Before
    fun setUp() {
        timeProvider = MockTimeProvider(50_000L)
        engine = StopwatchEngine(timeProvider = timeProvider)
    }

    @Test
    fun testLapRejectionDuringCountdown() {
        // Set 5s countdown
        engine.setPreStartDelay(5_000L)
        engine.onPrimaryButton() // Starts countdown

        // Attempt to record lap while time is negative (-5000ms)
        val lap = engine.recordLap()
        assertNull("Laps should not be recordable during pre-start countdown phase", lap)
        assertEquals(0, engine.currentSlot.laps.size)

        // Advance past countdown into positive timing
        timeProvider.advance(6_000L) // +1000ms
        assertEquals(1_000L, engine.getElapsedTimeMs())

        val validLap = engine.recordLap()
        assertNotNull("Laps should be recordable once time is >= 0", validLap)
        assertEquals(1, engine.currentSlot.laps.size)
        assertEquals(1_000L, validLap!!.splitTimeMs)
    }

    @Test
    fun testClearLapsWhileRunning() {
        engine.onPrimaryButton()
        timeProvider.advance(2_000L)
        engine.recordLap()
        timeProvider.advance(3_000L)
        engine.recordLap()

        assertEquals(2, engine.currentSlot.laps.size)

        // Clear laps while still running
        engine.clearLaps()
        assertEquals(0, engine.currentSlot.laps.size)
        assertTrue("Stopwatch should still be active and running after clearing laps", engine.currentSlot.isRunning)

        // Next lap recorded should be #1 with delta = elapsed time
        timeProvider.advance(1_000L) // elapsed = 6000ms
        val nextLap = engine.recordLap()
        assertNotNull(nextLap)
        assertEquals(1, nextLap!!.lapNumber)
        assertEquals(6_000L, nextLap.splitTimeMs)
        assertEquals(6_000L, nextLap.deltaTimeMs)
    }

    @Test
    fun testResetAllSlotsConcurrently() {
        // Start all 6 slots with different states
        for (i in 0 until 6) {
            engine.selectSlot(i)
            engine.onPrimaryButton()
            timeProvider.advance(1_000L * (i + 1))
            engine.recordLap()
        }

        // Verify all 6 have active laps
        for (i in 0 until 6) {
            assertTrue(engine.slots[i].isActive)
            assertTrue(engine.slots[i].laps.isNotEmpty())
        }

        // Reset all slots
        engine.resetAllSlots()

        for (i in 0 until 6) {
            assertFalse(engine.slots[i].isActive)
            assertFalse(engine.slots[i].isRunning)
            assertFalse(engine.slots[i].isPaused)
            assertEquals(0L, engine.getElapsedTimeMs(engine.slots[i]))
            assertTrue(engine.slots[i].laps.isEmpty())
        }
    }

    @Test
    fun testMarathonPaceCalculations() {
        // Half marathon: 21.0975 km in 1h 45m 30s (6,330,000 ms)
        val halfMarathonTimeMs = 6_330_000L
        val halfCalc = engine.calculatePaceAndSpeed(21.0975, "km", halfMarathonTimeMs)
        assertNotNull(halfCalc)
        assertEquals("05:00 /km", halfCalc!!.paceFormatted)
        assertEquals("12.00 km/h", halfCalc.speedFormatted)

        // Full marathon: 42.195 km in 3h 30m 00s (12,600,000 ms)
        val marathonTimeMs = 12_600_000L
        val fullCalc = engine.calculatePaceAndSpeed(42.195, "km", marathonTimeMs)
        assertNotNull(fullCalc)
        assertEquals("04:58 /km", fullCalc!!.paceFormatted)
        assertEquals("12.06 km/h", fullCalc.speedFormatted)
    }

    @Test
    fun testPrecisionDisplayTransitions() {
        val sampleMs = 73_849L // 1m 13s 849ms

        // Tenths
        val (_, fracTenths) = engine.getFormattedComponents(sampleMs, StopwatchPrecision.TENTHS)
        assertEquals(".8", fracTenths)

        // Hundredths
        val (_, fracHundredths) = engine.getFormattedComponents(sampleMs, StopwatchPrecision.HUNDREDTHS)
        assertEquals(".84", fracHundredths)

        // Milliseconds
        val (_, fracMillis) = engine.getFormattedComponents(sampleMs, StopwatchPrecision.MILLISECONDS)
        assertEquals(".849", fracMillis)
    }

    @Test
    fun testEqualDeltaLapsDoNotHighlightAllAsFastest() {
        engine.onPrimaryButton()

        // Lap 1: 4000ms delta
        timeProvider.advance(4_000L)
        engine.recordLap()

        // Lap 2: 4000ms delta (exact same duration)
        timeProvider.advance(4_000L)
        engine.recordLap()

        val laps = engine.currentSlot.laps
        assertEquals(2, laps.size)
        // Since both laps are equal, neither should be flagged as fastest or slowest
        assertFalse("When laps have equal deltas, neither should be marked fastest", laps[0].isFastest)
        assertFalse("When laps have equal deltas, neither should be marked fastest", laps[1].isFastest)
        assertFalse("When laps have equal deltas, neither should be marked slowest", laps[0].isSlowest)
        assertFalse("When laps have equal deltas, neither should be marked slowest", laps[1].isSlowest)
    }

    @Test
    fun testPaceCalculationNaNAndInfinitySafety() {
        val validTime = 60_000L

        // NaN distance
        assertNull(engine.calculatePaceAndSpeed(Double.NaN, "km", validTime))

        // Infinite distances
        assertNull(engine.calculatePaceAndSpeed(Double.POSITIVE_INFINITY, "km", validTime))
        assertNull(engine.calculatePaceAndSpeed(Double.NEGATIVE_INFINITY, "km", validTime))

        // Negative or zero time
        assertNull(engine.calculatePaceAndSpeed(5.0, "km", 0L))
        assertNull(engine.calculatePaceAndSpeed(5.0, "km", -5000L))

        // Infinitesimal distance that would overflow Long
        assertNull(engine.calculatePaceAndSpeed(1e-18, "km", validTime))
    }
}
