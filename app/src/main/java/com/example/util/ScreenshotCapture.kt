package com.example.util

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.webkit.WebView
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

object ScreenshotCapture {

    /**
     * Captures the visible content of a WebView into a high-fidelity Bitmap.
     * Uses PixelCopy when possible with an immediate Canvas drawing fallback.
     */
    suspend fun captureView(view: View, activity: Activity? = null): Bitmap? = withContext(Dispatchers.Main) {
        if (view.width <= 0 || view.height <= 0) return@withContext null

        val width = view.width
        val height = view.height

        // Try PixelCopy if Activity window is present on Android O+ (API 26+)
        if (activity != null && activity.window != null) {
            try {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val location = IntArray(2)
                view.getLocationInWindow(location)

                val sourceRect = Rect(
                    location[0],
                    location[1],
                    location[0] + width,
                    location[1] + height
                )

                val success = suspendCancellableCoroutine<Boolean> { cont ->
                    val handler = Handler(Looper.getMainLooper())
                    PixelCopy.request(
                        activity.window,
                        sourceRect,
                        bitmap,
                        { copyResult ->
                            if (cont.isActive) {
                                cont.resume(copyResult == PixelCopy.SUCCESS)
                            }
                        },
                        handler
                    )
                }

                if (success) {
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                // Fall through to canvas draw
            }
        }

        // Standard Canvas capture fallback
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return@withContext bitmap
        } catch (e: Exception) {
            return@withContext null
        }
    }
}
