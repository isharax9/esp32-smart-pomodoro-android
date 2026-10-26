package com.esp32.smartpomodoro

import com.esp32.smartpomodoro.data.clock.ClockEngine
import com.esp32.smartpomodoro.data.clock.ClockTimeProvider
import com.esp32.smartpomodoro.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MockClockTimeProvider(
    var mockTime: Long = 0L,
    var mockTimeZone: TimeZone = TimeZone.getTimeZone("UTC"),
    var mockLocale: Locale = Locale.US
) : ClockTimeProvider {
    override fun currentTimeMillis(): Long = mockTime
    override fun timeZone(): TimeZone = mockTimeZone
    override fun locale(): Locale = mockLocale

    fun setCalendarTime(year: Int, monthZeroBased: Int, day: Int, hour: Int, minute: Int, second: Int, millis: Int = 0) {
        val cal = Calendar.getInstance(mockTimeZone, mockLocale).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, millis)
        }
        mockTime = cal.timeInMillis
    }
}

class ClockEngineTest {

    private lateinit var timeProvider: MockClockTimeProvider
    private lateinit var engine: ClockEngine

    @Before
    fun setUp() {
        timeProvider = MockClockTimeProvider()
        engine = ClockEngine(timeProvider = timeProvider)
    }

    @Test
    fun testTwelveHourMidnight() {
        // 2026-10-26 00:00:00 UTC
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 0, 0, 0)
        engine.timeFormat = ClockTimeFormat.TWELVE_HOUR

        val snapshot = engine.createSnapshot()
        assertEquals("12", snapshot.hours)
        assertEquals("00", snapshot.minutes)
        assertEquals("00", snapshot.seconds)
        assertEquals("AM", snapshot.amPm)
        assertEquals("12:00:00 AM", snapshot.fullTimeFormatted)
    }

    @Test
    fun testTwelveHourNoon() {
        // 2026-10-26 12:00:00 UTC
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 12, 0, 0)
        engine.timeFormat = ClockTimeFormat.TWELVE_HOUR

        val snapshot = engine.createSnapshot()
        assertEquals("12", snapshot.hours)
        assertEquals("00", snapshot.minutes)
        assertEquals("00", snapshot.seconds)
        assertEquals("PM", snapshot.amPm)
        assertEquals("12:00:00 PM", snapshot.fullTimeFormatted)
    }

    @Test
    fun testTwelveHourAfternoonAndEvening() {
        // 2026-10-26 13:45:30 UTC
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 13, 45, 30)
        engine.timeFormat = ClockTimeFormat.TWELVE_HOUR

        val snapshot = engine.createSnapshot()
        assertEquals("01", snapshot.hours)
        assertEquals("45", snapshot.minutes)
        assertEquals("30", snapshot.seconds)
        assertEquals("PM", snapshot.amPm)

        // 2026-10-26 23:59:59 UTC
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 23, 59, 59)
        val snapshotNight = engine.createSnapshot()
        assertEquals("11", snapshotNight.hours)
        assertEquals("59", snapshotNight.minutes)
        assertEquals("59", snapshotNight.seconds)
        assertEquals("PM", snapshotNight.amPm)
    }

    @Test
    fun testTwentyFourHourFormat() {
        engine.timeFormat = ClockTimeFormat.TWENTY_FOUR_HOUR

        // Midnight
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 0, 0, 0)
        var snapshot = engine.createSnapshot()
        assertEquals("00", snapshot.hours)
        assertEquals("00", snapshot.minutes)
        assertEquals("", snapshot.amPm)
        assertEquals("00:00:00", snapshot.fullTimeFormatted)

        // 13:15:20
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 13, 15, 20)
        snapshot = engine.createSnapshot()
        assertEquals("13", snapshot.hours)
        assertEquals("15", snapshot.minutes)
        assertEquals("20", snapshot.seconds)
        assertEquals("", snapshot.amPm)
        assertEquals("13:15:20", snapshot.fullTimeFormatted)

        // 23:59:59
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 23, 59, 59)
        snapshot = engine.createSnapshot()
        assertEquals("23", snapshot.hours)
        assertEquals("59", snapshot.minutes)
        assertEquals("59", snapshot.seconds)
        assertEquals("", snapshot.amPm)
        assertEquals("23:59:59", snapshot.fullTimeFormatted)
    }

    @Test
    fun testBlinkingColon() {
        engine.blinkingColon = true

        // First 500ms of second -> visible
        timeProvider.mockTime = 1000L
        assertTrue(engine.createSnapshot().colonVisible)

        timeProvider.mockTime = 1499L
        assertTrue(engine.createSnapshot().colonVisible)

        // Second 500ms of second -> hidden
        timeProvider.mockTime = 1500L
        assertFalse(engine.createSnapshot().colonVisible)

        timeProvider.mockTime = 1999L
        assertFalse(engine.createSnapshot().colonVisible)

        // Non-blinking colon -> always visible
        engine.blinkingColon = false
        timeProvider.mockTime = 1500L
        assertTrue(engine.createSnapshot().colonVisible)
        timeProvider.mockTime = 1800L
        assertTrue(engine.createSnapshot().colonVisible)
    }

    @Test
    fun testDateFormatting() {
        // 2026-10-26 (Monday) 10:00:00 UTC
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 10, 0, 0)
        val snapshot = engine.createSnapshot()

        assertEquals("Mon, Oct 26, 2026", snapshot.dateFormatted)
        assertEquals("Monday", snapshot.dayOfWeek)
    }

    @Test
    fun testCycleRotation() {
        assertEquals(ClockRotationAngle.ROTATION_0, engine.rotationAngle)
        assertFalse(engine.rotationAngle.isLandscape)

        assertEquals(ClockRotationAngle.ROTATION_90, engine.cycleRotation())
        assertTrue(engine.rotationAngle.isLandscape)

        assertEquals(ClockRotationAngle.ROTATION_180, engine.cycleRotation())
        assertFalse(engine.rotationAngle.isLandscape)

        assertEquals(ClockRotationAngle.ROTATION_270, engine.cycleRotation())
        assertTrue(engine.rotationAngle.isLandscape)

        assertEquals(ClockRotationAngle.ROTATION_0, engine.cycleRotation())
        assertFalse(engine.rotationAngle.isLandscape)
    }

    @Test
    fun testToggleOrientation() {
        // 0 -> 90
        engine.rotationAngle = ClockRotationAngle.ROTATION_0
        assertEquals(ClockRotationAngle.ROTATION_90, engine.toggleOrientation())

        // 90 -> 0
        assertEquals(ClockRotationAngle.ROTATION_0, engine.toggleOrientation())

        // 180 -> 90
        engine.rotationAngle = ClockRotationAngle.ROTATION_180
        assertEquals(ClockRotationAngle.ROTATION_90, engine.toggleOrientation())

        // 270 -> 0
        engine.rotationAngle = ClockRotationAngle.ROTATION_270
        assertEquals(ClockRotationAngle.ROTATION_0, engine.toggleOrientation())
    }

    @Test
    fun testCycleSecondsDisplay() {
        engine.secondsDisplay = ClockSecondsDisplay.NONE
        assertEquals(ClockSecondsDisplay.SMALL, engine.cycleSecondsDisplay())
        assertEquals(ClockSecondsDisplay.BIG, engine.cycleSecondsDisplay())
        assertEquals(ClockSecondsDisplay.NONE, engine.cycleSecondsDisplay())
    }

    @Test
    fun testCycleThemes() {
        val initial = engine.theme
        val allThemes = ClockTheme.entries
        var current = initial
        for (i in 1..allThemes.size) {
            current = engine.cycleTheme()
        }
        assertEquals(initial, current)
    }

    @Test
    fun testToggleTimeFormat() {
        engine.timeFormat = ClockTimeFormat.TWELVE_HOUR
        assertEquals(ClockTimeFormat.TWENTY_FOUR_HOUR, engine.toggleTimeFormat())
        assertEquals(ClockTimeFormat.TWELVE_HOUR, engine.toggleTimeFormat())
    }
}
