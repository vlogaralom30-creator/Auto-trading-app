package com.example.engine

import com.example.model.Candle
import com.example.model.SupportResistanceZone
import com.example.model.SwingPoint
import com.example.model.TrendDirection
import com.example.model.TrendLine
import kotlin.math.abs

/**
 * Technical Structure Engine: ATR, Swings, S/R Zones, Trend Analysis, Breakouts/Retests
 * Built strictly according to ChartMind Knowledge Pack schema.
 */
object StructureEngine {

    /**
     * Calculates ATR: average range of last 14 candles.
     */
    fun calculateAtr(candles: List<Candle>, period: Int = 14): Float {
        if (candles.isEmpty()) return 15f
        val window = candles.takeLast(minOf(period, candles.size))
        return (window.map { it.range }.average().toFloat()).coerceAtLeast(4f)
    }

    /**
     * Detects swing highs and swing lows with swing_strength = 3 lookaround.
     */
    fun detectSwings(candles: List<Candle>, lookaround: Int = 3): List<SwingPoint> {
        val actualLookaround = if (candles.size >= 15) lookaround else 2
        if (candles.size < (actualLookaround * 2 + 1)) return emptyList()

        val swings = mutableListOf<SwingPoint>()

        for (i in actualLookaround until (candles.size - actualLookaround)) {
            val current = candles[i]
            var isSwingHigh = true
            var isSwingLow = true

            for (offset in 1..actualLookaround) {
                val left = candles[i - offset]
                val right = candles[i + offset]

                // Screen coordinates: smaller Y = higher price
                if (current.highY >= left.highY || current.highY >= right.highY) {
                    isSwingHigh = false
                }
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
                        strength = actualLookaround
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
                        strength = actualLookaround
                    )
                )
            }
        }

        return swings
    }

    /**
     * Support & Resistance per Knowledge Pack:
     * - Cluster swing highs/lows whose prices are within 0.25 * ATR
     * - min_touches = 2
     * - zone_half_height = 0.2 * ATR
     * - strength = touches + 0.5 * rejections(wick > 50% of range)
     */
    fun detectZones(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        atr: Float
    ): List<SupportResistanceZone> {
        if (swings.isEmpty()) return emptyList()

        val clusterTolerance = maxOf(4f, 0.25f * atr)
        val zoneHalfHeight = maxOf(4f, 0.20f * atr)
        val zones = mutableListOf<SupportResistanceZone>()

        // Cluster swing lows for Support zones
        val lows = swings.filter { !it.isHigh }.sortedBy { it.y }
        clusterSwings(candles, lows, clusterTolerance, zoneHalfHeight, isSupport = true, zones)

        // Cluster swing highs for Resistance zones
        val highs = swings.filter { it.isHigh }.sortedBy { it.y }
        clusterSwings(candles, highs, clusterTolerance, zoneHalfHeight, isSupport = false, zones)

        return zones.sortedByDescending { it.strengthScore }
    }

    private fun clusterSwings(
        candles: List<Candle>,
        points: List<SwingPoint>,
        tolerance: Float,
        zoneHalfHeight: Float,
        isSupport: Boolean,
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

            if (cluster.size >= 2) {
                val avgY = cluster.map { it.y }.average().toFloat()
                val thickness = maxOf(zoneHalfHeight * 2f, (cluster.maxOf { it.y } - cluster.minOf { it.y }) + 4f)

                // Count rejection wicks (> 50% of range) near this level
                val rejections = candles.count { c ->
                    val near = abs((if (isSupport) c.lowY else c.highY) - avgY) <= tolerance * 1.5f
                    val wickOver50 = (if (isSupport) c.lowerWick else c.upperWick) >= (0.50f * c.range)
                    near && wickOver50
                }

                // strength = touches + 0.5 * rejections
                val rawStrength = cluster.size + (0.5f * rejections)
                val normalizedStrength = (rawStrength / 5.0f).coerceIn(0.4f, 1.0f)

                outZones.add(
                    SupportResistanceZone(
                        yLevel = avgY,
                        thickness = thickness,
                        isSupport = isSupport,
                        touchCount = cluster.size,
                        strengthScore = normalizedStrength,
                        rejectionsCount = rejections
                    )
                )
            }
        }
    }

    /**
     * Evaluates Trend Direction per Knowledge Pack trend_rules:
     * - trend_up: last 2 swing highs rising (lower Y) AND last 2 swing lows rising (lower Y)
     * - trend_down: last 2 swing highs falling (higher Y) AND last 2 swing lows falling (higher Y)
     * - sideways: swing highs and lows within 0.8*ATR band for 10+ candles
     */
    fun evaluateTrend(
        candles: List<Candle>,
        swings: List<SwingPoint>,
        atr: Float
    ): TrendDirection {
        if (candles.size < 6) return TrendDirection.SIDEWAYS

        // Check sideways condition: range of swings in last 10 candles <= 0.8 * ATR
        val recentSwings = swings.takeLast(6)
        if (recentSwings.size >= 3) {
            val minY = recentSwings.minOf { it.y }
            val maxY = recentSwings.maxOf { it.y }
            if ((maxY - minY) <= (0.8f * atr)) {
                return TrendDirection.SIDEWAYS
            }
        }

        // Swings progression check
        val swingHighs = swings.filter { it.isHigh }.takeLast(2)
        val swingLows = swings.filter { !it.isHigh }.takeLast(2)

        if (swingHighs.size == 2 && swingLows.size == 2) {
            // Rising in price means smaller Y in screen coordinates
            val higherHighs = swingHighs[1].y < swingHighs[0].y
            val higherLows = swingLows[1].y < swingLows[0].y

            val lowerHighs = swingHighs[1].y > swingHighs[0].y
            val lowerLows = swingLows[1].y > swingLows[0].y

            if (higherHighs && higherLows) return TrendDirection.UPTREND
            if (lowerHighs && lowerLows) return TrendDirection.DOWNTREND
        }

        // Fallback to EMA / candle slope over lookback 10
        val lookback = minOf(10, candles.size)
        val firstClose = candles[candles.size - lookback].closeY
        val lastClose = candles.last().closeY
        val diffY = lastClose - firstClose

        return when {
            diffY < -(0.75f * atr) -> TrendDirection.UPTREND
            diffY > (0.75f * atr) -> TrendDirection.DOWNTREND
            else -> TrendDirection.SIDEWAYS
        }
    }

    /**
     * Checks for Breakouts, Retests, and Fakeouts:
     * - breakout: 2 consecutive closes beyond level by >= 0.3*ATR
     * - fakeout: wick beyond level by >= 0.2*ATR but close back inside the zone
     * - retest: after breakout, price returns to the level and holds = continuation
     */
    fun detectBreakoutAndFakeout(
        candles: List<Candle>,
        srZones: List<SupportResistanceZone>,
        atr: Float
    ): Pair<String?, Float> {
        if (candles.size < 3) return Pair(null, 0f)

        val latest = candles.last()
        val prev = candles[candles.size - 2]
        val breakoutMargin = 0.30f * atr
        val fakeoutMargin = 0.20f * atr

        for (zone in srZones) {
            if (!zone.isSupport) {
                // Resistance level
                val brokenUp = latest.closeY < (zone.yLevel - breakoutMargin) && prev.closeY < (zone.yLevel - breakoutMargin)
                if (brokenUp) {
                    return Pair("Breakout above Resistance (${zone.touchCount}x)", 0.10f)
                }

                // Fakeout above resistance
                val piercedUp = latest.highY < (zone.yLevel - fakeoutMargin) && latest.closeY >= zone.yLevel
                if (piercedUp) {
                    return Pair("Fakeout above Resistance (Bearish Rejection)", -0.11f)
                }
            } else {
                // Support level
                val brokenDown = latest.closeY > (zone.yLevel + breakoutMargin) && prev.closeY > (zone.yLevel + breakoutMargin)
                if (brokenDown) {
                    return Pair("Breakdown below Support (${zone.touchCount}x)", -0.10f)
                }

                // Fakeout below support
                val piercedDown = latest.lowY > (zone.yLevel + fakeoutMargin) && latest.closeY <= zone.yLevel
                if (piercedDown) {
                    return Pair("Fakeout below Support (Bullish Rejection)", 0.11f)
                }
            }
        }

        return Pair(null, 0f)
    }

    /**
     * Computes linear regression trendlines for overlay.
     */
    fun fitTrendlines(swings: List<SwingPoint>): List<TrendLine> {
        val lines = mutableListOf<TrendLine>()
        val lows = swings.filter { !it.isHigh }
        val highs = swings.filter { it.isHigh }

        if (lows.size >= 2) {
            lines.add(fitLine(lows, isSupport = true, label = "Support Trendline"))
        }
        if (highs.size >= 2) {
            lines.add(fitLine(highs, isSupport = false, label = "Resistance Trendline"))
        }
        return lines
    }

    private fun fitLine(points: List<SwingPoint>, isSupport: Boolean, label: String): TrendLine {
        val n = points.size.toFloat()
        val sumX = points.sumOf { it.x.toDouble() }.toFloat()
        val sumY = points.sumOf { it.y.toDouble() }.toFloat()
        val sumXY = points.sumOf { (it.x * it.y).toDouble() }.toFloat()
        val sumX2 = points.sumOf { (it.x * it.x).toDouble() }.toFloat()

        val denominator = (n * sumX2 - sumX * sumX)
        val slope = if (abs(denominator) > 1e-4) (n * sumXY - sumX * sumY) / denominator else 0f
        val intercept = (sumY - slope * sumX) / n

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x } + 60f

        val startY = slope * minX + intercept
        val endY = slope * maxX + intercept

        return TrendLine(
            startX = minX,
            startY = startY,
            endX = maxX,
            endY = endY,
            slope = slope,
            isSupport = isSupport,
            label = label
        )
    }
}
