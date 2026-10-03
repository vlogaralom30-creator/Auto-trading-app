package com.example.engine

import android.app.Activity
import android.webkit.WebView
import com.example.data.entity.RuleEntity
import com.example.model.ChartAnalysisResult
import com.example.model.ColorCalibration
import com.example.model.SiteTimeframeProfile
import com.example.util.ScreenshotCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Handles automated Multi-Timeframe Scanning strictly inside the app's own WebView:
 * Executes JS click on timeframe selectors, waits for chart stability, captures view,
 * and passes 15m trend & 5m trend to the 1m entry confluence engine.
 */
object MultiTimeframeScanner {

    suspend fun runMultiTimeframeScan(
        webView: WebView,
        activity: Activity?,
        calibration: ColorCalibration,
        activeRules: List<RuleEntity>,
        profile: SiteTimeframeProfile,
        consecutiveLosses: Int = 0,
        dailySignalsUsed: Int = 0,
        dailySignalLimit: Int = 10,
        onProgress: (String) -> Unit = {}
    ): ChartAnalysisResult = withContext(Dispatchers.Main) {
        onProgress("1/3 Switching to 15m (Macro Trend & S/R)...")
        var trend15m = "ANY"
        var trend5m = "ANY"

        // 1. Try 15m Timeframe
        if (profile.timeframe15mSelector.isNotBlank()) {
            switchTimeframeInWebView(webView, profile.timeframe15mSelector)
            delay(1200) // Allow chart data to settle over network
            val bm15m = ScreenshotCapture.captureView(webView, activity)
            if (bm15m != null) {
                val res15m = ChartAnalyzer.analyzeChartBitmap(
                    bitmap = bm15m,
                    calibration = calibration,
                    activeRules = activeRules,
                    timeframe = "15m"
                )
                trend15m = when (res15m.trendDirection) {
                    com.example.model.TrendDirection.UPTREND -> "UP"
                    com.example.model.TrendDirection.DOWNTREND -> "DOWN"
                    else -> "SIDEWAYS"
                }
            }
        }

        // 2. Try 5m Timeframe
        onProgress("2/3 Switching to 5m (Pattern & Setup)...")
        if (profile.timeframe5mSelector.isNotBlank()) {
            switchTimeframeInWebView(webView, profile.timeframe5mSelector)
            delay(1000)
            val bm5m = ScreenshotCapture.captureView(webView, activity)
            if (bm5m != null) {
                val res5m = ChartAnalyzer.analyzeChartBitmap(
                    bitmap = bm5m,
                    calibration = calibration,
                    activeRules = activeRules,
                    timeframe = "5m",
                    higherTfTrend = trend15m
                )
                trend5m = when (res5m.trendDirection) {
                    com.example.model.TrendDirection.UPTREND -> "UP"
                    com.example.model.TrendDirection.DOWNTREND -> "DOWN"
                    else -> "SIDEWAYS"
                }
            }
        }

        // 3. Switch to 1m Timeframe (Entry Timing)
        onProgress("3/3 Switching to 1m (Precision Entry Execution)...")
        if (profile.timeframe1mSelector.isNotBlank()) {
            switchTimeframeInWebView(webView, profile.timeframe1mSelector)
            delay(800)
        }

        val bm1m = ScreenshotCapture.captureView(webView, activity)
            ?: throw IllegalStateException("Failed to capture 1m chart viewport")

        onProgress("Synthesizing Multi-Timeframe Confluence Engine...")
        ChartAnalyzer.analyzeChartBitmap(
            bitmap = bm1m,
            calibration = calibration,
            activeRules = activeRules,
            timeframe = "1m",
            higherTfTrend = trend15m,
            m5Trend = trend5m,
            consecutiveLosses = consecutiveLosses,
            dailySignalsUsed = dailySignalsUsed,
            dailySignalLimit = dailySignalLimit
        )
    }

    /**
     * Executes safe JavaScript selector click in the app's own WebView.
     */
    private suspend fun switchTimeframeInWebView(webView: WebView, selector: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val escapedSelector = selector.replace("'", "\\'")
            val js = """
                (function() {
                    try {
                        var el = document.querySelector('$escapedSelector');
                        if (el) {
                            el.click();
                            return 'CLICKED';
                        }
                        // Fallback search by text inside button
                        var btns = document.querySelectorAll('button, [role="button"], div');
                        for (var i = 0; i < btns.length; i++) {
                            if (btns[i].innerText && btns[i].innerText.trim() === '$escapedSelector') {
                                btns[i].click();
                                return 'CLICKED_TEXT';
                            }
                        }
                        return 'NOT_FOUND';
                    } catch(e) {
                        return 'ERROR: ' + e.message;
                    }
                })();
            """.trimIndent()

            webView.evaluateJavascript(js) { result ->
                if (continuation.isActive) {
                    val success = result != null && (result.contains("CLICKED") || !result.contains("NOT_FOUND"))
                    continuation.resume(success)
                }
            }
        }
}
