package com.esp32.smartpomodoro.data.clock

import com.esp32.smartpomodoro.data.model.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Time provider interface allowing deterministic stepping in tests.
 */
interface ClockTimeProvider {
    fun currentTimeMillis(): Long
    fun timeZone(): TimeZone = TimeZone.getDefault()
    fun locale(): Locale = Locale.getDefault()
}

/**
 * Default system time provider.
 */
class SystemClockTimeProvider : ClockTimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/**
 * Reverse-engineered Giant Clock engine.
 * Generates accurate time snapshots, supports orientation rotations, 12h/24h toggle,
 * seconds display modes, blinking colons, night dimming, and themes.
 */
class ClockEngine(
    val timeProvider: ClockTimeProvider = SystemClockTimeProvider()
) {
    var timeFormat: ClockTimeFormat = ClockTimeFormat.TWELVE_HOUR
    var secondsDisplay: ClockSecondsDisplay = ClockSecondsDisplay.SMALL
    var showDate: Boolean = true
    var blinkingColon: Boolean = true
    var isAutoRotate: Boolean = true
    var rotationAngle: ClockRotationAngle = ClockRotationAngle.ROTATION_0
    var theme: ClockTheme = ClockTheme.AMBER_NIGHT
    var fontFamily: ClockFontFamily = ClockFontFamily.MONOSPACE
    var fontSizeScale: Float = 1.0f
    var nightDimming: Boolean = false
    var brightnessFactor: Float = 1.0f
    var keepScreenOn: Boolean = true
    var isFullScreen: Boolean = false

    private var cachedLocale: Locale? = null
    private var cachedTimeZone: TimeZone? = null
    private var cachedDateFormat: SimpleDateFormat? = null
    private var cachedDayFormat: SimpleDateFormat? = null

    @Synchronized
    private fun getDateFormatters(): Pair<SimpleDateFormat, SimpleDateFormat> {
        val currentLocale = timeProvider.locale()
        val currentTimeZone = timeProvider.timeZone()
        if (cachedLocale != currentLocale || cachedTimeZone != currentTimeZone || cachedDateFormat == null) {
            cachedLocale = currentLocale
            cachedTimeZone = currentTimeZone
            cachedDateFormat = SimpleDateFormat("MMM d, yyyy", currentLocale).apply {
                timeZone = currentTimeZone
            }
            cachedDayFormat = SimpleDateFormat("EEEE", currentLocale).apply {
                timeZone = currentTimeZone
            }
        }
        return Pair(cachedDateFormat!!, cachedDayFormat!!)
    }

    fun createSnapshot(nowMs: Long = timeProvider.currentTimeMillis()): ClockTimeSnapshot {
        val calendar = Calendar.getInstance(timeProvider.timeZone(), timeProvider.locale())
        calendar.timeInMillis = nowMs

        val (hoursStr, amPmStr) = when (timeFormat) {
            ClockTimeFormat.TWELVE_HOUR -> {
                val rawHour = calendar.get(Calendar.HOUR)
                val hour = if (rawHour == 0) 12 else rawHour
                val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                Pair(hour.toString().padStart(2, '0'), amPm)
            }
            ClockTimeFormat.TWENTY_FOUR_HOUR -> {
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                Pair(hour.toString().padStart(2, '0'), "")
            }
        }

        val minutes = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        val seconds = calendar.get(Calendar.SECOND).toString().padStart(2, '0')

        // Blinking colon toggles every 500ms (even half of second visible, odd half hidden)
        val colonVisible = if (!blinkingColon) {
            true
        } else {
            (nowMs % 1000L) < 500L
        }

        val (dateFormat, dayOfWeekFormat) = getDateFormatters()
        val dateStr = dateFormat.format(calendar.time)
        val dayStr = dayOfWeekFormat.format(calendar.time)

        val fullTime = buildString {
            append(hoursStr)
            append(":")
            append(minutes)
            append(":")
            append(seconds)
            if (amPmStr.isNotEmpty()) {
                append(" ")
                append(amPmStr)
            }
        }

        return ClockTimeSnapshot(
            hours = hoursStr,
            minutes = minutes,
            seconds = seconds,
            colonVisible = colonVisible,
            amPm = amPmStr,
            dateFormatted = dateStr,
            dayOfWeek = dayStr,
            fullTimeFormatted = fullTime,
            timestampMs = nowMs
        )
    }

    fun cycleRotation(): ClockRotationAngle {
        isAutoRotate = false
        rotationAngle = rotationAngle.next()
        return rotationAngle
    }

    fun toggleOrientation(): ClockRotationAngle {
        isAutoRotate = false
        rotationAngle = rotationAngle.toggleOrientation()
        return rotationAngle
    }

    fun toggleAutoRotate(): Boolean {
        isAutoRotate = !isAutoRotate
        return isAutoRotate
    }

    fun cycleTheme(): ClockTheme {
        theme = theme.next()
        return theme
    }

    fun cycleSecondsDisplay(): ClockSecondsDisplay {
        secondsDisplay = secondsDisplay.next()
        return secondsDisplay
    }

    fun toggleTimeFormat(): ClockTimeFormat {
        timeFormat = timeFormat.toggle()
        return timeFormat
    }

    fun cycleFontFamily(): ClockFontFamily {
        fontFamily = fontFamily.next()
        return fontFamily
    }

    fun setFontSizeScaleFactor(scale: Float): Float {
        fontSizeScale = scale.coerceIn(0.60f, 1.60f)
        return fontSizeScale
    }

    fun cycleFontSizeScale(): Float {
        val presets = ClockFontSizeScale.entries
        val currentFactor = fontSizeScale
        val currentIndex = presets.indexOfFirst { kotlin.math.abs(it.scaleFactor - currentFactor) < 0.08f }
        val nextIndex = if (currentIndex == -1 || currentIndex >= presets.size - 1) 0 else currentIndex + 1
        fontSizeScale = presets[nextIndex].scaleFactor
        return fontSizeScale
    }
}
