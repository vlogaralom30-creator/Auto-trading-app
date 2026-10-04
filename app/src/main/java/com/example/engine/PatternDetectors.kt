package com.example.engine

import com.example.model.Candle
import com.example.model.PatternMatchResult
import com.example.model.SupportResistanceZone
import com.example.model.SwingPoint
import com.example.model.TrendDirection
import kotlin.math.abs

interface KnowledgePackPatternDetector {
    val id: String
    val name: String
    fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult>
}

private fun isNearZone(priceY: Float, zones: List<SupportResistanceZone>, atr: Float, isSupport: Boolean): Boolean {
    // near_level: abs(price - level) <= 0.5*ATR
    val threshold = 0.5f * atr
    return zones.filter { it.isSupport == isSupport }
        .any { abs(it.yLevel - priceY) <= threshold }
}

/**
 * 1. Hammer (UP)
 * detect: lower_wick >= 2*body AND upper_wick <= 0.15*range AND body_ratio <= 0.35
 * context_required: trend = DOWN AND near support
 */
class KnowledgePackHammerDetector : KnowledgePackPatternDetector {
    override val id: String = "hammer"
    override val name: String = "Hammer"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.isEmpty()) return matches

        for (i in candles.indices) {
            val c = candles[i]
            val passesShape = c.lowerWick >= (2f * c.body) &&
                    c.upperWick <= (0.15f * c.range) &&
                    c.bodyRatio <= 0.35f

            if (passesShape) {
                val nearSupport = isNearZone(c.lowY, srZones, atr, isSupport = true)
                val hasContext = trend == TrendDirection.DOWNTREND && nearSupport
                val weight = if (hasContext) 0.10f else 0.0f

                if (hasContext || i == candles.lastIndex) {
                    matches.add(
                        PatternMatchResult(
                            patternId = id,
                            patternName = name,
                            candleIndex = i,
                            x = c.centerX,
                            y = c.lowY,
                            direction = "UP",
                            confidence = if (hasContext) 0.85f else 0.50f,
                            weight = weight,
                            quality = if (c.lowerWick >= 3f * c.body) 1.0f else 0.8f,
                            hasContext = hasContext,
                            description = "Bullish Hammer: Long lower wick rejection at lows" +
                                    if (hasContext) " (Aligned: Downtrend + Support Zone)" else " (Ignored: Missing context)"
                        )
                    )
                }
            }
        }
        return matches
    }
}

/**
 * 2. Shooting Star (DOWN)
 * detect: upper_wick >= 2*body AND lower_wick <= 0.15*range AND body_ratio <= 0.35
 * context_required: trend = UP AND near resistance
 */
class KnowledgePackShootingStarDetector : KnowledgePackPatternDetector {
    override val id: String = "shooting_star"
    override val name: String = "Shooting Star"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.isEmpty()) return matches

        for (i in candles.indices) {
            val c = candles[i]
            val passesShape = c.upperWick >= (2f * c.body) &&
                    c.lowerWick <= (0.15f * c.range) &&
                    c.bodyRatio <= 0.35f

            if (passesShape) {
                val nearResistance = isNearZone(c.highY, srZones, atr, isSupport = false)
                val hasContext = trend == TrendDirection.UPTREND && nearResistance
                val weight = if (hasContext) 0.10f else 0.0f

                if (hasContext || i == candles.lastIndex) {
                    matches.add(
                        PatternMatchResult(
                            patternId = id,
                            patternName = name,
                            candleIndex = i,
                            x = c.centerX,
                            y = c.highY,
                            direction = "DOWN",
                            confidence = if (hasContext) 0.85f else 0.50f,
                            weight = weight,
                            quality = if (c.upperWick >= 3f * c.body) 1.0f else 0.8f,
                            hasContext = hasContext,
                            description = "Bearish Shooting Star: High rejection wick" +
                                    if (hasContext) " (Aligned: Uptrend + Resistance Zone)" else " (Ignored: Missing context)"
                        )
                    )
                }
            }
        }
        return matches
    }
}

/**
 * 3. Bullish Engulfing (UP)
 * detect: prev is down candle AND current is up candle AND current body fully covers prev body AND current body >= 1.2*prev body
 * context_required: near support OR trend = DOWN exhausted
 */
class KnowledgePackBullEngulfingDetector : KnowledgePackPatternDetector {
    override val id: String = "bull_engulfing"
    override val name: String = "Bullish Engulfing"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val prev = candles[i - 1]
            val curr = candles[i]

            // In screen coordinates: smaller Y = higher price
            val isPrevDown = !prev.isBullish
            val isCurrUp = curr.isBullish
            val coversBody = curr.bodyTop <= prev.bodyTop && curr.bodyBottom >= prev.bodyBottom
            val isLarger = curr.body >= (1.2f * prev.body)

            if (isPrevDown && isCurrUp && coversBody && isLarger) {
                val nearSupport = isNearZone(curr.bodyBottom, srZones, atr, isSupport = true)
                val hasContext = nearSupport || trend == TrendDirection.DOWNTREND
                val weight = if (hasContext) 0.12f else 0.03f

                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = name,
                        candleIndex = i,
                        x = curr.centerX,
                        y = curr.bodyBottom,
                        direction = "UP",
                        confidence = if (hasContext) 0.88f else 0.65f,
                        weight = weight,
                        quality = if (curr.body >= 1.5f * prev.body) 1.0f else 0.85f,
                        hasContext = hasContext,
                        description = "Bullish Engulfing: Current green candle completely engulfs prior red body" +
                                if (hasContext) " (With Support / Downtrend context)" else ""
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 4. Bearish Engulfing (DOWN)
 * detect: prev is up candle AND current is down candle AND current body fully covers prev body AND current body >= 1.2*prev body
 * context_required: near resistance OR trend = UP exhausted
 */
class KnowledgePackBearEngulfingDetector : KnowledgePackPatternDetector {
    override val id: String = "bear_engulfing"
    override val name: String = "Bearish Engulfing"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val prev = candles[i - 1]
            val curr = candles[i]

            val isPrevUp = prev.isBullish
            val isCurrDown = !curr.isBullish
            val coversBody = curr.bodyTop <= prev.bodyTop && curr.bodyBottom >= prev.bodyBottom
            val isLarger = curr.body >= (1.2f * prev.body)

            if (isPrevUp && isCurrDown && coversBody && isLarger) {
                val nearResistance = isNearZone(curr.bodyTop, srZones, atr, isSupport = false)
                val hasContext = nearResistance || trend == TrendDirection.UPTREND
                val weight = if (hasContext) 0.12f else 0.03f

                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = name,
                        candleIndex = i,
                        x = curr.centerX,
                        y = curr.bodyTop,
                        direction = "DOWN",
                        confidence = if (hasContext) 0.88f else 0.65f,
                        weight = weight,
                        quality = if (curr.body >= 1.5f * prev.body) 1.0f else 0.85f,
                        hasContext = hasContext,
                        description = "Bearish Engulfing: Current red candle completely engulfs prior green body" +
                                if (hasContext) " (With Resistance / Uptrend context)" else ""
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 5. Morning Star (UP)
 * detect: c1 big down body, c2 small body (body_ratio<=0.3) gapping or sitting low, c3 up candle closing above c1 midpoint
 * context_required: trend = DOWN AND near support
 */
class KnowledgePackMorningStarDetector : KnowledgePackPatternDetector {
    override val id: String = "morning_star"
    override val name: String = "Morning Star"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 3) return matches

        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val c2 = candles[i - 1]
            val c3 = candles[i]

            val isC1Down = !c1.isBullish && c1.bodyRatio >= 0.40f
            val isC2Small = c2.bodyRatio <= 0.30f && c2.bodyTop >= c1.bodyBottom - (0.2f * atr)
            val c1MidpointY = (c1.openY + c1.closeY) / 2f
            val isC3UpAboveMid = c3.isBullish && c3.closeY < c1MidpointY // smaller Y = higher price

            if (isC1Down && isC2Small && isC3UpAboveMid) {
                val nearSupport = isNearZone(c2.lowY, srZones, atr, isSupport = true)
                val hasContext = trend == TrendDirection.DOWNTREND && nearSupport
                val weight = if (hasContext) 0.13f else 0.02f

                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = name,
                        candleIndex = i,
                        x = c3.centerX,
                        y = c2.lowY,
                        direction = "UP",
                        confidence = if (hasContext) 0.89f else 0.60f,
                        weight = weight,
                        quality = 0.95f,
                        hasContext = hasContext,
                        description = "Morning Star: 3-bar bottom reversal confirming transition to buyers"
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 6. Evening Star (DOWN)
 * detect: c1 big up body, c2 small body (body_ratio<=0.3) sitting high, c3 down candle closing below c1 midpoint
 * context_required: trend = UP AND near resistance
 */
class KnowledgePackEveningStarDetector : KnowledgePackPatternDetector {
    override val id: String = "evening_star"
    override val name: String = "Evening Star"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 3) return matches

        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val c2 = candles[i - 1]
            val c3 = candles[i]

            val isC1Up = c1.isBullish && c1.bodyRatio >= 0.40f
            val isC2Small = c2.bodyRatio <= 0.30f && c2.bodyBottom <= c1.bodyTop + (0.2f * atr)
            val c1MidpointY = (c1.openY + c1.closeY) / 2f
            val isC3DownBelowMid = !c3.isBullish && c3.closeY > c1MidpointY // larger Y = lower price

            if (isC1Up && isC2Small && isC3DownBelowMid) {
                val nearResistance = isNearZone(c2.highY, srZones, atr, isSupport = false)
                val hasContext = trend == TrendDirection.UPTREND && nearResistance
                val weight = if (hasContext) 0.13f else 0.02f

                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = name,
                        candleIndex = i,
                        x = c3.centerX,
                        y = c2.highY,
                        direction = "DOWN",
                        confidence = if (hasContext) 0.89f else 0.60f,
                        weight = weight,
                        quality = 0.95f,
                        hasContext = hasContext,
                        description = "Evening Star: 3-bar top reversal confirming transition to sellers"
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 7. Bullish Pin Bar (UP)
 * detect: lower_wick >= 0.66*range AND body in upper third
 * context_required: wick pierces support and closes back above it
 */
class KnowledgePackPinBarBullDetector : KnowledgePackPatternDetector {
    override val id: String = "pin_bar_bull"
    override val name: String = "Bullish Pin Bar"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.isEmpty()) return matches

        for (i in candles.indices) {
            val c = candles[i]
            val lowerWickOk = c.lowerWick >= (0.66f * c.range)
            val bodyInUpperThird = c.bodyBottom <= (c.highY + (c.range * 0.35f))

            if (lowerWickOk && bodyInUpperThird) {
                // context_required: wick pierces support and closes back above it
                val piercedSupport = srZones.filter { it.isSupport }
                    .any { it.containsPrice(c.lowY) || (c.lowY >= it.yLevel && c.closeY <= it.yLevel) }

                val weight = if (piercedSupport) 0.12f else 0.0f
                if (piercedSupport || i == candles.lastIndex) {
                    matches.add(
                        PatternMatchResult(
                            patternId = id,
                            patternName = name,
                            candleIndex = i,
                            x = c.centerX,
                            y = c.lowY,
                            direction = "UP",
                            confidence = if (piercedSupport) 0.88f else 0.50f,
                            weight = weight,
                            quality = 0.90f,
                            hasContext = piercedSupport,
                            description = "Bullish Pin Bar: 66%+ lower wick piercing support and closing above"
                        )
                    )
                }
            }
        }
        return matches
    }
}

/**
 * 8. Bearish Pin Bar (DOWN)
 * detect: upper_wick >= 0.66*range AND body in lower third
 * context_required: wick pierces resistance and closes back below it
 */
class KnowledgePackPinBarBearDetector : KnowledgePackPatternDetector {
    override val id: String = "pin_bar_bear"
    override val name: String = "Bearish Pin Bar"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.isEmpty()) return matches

        for (i in candles.indices) {
            val c = candles[i]
            val upperWickOk = c.upperWick >= (0.66f * c.range)
            val bodyInLowerThird = c.bodyTop >= (c.lowY - (c.range * 0.35f))

            if (upperWickOk && bodyInLowerThird) {
                val piercedResistance = srZones.filter { !it.isSupport }
                    .any { it.containsPrice(c.highY) || (c.highY <= it.yLevel && c.closeY >= it.yLevel) }

                val weight = if (piercedResistance) 0.12f else 0.0f
                if (piercedResistance || i == candles.lastIndex) {
                    matches.add(
                        PatternMatchResult(
                            patternId = id,
                            patternName = name,
                            candleIndex = i,
                            x = c.centerX,
                            y = c.highY,
                            direction = "DOWN",
                            confidence = if (piercedResistance) 0.88f else 0.50f,
                            weight = weight,
                            quality = 0.90f,
                            hasContext = piercedResistance,
                            description = "Bearish Pin Bar: 66%+ upper wick piercing resistance and closing below"
                        )
                    )
                }
            }
        }
        return matches
    }
}

/**
 * 9. Tweezer Bottom (UP)
 * detect: two candles with lows within 0.05*ATR, first down second up
 * context_required: near support
 */
class KnowledgePackTweezerBottomDetector : KnowledgePackPatternDetector {
    override val id: String = "tweezer_bottom"
    override val name: String = "Tweezer Bottom"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val c1 = candles[i - 1]
            val c2 = candles[i]

            val sameLows = abs(c1.lowY - c2.lowY) <= (0.05f * atr).coerceAtLeast(3f)
            val correctColors = !c1.isBullish && c2.isBullish

            if (sameLows && correctColors) {
                val nearSupport = isNearZone(c2.lowY, srZones, atr, isSupport = true)
                val weight = if (nearSupport) 0.08f else 0.0f

                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = name,
                        candleIndex = i,
                        x = c2.centerX,
                        y = c2.lowY,
                        direction = "UP",
                        confidence = if (nearSupport) 0.82f else 0.50f,
                        weight = weight,
                        quality = 0.85f,
                        hasContext = nearSupport,
                        description = "Tweezer Bottom: Matching lows within 0.05*ATR rejected at Support"
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 10. Tweezer Top (DOWN)
 * detect: two candles with highs within 0.05*ATR, first up second down
 * context_required: near resistance
 */
class KnowledgePackTweezerTopDetector : KnowledgePackPatternDetector {
    override val id: String = "tweezer_top"
    override val name: String = "Tweezer Top"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val c1 = candles[i - 1]
            val c2 = candles[i]

            val sameHighs = abs(c1.highY - c2.highY) <= (0.05f * atr).coerceAtLeast(3f)
            val correctColors = c1.isBullish && !c2.isBullish

            if (sameHighs && correctColors) {
                val nearResistance = isNearZone(c2.highY, srZones, atr, isSupport = false)
                val weight = if (nearResistance) 0.08f else 0.0f

                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = name,
                        candleIndex = i,
                        x = c2.centerX,
                        y = c2.highY,
                        direction = "DOWN",
                        confidence = if (nearResistance) 0.82f else 0.50f,
                        weight = weight,
                        quality = 0.85f,
                        hasContext = nearResistance,
                        description = "Tweezer Top: Matching highs within 0.05*ATR rejected at Resistance"
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 11. Inside Bar (BREAKOUT)
 * detect: current high < prev high AND current low > prev low (in price space)
 * Screen Y: curr.highY > prev.highY AND curr.lowY < prev.lowY
 * context_required: trade only the break of mother bar in direction of higher timeframe trend
 */
class KnowledgePackInsideBarDetector : KnowledgePackPatternDetector {
    override val id: String = "inside_bar"
    override val name: String = "Inside Bar"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 2) return matches

        for (i in 1 until candles.size) {
            val mother = candles[i - 1]
            val inside = candles[i]

            // In screen coordinates: smaller Y = higher price
            val isInsideHigh = inside.highY > mother.highY
            val isInsideLow = inside.lowY < mother.lowY

            if (isInsideHigh && isInsideLow) {
                val dir = when (trend) {
                    TrendDirection.UPTREND -> "UP"
                    TrendDirection.DOWNTREND -> "DOWN"
                    TrendDirection.SIDEWAYS -> "NEUTRAL"
                }
                val hasContext = trend != TrendDirection.SIDEWAYS
                val weight = if (hasContext) 0.07f else 0.0f

                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = name,
                        candleIndex = i,
                        x = inside.centerX,
                        y = inside.bodyTop,
                        direction = dir,
                        confidence = if (hasContext) 0.75f else 0.55f,
                        weight = weight,
                        quality = 0.80f,
                        hasContext = hasContext,
                        description = "Inside Bar: Volatility contraction inside mother bar; trade trend breakout"
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 12. Doji (NEUTRAL)
 * detect: body_ratio <= 0.1
 * effect: Reduces confidence of continuation by 0.05; adds 0.03 to reversal signals at S/R. Never a signal alone.
 */
class KnowledgePackDojiDetector : KnowledgePackPatternDetector {
    override val id: String = "doji"
    override val name: String = "Doji"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.isEmpty()) return matches

        for (i in candles.indices) {
            val c = candles[i]
            if (c.bodyRatio <= 0.10f && c.range >= 5f) {
                val nearSr = isNearZone(c.closeY, srZones, atr, isSupport = true) ||
                        isNearZone(c.closeY, srZones, atr, isSupport = false)

                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = name,
                        candleIndex = i,
                        x = c.centerX,
                        y = c.bodyTop,
                        direction = "NEUTRAL",
                        confidence = 0.50f,
                        weight = if (nearSr) 0.03f else -0.05f,
                        quality = 0.70f,
                        hasContext = nearSr,
                        description = "Doji: Indecision bar (body <= 10% range). Modifies reversal / continuation confluence."
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 13. Marubozu (WITH_CANDLE)
 * detect: body_ratio >= 0.85
 * context_required: closes beyond a level (breakout) OR trend continuation
 */
class KnowledgePackMarubozuDetector : KnowledgePackPatternDetector {
    override val id: String = "marubozu"
    override val name: String = "Marubozu"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.isEmpty()) return matches

        for (i in candles.indices) {
            val c = candles[i]
            if (c.bodyRatio >= 0.85f && c.range >= (0.6f * atr)) {
                val dir = if (c.isBullish) "UP" else "DOWN"
                val alignedTrend = (dir == "UP" && trend == TrendDirection.UPTREND) ||
                        (dir == "DOWN" && trend == TrendDirection.DOWNTREND)

                val weight = if (alignedTrend) 0.08f else 0.0f
                matches.add(
                    PatternMatchResult(
                        patternId = id,
                        patternName = "Marubozu (Momentum)",
                        candleIndex = i,
                        x = c.centerX,
                        y = if (c.isBullish) c.closeY else c.openY,
                        direction = dir,
                        confidence = if (alignedTrend) 0.84f else 0.60f,
                        weight = weight,
                        quality = 0.90f,
                        hasContext = alignedTrend,
                        description = "Marubozu: Dominant momentum body (>= 85% range) driving trend"
                    )
                )
            }
        }
        return matches
    }
}

/**
 * 14. Three Soldiers / Three Crows (WITH_CANDLES)
 * detect: 3 consecutive same-color candles, each opening within previous body and closing near its high/low
 * context_required: not already extended more than 3*ATR
 */
class KnowledgePackThreeSoldiersDetector : KnowledgePackPatternDetector {
    override val id: String = "three_soldiers"
    override val name: String = "Three Soldiers/Crows"

    override fun detect(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        val matches = mutableListOf<PatternMatchResult>()
        if (candles.size < 3) return matches

        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val c2 = candles[i - 1]
            val c3 = candles[i]

            val allBullish = c1.isBullish && c2.isBullish && c3.isBullish
            val allBearish = !c1.isBullish && !c2.isBullish && !c3.isBullish

            if (allBullish) {
                val opensWithinBody = c2.openY in c1.closeY..c1.openY && c3.openY in c2.closeY..c2.openY
                val closesNearHigh = c1.upperWick <= 0.2f * c1.range &&
                        c2.upperWick <= 0.2f * c2.range &&
                        c3.upperWick <= 0.2f * c3.range

                if (opensWithinBody && closesNearHigh) {
                    val totalExtension = abs(c3.closeY - c1.openY)
                    val notExtended = totalExtension <= (3f * atr)
                    val weight = if (notExtended) 0.08f else 0.02f

                    matches.add(
                        PatternMatchResult(
                            patternId = id,
                            patternName = "Three White Soldiers",
                            candleIndex = i,
                            x = c3.centerX,
                            y = c3.closeY,
                            direction = "UP",
                            confidence = if (notExtended) 0.86f else 0.65f,
                            weight = weight,
                            quality = 0.90f,
                            hasContext = notExtended,
                            description = "Three White Soldiers: Consecutive advancing green candles"
                        )
                    )
                }
            } else if (allBearish) {
                val opensWithinBody = c2.openY in c1.openY..c1.closeY && c3.openY in c2.openY..c2.closeY
                val closesNearLow = c1.lowerWick <= 0.2f * c1.range &&
                        c2.lowerWick <= 0.2f * c2.range &&
                        c3.lowerWick <= 0.2f * c3.range

                if (opensWithinBody && closesNearLow) {
                    val totalExtension = abs(c3.closeY - c1.openY)
                    val notExtended = totalExtension <= (3f * atr)
                    val weight = if (notExtended) 0.08f else 0.02f

                    matches.add(
                        PatternMatchResult(
                            patternId = id,
                            patternName = "Three Black Crows",
                            candleIndex = i,
                            x = c3.centerX,
                            y = c3.closeY,
                            direction = "DOWN",
                            confidence = if (notExtended) 0.86f else 0.65f,
                            weight = weight,
                            quality = 0.90f,
                            hasContext = notExtended,
                            description = "Three Black Crows: Consecutive falling red candles"
                        )
                    )
                }
            }
        }
        return matches
    }
}

/**
 * Composite engine organizing all 14 Knowledge Pack pattern detectors.
 */
object KnowledgePackPatternEngine {
    val detectors: List<KnowledgePackPatternDetector> = listOf(
        KnowledgePackHammerDetector(),
        KnowledgePackShootingStarDetector(),
        KnowledgePackBullEngulfingDetector(),
        KnowledgePackBearEngulfingDetector(),
        KnowledgePackMorningStarDetector(),
        KnowledgePackEveningStarDetector(),
        KnowledgePackPinBarBullDetector(),
        KnowledgePackPinBarBearDetector(),
        KnowledgePackTweezerBottomDetector(),
        KnowledgePackTweezerTopDetector(),
        KnowledgePackInsideBarDetector(),
        KnowledgePackDojiDetector(),
        KnowledgePackMarubozuDetector(),
        KnowledgePackThreeSoldiersDetector()
    )

    fun detectAll(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trend: TrendDirection,
        atr: Float
    ): List<PatternMatchResult> {
        return detectors.flatMap { it.detect(candles, swings, srZones, trend, atr) }
    }
}
