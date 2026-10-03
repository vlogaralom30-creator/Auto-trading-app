package com.example.engine

import com.example.data.entity.RuleEntity
import com.example.model.Candle
import com.example.model.ChartAnalysisResult
import com.example.model.PatternMatchResult
import com.example.model.SupportResistanceZone
import com.example.model.SwingPoint
import com.example.model.TrendDirection
import com.example.model.TrendLine
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Confluence Scoring & No-Trade Filter Engine
 * Built strictly per ChartMind Knowledge Pack specification.
 */
object RuleMatchingEngine {

    private const val CONFIDENCE_BASE = 0.50
    private const val CONFIDENCE_MAX_CAP = 0.78f
    private const val MIN_TO_SIGNAL = 0.60f
    private const val LOGISTIC_K = 6.0

    data class ScoredComponent(
        val name: String,
        val description: String,
        val signedContribution: Double,
        val absMagnitude: Double
    )

    fun evaluateChart(
        bitmapWidth: Int,
        bitmapHeight: Int,
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trendLines: List<TrendLine>,
        trendDirection: TrendDirection,
        breakoutHint: String?,
        breakoutScore: Float = 0f,
        patterns: List<PatternMatchResult>,
        ema9: List<Pair<Float, Float>>,
        ema21: List<Pair<Float, Float>>,
        rsi: Float?,
        atr: Float,
        momentum: Float,
        activeRules: List<RuleEntity>,
        timeframe: String = "1m",
        higherTfTrend: String = "ANY",
        m5Trend: String = "ANY",
        consecutiveLosses: Int = 0,
        dailySignalsUsed: Int = 0,
        dailySignalLimit: Int = 10,
        isUserNewsWindow: Boolean = false,
        executionDurationMs: Long = 0L
    ): ChartAnalysisResult {
        // Base case: empty or invalid
        if (candles.isEmpty()) {
            return ChartAnalysisResult(
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight,
                overallSignal = "NEUTRAL",
                confidenceScore = 0f,
                reasonList = listOf("No distinct candles detected in chart viewport"),
                analysisTimeMs = executionDurationMs
            )
        }

        val latestCandle = candles.last()
        val components = mutableListOf<ScoredComponent>()

        // -------------------------------------------------------------
        // Check "no_trade_filters" FIRST
        // -------------------------------------------------------------
        var blockReason: String? = null

        // 1. Fewer than 30 candles visible (or fewer than 15 in prototype)
        if (candles.size < 20) {
            blockReason = "Fewer than 20 candles visible (need sufficient historical price action)"
        }

        // 2. Latest candle range > 3*ATR (news spike)
        if (blockReason == null && latestCandle.range > (3f * atr)) {
            blockReason = "Latest candle range (${latestCandle.range.toInt()}px) > 3*ATR (${(3 * atr).toInt()}px) - high-volatility news spike"
        }

        // 3. ATR below 0.3 * average ATR of last 100 candles (dead market)
        if (blockReason == null && candles.size >= 25) {
            val longTermAtr = candles.map { it.range }.average().toFloat()
            if (atr < (0.30f * longTermAtr)) {
                blockReason = "Market volatility dead: ATR (${atr.toInt()}px) < 0.3*historical average (${(0.3 * longTermAtr).toInt()}px)"
            }
        }

        // 4. Sideways trend and price in the middle 50% of the range
        if (blockReason == null && trendDirection == TrendDirection.SIDEWAYS && srZones.size >= 2) {
            val nearestSup = srZones.filter { it.isSupport }.minByOrNull { abs(it.yLevel - latestCandle.closeY) }
            val nearestRes = srZones.filter { !it.isSupport }.minByOrNull { abs(it.yLevel - latestCandle.closeY) }
            if (nearestSup != null && nearestRes != null && nearestSup.yLevel > nearestRes.yLevel) {
                val totalSpan = nearestSup.yLevel - nearestRes.yLevel
                val distFromSup = nearestSup.yLevel - latestCandle.closeY
                val ratio = distFromSup / totalSpan
                if (ratio in 0.25f..0.75f) {
                    blockReason = "Sideways trend: price is caught in the middle 50% equilibrium of the range"
                }
            }
        }

        // 5. Consecutive losses cooldown (3 losses in a row)
        if (blockReason == null && consecutiveLosses >= 3) {
            blockReason = "Cooldown enforced: 3 consecutive losses (pause to protect capital)"
        }

        // 6. Daily signal limit reached
        if (blockReason == null && dailySignalsUsed >= dailySignalLimit) {
            blockReason = "Daily signal ceiling reached ($dailySignalsUsed/$dailySignalLimit trades used today)"
        }

        // 7. User-flagged news window
        if (blockReason == null && isUserNewsWindow) {
            blockReason = "User-flagged major news event window active (trading paused)"
        }

        // -------------------------------------------------------------
        // Confluence Scoring Components:
        // score = sum(direction_sign * weight * quality)
        // -------------------------------------------------------------

        // Component 1: Pattern in context [0.0, 0.14]
        val activePattern = patterns.lastOrNull()
        if (activePattern != null && activePattern.hasContext) {
            val dirSign = when (activePattern.direction) {
                "UP" -> 1.0
                "DOWN" -> -1.0
                else -> 0.0
            }
            val quality = (activePattern.confidence).coerceIn(0.5f, 1.0f).toDouble()
            val w = activePattern.weight.toDouble().coerceIn(0.0, 0.14)
            val contrib = dirSign * w * quality
            components.add(
                ScoredComponent(
                    name = "pattern_in_context",
                    description = "${activePattern.patternName} in context (${(activePattern.confidence * 100).toInt()}% conf)",
                    signedContribution = contrib,
                    absMagnitude = abs(contrib)
                )
            )
        }

        // Component 2: Support & Resistance Position [0.0, 0.12]
        val nearThreshold = maxOf(6f, 0.50f * atr)
        val nearestSupport = srZones.filter { it.isSupport }.minByOrNull { abs(it.yLevel - latestCandle.closeY) }
        val nearestResistance = srZones.filter { !it.isSupport }.minByOrNull { abs(it.yLevel - latestCandle.closeY) }

        val supportDist = nearestSupport?.let { abs(it.yLevel - latestCandle.closeY) }
        val resistanceDist = nearestResistance?.let { abs(it.yLevel - latestCandle.closeY) }

        val isNearSupport = supportDist != null && supportDist <= nearThreshold
        val isNearResistance = resistanceDist != null && resistanceDist <= nearThreshold

        if (isNearSupport && nearestSupport != null) {
            val strength = nearestSupport.strengthScore.coerceIn(0.5f, 1.0f).toDouble()
            val contrib = 1.0 * 0.12 * strength
            components.add(
                ScoredComponent(
                    name = "support_resistance_position",
                    description = "Holding at Support Floor (${nearestSupport.touchCount}x touches, ${(nearestSupport.strengthScore * 100).toInt()}% strength)",
                    signedContribution = contrib,
                    absMagnitude = contrib
                )
            )
        } else if (isNearResistance && nearestResistance != null) {
            val strength = nearestResistance.strengthScore.coerceIn(0.5f, 1.0f).toDouble()
            val contrib = -1.0 * 0.12 * strength
            components.add(
                ScoredComponent(
                    name = "support_resistance_position",
                    description = "Testing Resistance Ceiling (${nearestResistance.touchCount}x touches, ${(nearestResistance.strengthScore * 100).toInt()}% strength)",
                    signedContribution = contrib,
                    absMagnitude = abs(contrib)
                )
            )
        }

        // Component 3: Trend alignment current TF [0.0, 0.08]
        val trendSign = when (trendDirection) {
            TrendDirection.UPTREND -> 1.0
            TrendDirection.DOWNTREND -> -1.0
            TrendDirection.SIDEWAYS -> 0.0
        }
        if (trendSign != 0.0) {
            val contrib = trendSign * 0.08
            components.add(
                ScoredComponent(
                    name = "trend_alignment_current_tf",
                    description = "Trend aligned with ${trendDirection.name} on $timeframe",
                    signedContribution = contrib,
                    absMagnitude = abs(contrib)
                )
            )
        }

        // Component 4: Higher timeframe alignment [-0.15, 0.10]
        // "If 15m trend conflicts with signal direction, subtract 0.15 from score; if 15m and 5m both agree, add 0.10."
        var htfContrib = 0.0
        if (higherTfTrend != "ANY") {
            val htfSign = if (higherTfTrend == "UP") 1.0 else if (higherTfTrend == "DOWN") -1.0 else 0.0
            if (htfSign != 0.0) {
                if (m5Trend == higherTfTrend) {
                    htfContrib = htfSign * 0.10 // Both 15m and 5m agree
                    components.add(
                        ScoredComponent(
                            name = "higher_tf_alignment",
                            description = "Multi-timeframe consensus: 15m & 5m both confirm $higherTfTrend trend (+0.10)",
                            signedContribution = htfContrib,
                            absMagnitude = abs(htfContrib)
                        )
                    )
                } else {
                    htfContrib = htfSign * 0.06
                    components.add(
                        ScoredComponent(
                            name = "higher_tf_alignment",
                            description = "15m macro trend confirms $higherTfTrend",
                            signedContribution = htfContrib,
                            absMagnitude = abs(htfContrib)
                        )
                    )
                }
            }
        }

        // Component 5: Breakout / Retest / Fakeout [0.0, 0.10]
        if (breakoutHint != null && breakoutScore != 0f) {
            val contrib = breakoutScore.toDouble().coerceIn(-0.10, 0.10)
            components.add(
                ScoredComponent(
                    name = "breakout_retest_or_fakeout",
                    description = breakoutHint,
                    signedContribution = contrib,
                    absMagnitude = abs(contrib)
                )
            )
        }

        // Component 6: Taught Rules Match [-0.20, 0.20]
        var matchedRuleName: String? = null
        for (rule in activeRules.filter { it.isEnabled }) {
            var matchesPattern = rule.patternType == "ANY" || patterns.any { it.patternName.equals(rule.patternType, ignoreCase = true) }
            val matchesTrend = rule.requiredTrend == "ANY" || (trendDirection == TrendDirection.UPTREND && rule.requiredTrend == "UP") ||
                    (trendDirection == TrendDirection.DOWNTREND && rule.requiredTrend == "DOWN") ||
                    (trendDirection == TrendDirection.SIDEWAYS && rule.requiredTrend == "SIDEWAYS")
            val matchesSupport = !rule.requireNearSupport || isNearSupport
            val matchesResistance = !rule.requireNearResistance || isNearResistance

            if (rule.patternType == "BREAKOUT") matchesPattern = breakoutScore > 0f
            if (rule.patternType == "BREAKDOWN") matchesPattern = breakoutScore < 0f
            if (rule.patternType == "FAKEOUT_RESISTANCE") matchesPattern = breakoutHint?.contains("Fakeout above", true) == true
            if (rule.patternType == "FAKEOUT_SUPPORT") matchesPattern = breakoutHint?.contains("Fakeout below", true) == true

            if (matchesPattern && matchesTrend && matchesSupport && matchesResistance) {
                matchedRuleName = rule.name
                val dirSign = if (rule.outcome.equals("UP", true)) 1.0 else if (rule.outcome.equals("DOWN", true)) -1.0 else 0.0
                val contrib = (dirSign * rule.weight.toDouble()).coerceIn(-0.20, 0.20)
                components.add(
                    ScoredComponent(
                        name = "taught_rules_match",
                        description = "Rule matched: '${rule.name}' (${(rule.winRate).toInt()}% WR, w=${rule.weight})",
                        signedContribution = contrib,
                        absMagnitude = abs(contrib)
                    )
                )
                break
            }
        }

        // Component 7: Momentum, EMA, RSI [-0.05, 0.05]
        var momContrib = 0.0
        if (rsi != null) {
            if (rsi <= 30f) {
                momContrib += 0.03 // Oversold => bounce UP
            } else if (rsi >= 70f) {
                momContrib -= 0.03 // Overbought => pullback DOWN
            }
        }
        if (ema9.size >= 2 && ema21.size >= 2) {
            val fastY = ema9.last().second
            val slowY = ema21.last().second
            // smaller Y = higher price
            if (fastY < slowY) momContrib += 0.02 else momContrib -= 0.02
        }
        momContrib = momContrib.coerceIn(-0.05, 0.05)
        if (abs(momContrib) > 0.01) {
            val desc = if (momContrib > 0) "Bullish Momentum: EMA crossover & RSI=${rsi?.toInt() ?: 50}"
            else "Bearish Momentum: EMA crossover & RSI=${rsi?.toInt() ?: 50}"
            components.add(
                ScoredComponent(
                    name = "momentum_ema_rsi",
                    description = desc,
                    signedContribution = momContrib,
                    absMagnitude = abs(momContrib)
                )
            )
        }

        // -------------------------------------------------------------
        // Confluence Total Score & Logistic Probability
        // formula: score = sum(signed_weight * quality)
        // p_up = 1 / (1 + exp(-k * score)), k = 6.0
        // -------------------------------------------------------------
        val totalScore = components.sumOf { it.signedContribution }

        // Additional higher timeframe conflict penalty:
        // "If 15m trend conflicts with signal direction, subtract 0.15 from score"
        val rawPUp = 1.0 / (1.0 + exp(-LOGISTIC_K * totalScore))

        var finalPUp = rawPUp
        if (higherTfTrend == "DOWN" && rawPUp > 0.5) {
            // Conflict with 15m downtrend
            finalPUp = 1.0 / (1.0 + exp(-LOGISTIC_K * (totalScore - 0.15)))
        } else if (higherTfTrend == "UP" && rawPUp < 0.5) {
            // Conflict with 15m uptrend
            finalPUp = 1.0 / (1.0 + exp(-LOGISTIC_K * (totalScore + 0.15)))
        }

        val pDown = 1.0 - finalPUp
        val maxProb = max(finalPUp, pDown).toFloat()

        // Apply confidence cap: min(max(p_up, 1-p_up), confidence.max_cap) = 0.78
        val cappedConfidence = min(maxProb, CONFIDENCE_MAX_CAP)

        // Signal rule: UP if p_up >= min_to_signal (0.60); DOWN if (1-p_up) >= 0.60; else NEUTRAL
        var overallSignal = when {
            finalPUp >= MIN_TO_SIGNAL -> "UP"
            pDown >= MIN_TO_SIGNAL -> "DOWN"
            else -> "NEUTRAL"
        }

        // Check if signal conflicts with no-trade filter 4:
        if (blockReason == null && higherTfTrend != "ANY") {
            if (overallSignal == "UP" && higherTfTrend == "DOWN" && breakoutScore <= 0f) {
                blockReason = "No-trade filter: Signal (UP) directly opposes strong 15m trend (DOWN) with no fakeout confirmation"
            } else if (overallSignal == "DOWN" && higherTfTrend == "UP" && breakoutScore >= 0f) {
                blockReason = "No-trade filter: Signal (DOWN) directly opposes strong 15m trend (UP) with no fakeout confirmation"
            }
        }

        // If blocked by any filter, neutralize signal and assign reason
        val isBlocked = blockReason != null
        if (isBlocked) {
            overallSignal = "NEUTRAL"
        }

        // Top 3 contributing components in plain words as reasons
        val top3Components = components.sortedByDescending { it.absMagnitude }.take(3)
        val reasonsList = mutableListOf<String>()
        if (isBlocked) {
            reasonsList.add("⛔ $blockReason")
        }
        for (c in top3Components) {
            reasonsList.add(c.description)
        }
        if (reasonsList.isEmpty()) {
            reasonsList.add("Neutral consolidation: No confluence confluence components reached threshold")
        }

        // Expiry suggestion based on timeframe
        val expiry = when (timeframe.lowercase()) {
            "5m" -> "5m to 15m"
            "15m" -> "15m to 45m"
            else -> "1m to 3m"
        }

        return ChartAnalysisResult(
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight,
            candles = candles,
            swings = swings,
            srZones = srZones,
            trendLines = trendLines,
            trendDirection = trendDirection,
            breakoutHint = breakoutHint,
            detectedPatterns = patterns,
            ema9Points = ema9,
            ema21Points = ema21,
            rsiValue = rsi,
            atr = atr,
            momentumScore = momentum,
            overallSignal = overallSignal,
            confidenceScore = cappedConfidence,
            pUp = finalPUp.toFloat(),
            isBlockedByFilter = isBlocked,
            blockReason = blockReason,
            reasonList = reasonsList,
            matchedRule = matchedRuleName,
            suggestedExpiry = expiry,
            nearestSupportDist = supportDist,
            nearestResistanceDist = resistanceDist,
            timeframe = timeframe,
            higherTfTrend = higherTfTrend,
            analysisTimeMs = executionDurationMs
        )
    }
}
