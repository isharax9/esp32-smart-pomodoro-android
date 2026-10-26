package com.esp32.smartpomodoro.data.model

import androidx.compose.ui.graphics.Color

/**
 * Multi-orientation rotation angles for the Giant Clock.
 * Modeled after omegacentauri.mobi.simplestopwatch rotation & orientation controls.
 */
enum class ClockRotationAngle(val degrees: Int, val label: String, val isLandscape: Boolean) {
    ROTATION_0(0, "0° Portrait", false),
    ROTATION_90(90, "90° Landscape Right", true),
    ROTATION_180(180, "180° Inverted", false),
    ROTATION_270(270, "270° Landscape Left", true);

    fun next(): ClockRotationAngle = when (this) {
        ROTATION_0 -> ROTATION_90
        ROTATION_90 -> ROTATION_180
        ROTATION_180 -> ROTATION_270
        ROTATION_270 -> ROTATION_0
    }

    fun toggleOrientation(): ClockRotationAngle = if (isLandscape) ROTATION_0 else ROTATION_90
}

/**
 * Seconds display modes supported by the Giant Clock.
 */
enum class ClockSecondsDisplay(val label: String) {
    NONE("Hide Seconds"),
    SMALL("Small Seconds"),
    BIG("Giant Seconds");

    fun next(): ClockSecondsDisplay = when (this) {
        NONE -> SMALL
        SMALL -> BIG
        BIG -> NONE
    }
}

/**
 * 12-hour vs 24-hour time representation.
 */
enum class ClockTimeFormat(val label: String) {
    TWELVE_HOUR("12-Hour (AM/PM)"),
    TWENTY_FOUR_HOUR("24-Hour (Military)");

    fun toggle(): ClockTimeFormat = if (this == TWELVE_HOUR) TWENTY_FOUR_HOUR else TWELVE_HOUR
}

/**
 * Color themes inspired by Giant Stopwatch & bedside digital LED clocks.
 */
enum class ClockTheme(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color
) {
    AMBER_NIGHT(
        title = "Amber Night",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFD97706),
        backgroundColor = Color(0xFF0D0A05)
    ),
    CYAN_ELECTRIC(
        title = "Electric Cyan",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF0891B2),
        backgroundColor = Color(0xFF040F17)
    ),
    EMERALD_LED(
        title = "Retro Emerald",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF059669),
        backgroundColor = Color(0xFF031209)
    ),
    CRIMSON_DARK(
        title = "Crimson Dark",
        primaryColor = Color(0xFFEF4444),
        secondaryColor = Color(0xFFDC2626),
        backgroundColor = Color(0xFF140505)
    ),
    GLACIER_OLED(
        title = "OLED White",
        primaryColor = Color(0xFFF8FAFC),
        secondaryColor = Color(0xFF94A3B8),
        backgroundColor = Color(0xFF000000)
    ),
    PURPLE_NEON(
        title = "Synthwave Purple",
        primaryColor = Color(0xFFA855F7),
        secondaryColor = Color(0xFF9333EA),
        backgroundColor = Color(0xFF10051C)
    ),
    GOLD_WARM(
        title = "Warm Candlelight",
        primaryColor = Color(0xFFFBBF24),
        secondaryColor = Color(0xFFD97706),
        backgroundColor = Color(0xFF140D04)
    );

    fun next(): ClockTheme {
        val entries = entries
        val nextIdx = (ordinal + 1) % entries.size
        return entries[nextIdx]
    }
}

/**
 * Formatted snapshot of current time for digital display.
 */
data class ClockTimeSnapshot(
    val hours: String = "12",
    val minutes: String = "00",
    val seconds: String = "00",
    val colonVisible: Boolean = true,
    val amPm: String = "PM",
    val dateFormatted: String = "",
    val dayOfWeek: String = "",
    val fullTimeFormatted: String = "",
    val timestampMs: Long = 0L
)

/**
 * UI State for the Giant Rotating Desk Clock.
 */
data class ClockUiState(
    val timeFormat: ClockTimeFormat = ClockTimeFormat.TWELVE_HOUR,
    val secondsDisplay: ClockSecondsDisplay = ClockSecondsDisplay.SMALL,
    val showDate: Boolean = true,
    val blinkingColon: Boolean = true,
    val isAutoRotate: Boolean = false,
    val rotationAngle: ClockRotationAngle = ClockRotationAngle.ROTATION_0,
    val theme: ClockTheme = ClockTheme.AMBER_NIGHT,
    val nightDimming: Boolean = false,
    val brightnessFactor: Float = 1.0f, // 0.1f to 1.0f
    val keepScreenOn: Boolean = true,
    val isFullScreen: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val snapshot: ClockTimeSnapshot = ClockTimeSnapshot()
) {
    val effectiveColor: Color
        get() {
            val base = theme.primaryColor
            return if (nightDimming) {
                base.copy(alpha = (0.35f * brightnessFactor).coerceIn(0.15f, 1.0f))
            } else {
                base.copy(alpha = brightnessFactor.coerceIn(0.2f, 1.0f))
            }
        }

    val effectiveSecondaryColor: Color
        get() {
            val base = theme.secondaryColor
            return if (nightDimming) {
                base.copy(alpha = (0.25f * brightnessFactor).coerceIn(0.12f, 0.8f))
            } else {
                base.copy(alpha = (0.75f * brightnessFactor).coerceIn(0.2f, 1.0f))
            }
        }

    val effectiveBackgroundColor: Color
        get() = if (nightDimming || isFullScreen) Color.Black else theme.backgroundColor
}
