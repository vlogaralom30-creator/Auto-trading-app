package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AudioHapticNotifier {

    fun notifySignal(
        context: Context,
        isBullish: Boolean,
        isBearish: Boolean,
        soundEnabled: Boolean,
        hapticEnabled: Boolean
    ) {
        if (soundEnabled && (isBullish || isBearish)) {
            playSignalSound(isBullish)
        }
        if (hapticEnabled && (isBullish || isBearish)) {
            vibrateForSignal(context, isBullish)
        }
    }

    fun playSignalSound(isBullish: Boolean) {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            if (isBullish) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } else {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            }
        } catch (_: Exception) {}
    }

    fun vibrateForSignal(context: Context, isBullish: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            val pattern = if (isBullish) {
                longArrayOf(0, 100, 50, 150)
            } else {
                longArrayOf(0, 150, 50, 100)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }
}
