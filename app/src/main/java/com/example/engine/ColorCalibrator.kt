package com.example.engine

import android.graphics.Color
import com.example.model.ColorCalibration

/**
 * Fast HSV/RGB color segmentation utility for detecting green (bullish)
 * and red (bearish) candlestick bodies and wicks in trading charts.
 */
object ColorCalibrator {

    enum class PixelType {
        BULLISH,
        BEARISH,
        BACKGROUND_OR_OTHER
    }

    /**
     * Classifies a single ARGB pixel into Bullish (Green), Bearish (Red), or Background/Neutral.
     */
    fun classifyPixel(pixel: Int, calibration: ColorCalibration): PixelType {
        val alpha = Color.alpha(pixel)
        if (alpha < 100) return PixelType.BACKGROUND_OR_OTHER

        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)

        // Fast path: direct RGB contrast check (very common in dark theme charts)
        // Green dominates Red and Blue by noticeable margin
        if (g > 80 && g > (r * 1.25f) && g > (b * 1.15f)) {
            return PixelType.BULLISH
        }
        // Red dominates Green and Blue by noticeable margin
        if (r > 80 && r > (g * 1.25f) && r > (b * 1.20f)) {
            return PixelType.BEARISH
        }

        // Detailed HSV classification for calibrated/unusual broker palettes
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0]
        val saturation = hsv[1]
        val value = hsv[2]

        if (saturation < calibration.minSaturation || value < calibration.minValue) {
            return PixelType.BACKGROUND_OR_OTHER
        }

        // Bullish Hue range (typically 70° to 170° - green/cyan-green)
        if (hue in calibration.bullishHueMin..calibration.bullishHueMax) {
            return PixelType.BULLISH
        }

        // Bearish Hue range (wraps around 340° to 360° and 0° to 25° - red/magenta-red)
        if (hue >= calibration.bearishHueMin || hue <= calibration.bearishHueMax) {
            return PixelType.BEARISH
        }

        return PixelType.BACKGROUND_OR_OTHER
    }
}
