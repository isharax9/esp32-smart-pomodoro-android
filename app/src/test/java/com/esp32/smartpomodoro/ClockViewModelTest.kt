package com.esp32.smartpomodoro

import com.esp32.smartpomodoro.data.clock.ClockEngine
import com.esp32.smartpomodoro.data.model.*
import com.esp32.smartpomodoro.ui.viewmodel.ClockViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class ClockViewModelTest {

    private lateinit var timeProvider: MockClockTimeProvider
    private lateinit var engine: ClockEngine
    private lateinit var viewModel: ClockViewModel

    @Before
    fun setUp() {
        timeProvider = MockClockTimeProvider()
        timeProvider.setCalendarTime(2026, Calendar.OCTOBER, 26, 14, 30, 45)
        engine = ClockEngine(timeProvider = timeProvider)
        viewModel = ClockViewModel(engine = engine, autoStartTicker = false)
    }

    @Test
    fun testInitialUiState() {
        val state = viewModel.uiState.value
        assertEquals(ClockTimeFormat.TWELVE_HOUR, state.timeFormat)
        assertEquals(ClockSecondsDisplay.SMALL, state.secondsDisplay)
        assertTrue(state.showDate)
        assertTrue(state.blinkingColon)
        assertTrue(state.isAutoRotate)
        assertEquals(ClockRotationAngle.ROTATION_0, state.rotationAngle)
        assertEquals(ClockTheme.AMBER_NIGHT, state.theme)
        assertFalse(state.nightDimming)
        assertEquals(1.0f, state.brightnessFactor, 0.001f)
        assertTrue(state.keepScreenOn)
        assertFalse(state.isFullScreen)
        assertFalse(state.showSettingsDialog)
        assertEquals("02", state.snapshot.hours)
        assertEquals("30", state.snapshot.minutes)
        assertEquals("45", state.snapshot.seconds)
        assertEquals("PM", state.snapshot.amPm)
    }

    @Test
    fun testRotationControls() {
        viewModel.cycleRotation()
        assertEquals(ClockRotationAngle.ROTATION_90, viewModel.uiState.value.rotationAngle)

        viewModel.setRotationAngle(ClockRotationAngle.ROTATION_270)
        assertEquals(ClockRotationAngle.ROTATION_270, viewModel.uiState.value.rotationAngle)

        viewModel.toggleOrientation() // 270 (landscape) -> 0 (portrait)
        assertEquals(ClockRotationAngle.ROTATION_0, viewModel.uiState.value.rotationAngle)

        viewModel.toggleOrientation() // 0 (portrait) -> 90 (landscape)
        assertEquals(ClockRotationAngle.ROTATION_90, viewModel.uiState.value.rotationAngle)
    }

    @Test
    fun testAutoRotateAndSensorOrientationHandling() {
        assertTrue(viewModel.uiState.value.isAutoRotate)
        viewModel.toggleAutoRotate()
        assertFalse(viewModel.uiState.value.isAutoRotate)
        viewModel.toggleAutoRotate()
        assertTrue(viewModel.uiState.value.isAutoRotate)

        // Sensor update should apply when autoRotate is enabled
        viewModel.onSensorOrientationChanged(ClockRotationAngle.ROTATION_90)
        assertEquals(ClockRotationAngle.ROTATION_90, viewModel.uiState.value.rotationAngle)

        // Selecting a manual angle should lock orientation and disable auto-rotate
        viewModel.setRotationAngle(ClockRotationAngle.ROTATION_180)
        assertFalse(viewModel.uiState.value.isAutoRotate)
        assertEquals(ClockRotationAngle.ROTATION_180, viewModel.uiState.value.rotationAngle)

        // Sensor update should NOT apply when auto-rotate is disabled
        viewModel.onSensorOrientationChanged(ClockRotationAngle.ROTATION_270)
        assertEquals(ClockRotationAngle.ROTATION_180, viewModel.uiState.value.rotationAngle)

        // Re-enabling auto-rotate accepts sensor updates again
        viewModel.setAutoRotate(true)
        assertTrue(viewModel.uiState.value.isAutoRotate)
        viewModel.onSensorOrientationChanged(ClockRotationAngle.ROTATION_270)
        assertEquals(ClockRotationAngle.ROTATION_270, viewModel.uiState.value.rotationAngle)
    }

    @Test
    fun testTimeFormatToggling() {
        viewModel.toggleTimeFormat()
        assertEquals(ClockTimeFormat.TWENTY_FOUR_HOUR, viewModel.uiState.value.timeFormat)
        assertEquals("14", viewModel.uiState.value.snapshot.hours)
        assertEquals("", viewModel.uiState.value.snapshot.amPm)

        viewModel.toggleTimeFormat()
        assertEquals(ClockTimeFormat.TWELVE_HOUR, viewModel.uiState.value.timeFormat)
        assertEquals("02", viewModel.uiState.value.snapshot.hours)
        assertEquals("PM", viewModel.uiState.value.snapshot.amPm)
    }

    @Test
    fun testSecondsDisplayCycling() {
        viewModel.cycleSecondsDisplay() // SMALL -> BIG
        assertEquals(ClockSecondsDisplay.BIG, viewModel.uiState.value.secondsDisplay)

        viewModel.cycleSecondsDisplay() // BIG -> NONE
        assertEquals(ClockSecondsDisplay.NONE, viewModel.uiState.value.secondsDisplay)

        viewModel.setSecondsDisplay(ClockSecondsDisplay.SMALL)
        assertEquals(ClockSecondsDisplay.SMALL, viewModel.uiState.value.secondsDisplay)
    }

    @Test
    fun testNightDimmingAndBrightness() {
        assertFalse(viewModel.uiState.value.nightDimming)
        viewModel.toggleNightDimming()
        assertTrue(viewModel.uiState.value.nightDimming)

        // Brightness clamped to [0.1, 1.0]
        viewModel.setBrightnessFactor(0.05f)
        assertEquals(0.1f, viewModel.uiState.value.brightnessFactor, 0.001f)

        viewModel.setBrightnessFactor(1.5f)
        assertEquals(1.0f, viewModel.uiState.value.brightnessFactor, 0.001f)

        viewModel.setBrightnessFactor(0.5f)
        assertEquals(0.5f, viewModel.uiState.value.brightnessFactor, 0.001f)
    }

    @Test
    fun testEffectiveColorsUnderNightDimming() {
        viewModel.setTheme(ClockTheme.CYAN_ELECTRIC)
        viewModel.setNightDimming(false)
        viewModel.setBrightnessFactor(1.0f)

        val fullBrightnessColor = viewModel.uiState.value.effectiveColor
        assertEquals(1.0f, fullBrightnessColor.alpha, 0.01f)

        viewModel.setNightDimming(true)
        val dimmedColor = viewModel.uiState.value.effectiveColor
        assertTrue("Alpha should be dimmed under night mode", dimmedColor.alpha < 0.5f)
    }

    @Test
    fun testFullScreenAndSettingsToggles() {
        assertFalse(viewModel.uiState.value.isFullScreen)
        viewModel.toggleFullScreen()
        assertTrue(viewModel.uiState.value.isFullScreen)
        viewModel.setFullScreen(false)
        assertFalse(viewModel.uiState.value.isFullScreen)

        assertFalse(viewModel.uiState.value.showSettingsDialog)
        viewModel.setShowSettingsDialog(true)
        assertTrue(viewModel.uiState.value.showSettingsDialog)
        viewModel.setShowSettingsDialog(false)
        assertFalse(viewModel.uiState.value.showSettingsDialog)
    }

    @Test
    fun testKeepScreenOnToggle() {
        assertTrue(viewModel.uiState.value.keepScreenOn)
        viewModel.toggleKeepScreenOn()
        assertFalse(viewModel.uiState.value.keepScreenOn)
        viewModel.setKeepScreenOn(true)
        assertTrue(viewModel.uiState.value.keepScreenOn)
    }

    @Test
    fun testClockLifecycleAndFontSelection() {
        viewModel.onAppBackgrounded()
        viewModel.onAppForegrounded()
        viewModel.setFontFamily(ClockFontFamily.SEVEN_SEGMENT)
        assertEquals(ClockFontFamily.SEVEN_SEGMENT, viewModel.uiState.value.fontFamily)
        viewModel.cycleFontFamily()
        assertEquals(ClockFontFamily.MONOSPACE, viewModel.uiState.value.fontFamily)
    }
}
