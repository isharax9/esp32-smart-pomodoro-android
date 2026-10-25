package com.esp32.smartpomodoro

import com.esp32.smartpomodoro.data.model.StopwatchControlScheme
import com.esp32.smartpomodoro.data.model.StopwatchPrecision
import com.esp32.smartpomodoro.data.stopwatch.StopwatchEngine
import com.esp32.smartpomodoro.data.stopwatch.StopwatchSignal
import com.esp32.smartpomodoro.data.stopwatch.TimeProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Mock time provider allowing deterministic step-by-step millisecond clock advancement.
 */
class MockTimeProvider(var currentTime: Long = 100_000L) : TimeProvider {
    override fun elapsedRealtime(): Long = currentTime

    fun advance(ms: Long) {
        currentTime += ms
    }
}

class StopwatchEngineTest {

    private lateinit var timeProvider: MockTimeProvider
    private lateinit var engine: StopwatchEngine

    @Before
    fun setUp() {
        timeProvider = MockTimeProvider(100_000L)
        engine = StopwatchEngine(timeProvider = timeProvider)
    }

    @Test
    fun testInitialState() {
        assertEquals(6, engine.slots.size)
        assertEquals(0, engine.currentSlotIndex)
        assertEquals(1, engine.currentSlot.id)
        assertFalse(engine.currentSlot.isActive)
        assertFalse(engine.currentSlot.isRunning)
        assertEquals(0L, engine.getElapsedTimeMs())
    }

    @Test
    fun testBasicStartPauseResume() {
        // Start
        engine.onPrimaryButton()
        assertTrue(engine.currentSlot.isActive)
        assertTrue(engine.currentSlot.isRunning)

        // Advance 2,500 ms
        timeProvider.advance(2_500L)
        assertEquals(2_500L, engine.getElapsedTimeMs())

        // Pause
        engine.onPrimaryButton()
        assertTrue(engine.currentSlot.isPaused)
        assertFalse(engine.currentSlot.isRunning)
        assertEquals(2_500L, engine.getElapsedTimeMs())

        // While paused, time passes in wall clock
        timeProvider.advance(5_000L)
        assertEquals(2_500L, engine.getElapsedTimeMs()) // Remains frozen at 2500ms

        // Resume (Continue)
        engine.onPrimaryButton()
        assertTrue(engine.currentSlot.isRunning)
        assertFalse(engine.currentSlot.isPaused)

        // Advance 1,000 ms more
        timeProvider.advance(1_000L)
        assertEquals(3_500L, engine.getElapsedTimeMs())
    }

    @Test
    fun testPreStartDelayCountdown() {
        // Set 3 seconds delay
        engine.setPreStartDelay(3_000L)
        assertEquals(-3_000L, engine.getElapsedTimeMs())

        // Start countdown
        engine.onPrimaryButton()
        assertTrue(engine.currentSlot.isRunning)
        assertEquals(-3_000L, engine.getElapsedTimeMs())

        // Check tick signals
        var signals = engine.checkSignals()
        assertTrue(signals.any { it is StopwatchSignal.CountdownTick && it.secondRemaining == 3 })

        // Advance 1 second -> -2,000 ms
        timeProvider.advance(1_000L)
        assertEquals(-2_000L, engine.getElapsedTimeMs())
        signals = engine.checkSignals()
        assertTrue(signals.any { it is StopwatchSignal.CountdownTick && it.secondRemaining == 2 })

        // Advance 1 second -> -1,000 ms
        timeProvider.advance(1_000L)
        assertEquals(-1_000L, engine.getElapsedTimeMs())
        signals = engine.checkSignals()
        assertTrue(signals.any { it is StopwatchSignal.CountdownTick && it.secondRemaining == 1 })

        // Advance 1 second -> hits 0 ms (Countdown Complete!)
        timeProvider.advance(1_000L)
        assertEquals(0L, engine.getElapsedTimeMs())
        signals = engine.checkSignals()
        assertTrue(signals.any { it is StopwatchSignal.CountdownComplete })

        // Advance into positive running time
        timeProvider.advance(1_250L)
        assertEquals(1_250L, engine.getElapsedTimeMs())
    }

    @Test
    fun testLapRecordingAndHighlighting() {
        engine.onPrimaryButton()

        // Lap 1 at 5,000 ms (delta = 5,000 ms)
        timeProvider.advance(5_000L)
        val lap1 = engine.recordLap()
        assertNotNull(lap1)
        assertEquals(1, lap1!!.lapNumber)
        assertEquals(5_000L, lap1.splitTimeMs)
        assertEquals(5_000L, lap1.deltaTimeMs)

        // Lap 2 at 8,000 ms (delta = 3,000 ms) -> faster lap
        timeProvider.advance(3_000L)
        val lap2 = engine.recordLap()
        assertNotNull(lap2)
        assertEquals(2, lap2!!.lapNumber)
        assertEquals(8_000L, lap2.splitTimeMs)
        assertEquals(3_000L, lap2.deltaTimeMs)

        // Lap 3 at 16,000 ms (delta = 8,000 ms) -> slower lap
        timeProvider.advance(8_000L)
        val lap3 = engine.recordLap()
        assertNotNull(lap3)
        assertEquals(3, lap3!!.lapNumber)
        assertEquals(16_000L, lap3.splitTimeMs)
        assertEquals(8_000L, lap3.deltaTimeMs)

        val laps = engine.currentSlot.laps
        assertEquals(3, laps.size)

        // Verify lap highlights
        val bestLap = laps.first { it.isFastest }
        assertEquals(2, bestLap.lapNumber) // 3000ms was fastest
        assertEquals(3_000L, bestLap.deltaTimeMs)

        val worstLap = laps.first { it.isSlowest }
        assertEquals(3, worstLap.lapNumber) // 8000ms was slowest
        assertEquals(8_000L, worstLap.deltaTimeMs)
    }

    @Test
    fun testMultiSlotIndependence() {
        // Start Slot 1 (index 0)
        engine.selectSlot(0)
        engine.onPrimaryButton()
        timeProvider.advance(4_000L)
        assertEquals(4_000L, engine.getElapsedTimeMs())

        // Switch to Slot 2 (index 1) and start it
        engine.selectSlot(1)
        assertEquals(1, engine.currentSlotIndex)
        assertEquals(0L, engine.getElapsedTimeMs())
        engine.onPrimaryButton()

        // Advance 2,000 ms
        timeProvider.advance(2_000L)
        assertEquals(2_000L, engine.getElapsedTimeMs(engine.slots[1]))
        assertEquals(6_000L, engine.getElapsedTimeMs(engine.slots[0]))

        // Reset Slot 2
        engine.resetSlot(1)
        assertEquals(0L, engine.getElapsedTimeMs(engine.slots[1]))
        assertFalse(engine.slots[1].isActive)

        // Verify Slot 1 is still running at 6,000 ms!
        assertTrue(engine.slots[0].isRunning)
        assertEquals(6_000L, engine.getElapsedTimeMs(engine.slots[0]))
    }

    @Test
    fun testPaceAndSpeedCalculation() {
        // Scenario: 10 km in 3,600,000 ms (1 hour = 3600s)
        val timeMs = 3_600_000L
        val calc = engine.calculatePaceAndSpeed(distance = 10.0, unit = "km", timeMs = timeMs)

        assertNotNull(calc)
        assertEquals("10.00 km/h", calc!!.speedFormatted)
        assertEquals("06:00 /km", calc.paceFormatted)

        // Scenario: 5 km in 1,200,000 ms (20 minutes = 1200s -> 4 min/km, 15 km/h)
        val calc2 = engine.calculatePaceAndSpeed(distance = 5.0, unit = "km", timeMs = 1_200_000L)
        assertNotNull(calc2)
        assertEquals("15.00 km/h", calc2!!.speedFormatted)
        assertEquals("04:00 /km", calc2.paceFormatted)

        // Boundary cases: invalid distance or time
        assertNull(engine.calculatePaceAndSpeed(distance = 0.0, unit = "km", timeMs = timeMs))
        assertNull(engine.calculatePaceAndSpeed(distance = -5.0, unit = "km", timeMs = timeMs))
        assertNull(engine.calculatePaceAndSpeed(distance = 10.0, unit = "km", timeMs = 0L))
    }

    @Test
    fun testFormattingEngine() {
        // Test component breakdown (H:M:S and fraction)
        val timeHundredths = 65_430L // 1 min, 5 sec, 430 ms
        val (main1, frac1) = engine.getFormattedComponents(timeHundredths, StopwatchPrecision.HUNDREDTHS)
        assertEquals("01:05", main1)
        assertEquals(".43", frac1)

        // Test Milliseconds precision
        val (main2, frac2) = engine.getFormattedComponents(timeHundredths, StopwatchPrecision.MILLISECONDS)
        assertEquals("01:05", main2)
        assertEquals(".430", frac2)

        // Test Hours included
        val timeHours = 3_725_000L // 1 hr, 2 min, 5 sec
        val (mainH, _) = engine.getFormattedComponents(timeHours, StopwatchPrecision.HUNDREDTHS)
        assertEquals("01:02:05", mainH)

        // Test Negative time (countdown)
        val (negMain, negFrac) = engine.getFormattedComponents(-3_000L, StopwatchPrecision.HUNDREDTHS)
        assertEquals("−00:03", negMain)
        assertEquals(".00", negFrac)

        // Test Clipboard formatting
        engine.onPrimaryButton()
        timeProvider.advance(2_000L)
        engine.recordLap()
        val clipText = engine.formatLapsForClipboard()
        assertTrue(clipText.contains("Lap\tSplit Time\tInterval Time"))
        assertTrue(clipText.contains("#1\t00:02.00\t+00:02.00"))
    }

    @Test
    fun testControlSchemes() {
        // Test Restart Scheme
        engine.controlScheme = StopwatchControlScheme.RESTART
        engine.onPrimaryButton() // Starts
        assertTrue(engine.currentSlot.isRunning)

        timeProvider.advance(3_000L)
        assertEquals(3_000L, engine.getElapsedTimeMs())

        // Pressing primary button in RESTART scheme immediately resets to 0 and keeps running
        engine.onPrimaryButton()
        assertTrue(engine.currentSlot.isRunning)
        assertEquals(0L, engine.getElapsedTimeMs())

        timeProvider.advance(1_000L)
        assertEquals(1_000L, engine.getElapsedTimeMs())
    }

    @Test
    fun testRestartControlSchemeLongPressStopsAndResets() {
        engine.controlScheme = StopwatchControlScheme.RESTART
        engine.onPrimaryButton() // Starts
        assertTrue(engine.currentSlot.isRunning)
        assertTrue(engine.currentSlot.isActive)

        timeProvider.advance(4_500L)
        assertEquals(4_500L, engine.getElapsedTimeMs())

        // Long-press primary button in RESTART scheme stops and resets timer
        engine.onPrimaryButtonLong()
        assertFalse(engine.currentSlot.isRunning)
        assertFalse(engine.currentSlot.isActive)
        assertEquals(0L, engine.getElapsedTimeMs())
    }

    @Test
    fun testIdleDelayDoesNotShowAsRunningCountdown() {
        // When pre-start delay is set but stopwatch has NOT started
        engine.setPreStartDelay(3_000L)
        assertFalse(engine.currentSlot.isActive)
        assertFalse(engine.currentSlot.isRunning)
        assertEquals(-3_000L, engine.getElapsedTimeMs())

        // Signal check should emit NOTHING while idle
        val signals = engine.checkSignals()
        assertTrue(signals.isEmpty())
    }

    @Test
    fun testMultiSlotConcurrentSignalDispatch() {
        // Slot 0 has 3s delay and is started
        engine.selectSlot(0)
        engine.setPreStartDelay(3_000L)
        engine.onPrimaryButton() // Starts countdown

        // User immediately switches to Slot 1 (which is IDLE)
        engine.selectSlot(1)
        assertEquals(1, engine.currentSlotIndex)
        assertFalse(engine.currentSlot.isRunning)

        // Advance 1s: Slot 0 should emit tick 2 even though Slot 1 is active in UI
        timeProvider.advance(1_000L)
        val signals1 = engine.checkSignals()
        assertTrue(signals1.any { it is StopwatchSignal.CountdownTick && it.secondRemaining == 2 })

        // Advance 2s: Slot 0 completes countdown!
        timeProvider.advance(2_000L)
        val signals2 = engine.checkSignals()
        assertTrue(signals2.any { it is StopwatchSignal.CountdownComplete })
    }

    @Test
    fun testNegativeHoursTimeFormatting() {
        // Countdown greater than 1 hour (-3,665,000 ms = -1h 1m 5s)
        val (negHoursMain, negHoursFrac) = engine.getFormattedComponents(-3_665_000L, StopwatchPrecision.HUNDREDTHS)
        assertEquals("−01:01:05", negHoursMain)
        assertEquals(".00", negHoursFrac)
    }
}
