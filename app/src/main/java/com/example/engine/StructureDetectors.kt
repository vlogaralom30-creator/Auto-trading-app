package com.example.engine

import com.example.model.Candle
import com.example.model.SupportResistanceZone
import com.example.model.SwingPoint
import com.example.model.TrendDirection
import com.example.model.TrendLine
import kotlin.math.abs

/**
 * Calculates volatility and technical indicators per Knowledge Pack specifications.
 */
object IndicatorCalculator {

    fun calculateAtr(candles: List<Candle>, period: Int = 14): Float {
        if (candles.isEmpty()) return 10f
        val window = candles.takeLast(period)
        return window.map { it.range }.average().toFloat().coerceAtLeast(4f)
    }

    fun calculateEma(candles: List<Candle>, period: Int): List<Pair<Float, Float>> {
        if (candles.size < 2) return emptyList()
        val result = mutableListOf<Pair<Float, Float>>()
        val k = 2f / (period + 1f)

        // Seed with SMA
        val seedCount = minOf(candles.size, period)
        var currentEma = candles.take(seedCount).map { it.closeY }.average().toFloat()
        result.add(Pair(candles[seedCount - 1].centerX, currentEma))

        for (i in seedCount until candles.size) {
            val c = candles[i]
            currentEma = (c.closeY * k) + (currentEma * (1f - k))
            result.add(Pair(c.centerX, currentEma))
        }
        return result
    }

    fun calculateRsi(candles: List<Candle>, period: Int = 14): Float? {
        if (candles.size <= period) return null
        var gains = 0f
        var losses = 0f

        val recent = candles.takeLast(period + 1)
        for (i in 1 until recent.size) {
            val prevClose = recent[i - 1].closeY
            val currClose = recent[i].closeY
            // Screen Y: smaller Y = higher price (gain)
            val diff = prevClose - currClose
            if (diff > 0) gains += diff else losses += abs(diff)
        }

        if (losses == 0f) return 100f
        val rs = (gains / period) / (losses / period)
        return 100f - (100f / (1f + rs))
    }

    fun calculateMomentum(candles: List<Candle>): Float {
        if (candles.size < 5) return 0f
        val recent = candles.takeLast(5)
        var netY = 0f
        for (i in 1 until recent.size) {
            netY += (recent[i - 1].closeY - recent[i].closeY)
        }
        val atr = calculateAtr(candles)
        return (netY / (atr * 5f)).coerceIn(-1f, 1f)
    }
}

/**
 * Detects swing highs and swing lows across candlesticks per swing_strength (3).
 */
object SwingDetector {
    fun detectSwings(candles: List<Candle>, lookaround: Int = 3): List<SwingPoint> {
        val effectiveLookaround = if (candles.size >= 15) lookaround else 2
        if (candles.size < (effectiveLookaround * 2 + 1)) return emptyList()

        val swings = mutableListOf<SwingPoint>()

        for (i in effectiveLookaround until (candles.size - effectiveLookaround)) {
            val current = candles[i]

            // In screen coordinates: smaller Y = higher price
            var isSwingHigh = true
            var isSwingLow = true

            for (offset in 1..effectiveLookaround) {
                val left = candles[i - offset]
                val right = candles[i + offset]

                // For swing high: current high price must be higher than neighbors -> current.highY < neighbor.highY
                if (current.highY >= left.highY || current.highY >= right.highY) {
                    isSwingHigh = false
                }
                // For swing low: current low price must be lower than neighbors -> current.lowY > neighbor.lowY
                if (current.lowY <= left.lowY || current.lowY <= right.lowY) {
                    isSwingLow = false
                }
            }

            if (isSwingHigh) {
                swings.add(
                    SwingPoint(
                        candleIndex = i,
                        x = current.centerX,
                        y = current.highY,
                        isHigh = true,
                        strength = effectiveLookaround
                    )
                )
            }
            if (isSwingLow) {
                swings.add(
                    SwingPoint(
                        candleIndex = i,
                        x = current.centerX,
                        y = current.lowY,
                        isHigh = false,
                        strength = effectiveLookaround
                    )
                )
            }
        }

        return swings
    }
}

/**
 * Implements Support & Resistance per Knowledge Pack support_resistance rules:
 * - build: cluster swing highs/lows whose prices are within 0.25*ATR
 * - min_touches: 2
 * - zone_half_height: 0.2*ATR
 * - strength: touches + 0.5 * rejections(wick > 50% of range)
 * - near_level: abs(price - level) <= 0.5*ATR
 * - breakout: 2 consecutive closes beyond level by >= 0.3*ATR
 * - fakeout: wick beyond level by >= 0.2*ATR but close back inside zone
 * - retest: after breakout, price returns to level and holds = continuation (+0.08)
 */
object SupportResistanceDetector {

    fun detectZones(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        atr: Float
    ): List<SupportResistanceZone> {
        if (swings.isEmpty()) return emptyList()

        val clusterTolerance = (0.25f * atr).coerceAtLeast(6f)
        val zoneHalfHeight = (0.20f * atr).coerceAtLeast(4f)
        val zones = mutableListOf<SupportResistanceZone>()

        // Cluster swing lows for Support
        val lows = swings.filter { !it.isHigh }.sortedBy { it.y }
        clusterSwings(lows, clusterTolerance, zoneHalfHeight, isSupport = true, candles, atr, zones)

        // Cluster swing highs for Resistance
        val highs = swings.filter { it.isHigh }.sortedBy { it.y }
        clusterSwings(highs, clusterTolerance, zoneHalfHeight, isSupport = false, candles, atr, zones)

        return zones.sortedByDescending { it.strengthScore }
    }

    private fun clusterSwings(
        points: List<SwingPoint>,
        tolerance: Float,
        zoneHalfHeight: Float,
        isSupport: Boolean,
        candles: List<Candle>,
        atr: Float,
        outZones: MutableList<SupportResistanceZone>
    ) {
        val visited = BooleanArray(points.size)

        for (i in points.indices) {
            if (visited[i]) continue
            val cluster = mutableListOf(points[i])
            visited[i] = true

            for (j in (i + 1) until points.size) {
                if (!visited[j] && abs(points[j].y - points[i].y) <= tolerance) {
                    cluster.add(points[j])
                    visited[j] = true
                }
            }

            // min_touches: 2
            if (cluster.size >= 2) {
                val avgY = cluster.map { it.y }.average().toFloat()

                // Calculate rejections: candles whose wick > 50% of range near this zone
                var rejections = 0
                for (candle in candles) {
                    val inZone = abs(candle.closeY - avgY) <= (0.5f * atr) ||
                            (isSupport && abs(candle.lowY - avgY) <= (0.5f * atr)) ||
                            (!isSupport && abs(candle.highY - avgY) <= (0.5f * atr))

                    if (inZone) {
                        if (isSupport && candle.lowerWickRatio >= 0.50f) {
                            rejections++
                        } else if (!isSupport && candle.upperWickRatio >= 0.50f) {
                            rejections++
                        }
                    }
                }

                // strength: touches + 0.5 * rejections
                val strengthScore = cluster.size + (0.5f * rejections)

                outZones.add(
                    SupportResistanceZone(
                        yLevel = avgY,
                        halfHeight = zoneHalfHeight,
                        isSupport = isSupport,
                        touchCount = cluster.size,
                        rejectionCount = rejections,
                        strengthScore = strengthScore
                    )
                )
            }
        }
    }

    fun evaluateBreakoutRetestFakeout(
        candles: List<Candle>,
        zones: List<SupportResistanceZone>,
        atr: Float
    ): Triple<String?, Boolean, Boolean> {
        if (candles.size < 3 || zones.isEmpty()) return Triple(null, false, false)

        val latest = candles.last()
        val prev1 = candles[candles.size - 2]
        val prev2 = candles[candles.size - 3]

        for (zone in zones) {
            // breakout: 2 consecutive closes beyond level by >= 0.3*ATR
            if (!zone.isSupport) {
                // Resistance breakout (above level = smaller Y)
                val isBreakoutUp = (prev1.closeY <= zone.yLevel - 0.3f * atr) &&
                        (latest.closeY <= zone.yLevel - 0.3f * atr)
                if (isBreakoutUp) {
                    // Check retest holding: prior breakout, now price returns to level and holds
                    val isRetest = (latest.lowY in (zone.yLevel - 0.4f * atr)..(zone.yLevel + 0.4f * atr)) &&
                            latest.closeY <= zone.yLevel
                    if (isRetest) {
                        return Triple("Retest of broken Resistance holding as Support (+0.08 weight)", true, false)
                    }
                    return Triple("Bullish Breakout: 2 consecutive closes above Resistance", true, false)
                }

                // fakeout: wick beyond level by >= 0.2*ATR but close back inside the zone
                val isFakeoutUp = (latest.highY <= zone.yLevel - 0.2f * atr) &&
                        (latest.closeY >= zone.yLevel - zone.halfHeight)
                if (isFakeoutUp) {
                    return Triple("Fakeout above Resistance rejected, closing back inside zone (+0.11 weight)", false, true)
                }
            } else {
                // Support breakdown (below level = larger Y)
                val isBreakoutDown = (prev1.closeY >= zone.yLevel + 0.3f * atr) &&
                        (latest.closeY >= zone.yLevel + 0.3f * atr)
                if (isBreakoutDown) {
                    val isRetest = (latest.highY in (zone.yLevel - 0.4f * atr)..(zone.yLevel + 0.4f * atr)) &&
                            latest.closeY >= zone.yLevel
                    if (isRetest) {
                        return Triple("Retest of broken Support holding as Resistance (+0.08 weight)", true, false)
                    }
                    return Triple("Bearish Breakdown: 2 consecutive closes below Support", true, false)
                }

                // fakeout: wick beyond level by >= 0.2*ATR but close back inside the zone
                val isFakeoutDown = (latest.lowY >= zone.yLevel + 0.2f * atr) &&
                        (latest.closeY <= zone.yLevel + zone.halfHeight)
                if (isFakeoutDown) {
                    return Triple("Fakeout below Support rejected, closing back inside zone (+0.11 weight)", false, true)
                }
            }
        }

        return Triple(null, false, false)
    }
}

/**
 * Implements Trend detection per Knowledge Pack trend_rules:
 * - trend_up: last 2 swing highs rising AND last 2 swing lows rising
 * - trend_down: last 2 swing highs falling AND last 2 swing lows falling
 * - sideways: swing highs and lows within 0.8*ATR band for 10+ candles
 * - ema_bias: EMA20 > EMA50 and price > EMA20 = UP bias (+0.05 weight); reverse = DOWN bias
 */
object TrendDetector {

    data class TrendEvaluation(
        val direction: TrendDirection,
        val trendLines: List<TrendLine>,
        val emaBiasWeight: Float, // +0.05 for UP, -0.05 for DOWN
        val isSidewaysMiddle50: Boolean,
        val description: String
    )

    fun evaluate(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        atr: Float,
        ema20: List<Pair<Float, Float>>,
        ema50: List<Pair<Float, Float>>
    ): TrendEvaluation {
        if (candles.isEmpty()) {
            return TrendEvaluation(TrendDirection.SIDEWAYS, emptyList(), 0f, false, "Insufficient candles")
        }

        val swingHighs = swings.filter { it.isHigh }
        val swingLows = swings.filter { !it.isHigh }

        // 1. Check sideways: swing highs and lows within 0.8*ATR band for 10+ candles
        val last10Candles = candles.takeLast(10)
        val highestHigh10 = last10Candles.minOfOrNull { it.highY } ?: 0f
        val lowestLow10 = last10Candles.maxOfOrNull { it.lowY } ?: 0f
        val isSidewaysBand = (lowestLow10 - highestHigh10) <= (0.8f * atr * 1.5f)

        // Middle 50% check of range
        val latestCandle = candles.last()
        val range10 = lowestLow10 - highestHigh10
        val distFromTop = latestCandle.closeY - highestHigh10
        val isMiddle50 = range10 > 0 && (distFromTop / range10) in 0.25f..0.75f

        // 2. Check swing rules
        // In screen Y: smaller Y = higher price!
        // Rising swing highs: second swing high has smaller Y than first
        val isHighsRising = if (swingHighs.size >= 2) {
            val hPrev = swingHighs[swingHighs.size - 2]
            val hCurr = swingHighs[swingHighs.size - 1]
            hCurr.y < hPrev.y
        } else false

        val isLowsRising = if (swingLows.size >= 2) {
            val lPrev = swingLows[swingLows.size - 2]
            val lCurr = swingLows[swingLows.size - 1]
            lCurr.y < lPrev.y
        } else false

        val isHighsFalling = if (swingHighs.size >= 2) {
            val hPrev = swingHighs[swingHighs.size - 2]
            val hCurr = swingHighs[swingHighs.size - 1]
            hCurr.y > hPrev.y
        } else false

        val isLowsFalling = if (swingLows.size >= 2) {
            val lPrev = swingLows[swingLows.size - 2]
            val lCurr = swingLows[swingLows.size - 1]
            lCurr.y > lPrev.y
        } else false

        val direction = when {
            isHighsRising && isLowsRising -> TrendDirection.UPTREND
            isHighsFalling && isLowsFalling -> TrendDirection.DOWNTREND
            isSidewaysBand -> TrendDirection.SIDEWAYS
            isHighsRising || isLowsRising -> TrendDirection.UPTREND
            isHighsFalling || isLowsFalling -> TrendDirection.DOWNTREND
            else -> TrendDirection.SIDEWAYS
        }

        // 3. EMA bias: EMA20 > EMA50 and price > EMA20 = UP bias (+0.05); reverse = DOWN bias (-0.05)
        var emaBiasWeight = 0f
        if (ema20.isNotEmpty() && ema50.isNotEmpty()) {
            val latestEma20 = ema20.last().second
            val latestEma50 = ema50.last().second
            val latestPrice = latestCandle.closeY

            // In screen coordinates: smaller Y = higher price
            if (latestEma20 < latestEma50 && latestPrice < latestEma20) {
                emaBiasWeight = 0.05f // Bullish bias
            } else if (latestEma20 > latestEma50 && latestPrice > latestEma20) {
                emaBiasWeight = -0.05f // Bearish bias
            }
        }

        // Generate trendlines for overlay
        val trendLines = mutableListOf<TrendLine>()
        if (swingHighs.size >= 2) {
            val p1 = swingHighs[swingHighs.size - 2]
            val p2 = swingHighs[swingHighs.size - 1]
            val slope = if (p2.x != p1.x) (p2.y - p1.y) / (p2.x - p1.x) else 0f
            trendLines.add(
                TrendLine(p1.x, p1.y, p2.x, p2.y, slope, isSupport = false, label = "Upper Swing Trendline")
            )
        }
        if (swingLows.size >= 2) {
            val p1 = swingLows[swingLows.size - 2]
            val p2 = swingLows[swingLows.size - 1]
            val slope = if (p2.x != p1.x) (p2.y - p1.y) / (p2.x - p1.x) else 0f
            trendLines.add(
                TrendLine(p1.x, p1.y, p2.x, p2.y, slope, isSupport = true, label = "Lower Swing Trendline")
            )
        }

        val desc = when (direction) {
            TrendDirection.UPTREND -> "Dominant UPTREND (Last 2 swing highs & lows rising)"
            TrendDirection.DOWNTREND -> "Dominant DOWNTREND (Last 2 swing highs & lows falling)"
            TrendDirection.SIDEWAYS -> "SIDEWAYS Consolidation (Swings within 0.8*ATR band)"
        }

        return TrendEvaluation(
            direction = direction,
            trendLines = trendLines,
            emaBiasWeight = emaBiasWeight,
            isSidewaysMiddle50 = direction == TrendDirection.SIDEWAYS && isMiddle50,
            description = desc
        )
    }
}
