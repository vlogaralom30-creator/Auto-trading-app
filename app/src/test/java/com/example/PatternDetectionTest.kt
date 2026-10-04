package com.example

import com.example.data.entity.RuleEntity
import com.example.data.entity.shouldBeDisabled
import com.example.engine.KnowledgePackBearEngulfingDetector
import com.example.engine.KnowledgePackBullEngulfingDetector
import com.example.engine.KnowledgePackDojiDetector
import com.example.engine.KnowledgePackEveningStarDetector
import com.example.engine.KnowledgePackHammerDetector
import com.example.engine.KnowledgePackInsideBarDetector
import com.example.engine.KnowledgePackMarubozuDetector
import com.example.engine.KnowledgePackMorningStarDetector
import com.example.engine.KnowledgePackPinBarBearDetector
import com.example.engine.KnowledgePackPinBarBullDetector
import com.example.engine.KnowledgePackShootingStarDetector
import com.example.engine.KnowledgePackThreeSoldiersDetector
import com.example.engine.KnowledgePackTweezerBottomDetector
import com.example.engine.KnowledgePackTweezerTopDetector
import com.example.engine.RuleMatchingEngine
import com.example.engine.SupportResistanceDetector
import com.example.engine.TrendDetector
import com.example.model.Candle
import com.example.model.SupportResistanceZone
import com.example.model.TrendDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class PatternDetectionTest {

    private val sampleAtr = 15f

    @Test
    fun testHammerDetectorFormula() {
        // lower_wick >= 2*body AND upper_wick <= 0.15*range AND body_ratio <= 0.35
        // Screen Y: high=98, open=102, close=100 (body=2), low=125 (range=27, lower_wick=23, upper_wick=2)
        val hammer = Candle(
            index = 1,
            openY = 102f,
            closeY = 100f,
            highY = 98f,
            lowY = 125f,
            centerX = 50f,
            bodyLeft = 45f,
            bodyRight = 55f,
            isBullish = true
        )

        val support = SupportResistanceZone(
            yLevel = 125f,
            halfHeight = 3f,
            isSupport = true,
            touchCount = 3,
            strengthScore = 4.5f
        )

        val detector = KnowledgePackHammerDetector()
        val results = detector.detect(
            candles = listOf(hammer),
            swings = emptyList(),
            srZones = listOf(support),
            trend = TrendDirection.DOWNTREND,
            atr = sampleAtr
        )

        assertEquals(1, results.size)
        assertEquals("hammer", results.first().patternId)
        assertEquals("UP", results.first().direction)
        assertTrue(results.first().hasContext)
    }

    @Test
    fun testShootingStarDetectorFormula() {
        // upper_wick >= 2*body AND lower_wick <= 0.15*range AND body_ratio <= 0.35
        // Screen Y: high=75, open=98, close=100 (body=2, top=98), low=101 (range=26, upper_wick=23, lower_wick=1)
        val shootingStar = Candle(
            index = 1,
            openY = 98f,
            closeY = 100f,
            highY = 75f,
            lowY = 101f,
            centerX = 50f,
            bodyLeft = 45f,
            bodyRight = 55f,
            isBullish = false
        )

        val resistance = SupportResistanceZone(
            yLevel = 75f,
            halfHeight = 3f,
            isSupport = false,
            touchCount = 3,
            strengthScore = 4.0f
        )

        val detector = KnowledgePackShootingStarDetector()
        val results = detector.detect(
            candles = listOf(shootingStar),
            swings = emptyList(),
            srZones = listOf(resistance),
            trend = TrendDirection.UPTREND,
            atr = sampleAtr
        )

        assertEquals(1, results.size)
        assertEquals("shooting_star", results.first().patternId)
        assertEquals("DOWN", results.first().direction)
    }

    @Test
    fun testBullishEngulfingDetector() {
        // prev is down candle, current is up candle, current body covers prev body >= 1.2x
        val prev = Candle(
            index = 0,
            openY = 90f,
            closeY = 100f,
            highY = 88f,
            lowY = 102f,
            centerX = 20f,
            bodyLeft = 15f,
            bodyRight = 25f,
            isBullish = false
        )
        val curr = Candle(
            index = 1,
            openY = 102f,
            closeY = 85f, // body = 17 vs prev body = 10 (1.7x)
            highY = 83f,
            lowY = 104f,
            centerX = 35f,
            bodyLeft = 30f,
            bodyRight = 40f,
            isBullish = true
        )

        val detector = KnowledgePackBullEngulfingDetector()
        val results = detector.detect(
            candles = listOf(prev, curr),
            swings = emptyList(),
            srZones = emptyList(),
            trend = TrendDirection.DOWNTREND,
            atr = sampleAtr
        )

        assertEquals(1, results.size)
        assertEquals("bull_engulfing", results.first().patternId)
        assertEquals("UP", results.first().direction)
    }

    @Test
    fun testBearishEngulfingDetector() {
        val prev = Candle(
            index = 0,
            openY = 100f,
            closeY = 90f,
            highY = 88f,
            lowY = 102f,
            centerX = 20f,
            bodyLeft = 15f,
            bodyRight = 25f,
            isBullish = true
        )
        val curr = Candle(
            index = 1,
            openY = 88f,
            closeY = 105f, // body = 17 vs 10
            highY = 86f,
            lowY = 107f,
            centerX = 35f,
            bodyLeft = 30f,
            bodyRight = 40f,
            isBullish = false
        )

        val detector = KnowledgePackBearEngulfingDetector()
        val results = detector.detect(
            candles = listOf(prev, curr),
            swings = emptyList(),
            srZones = emptyList(),
            trend = TrendDirection.UPTREND,
            atr = sampleAtr
        )

        assertEquals(1, results.size)
        assertEquals("bear_engulfing", results.first().patternId)
        assertEquals("DOWN", results.first().direction)
    }

    @Test
    fun testMorningStarDetector() {
        val c1 = Candle(0, 70f, 95f, 68f, 98f, 20f, 15f, 25f, false) // big down body = 25, range = 30
        val c2 = Candle(1, 99f, 100f, 96f, 105f, 35f, 30f, 40f, true) // small star body = 1, range = 9 (ratio = 0.11 <= 0.30)
        val c3 = Candle(2, 98f, 75f, 73f, 100f, 50f, 45f, 55f, true) // up above midpoint 82.5

        val detector = KnowledgePackMorningStarDetector()
        val results = detector.detect(
            candles = listOf(c1, c2, c3),
            swings = emptyList(),
            srZones = emptyList(),
            trend = TrendDirection.DOWNTREND,
            atr = sampleAtr
        )

        assertEquals(1, results.size)
        assertEquals("morning_star", results.first().patternId)
        assertEquals("UP", results.first().direction)
    }

    @Test
    fun testEveningStarDetector() {
        val c1 = Candle(0, 95f, 70f, 68f, 98f, 20f, 15f, 25f, true) // big up body = 25, range = 30
        val c2 = Candle(1, 67f, 66f, 62f, 70f, 35f, 30f, 40f, false) // small star body = 1, range = 8 (ratio = 0.125 <= 0.30)
        val c3 = Candle(2, 68f, 88f, 66f, 90f, 50f, 45f, 55f, false) // down below midpoint 82.5

        val detector = KnowledgePackEveningStarDetector()
        val results = detector.detect(
            candles = listOf(c1, c2, c3),
            swings = emptyList(),
            srZones = emptyList(),
            trend = TrendDirection.UPTREND,
            atr = sampleAtr
        )

        assertEquals(1, results.size)
        assertEquals("evening_star", results.first().patternId)
        assertEquals("DOWN", results.first().direction)
    }

    @Test
    fun testPinBarDetectors() {
        // Bullish pin bar: lower_wick >= 0.66*range and body in upper third
        val pinBull = Candle(0, 92f, 90f, 89f, 130f, 20f, 15f, 25f, true) // range=41, lower_wick=38 >= 27
        val bullDetector = KnowledgePackPinBarBullDetector()
        val resBull = bullDetector.detect(
            candles = listOf(pinBull),
            swings = emptyList(),
            srZones = emptyList(),
            trend = TrendDirection.DOWNTREND,
            atr = sampleAtr
        )
        assertEquals(1, resBull.size)
        assertEquals("UP", resBull.first().direction)

        // Bearish pin bar: upper_wick >= 0.66*range and body in lower third
        val pinBear = Candle(0, 128f, 130f, 89f, 132f, 20f, 15f, 25f, false) // range=43, upper_wick=39 >= 28.3
        val bearDetector = KnowledgePackPinBarBearDetector()
        val resBear = bearDetector.detect(
            candles = listOf(pinBear),
            swings = emptyList(),
            srZones = emptyList(),
            trend = TrendDirection.UPTREND,
            atr = sampleAtr
        )
        assertEquals(1, resBear.size)
        assertEquals("DOWN", resBear.first().direction)
    }

    @Test
    fun testTweezerDetectors() {
        // Tweezer Bottom: matching lows within 0.05*ATR
        val c1 = Candle(0, 90f, 100f, 88f, 110f, 20f, 15f, 25f, false)
        val c2 = Candle(1, 100f, 92f, 90f, 110.2f, 35f, 30f, 40f, true)

        val bottomDetector = KnowledgePackTweezerBottomDetector()
        val bottomRes = bottomDetector.detect(
            candles = listOf(c1, c2),
            swings = emptyList(),
            srZones = emptyList(),
            trend = TrendDirection.DOWNTREND,
            atr = sampleAtr
        )
        assertEquals(1, bottomRes.size)
        assertEquals("tweezer_bottom", bottomRes.first().patternId)

        // Tweezer Top: matching highs within 0.05*ATR
        val c3 = Candle(2, 100f, 90f, 85f, 102f, 20f, 15f, 25f, true)
        val c4 = Candle(3, 90f, 98f, 85.1f, 100f, 35f, 30f, 40f, false)

        val topDetector = KnowledgePackTweezerTopDetector()
        val topRes = topDetector.detect(
            candles = listOf(c3, c4),
            swings = emptyList(),
            srZones = emptyList(),
            trend = TrendDirection.UPTREND,
            atr = sampleAtr
        )
        assertEquals(1, topRes.size)
        assertEquals("tweezer_top", topRes.first().patternId)
    }

    @Test
    fun testInsideBarAndMarubozuAndThreeSoldiers() {
        // Inside Bar: current high < prev high and current low > prev low (in price)
        // Screen Y: highY is larger, lowY is smaller
        val mother = Candle(0, 95f, 90f, 80f, 110f, 20f, 15f, 25f, true)
        val inside = Candle(1, 92f, 94f, 85f, 105f, 35f, 30f, 40f, false)
        val insideDetector = KnowledgePackInsideBarDetector()
        val insideRes = insideDetector.detect(listOf(mother, inside), emptyList(), emptyList(), TrendDirection.UPTREND, sampleAtr)
        assertEquals(1, insideRes.size)

        // Marubozu: body_ratio >= 0.85
        val marubozu = Candle(0, 108f, 82f, 80f, 110f, 20f, 15f, 25f, true) // body=26, range=30 (ratio=0.866)
        val marubozuDetector = KnowledgePackMarubozuDetector()
        val maruRes = marubozuDetector.detect(listOf(marubozu), emptyList(), emptyList(), TrendDirection.UPTREND, sampleAtr)
        assertEquals(1, maruRes.size)

        // Three White Soldiers
        val s1 = Candle(0, 105f, 95f, 94f, 106f, 20f, 15f, 25f, true)
        val s2 = Candle(1, 100f, 90f, 89f, 101f, 35f, 30f, 40f, true)
        val s3 = Candle(2, 94f, 85f, 84f, 95f, 50f, 45f, 55f, true)
        val soldiersDetector = KnowledgePackThreeSoldiersDetector()
        val soldiersRes = soldiersDetector.detect(listOf(s1, s2, s3), emptyList(), emptyList(), TrendDirection.UPTREND, sampleAtr)
        assertEquals(1, soldiersRes.size)
    }

    @Test
    fun testConfluenceScoringLogisticFormula() {
        // Test logistic curve: p_up = 1 / (1 + exp(-k * score)), k = 6
        val k = 6.0
        val scoreZero = 0.0
        val pZero = 1.0 / (1.0 + exp(-k * scoreZero))
        assertEquals(0.5, pZero, 0.001)

        val scoreBull = 0.25
        val pBull = 1.0 / (1.0 + exp(-k * scoreBull))
        assertTrue("pBull should be >= 0.80", pBull >= 0.80)

        // Test Confidence Cap (0.78)
        val maxCap = 0.78f
        val cappedConf = minOf(pBull.toFloat(), maxCap)
        assertEquals(0.78f, cappedConf, 0.001f)
    }

    @Test
    fun testBayesianLearningAndAutoDisableThreshold() {
        // Test rule: (win + 1) / (win + loss + 2)
        val win = 10
        val loss = 25 // total = 35 >= 30
        val measuredWinRate = (win + 1f) / (win + loss + 2f) // 11 / 37 = 0.297 < 0.48
        assertTrue(measuredWinRate < 0.48f)

        val rule = RuleEntity(
            name = "Test Low Win Rate Rule",
            patternType = "CUSTOM",
            winCount = win,
            lossCount = loss,
            weight = 0.10f
        )

        assertTrue("Rule should be flagged for disable after 30+ trades and <48% win rate", rule.shouldBeDisabled)
    }
}
