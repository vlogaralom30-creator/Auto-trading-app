package com.example.engine

import com.example.model.Candle
import com.example.model.PatternMatchResult
import com.example.model.SupportResistanceZone
import com.example.model.SwingPoint
import com.example.model.TrendDirection
import kotlin.math.abs

interface PatternDetector {
    val id: String
    val name: String
    fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float = 15f,
        trend: TrendDirection = TrendDirection.SIDEWAYS,
        higherTfTrend: String = "ANY"
    ): List<PatternMatchResult>
}

/**
 * 1. Hammer
 * detect: lower_wick >= 2*body AND upper_wick <= 0.15*range AND body_ratio <= 0.35
 * context_required: trend = DOWN AND near support
 * weight_with_context: 0.1, weight_without_context: 0.0
 */
class HammerDetector : PatternDetector {
    override val id: String = "hammer"
    override val name: String = "Hammer"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val candle = candles[i]
            val isHammerGeom = candle.lowerWick >= (2f * candle.body) &&
                    candle.upperWick <= (0.15f * candle.range) &&
                    candle.bodyRatio <= 0.35f

            if (isHammerGeom) {
                val nearThreshold = if (atr > 0) 0.5f * atr else 20f
                val nearSupport = srZones.any { it.isSupport && abs(it.yLevel - candle.lowY) <= nearThreshold }
                val hasContext = (trend == TrendDirection.DOWNTREND || candles[i - 1].closeY > candles[i - 1].openY) && nearSupport

                val weight = if (hasContext) 0.10f else 0.0f
                val confidence = if (hasContext) 0.78f else 0.60f

                matches.add(
                    PatternMatchResult(
                        patternName = "HAMMER",
                        candleIndex = i,
                        x = candle.centerX,
                        y = candle.lowY,
                        direction = "UP",
                        confidence = confidence,
                        weight = weight,
                        description = "Hammer rejection at support: lower wick >= 2*body, buyer absorption",
                        hasContext = hasContext
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 2. Shooting Star
 * detect: upper_wick >= 2*body AND lower_wick <= 0.15*range AND body_ratio <= 0.35
 * context_required: trend = UP AND near resistance
 * weight_with_context: 0.1, weight_without_context: 0.0
 */
class ShootingStarDetector : PatternDetector {
    override val id: String = "shooting_star"
    override val name: String = "Shooting Star"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val candle = candles[i]
            val isStarGeom = candle.upperWick >= (2f * candle.body) &&
                    candle.lowerWick <= (0.15f * candle.range) &&
                    candle.bodyRatio <= 0.35f

            if (isStarGeom) {
                val nearThreshold = if (atr > 0) 0.5f * atr else 20f
                val nearResistance = srZones.any { !it.isSupport && abs(it.yLevel - candle.highY) <= nearThreshold }
                val hasContext = (trend == TrendDirection.UPTREND || candles[i - 1].closeY < candles[i - 1].openY) && nearResistance

                val weight = if (hasContext) 0.10f else 0.0f
                val confidence = if (hasContext) 0.78f else 0.60f

                matches.add(
                    PatternMatchResult(
                        patternName = "SHOOTING_STAR",
                        candleIndex = i,
                        x = candle.centerX,
                        y = candle.highY,
                        direction = "DOWN",
                        confidence = confidence,
                        weight = weight,
                        description = "Shooting star rejection at resistance: upper wick >= 2*body, seller dominance",
                        hasContext = hasContext
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 3. Bullish Engulfing
 * detect: prev is down candle AND current is up candle AND current body fully covers prev body AND current body >= 1.2*prev body
 * context_required: near support OR trend = DOWN exhausted
 * weight_with_context: 0.12, weight_without_context: 0.03
 */
class BullishEngulfingDetector : PatternDetector {
    override val id: String = "bull_engulfing"
    override val name: String = "Bullish Engulfing"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val prev = candles[i - 1]
            val curr = candles[i]

            // In screen Y: down candle has closeY > openY; up candle has closeY < openY
            val prevIsDown = !prev.isBullish
            val currIsUp = curr.isBullish

            val coversPrev = curr.bodyBottom >= prev.bodyBottom && curr.bodyTop <= prev.bodyTop
            val sizeFactor = curr.body >= (1.2f * prev.body)

            if (prevIsDown && currIsUp && coversPrev && sizeFactor) {
                val nearThreshold = if (atr > 0) 0.5f * atr else 20f
                val nearSupport = srZones.any { it.isSupport && abs(it.yLevel - curr.lowY) <= nearThreshold }
                val hasContext = nearSupport || trend == TrendDirection.DOWNTREND

                val weight = if (hasContext) 0.12f else 0.03f
                val confidence = if (hasContext) 0.78f else 0.65f

                matches.add(
                    PatternMatchResult(
                        patternName = "BULLISH_ENGULFING",
                        candleIndex = i,
                        x = curr.centerX,
                        y = curr.lowY,
                        direction = "UP",
                        confidence = confidence,
                        weight = weight,
                        description = "Bullish Engulfing: Strong green envelope completely covering previous red body",
                        hasContext = hasContext
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 4. Bearish Engulfing
 * detect: prev is up candle AND current is down candle AND current body fully covers prev body AND current body >= 1.2*prev body
 * context_required: near resistance OR trend = UP exhausted
 * weight_with_context: 0.12, weight_without_context: 0.03
 */
class BearishEngulfingDetector : PatternDetector {
    override val id: String = "bear_engulfing"
    override val name: String = "Bearish Engulfing"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val prev = candles[i - 1]
            val curr = candles[i]

            val prevIsUp = prev.isBullish
            val currIsDown = !curr.isBullish

            val coversPrev = curr.bodyBottom >= prev.bodyBottom && curr.bodyTop <= prev.bodyTop
            val sizeFactor = curr.body >= (1.2f * prev.body)

            if (prevIsUp && currIsDown && coversPrev && sizeFactor) {
                val nearThreshold = if (atr > 0) 0.5f * atr else 20f
                val nearResistance = srZones.any { !it.isSupport && abs(it.yLevel - curr.highY) <= nearThreshold }
                val hasContext = nearResistance || trend == TrendDirection.UPTREND

                val weight = if (hasContext) 0.12f else 0.03f
                val confidence = if (hasContext) 0.78f else 0.65f

                matches.add(
                    PatternMatchResult(
                        patternName = "BEARISH_ENGULFING",
                        candleIndex = i,
                        x = curr.centerX,
                        y = curr.highY,
                        direction = "DOWN",
                        confidence = confidence,
                        weight = weight,
                        description = "Bearish Engulfing: Dominant red envelope completely covering previous green body",
                        hasContext = hasContext
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 5. Morning Star
 * detect: c1 big down body, c2 small body (body_ratio<=0.3) gapping or sitting low, c3 up candle closing above c1 midpoint
 * context_required: trend = DOWN AND near support
 * weight_with_context: 0.13, weight_without_context: 0.02
 */
class MorningStarDetector : PatternDetector {
    override val id: String = "morning_star"
    override val name: String = "Morning Star"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 3) return matches

        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val c2 = candles[i - 1]
            val c3 = candles[i]

            val c1Down = !c1.isBullish && c1.bodyRatio >= 0.45f
            val c2Small = c2.bodyRatio <= 0.35f
            val c2Low = c2.lowY >= c1.bodyBottom - 5f // Sitting low/near bottom
            val c1MidpointY = (c1.openY + c1.closeY) / 2f
            val c3UpClosingAboveMid = c3.isBullish && c3.closeY < c1MidpointY

            if (c1Down && c2Small && c2Low && c3UpClosingAboveMid) {
                val nearThreshold = if (atr > 0) 0.5f * atr else 25f
                val nearSupport = srZones.any { it.isSupport && abs(it.yLevel - c2.lowY) <= nearThreshold }
                val hasContext = (trend == TrendDirection.DOWNTREND) && nearSupport

                val weight = if (hasContext) 0.13f else 0.02f
                val confidence = if (hasContext) 0.78f else 0.62f

                matches.add(
                    PatternMatchResult(
                        patternName = "MORNING_STAR",
                        candleIndex = i,
                        x = c3.centerX,
                        y = c3.lowY,
                        direction = "UP",
                        confidence = confidence,
                        weight = weight,
                        description = "Morning Star: 3-bar reversal sequence confirming bullish turnaround at support",
                        hasContext = hasContext
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 6. Evening Star
 * detect: c1 big up body, c2 small body (body_ratio<=0.3) sitting high, c3 down candle closing below c1 midpoint
 * context_required: trend = UP AND near resistance
 * weight_with_context: 0.13, weight_without_context: 0.02
 */
class EveningStarDetector : PatternDetector {
    override val id: String = "evening_star"
    override val name: String = "Evening Star"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 3) return matches

        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val c2 = candles[i - 1]
            val c3 = candles[i]

            val c1Up = c1.isBullish && c1.bodyRatio >= 0.45f
            val c2Small = c2.bodyRatio <= 0.35f
            val c2High = c2.highY <= c1.bodyTop + 5f // Sitting high
            val c1MidpointY = (c1.openY + c1.closeY) / 2f
            val c3DownClosingBelowMid = !c3.isBullish && c3.closeY > c1MidpointY

            if (c1Up && c2Small && c2High && c3DownClosingBelowMid) {
                val nearThreshold = if (atr > 0) 0.5f * atr else 25f
                val nearResistance = srZones.any { !it.isSupport && abs(it.yLevel - c2.highY) <= nearThreshold }
                val hasContext = (trend == TrendDirection.UPTREND) && nearResistance

                val weight = if (hasContext) 0.13f else 0.02f
                val confidence = if (hasContext) 0.78f else 0.62f

                matches.add(
                    PatternMatchResult(
                        patternName = "EVENING_STAR",
                        candleIndex = i,
                        x = c3.centerX,
                        y = c3.highY,
                        direction = "DOWN",
                        confidence = confidence,
                        weight = weight,
                        description = "Evening Star: 3-bar bearish exhaustion pattern turning down at resistance",
                        hasContext = hasContext
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 7. Bullish Pin Bar
 * detect: lower_wick >= 0.66*range AND body in upper third
 * context_required: wick pierces support and closes back above it
 * weight_with_context: 0.12, weight_without_context: 0.0
 */
class BullishPinBarDetector : PatternDetector {
    override val id: String = "pin_bar_bull"
    override val name: String = "Bullish Pin Bar"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        for (i in candles.indices) {
            val candle = candles[i]
            val isPin = candle.isPinBarBull

            if (isPin) {
                val piercesSupport = srZones.any { it.isSupport && candle.lowY > it.yLevel && candle.bodyBottom <= it.yLevel + 8f }
                val nearSupport = srZones.any { it.isSupport && abs(it.yLevel - candle.lowY) <= (0.5f * atr) }
                val hasContext = piercesSupport || nearSupport

                val weight = if (hasContext) 0.12f else 0.0f
                val confidence = if (hasContext) 0.78f else 0.60f

                matches.add(
                    PatternMatchResult(
                        patternName = "PIN_BAR_BULL",
                        candleIndex = i,
                        x = candle.centerX,
                        y = candle.lowY,
                        direction = "UP",
                        confidence = confidence,
                        weight = weight,
                        description = "Bullish Pin Bar: Lower wick >= 66% range piercing support level",
                        hasContext = hasContext
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 8. Bearish Pin Bar
 * detect: upper_wick >= 0.66*range AND body in lower third
 * context_required: wick pierces resistance and closes back below it
 * weight_with_context: 0.12, weight_without_context: 0.0
 */
class BearishPinBarDetector : PatternDetector {
    override val id: String = "pin_bar_bear"
    override val name: String = "Bearish Pin Bar"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        for (i in candles.indices) {
            val candle = candles[i]
            val isPin = candle.isPinBarBear

            if (isPin) {
                val piercesResistance = srZones.any { !it.isSupport && candle.highY < it.yLevel && candle.bodyTop >= it.yLevel - 8f }
                val nearResistance = srZones.any { !it.isSupport && abs(it.yLevel - candle.highY) <= (0.5f * atr) }
                val hasContext = piercesResistance || nearResistance

                val weight = if (hasContext) 0.12f else 0.0f
                val confidence = if (hasContext) 0.78f else 0.60f

                matches.add(
                    PatternMatchResult(
                        patternName = "PIN_BAR_BEAR",
                        candleIndex = i,
                        x = candle.centerX,
                        y = candle.highY,
                        direction = "DOWN",
                        confidence = confidence,
                        weight = weight,
                        description = "Bearish Pin Bar: Upper wick >= 66% range rejecting resistance level",
                        hasContext = hasContext
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 9. Tweezer Bottom
 * detect: two candles with lows within 0.05*ATR, first down second up
 * context_required: near support
 * weight_with_context: 0.08, weight_without_context: 0.0
 */
class TweezerBottomDetector : PatternDetector {
    override val id: String = "tweezer_bottom"
    override val name: String = "Tweezer Bottom"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        val threshold = maxOf(2f, 0.05f * atr)
        for (i in 1 until candles.size) {
            val c1 = candles[i - 1]
            val c2 = candles[i]

            val sameLows = abs(c1.lowY - c2.lowY) <= threshold
            val firstDownSecondUp = !c1.isBullish && c2.isBullish

            if (sameLows && firstDownSecondUp) {
                val nearSupport = srZones.any { it.isSupport && abs(it.yLevel - c2.lowY) <= (0.5f * atr) }
                val weight = if (nearSupport) 0.08f else 0.0f
                val confidence = if (nearSupport) 0.74f else 0.58f

                matches.add(
                    PatternMatchResult(
                        patternName = "TWEEZER_BOTTOM",
                        candleIndex = i,
                        x = c2.centerX,
                        y = c2.lowY,
                        direction = "UP",
                        confidence = confidence,
                        weight = weight,
                        description = "Tweezer Bottom: Matched lows confirming floor rejection at support",
                        hasContext = nearSupport
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 10. Tweezer Top
 * detect: two candles with highs within 0.05*ATR, first up second down
 * context_required: near resistance
 * weight_with_context: 0.08, weight_without_context: 0.0
 */
class TweezerTopDetector : PatternDetector {
    override val id: String = "tweezer_top"
    override val name: String = "Tweezer Top"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        val threshold = maxOf(2f, 0.05f * atr)
        for (i in 1 until candles.size) {
            val c1 = candles[i - 1]
            val c2 = candles[i]

            val sameHighs = abs(c1.highY - c2.highY) <= threshold
            val firstUpSecondDown = c1.isBullish && !c2.isBullish

            if (sameHighs && firstUpSecondDown) {
                val nearResistance = srZones.any { !it.isSupport && abs(it.yLevel - c2.highY) <= (0.5f * atr) }
                val weight = if (nearResistance) 0.08f else 0.0f
                val confidence = if (nearResistance) 0.74f else 0.58f

                matches.add(
                    PatternMatchResult(
                        patternName = "TWEEZER_TOP",
                        candleIndex = i,
                        x = c2.centerX,
                        y = c2.highY,
                        direction = "DOWN",
                        confidence = confidence,
                        weight = weight,
                        description = "Tweezer Top: Matched highs rejecting resistance ceiling twice",
                        hasContext = nearResistance
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 11. Inside Bar
 * detect: current high < prev high AND current low > prev low
 * context_required: trade only the break of mother bar in direction of higher timeframe trend
 * weight_with_context: 0.07, weight_without_context: 0.0
 */
class InsideBarDetector : PatternDetector {
    override val id: String = "inside_bar"
    override val name: String = "Inside Bar"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val mother = candles[i - 1]
            val curr = candles[i]

            // In screen coordinates: smaller Y = higher price
            val insideHigh = curr.highY > mother.highY
            val insideLow = curr.lowY < mother.lowY

            if (insideHigh && insideLow) {
                val dir = if (higherTfTrend == "UP" || trend == TrendDirection.UPTREND) "UP"
                else if (higherTfTrend == "DOWN" || trend == TrendDirection.DOWNTREND) "DOWN"
                else "BREAKOUT"

                matches.add(
                    PatternMatchResult(
                        patternName = "INSIDE_BAR",
                        candleIndex = i,
                        x = curr.centerX,
                        y = (curr.highY + curr.lowY) / 2f,
                        direction = dir,
                        confidence = 0.72f,
                        weight = 0.07f,
                        description = "Inside Bar: Volatility contraction awaiting breakout in trend direction",
                        hasContext = true
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 12. Doji
 * detect: body_ratio <= 0.1
 * effect: Reduces confidence of continuation by 0.05; adds 0.03 to reversal signals at S/R. Never a signal alone.
 */
class DojiDetector : PatternDetector {
    override val id: String = "doji"
    override val name: String = "Doji"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        for (i in candles.indices) {
            val candle = candles[i]
            if (candle.isDoji) {
                val nearSR = srZones.any { abs(it.yLevel - candle.centerX) <= (0.5f * atr) }
                matches.add(
                    PatternMatchResult(
                        patternName = "DOJI",
                        candleIndex = i,
                        x = candle.centerX,
                        y = (candle.highY + candle.lowY) / 2f,
                        direction = "NEUTRAL",
                        confidence = 0.50f,
                        weight = 0.03f,
                        description = "Doji Indecision: Body <= 10% range. Reversal warning at S/R levels.",
                        hasContext = nearSR
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 13. Marubozu (momentum)
 * detect: body_ratio >= 0.85
 * context_required: closes beyond a level (breakout) OR trend continuation
 * weight_with_context: 0.08, weight_without_context: 0.0
 */
class MarubozuDetector : PatternDetector {
    override val id: String = "marubozu"
    override val name: String = "Marubozu"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        for (i in candles.indices) {
            val candle = candles[i]
            if (candle.isMarubozu) {
                val dir = if (candle.isBullish) "UP" else "DOWN"
                matches.add(
                    PatternMatchResult(
                        patternName = "MARUBOZU",
                        candleIndex = i,
                        x = candle.centerX,
                        y = if (candle.isBullish) candle.lowY else candle.highY,
                        direction = dir,
                        confidence = 0.74f,
                        weight = 0.08f,
                        description = "Marubozu: Dominant momentum body >= 85% range without rejection wicks",
                        hasContext = true
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 14. Three White Soldiers / Three Black Crows
 * detect: 3 consecutive same-color candles, each opening within previous body and closing near its high/low
 * context_required: not already extended more than 3*ATR
 * weight_with_context: 0.08, weight_without_context: 0.02
 */
class ThreeSoldiersDetector : PatternDetector {
    override val id: String = "three_soldiers"
    override val name: String = "Three Soldiers / Crows"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float,
        trend: TrendDirection,
        higherTfTrend: String
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 3) return matches

        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val c2 = candles[i - 1]
            val c3 = candles[i]

            val allBull = c1.isBullish && c2.isBullish && c3.isBullish
            val allBear = !c1.isBullish && !c2.isBullish && !c3.isBullish

            if (allBull) {
                val totalMove = c1.lowY - c3.highY
                val notExtended = atr <= 0f || totalMove <= (3f * atr)
                val weight = if (notExtended) 0.08f else 0.02f

                matches.add(
                    PatternMatchResult(
                        patternName = "THREE_SOLDIERS",
                        candleIndex = i,
                        x = c3.centerX,
                        y = c3.lowY,
                        direction = "UP",
                        confidence = 0.76f,
                        weight = weight,
                        description = "Three White Soldiers: Consecutive advancing green bars showing persistent buying pressure",
                        hasContext = notExtended
                    )
                )
            } else if (allBear) {
                val totalMove = c3.lowY - c1.highY
                val notExtended = atr <= 0f || totalMove <= (3f * atr)
                val weight = if (notExtended) 0.08f else 0.02f

                matches.add(
                    PatternMatchResult(
                        patternName = "THREE_CROWS",
                        candleIndex = i,
                        x = c3.centerX,
                        y = c3.highY,
                        direction = "DOWN",
                        confidence = 0.76f,
                        weight = weight,
                        description = "Three Black Crows: Consecutive plunging red bars showing persistent selling pressure",
                        hasContext = notExtended
                    )
                )
            }
        }
        return matches
    }
}

/**
 * Composite manager orchestrating all modular candlestick detectors.
 */
object CompositePatternEngine {

    private val allDetectors: List<PatternDetector> = listOf(
        HammerDetector(),
        ShootingStarDetector(),
        BullishEngulfingDetector(),
        BearishEngulfingDetector(),
        MorningStarDetector(),
        EveningStarDetector(),
        BullishPinBarDetector(),
        BearishPinBarDetector(),
        TweezerBottomDetector(),
        TweezerTopDetector(),
        InsideBarDetector(),
        DojiDetector(),
        MarubozuDetector(),
        ThreeSoldiersDetector()
    )

    fun detectAll(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        atr: Float = 15f,
        trend: TrendDirection = TrendDirection.SIDEWAYS,
        higherTfTrend: String = "ANY"
    ): List<PatternMatchResult> {
        val results = mutableListOf<PatternMatchResult>()
        for (detector in allDetectors) {
            results.addAll(detector.detect(candles, swings, srZones, atr, trend, higherTfTrend))
        }
        return results
    }
}
