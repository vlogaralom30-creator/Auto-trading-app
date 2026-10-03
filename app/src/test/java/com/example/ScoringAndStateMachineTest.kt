package com.example

import com.example.data.entity.RuleEntity
import com.example.engine.RuleMatchingEngine
import com.example.model.AutoTraderState
import com.example.model.Candle
import com.example.model.PatternMatchResult
import com.example.model.SupportResistanceZone
import com.example.model.SwingPoint
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
            SupportResistanceZone(yLevel = 175f, thickness = 6f, isSupport = true, touchCount = 3, strengthScore = 0.9f)
        )
        val patterns = listOf(
            PatternMatchResult(
                patternName = "HAMMER",
                candleIndex = 34,
                x = 730f,
                y = 175f,
                direction = "UP",
                confidence = 0.95f,
                weight = 0.14f,
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
            trendDirection = TrendDirection.DOWNTREND,
            breakoutHint = null,
            patterns = patterns,
            ema9 = emptyList(),
            ema21 = emptyList(),
            rsi = 25f,
            atr = 20f,
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
            trendDirection = TrendDirection.SIDEWAYS,
            breakoutHint = null,
            patterns = emptyList(),
            ema9 = emptyList(),
            ema21 = emptyList(),
            rsi = 50f,
            atr = 15f,
            momentum = 0f,
            activeRules = emptyList()
        )

        assertTrue("Fewer than 20/30 candles must be blocked", result.isBlockedByFilter)
        assertEquals("NEUTRAL", result.overallSignal)
        assertNotNull(result.blockReason)
    }

    @Test
    fun testStateMachineTransitions() {
        var state: AutoTraderState = AutoTraderState.Idle
        assertEquals("Idle", state.javaClass.simpleName)

        state = AutoTraderState.Scanning("Scanning 15m...", 10)
        assertTrue(state is AutoTraderState.Scanning)

        state = AutoTraderState.WaitingEntry("UP", 0.74f, "Hammer at Support", listOf("Support bounce", "Oversold RSI"), 8)
        assertTrue(state is AutoTraderState.WaitingEntry)

        state = AutoTraderState.Placing("UP", 1.0, "1m")
        assertTrue(state is AutoTraderState.Placing)

        state = AutoTraderState.InTrade("T-12345", "UP", 1.0, System.currentTimeMillis(), 60, 45)
        assertTrue(state is AutoTraderState.InTrade)
        assertEquals(45, (state as AutoTraderState.InTrade).remainingSec)

        state = AutoTraderState.Result(isWin = true, profitLoss = 0.85, newBalance = 100.85, message = "Trade WON")
        assertTrue(state is AutoTraderState.Result)
        assertTrue((state as AutoTraderState.Result).isWin)
    }
}
