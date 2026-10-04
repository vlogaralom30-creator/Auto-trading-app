package com.example.util

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.webkit.WebView

object ScreenshotCapture {

    fun captureWebView(webView: WebView, onCaptured: (Bitmap?) -> Unit) {
        try {
            val width = webView.width
            val height = webView.height
            if (width <= 0 || height <= 0) {
                onCaptured(null)
                return
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)
            onCaptured(bitmap)
        } catch (e: Exception) {
            onCaptured(null)
        }
    }

    fun captureView(view: View, activity: Activity? = null): Bitmap? {
        return try {
            val width = view.width
            val height = view.height
            if (width <= 0 || height <= 0) return null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun captureView(view: View, activity: Activity? = null, onCaptured: (Bitmap?) -> Unit) {
        val bmp = captureView(view, activity)
        onCaptured(bmp)
    }
}
