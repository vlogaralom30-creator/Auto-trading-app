package com.example

import com.example.data.entity.RuleEntity
import com.example.engine.RuleMatchingEngine
import com.example.engine.TrendDetector
import com.example.model.Candle
import com.example.model.PatternMatchResult
import com.example.model.SupportResistanceZone
import com.example.model.TrendDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringAndStateMachineTest {

    private fun generateCandleSeries(count: Int = 35): List<Candle> {
        val candles = mutableListOf<Candle>()
        for (i in 0 until count) {
            val isBull = i % 2 == 0
            val high = 100f + i * 2f
            val low = 160f + i * 2f
            val open = if (isBull) 150f + i * 2f else 110f + i * 2f
            val close = if (isBull) 110f + i * 2f else 150f + i * 2f
            candles.add(
                Candle(
                    index = i,
                    openY = open,
                    closeY = close,
                    highY = high,
                    lowY = low,
                    centerX = 50f + i * 20f,
                    bodyLeft = 45f + i * 20f,
                    bodyRight = 55f + i * 20f,
                    isBullish = isBull
                )
            )
        }
        return candles
    }

    @Test
    fun testConfidenceCappedAt78Percent() {
        val candles = generateCandleSeries(35)
        val srZones = listOf(
            SupportResistanceZone(yLevel = 175f, halfHeight = 6f, isSupport = true, touchCount = 3, strengthScore = 0.9f)
        )
        val patterns = listOf(
            PatternMatchResult(
                patternId = "hammer",
                patternName = "HAMMER",
                candleIndex = 34,
                x = 730f,
                y = 175f,
                direction = "UP",
                confidence = 0.95f,
                weight = 0.14f,
                quality = 1.0f,
                description = "Hammer at support",
                hasContext = true
            )
        )

        val result = RuleMatchingEngine.evaluateChart(
            bitmapWidth = 1080,
            bitmapHeight = 1920,
            candles = candles,
            swings = emptyList(),
            srZones = srZones,
            trendLines = emptyList(),
            trendEval = TrendDetector.TrendEvaluation(TrendDirection.DOWNTREND, "trend_down", 0.08f),
            atr = 20f,
            breakoutHint = null,
            isBreakout = false,
            isFakeout = false,
            patterns = patterns,
            ema20 = emptyList(),
            ema50 = emptyList(),
            rsi = 25f,
            momentum = 0.05f,
            activeRules = emptyList()
        )

        assertTrue("Confidence must be capped at 0.78", result.confidenceScore <= 0.78f)
    }

    @Test
    fun testNoTradeFilter_FewerThan30CandlesBlocked() {
        val shortCandles = generateCandleSeries(15) // Only 15 candles
        val result = RuleMatchingEngine.evaluateChart(
            bitmapWidth = 1080,
            bitmapHeight = 1920,
            candles = shortCandles,
            swings = emptyList(),
            srZones = emptyList(),
            trendLines = emptyList(),
            trendEval = TrendDetector.TrendEvaluation(TrendDirection.SIDEWAYS, "trend_sideways", 0.0f),
            atr = 15f,
            breakoutHint = null,
            isBreakout = false,
            isFakeout = false,
            patterns = emptyList(),
            ema20 = emptyList(),
            ema50 = emptyList(),
            rsi = 50f,
            momentum = 0f,
            activeRules = emptyList()
        )

        assertTrue("Fewer than 30 candles must be blocked", result.isBlocked)
        assertEquals("NEUTRAL", result.overallSignal)
        assertNotNull(result.blockedReason)
    }

    @Test
    fun testConfluenceScoringOutputsTop3Reasons() {
        val candles = generateCandleSeries(35)
        val srZones = listOf(
            SupportResistanceZone(yLevel = 175f, halfHeight = 6f, isSupport = true, touchCount = 3, strengthScore = 0.9f)
        )
        val patterns = listOf(
            PatternMatchResult(
                patternId = "hammer",
                patternName = "HAMMER",
                candleIndex = 34,
                x = 730f,
                y = 175f,
                direction = "UP",
                confidence = 0.85f,
                weight = 0.14f,
                quality = 1.0f,
                description = "Hammer at support",
                hasContext = true
            )
        )

        val result = RuleMatchingEngine.evaluateChart(
            bitmapWidth = 1080,
            bitmapHeight = 1920,
            candles = candles,
            swings = emptyList(),
            srZones = srZones,
            trendLines = emptyList(),
            trendEval = TrendDetector.TrendEvaluation(TrendDirection.UPTREND, "trend_up", 0.08f),
            atr = 20f,
            breakoutHint = null,
            isBreakout = false,
            isFakeout = false,
            patterns = patterns,
            ema20 = emptyList(),
            ema50 = emptyList(),
            rsi = 30f,
            momentum = 0.08f,
            activeRules = emptyList()
        )

        assertTrue("Top contributing reasons should be at most 3", result.topContributingReasons.size <= 3)
    }
}
