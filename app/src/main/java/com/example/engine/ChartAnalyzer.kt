package com.example.engine

import android.graphics.Bitmap
import com.example.data.entity.RuleEntity
import com.example.model.ChartAnalysisResult
import com.example.model.ColorCalibration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main coordinator for running the end-to-end chart computer vision and confluence analysis.
 * Always executes on Dispatchers.Default for 60fps UI responsiveness (<500ms total).
 */
object ChartAnalyzer {

    suspend fun analyzeChartBitmap(
        bitmap: Bitmap,
        calibration: ColorCalibration,
        activeRules: List<RuleEntity>,
        timeframe: String = "1m",
        higherTfTrend: String = "ANY",
        m5Trend: String = "ANY",
        consecutiveLosses: Int = 0,
        dailySignalsUsed: Int = 0,
        dailySignalLimit: Int = 10,
        isUserNewsWindow: Boolean = false
    ): ChartAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. Extract Candlesticks from pixel data
        val candles = CandleExtractor.extractCandles(bitmap, calibration)

        // 2. Calculate ATR (average range of last 14 candles)
        val atr = StructureEngine.calculateAtr(candles, period = 14)

        // 3. Identify Swing Highs and Lows (swing_strength = 3)
        val swings = StructureEngine.detectSwings(candles, lookaround = 3)

        // 4. Cluster Support and Resistance Levels (0.25*ATR cluster tolerance)
        val srZones = StructureEngine.detectZones(candles, swings, atr)

        // 5. Evaluate Macro Trend Direction per trend_rules
        val trendDirection = StructureEngine.evaluateTrend(candles, swings, atr)

        // 6. Detect Breakouts, Retests, and Fakeouts
        val (breakoutHint, breakoutScore) = StructureEngine.detectBreakoutAndFakeout(candles, srZones, atr)

        // 7. Calculate Regression Trendlines
        val trendLines = StructureEngine.fitTrendlines(swings)

        // 8. Calculate Synthetic Technical Indicators (EMA 9, EMA 21, RSI 14, Momentum)
        val ema9 = calculateEma(candles, period = 9)
        val ema21 = calculateEma(candles, period = 21)
        val rsi = calculateRsi(candles, period = 14)
        val momentum = calculateMomentum(candles)

        // 9. Run Modular Candlestick Pattern Detectors per Knowledge Pack
        val patterns = CompositePatternEngine.detectAll(
            candles = candles,
            swings = swings,
            srZones = srZones,
            atr = atr,
            trend = trendDirection,
            higherTfTrend = higherTfTrend
        )

        val duration = System.currentTimeMillis() - startTime

        // 10. Run Confluence Engine & No-Trade Filters
        RuleMatchingEngine.evaluateChart(
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            candles = candles,
            swings = swings,
            srZones = srZones,
            trendLines = trendLines,
            trendDirection = trendDirection,
            breakoutHint = breakoutHint,
            breakoutScore = breakoutScore,
            patterns = patterns,
            ema9 = ema9,
            ema21 = ema21,
            rsi = rsi,
            atr = atr,
            momentum = momentum,
            activeRules = activeRules,
            timeframe = timeframe,
            higherTfTrend = higherTfTrend,
            m5Trend = m5Trend,
            consecutiveLosses = consecutiveLosses,
            dailySignalsUsed = dailySignalsUsed,
            dailySignalLimit = dailySignalLimit,
            isUserNewsWindow = isUserNewsWindow,
            executionDurationMs = duration
        )
    }

    private fun calculateEma(candles: List<com.example.model.Candle>, period: Int): List<Pair<Float, Float>> {
        if (candles.isEmpty()) return emptyList()
        val points = mutableListOf<Pair<Float, Float>>()
        val multiplier = 2.0f / (period + 1f)

        var currentEma = candles.first().closeY
        candles.forEachIndexed { index, candle ->
            if (index == 0) {
                currentEma = candle.closeY
            } else {
                currentEma = (candle.closeY - currentEma) * multiplier + currentEma
            }
            points.add(Pair(candle.centerX, currentEma))
        }
        return points
    }

    private fun calculateRsi(candles: List<com.example.model.Candle>, period: Int = 14): Float? {
        if (candles.size < 6) return null
        val actualPeriod = minOf(period, candles.size - 1)

        var gains = 0f
        var losses = 0f

        for (i in (candles.size - actualPeriod) until candles.size) {
            val change = candles[i - 1].closeY - candles[i].closeY
            if (change > 0) {
                gains += change
            } else {
                losses += kotlin.math.abs(change)
            }
        }

        val avgGain = gains / actualPeriod
        val avgLoss = losses / actualPeriod

        if (avgLoss == 0f) return 100f
        val rs = avgGain / avgLoss
        return (100f - (100f / (1f + rs))).coerceIn(0f, 100f)
    }

    private fun calculateMomentum(candles: List<com.example.model.Candle>): Float {
        if (candles.size < 4) return 0f
        val latest = candles.last().closeY
        val prev = candles[candles.size - 4].closeY
        return (prev - latest) / maxOf(1f, prev)
    }
}
