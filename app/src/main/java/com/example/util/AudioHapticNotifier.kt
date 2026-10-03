package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AudioHapticNotifier {

    fun notifySignal(
        context: Context,
        isBullish: Boolean,
        isBearish: Boolean,
        soundEnabled: Boolean = true,
        hapticEnabled: Boolean = true
    ) {
        if (hapticEnabled) {
            triggerHaptic(context, isBullish, isBearish)
        }
        if (soundEnabled) {
            triggerSound(context, isBullish, isBearish)
        }
    }

    private fun triggerHaptic(context: Context, isBullish: Boolean, isBearish: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = if (isBullish) {
                    longArrayOf(0, 70, 60, 100) // Double rising pulse
                } else if (isBearish) {
                    longArrayOf(0, 120, 50, 60) // Heavy drop pulse
                } else {
                    longArrayOf(0, 50)
                }
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            // Ignore vibration errors if permission or hardware absent
        }
    }

    private fun triggerSound(context: Context, isBullish: Boolean, isBearish: Boolean) {
        try {
            // Play system sound effect via AudioManager to avoid ToneGenerator native timeouts
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val effect = if (isBullish) {
                    AudioManager.FX_KEY_CLICK
                } else if (isBearish) {
                    AudioManager.FX_KEYPRESS_DELETE
                } else {
                    AudioManager.FX_KEYPRESS_STANDARD
                }
                audioManager.playSoundEffect(effect, 1.0f)
            } else {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
                ringtone?.play()
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
