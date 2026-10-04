package com.example.engine

import android.app.Activity
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebView
import com.example.data.entity.RuleEntity
import com.example.model.ChartAnalysisResult
import com.example.model.ColorCalibration
import com.example.model.MultiTimeframeAnalysisResult
import com.example.model.TrendDirection
import com.example.util.ScreenshotCapture
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object WebViewChartController {

    suspend fun setTimeframe(
        webView: WebView,
        timeframe: String,
        selectorTemplate: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val resolvedSelector = selectorTemplate.replace("{tf}", timeframe)
        val cleanTf = timeframe.replace("m", "").trim()
        val js = """
            (function() {
                var tf = '$timeframe';
                var cleanTf = '$cleanTf';
                
                // 1. Try configured user/site profile selector template
                try {
                    var el = document.querySelector('$resolvedSelector');
                    if (el) { el.click(); return 'SUCCESS_CUSTOM'; }
                } catch(e) {}
                
                // 2. TradingView Specific interval buttons & header items
                try {
                    var tvBtn = document.querySelector('[data-value="' + cleanTf + '"], [value="' + cleanTf + '"], #header-toolbar-intervals');
                    if (tvBtn) { tvBtn.click(); return 'SUCCESS_TV'; }
                } catch(e) {}

                // 3. Fallback smart heuristics for Quotex / Exness / Binance / PocketOption
                var buttons = Array.from(document.querySelectorAll('button, div[role="button"], span, div, a'));
                var match = buttons.find(function(b) {
                    var txt = (b.textContent || '').trim().toLowerCase();
                    var aria = (b.getAttribute('aria-label') || '').trim().toLowerCase();
                    var val = (b.getAttribute('data-value') || '').trim().toLowerCase();
                    var id = (b.id || '').toLowerCase();
                    return txt === tf.toLowerCase() || txt === cleanTf || 
                           aria.indexOf(tf.toLowerCase()) !== -1 || 
                           val === tf.toLowerCase() || val === cleanTf ||
                           id.indexOf('timeframe-' + cleanTf) !== -1;
                });
                if (match) { 
                    match.click(); 
                    return 'SUCCESS_HEURISTIC'; 
                }
                return 'NOT_FOUND';
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { result ->
            val success = result != null && !result.contains("NOT_FOUND", ignoreCase = true)
            if (continuation.isActive) {
                continuation.resume(success)
            }
        }
    }

    suspend fun fitChartZoom(webView: WebView, zoomMethod: String) {
        if (zoomMethod.contains("wheel", ignoreCase = true)) {
            // Dispatch JS Wheel Event on Chart Canvas
            val js = """
                (function() {
                    var canvas = document.querySelector('canvas') || document.body;
                    var evt = new WheelEvent('wheel', {
                        deltaY: -80,
                        deltaMode: 0,
                        bubbles: true,
                        cancelable: true,
                        clientX: window.innerWidth / 2,
                        clientY: window.innerHeight / 2
                    });
                    canvas.dispatchEvent(evt);
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        } else {
            dispatchPinchGesture(webView)
        }
    }

    private fun dispatchPinchGesture(webView: WebView) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        val centerX = webView.width / 2f
        val centerY = webView.height / 2f

        val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
        val upEvent = MotionEvent.obtain(downTime, eventTime + 50, MotionEvent.ACTION_UP, centerX, centerY, 0)
        webView.dispatchTouchEvent(downEvent)
        webView.dispatchTouchEvent(upEvent)
        downEvent.recycle()
        upEvent.recycle()
    }

    suspend fun executeMultiTimeframeScan(
        webView: WebView,
        activity: Activity?,
        calibration: ColorCalibration,
        activeRules: List<RuleEntity>,
        selectorTemplate: String,
        zoomMethod: String,
        consecutiveLosses: Int,
        signalsToday: Int,
        dailyLimit: Int,
        isNewsWindowFlagged: Boolean,
        onProgressUpdate: (step: String, percent: Float) -> Unit
    ): MultiTimeframeAnalysisResult {
        // Step 1: Scan 15m (trend direction + major support/resistance)
        onProgressUpdate("Scanning 15m: Trend & Major Levels...", 0.15f)
        setTimeframe(webView, "15m", selectorTemplate)
        delay(600) // wait_for_chart_stable(600ms)
        fitChartZoom(webView, zoomMethod)
        delay(200)

        val bmp15m = ScreenshotCapture.captureView(webView, activity)
        val res15m = bmp15m?.let {
            ChartAnalyzer.analyzeChartBitmap(
                bitmap = it,
                calibration = calibration,
                activeRules = activeRules,
                timeframe = "15m"
            )
        }

        // Step 2: Scan 5m (setup / pattern)
        onProgressUpdate("Scanning 5m: Pattern & Setup...", 0.50f)
        setTimeframe(webView, "5m", selectorTemplate)
        delay(600) // wait_for_chart_stable(600ms)
        fitChartZoom(webView, zoomMethod)
        delay(200)

        val bmp5m = ScreenshotCapture.captureView(webView, activity)
        val res5m = bmp5m?.let {
            ChartAnalyzer.analyzeChartBitmap(
                bitmap = it,
                calibration = calibration,
                activeRules = activeRules,
                timeframe = "5m"
            )
        }

        // Step 3: Scan 1m (entry timing)
        onProgressUpdate("Scanning 1m: Precision Entry Timing...", 0.85f)
        setTimeframe(webView, "1m", selectorTemplate)
        delay(600) // wait_for_chart_stable(600ms)
        fitChartZoom(webView, zoomMethod)
        delay(200)

        val bmp1m = ScreenshotCapture.captureView(webView, activity)

        val trend15m = res15m?.trendDirection ?: TrendDirection.SIDEWAYS
        val trend5m = res5m?.trendDirection ?: TrendDirection.SIDEWAYS
        val isAgree = trend15m != TrendDirection.SIDEWAYS && trend15m == trend5m

        val res1m = bmp1m?.let {
            ChartAnalyzer.analyzeChartBitmap(
                bitmap = it,
                calibration = calibration,
                activeRules = activeRules,
                higherTfTrend = trend15m,
                isHigherTfAgree = isAgree,
                consecutiveLosses = consecutiveLosses,
                signalsToday = signalsToday,
                dailyLimit = dailyLimit,
                isNewsWindowFlagged = isNewsWindowFlagged,
                timeframe = "1m"
            )
        }

        onProgressUpdate("Scan Complete", 1.0f)

        val finalSignal = res1m?.overallSignal ?: "NEUTRAL"
        val finalConfidence = res1m?.confidenceScore ?: 0.5f

        val agreementNote = when {
            isAgree -> "Strong multi-timeframe consensus: 15m and 5m both confirm $trend15m (+0.10 score)"
            trend15m != TrendDirection.SIDEWAYS && (res1m?.overallSignal == "UP" && trend15m == TrendDirection.DOWNTREND || res1m?.overallSignal == "DOWN" && trend15m == TrendDirection.UPTREND) ->
                "15m trend ($trend15m) conflicts with 1m setup (-0.15 score penalty)"
            else -> "Independent 1m entry timing confirmed"
        }

        val topReasons = mutableListOf<String>()
        res1m?.topContributingReasons?.let { topReasons.addAll(it) }
        topReasons.add(agreementNote)

        return MultiTimeframeAnalysisResult(
            tf15m = res15m,
            tf5m = res5m,
            tf1m = res1m,
            finalSignal = finalSignal,
            finalConfidence = finalConfidence,
            isAgreement = isAgree,
            agreementNote = agreementNote,
            topReasons = topReasons.take(3)
        )
    }
}
