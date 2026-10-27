package com.esp32.smartpomodoro.data.model

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName

enum class PomodoroState(val label: String) {
    IDLE("Idle"),
    WORK("Focusing"),
    BREAK("Break")
}

data class ColorPins(
    @SerializedName("r") val r: Int = 0,
    @SerializedName("g") val g: Int = 0,
    @SerializedName("b") val b: Int = 0
)

data class PomodoroStatus(
    @SerializedName("state") val state: String = "IDLE",
    @SerializedName("isPaused") val isPaused: Boolean = false,
    @SerializedName("totalSec") val totalSec: Long = 1500,
    @SerializedName("remainingSec") val remainingSec: Long = 1500,
    @SerializedName("isUserPresent") val isUserPresent: Boolean = false,
    @SerializedName("distanceCm") val distanceCm: Float = 0f,
    @SerializedName("autoPauseEnabled") val autoPauseEnabled: Boolean = true,
    @SerializedName("cyclesCompleted") val cyclesCompleted: Long = 0,
    @SerializedName("temperature") val temperature: Float = 0f,
    @SerializedName("humidity") val humidity: Float = 0f,
    @SerializedName("brightness") val brightness: Int = 255,
    @SerializedName("effect") val effect: Int = 0,
    @SerializedName("strobeMode") val strobeMode: Int = 0,
    @SerializedName("fadeSpeed") val fadeSpeed: Int = 60,
    @SerializedName("fadePalette") val fadePalette: Int = 0,
    @SerializedName("smoothSpeed") val smoothSpeed: Int = 75,
    @SerializedName("smoothBreath") val smoothBreath: Int = 7,
    @SerializedName("smoothPalette") val smoothPalette: Int = 0,
    @SerializedName("lastIrHex") val lastIrHex: String? = null,
    @SerializedName("lastIrName") val lastIrName: String? = null,
    @SerializedName("s1") val s1: ColorPins? = null,
    @SerializedName("s2") val s2: ColorPins? = null
) {
    val parsedState: PomodoroState
        get() = when (state.uppercase()) {
            "WORK" -> PomodoroState.WORK
            "BREAK" -> PomodoroState.BREAK
            else -> PomodoroState.IDLE
        }

    val progressFraction: Float
        get() = if (totalSec > 0) {
            1f - (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

data class ApiResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("error") val error: String? = null
)

enum class StripTarget(val apiVal: String, val displayName: String) {
    ALL("all", "Both Strips"),
    STRIP_1("1", "Strip 1 (Backlight)"),
    STRIP_2("2", "Strip 2 (Monitor Glow)")
}

data class RemoteColorButton(
    val name: String,
    val commandVal: String,
    val color: Color,
    val isEffect: Boolean = false
)

object RemoteKeyMap {
    val colorRows: List<List<RemoteColorButton>> = listOf(
        // Row 1 (Primary)
        listOf(
            RemoteColorButton("Red", "red", Color(0xFFEF4444)),
            RemoteColorButton("Green", "green", Color(0xFF10B981)),
            RemoteColorButton("Blue", "blue", Color(0xFF3B82F6)),
            RemoteColorButton("White", "white", Color(0xFFF8FAFC))
        ),
        // Row 2
        listOf(
            RemoteColorButton("Orange", "orange", Color(0xFFFB923C)),
            RemoteColorButton("Pea Green", "pea_green", Color(0xFF84CC16)),
            RemoteColorButton("Dark Blue", "dark_blue", Color(0xFF1D4ED8)),
            RemoteColorButton("Flash", "flash", Color(0xFFE2E8F0), isEffect = true)
        ),
        // Row 3
        listOf(
            RemoteColorButton("D-Orange", "dark_orange", Color(0xFFEA580C)),
            RemoteColorButton("Cyan", "cyan", Color(0xFF06B6D4)),
            RemoteColorButton("Magenta", "brown", Color(0xFFD946EF)),
            RemoteColorButton("Strobe", "strobe", Color(0xFFCBD5E1), isEffect = true)
        ),
        // Row 4
        listOf(
            RemoteColorButton("Amber", "amber", Color(0xFFF59E0B)),
            RemoteColorButton("Sky Blue", "light_blue", Color(0xFF38BDF8)),
            RemoteColorButton("Pink", "pink", Color(0xFFF472B6)),
            RemoteColorButton("Fade", "fade", Color(0xFF94A3B8), isEffect = true)
        ),
        // Row 5
        listOf(
            RemoteColorButton("Yellow", "yellow", Color(0xFFFACC15)),
            RemoteColorButton("Ice Blue", "ice_blue", Color(0xFFA5F3FC)),
            RemoteColorButton("Purple", "purple", Color(0xFFA855F7)),
            RemoteColorButton("Smooth", "smooth", Color(0xFF64748B), isEffect = true)
        )
    )
}
