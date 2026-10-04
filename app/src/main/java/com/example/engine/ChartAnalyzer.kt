package com.example.engine

import android.graphics.Bitmap
import com.example.data.entity.RuleEntity
import com.example.model.ChartAnalysisResult
import com.example.model.ColorCalibration
import com.example.model.TrendDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main coordinator for running the end-to-end chart computer vision and rule analysis.
 * Operates on Dispatchers.Default for maximum UI responsiveness (<500ms).
 */
object ChartAnalyzer {

    suspend fun analyzeChartBitmap(
        bitmap: Bitmap,
        calibration: ColorCalibration,
        activeRules: List<RuleEntity>,
        higherTfTrend: TrendDirection? = null,
        isHigherTfAgree: Boolean = false,
        consecutiveLosses: Int = 0,
        signalsToday: Int = 0,
        dailyLimit: Int = 10,
        isNewsWindowFlagged: Boolean = false,
        timeframe: String = "1m"
    ): ChartAnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. Extract Candlesticks from pixel data
        val candles = CandleExtractor.extractCandles(bitmap, calibration)

        // 2. Volatility (14-period ATR)
        val atr = IndicatorCalculator.calculateAtr(candles, period = 14)

        // 3. Identify Swing Highs and Lows (swing_strength = 3)
        val swings = SwingDetector.detectSwings(candles, lookaround = 3)

        // 4. Cluster Support and Resistance Levels (0.25*ATR clustering, 0.2*ATR half height)
        val srZones = SupportResistanceDetector.detectZones(candles, swings, atr)

        // 5. Technical Moving Averages (EMA20 & EMA50)
        val ema20 = IndicatorCalculator.calculateEma(candles, period = 20)
        val ema50 = IndicatorCalculator.calculateEma(candles, period = 50)

        // 6. Trend Rules Evaluation (swings rising/falling, 0.8*ATR sideways band, EMA bias)
        val trendEval = TrendDetector.evaluate(candles, swings, atr, ema20, ema50)

        // 7. Breakout, Fakeout, and Retest evaluation
        val (breakoutHint, isBreakout, isFakeout) =
            SupportResistanceDetector.evaluateBreakoutRetestFakeout(candles, srZones, atr)

        // 8. Oscillators: RSI and Momentum
        val rsi = IndicatorCalculator.calculateRsi(candles, period = 14)
        val momentum = IndicatorCalculator.calculateMomentum(candles)

        // 9. Knowledge Pack Candlestick Pattern Recognition (all 14 patterns with context)
        val patterns = KnowledgePackPatternEngine.detectAll(
            candles = candles,
            swings = swings,
            srZones = srZones,
            trend = trendEval.direction,
            atr = atr
        )

        // 10. Confluence Scoring & No-Trade Filters
        val duration = System.currentTimeMillis() - startTime

        RuleMatchingEngine.evaluateChart(
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            candles = candles,
            swings = swings,
            srZones = srZones,
            trendLines = trendEval.trendLines,
            trendEval = trendEval,
            atr = atr,
            breakoutHint = breakoutHint,
            isBreakout = isBreakout,
            isFakeout = isFakeout,
            patterns = patterns,
            ema20 = ema20,
            ema50 = ema50,
            rsi = rsi,
            momentum = momentum,
            activeRules = activeRules,
            higherTfTrend = higherTfTrend,
            isHigherTfAgree = isHigherTfAgree,
            consecutiveLosses = consecutiveLosses,
            signalsToday = signalsToday,
            dailyLimit = dailyLimit,
            isNewsWindowFlagged = isNewsWindowFlagged,
            timeframe = timeframe,
            executionDurationMs = duration
        )
    }
}
