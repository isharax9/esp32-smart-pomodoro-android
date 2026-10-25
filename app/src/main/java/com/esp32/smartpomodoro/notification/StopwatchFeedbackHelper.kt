package com.esp32.smartpomodoro.notification

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Handles audio tone synthesis and haptic vibrations for Stopwatch interactions.
 * Modeled after omegacentauri.mobi.simplestopwatch's AudioTrack and Vibrator implementations.
 */
class StopwatchFeedbackHelper(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.w("StopwatchFeedback", "Unable to initialize ToneGenerator: ${e.message}")
        }
    }

    /**
     * Short tick tone for countdown phase (3, 2, 1).
     */
    fun playCountdownTick() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 75)
        vibrate(40)
    }

    /**
     * Long confirmation tone when countdown hits 0 and timing begins.
     */
    fun playStartChime() {
        playTone(ToneGenerator.TONE_PROP_ACK, 250)
        vibrate(120)
    }

    /**
     * Subtle click feedback when a lap is recorded.
     */
    fun playLapChime() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 50)
        vibrate(50)
    }

    /**
     * Chime for periodic intervals (e.g. every 30s or 60s).
     */
    fun playPeriodicChime() {
        playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        vibrate(80)
    }

    /**
     * Light haptic tap for UI button presses.
     */
    fun performClickHaptic() {
        vibrate(25)
    }

    /**
     * Warning haptic pattern when screen is locked.
     */
    fun performLockWarningHaptic() {
        vibratePattern(longArrayOf(0, 50, 50, 50))
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (e: Exception) {
            Log.d("StopwatchFeedback", "Tone playback skipped: ${e.message}")
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.d("StopwatchFeedback", "Vibration skipped: ${e.message}")
        }
    }

    private fun vibratePattern(timings: LongArray) {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.d("StopwatchFeedback", "Pattern vibration skipped: ${e.message}")
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore on release
        }
    }
}
