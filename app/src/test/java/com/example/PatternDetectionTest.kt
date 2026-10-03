package com.example

import com.example.data.entity.RuleEntity
import com.example.engine.BullishEngulfingDetector
import com.example.engine.ChartAnalyzer
import com.example.engine.DojiDetector
import com.example.engine.HammerDetector
import com.example.engine.RuleMatchingEngine
import com.example.engine.ShootingStarDetector
import com.example.engine.StructureEngine
import com.example.model.Candle
import com.example.model.SwingPoint
import com.example.model.TrendDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternDetectionTest {

    @Test
    fun testHammerDetection() {
        val hammerCandle = Candle(
            index = 1,
            openY = 100f,
            closeY = 95f, // Bullish small body: 5px
            highY = 93f,  // Upper wick: 2px
            lowY = 140f,  // Lower wick: 40px
            centerX = 50f,
            bodyLeft = 45f,
            bodyRight = 55f,
            isBullish = true
        )

        assertTrue("Candle should be classified as Hammer", hammerCandle.isHammer)

        val previousCandle = Candle(
            index = 0,
            openY = 80f,
            closeY = 100f,
            highY = 75f,
            lowY = 105f,
            centerX = 30f,
            bodyLeft = 25f,
            bodyRight = 35f,
            isBullish = false
        )

        val detector = HammerDetector()
        val results = detector.detect(
            candles = listOf(previousCandle, hammerCandle),
            swings = emptyList(),
            srZones = emptyList(),
            atr = 20f
        )

        assertEquals(1, results.size)
        assertEquals("HAMMER", results.first().patternName)
        assertEquals("UP", results.first().direction)
        assertTrue(results.first().confidence >= 0.60f)
    }

    @Test
    fun testShootingStarDetection() {
        val shootingStarCandle = Candle(
            index = 1,
            openY = 135f,
            closeY = 140f, // Bearish small body: 5px
            highY = 90f,   // Long upper wick: 45px
            lowY = 142f,   // Tiny lower wick: 2px
            centerX = 50f,
            bodyLeft = 45f,
            bodyRight = 55f,
            isBullish = false
        )

        assertTrue("Candle should be classified as Shooting Star", shootingStarCandle.isShootingStar)

        val detector = ShootingStarDetector()
        val results = detector.detect(
            candles = listOf(
                Candle(0, 150f, 135f, 130f, 155f, 30f, 25f, 35f, true),
                shootingStarCandle
            ),
            swings = emptyList(),
            srZones = emptyList(),
            atr = 20f
        )

        assertEquals(1, results.size)
        assertEquals("SHOOTING_STAR", results.first().patternName)
        assertEquals("DOWN", results.first().direction)
    }

    @Test
    fun testBullishEngulfingDetection() {
        val redCandle = Candle(
            index = 0,
            openY = 100f,
            closeY = 120f,
            highY = 95f,
            lowY = 125f,
            centerX = 20f,
            bodyLeft = 15f,
            bodyRight = 25f,
            isBullish = false
        )

        val greenEngulfing = Candle(
            index = 1,
            openY = 125f,
            closeY = 90f,
            highY = 85f,
            lowY = 130f,
            centerX = 40f,
            bodyLeft = 35f,
            bodyRight = 45f,
            isBullish = true
        )

        val detector = BullishEngulfingDetector()
        val results = detector.detect(
            candles = listOf(redCandle, greenEngulfing),
            swings = emptyList(),
            srZones = emptyList(),
            atr = 20f
        )

        assertEquals(1, results.size)
        assertEquals("BULLISH_ENGULFING", results.first().patternName)
        assertEquals("UP", results.first().direction)
    }

    @Test
    fun testDojiDetection() {
        val dojiCandle = Candle(
            index = 0,
            openY = 100f,
            closeY = 100.5f,
            highY = 70f,
            lowY = 130f,
            centerX = 20f,
            bodyLeft = 15f,
            bodyRight = 25f,
            isBullish = true
        )

        assertTrue(dojiCandle.isDoji)
        val detector = DojiDetector()
        val results = detector.detect(listOf(dojiCandle), emptyList(), emptyList(), atr = 20f)
        assertEquals(1, results.size)
        assertEquals("DOJI", results.first().patternName)
    }

    @Test
    fun testSupportResistanceClustering() {
        val swings = listOf(
            SwingPoint(candleIndex = 2, x = 40f, y = 201f, isHigh = false),
            SwingPoint(candleIndex = 6, x = 90f, y = 199f, isHigh = false),
            SwingPoint(candleIndex = 11, x = 140f, y = 202f, isHigh = false)
        )

        val zones = StructureEngine.detectZones(
            candles = emptyList(),
            swings = swings,
            atr = 20f
        )
        assertEquals(1, zones.size)
        assertTrue(zones.first().isSupport)
        assertEquals(3, zones.first().touchCount)
    }

    @Test
    fun testRuleMatchingInference() {
        val hammerCandle = Candle(
            index = 24,
            openY = 198f,
            closeY = 195f,
            highY = 193f,
            lowY = 240f,
            centerX = 500f,
            bodyLeft = 495f,
            bodyRight = 505f,
            isBullish = true
        )

        val candles = (0 until 24).map { i ->
            Candle(i, 150f + i * 2, 160f + i * 2, 145f + i * 2, 165f + i * 2, 20f * (i + 1), 15f, 25f, false)
        } + hammerCandle

        val swings = StructureEngine.detectSwings(candles, lookaround = 2)
        val zones = StructureEngine.detectZones(
            candles = candles,
            swings = listOf(SwingPoint(10, 200f, 240f, false), SwingPoint(20, 400f, 238f, false)),
            atr = 20f
        )

        val activeRules = listOf(
            RuleEntity(
                name = "Bullish Hammer at Support",
                patternType = "HAMMER",
                requiredTrend = "ANY",
                requireNearSupport = true,
                outcome = "UP",
                weight = 0.12f
            )
        )

        val result = RuleMatchingEngine.evaluateChart(
            bitmapWidth = 800,
            bitmapHeight = 1200,
            candles = candles,
            swings = swings,
            srZones = zones,
            trendLines = emptyList(),
            trendDirection = TrendDirection.DOWNTREND,
            breakoutHint = null,
            patterns = emptyList(),
            ema9 = emptyList(),
            ema21 = emptyList(),
            rsi = 28f, // Oversold
            atr = 20f,
            momentum = 0.05f,
            activeRules = activeRules
        )

        assertTrue(result.confidenceScore in 0.50f..0.78f)
    }
}
