package com.example.engine

import com.example.data.entity.RuleEntity
import com.example.model.Candle
import com.example.model.ChartAnalysisResult
import com.example.model.PatternMatchResult
import com.example.model.ScoringContribution
import com.example.model.SupportResistanceZone
import com.example.model.SwingPoint
import com.example.model.TrendDirection
import com.example.model.TrendLine
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object RuleMatchingEngine {

    private const val K_FACTOR = 6.0
    private const val MIN_TO_SIGNAL = 0.60f
    private const val MAX_CONFIDENCE_CAP = 0.78f

    data class FilterCheckResult(
        val isBlocked: Boolean,
        val reason: String?
    )

    fun checkNoTradeFilters(
        candles: List<Candle>,
        atr: Float,
        trendEval: TrendDetector.TrendEvaluation,
        srZones: List<SupportResistanceZone>,
        higherTfTrend: TrendDirection?,
        candidateSignal: String,
        hasFakeoutConfirmation: Boolean,
        consecutiveLosses: Int,
        signalsToday: Int,
        dailyLimit: Int,
        isNewsWindowFlagged: Boolean
    ): FilterCheckResult {
        // 1. fewer than 30 candles visible
        if (candles.size < 30) {
            return FilterCheckResult(true, "Blocked: Fewer than 30 candles visible (Insufficient price action history)")
        }

        // 2. latest candle range > 3*ATR (news spike)
        val latest = candles.last()
        if (latest.range > (3f * atr)) {
            return FilterCheckResult(true, "Blocked: Latest candle range exceeds 3x ATR (News spike volatility)")
        }

        // 3. ATR below 0.3 * average ATR of last 100 candles (dead market)
        if (candles.size >= 35) {
            val longWindow = candles.takeLast(100)
            val avgLongAtr = longWindow.map { it.range }.average().toFloat()
            if (avgLongAtr > 0 && atr < (0.30f * avgLongAtr)) {
                return FilterCheckResult(true, "Blocked: Volatility below 30% of average (Inactive / dead market)")
            }
        }

        // 4. trend = SIDEWAYS and price is in the middle 50% of the range
        if (trendEval.isSidewaysMiddle50) {
            return FilterCheckResult(true, "Blocked: Range-bound consolidation; price hovering in middle 50% without edge")
        }

        // 5. price is inside a strong S/R zone with no confirmation candle yet
        val insideStrongZone = srZones.any { it.containsPrice(latest.closeY) && it.strengthScore >= 3.0f }
        if (insideStrongZone && (latest.isDoji || latest.bodyRatio < 0.20f)) {
            return FilterCheckResult(true, "Blocked: Price inside major S/R zone without directional confirmation candle")
        }

        // 6. signal direction is against a strong higher timeframe trend and no fakeout confirmation
        if (higherTfTrend != null && higherTfTrend != TrendDirection.SIDEWAYS) {
            val isConflict = (candidateSignal == "UP" && higherTfTrend == TrendDirection.DOWNTREND) ||
                    (candidateSignal == "DOWN" && higherTfTrend == TrendDirection.UPTREND)
            if (isConflict && !hasFakeoutConfirmation) {
                return FilterCheckResult(true, "Blocked: Signal conflicts with higher timeframe trend without fakeout confirmation")
            }
        }

        // 7. 3 losses in a row (cooldown 30 min)
        if (consecutiveLosses >= 3) {
            return FilterCheckResult(true, "Blocked: 3 consecutive losses hit. 30-minute emotional risk cooldown active.")
        }

        // 8. daily signal limit reached
        if (signalsToday >= dailyLimit) {
            return FilterCheckResult(true, "Blocked: Daily discipline limit ($dailyLimit signals) reached.")
        }

        // 9. user-flagged news time window
        if (isNewsWindowFlagged) {
            return FilterCheckResult(true, "Blocked: High-impact economic news release window active.")
        }

        return FilterCheckResult(false, null)
    }

    fun evaluateChart(
        bitmapWidth: Int,
        bitmapHeight: Int,
        candles: List<Candle>,
        swings: List<SwingPoint>,
        srZones: List<SupportResistanceZone>,
        trendLines: List<TrendLine>,
        trendEval: TrendDetector.TrendEvaluation,
        atr: Float,
        breakoutHint: String?,
        isBreakout: Boolean,
        isFakeout: Boolean,
        patterns: List<PatternMatchResult>,
        ema20: List<Pair<Float, Float>>,
        ema50: List<Pair<Float, Float>>,
        rsi: Float?,
        momentum: Float,
        activeRules: List<RuleEntity>,
        higherTfTrend: TrendDirection? = null,
        isHigherTfAgree: Boolean = false,
        consecutiveLosses: Int = 0,
        signalsToday: Int = 0,
        dailyLimit: Int = 10,
        isNewsWindowFlagged: Boolean = false,
        timeframe: String = "1m",
        executionDurationMs: Long = 0L
    ): ChartAnalysisResult {
        if (candles.isEmpty()) {
            return ChartAnalysisResult(
                bitmapWidth = bitmapWidth,
                bitmapHeight = bitmapHeight,
                overallSignal = "NEUTRAL",
                confidenceScore = 0.5f,
                reasonList = listOf("No distinct candles detected in chart viewport"),
                analysisTimeMs = executionDurationMs
            )
        }

        val latest = candles.last()
        val contributions = mutableListOf<ScoringContribution>()
        val contributingRules = mutableListOf<String>()

        // 1. pattern_in_context: [0.0, 0.14]
        val recentPatterns = patterns.filter { it.candleIndex >= (candles.size - 2) }
        val bestPattern = recentPatterns.maxByOrNull { it.weight * it.quality }
        if (bestPattern != null && bestPattern.weight > 0f) {
            val sign = if (bestPattern.direction == "UP") 1f else if (bestPattern.direction == "DOWN") -1f else 0f
            val contrib = (bestPattern.weight * bestPattern.quality).coerceIn(0f, 0.14f)
            contributions.add(
                ScoringContribution(
                    componentName = "pattern_in_context",
                    signedContribution = sign * contrib,
                    description = "${bestPattern.patternName} in context (+${(contrib * 100).toInt()}% ${bestPattern.direction})"
                )
            )
            contributingRules.add(bestPattern.patternName)
        }

        // 2. support_resistance_position: [0.0, 0.12]
        val nearestSupport = srZones.filter { it.isSupport }.minByOrNull { abs(it.yLevel - latest.closeY) }
        val nearestResistance = srZones.filter { !it.isSupport }.minByOrNull { abs(it.yLevel - latest.closeY) }

        val supportDist = nearestSupport?.let { abs(it.yLevel - latest.closeY) }
        val resistanceDist = nearestResistance?.let { abs(it.yLevel - latest.closeY) }

        val isNearSupport = supportDist != null && supportDist <= (0.5f * atr)
        val isNearResistance = resistanceDist != null && resistanceDist <= (0.5f * atr)

        if (isNearSupport && nearestSupport != null) {
            val srQuality = (nearestSupport.strengthScore / 5f).coerceIn(0.5f, 1.0f)
            val contrib = (0.12f * srQuality).coerceIn(0.04f, 0.12f)
            contributions.add(
                ScoringContribution(
                    componentName = "support_resistance_position",
                    signedContribution = contrib, // Bullish bounce at support
                    description = "Support zone proximity (${nearestSupport.touchCount} touches, +${(contrib * 100).toInt()}%)"
                )
            )
        } else if (isNearResistance && nearestResistance != null) {
            val srQuality = (nearestResistance.strengthScore / 5f).coerceIn(0.5f, 1.0f)
            val contrib = (0.12f * srQuality).coerceIn(0.04f, 0.12f)
            contributions.add(
                ScoringContribution(
                    componentName = "support_resistance_position",
                    signedContribution = -contrib, // Bearish rejection at resistance
                    description = "Resistance zone proximity (${nearestResistance.touchCount} touches, -${(contrib * 100).toInt()}%)"
                )
            )
        }

        // 3. trend_alignment_current_tf: [0.0, 0.08]
        when (trendEval.direction) {
            TrendDirection.UPTREND -> contributions.add(
                ScoringContribution(
                    componentName = "trend_alignment_current_tf",
                    signedContribution = 0.08f,
                    description = "Current timeframe dominant Uptrend (+8%)"
                )
            )
            TrendDirection.DOWNTREND -> contributions.add(
                ScoringContribution(
                    componentName = "trend_alignment_current_tf",
                    signedContribution = -0.08f,
                    description = "Current timeframe dominant Downtrend (-8%)"
                )
            )
            TrendDirection.SIDEWAYS -> {}
        }

        // 4. higher_tf_alignment: [-0.15, 0.10]
        if (higherTfTrend != null) {
            if (isHigherTfAgree) {
                contributions.add(
                    ScoringContribution(
                        componentName = "higher_tf_alignment",
                        signedContribution = if (trendEval.direction == TrendDirection.UPTREND) 0.10f else -0.10f,
                        description = "Multi-timeframe consensus: 15m & 5m both agree (+10%)"
                    )
                )
            } else if (higherTfTrend != TrendDirection.SIDEWAYS && higherTfTrend != trendEval.direction) {
                val penalty = if (higherTfTrend == TrendDirection.DOWNTREND) -0.15f else 0.15f
                contributions.add(
                    ScoringContribution(
                        componentName = "higher_tf_alignment",
                        signedContribution = penalty,
                        description = "Higher 15m timeframe trend bias alignment (${if (penalty > 0) "+15%" else "-15%"})"
                    )
                )
            }
        }

        // 5. breakout_retest_or_fakeout: [0.0, 0.1]
        if (isBreakout && breakoutHint != null) {
            val sign = if (breakoutHint.contains("Bullish", true) || breakoutHint.contains("Support", true)) 1f else -1f
            contributions.add(
                ScoringContribution(
                    componentName = "breakout_retest_or_fakeout",
                    signedContribution = sign * 0.08f,
                    description = breakoutHint
                )
            )
        } else if (isFakeout && breakoutHint != null) {
            // Fakeout rejection: opposite to break direction
            val sign = if (breakoutHint.contains("Support", true)) 1f else -1f
            contributions.add(
                ScoringContribution(
                    componentName = "breakout_retest_or_fakeout",
                    signedContribution = sign * 0.10f,
                    description = breakoutHint
                )
            )
        }

        // 6. taught_rules_match: [-0.2, 0.2]
        var taughtScore = 0f
        for (rule in activeRules) {
            val patternMatches = recentPatterns.any {
                it.patternName.equals(rule.patternType, ignoreCase = true) ||
                        it.patternId.equals(rule.patternType, ignoreCase = true) ||
                        rule.patternType.equals("CUSTOM", ignoreCase = true) ||
                        rule.patternType.equals("ANY", ignoreCase = true)
            }
            if (!patternMatches && !rule.patternType.equals("TREND", ignoreCase = true) && !rule.patternType.equals("EMA_PULLBACK", ignoreCase = true)) {
                continue
            }

            val trendMatches = when (rule.requiredTrend.uppercase()) {
                "UP" -> trendEval.direction == TrendDirection.UPTREND
                "DOWN" -> trendEval.direction == TrendDirection.DOWNTREND
                "SIDEWAYS" -> trendEval.direction == TrendDirection.SIDEWAYS
                else -> true
            }
            if (!trendMatches) continue
            if (rule.requireNearSupport && !isNearSupport) continue
            if (rule.requireNearResistance && !isNearResistance) continue

            val totalTrades = rule.winCount + rule.lossCount
            val ruleSign = if (rule.outcome.equals("UP", true)) 1f else -1f
            // Knowledge Pack calibration: When >=30 journal results, replace prior weight with measured win rate
            val effectiveWeight = if (totalTrades >= 30) {
                val winRate = rule.winCount.toFloat() / totalTrades.toFloat()
                (winRate * 0.25f).coerceIn(0.05f, 0.25f)
            } else {
                rule.weight.coerceIn(0.05f, 0.20f)
            }
            taughtScore += (ruleSign * effectiveWeight)
            contributingRules.add(rule.name)
        }

        if (taughtScore != 0f) {
            val clampedTaught = taughtScore.coerceIn(-0.20f, 0.20f)
            contributions.add(
                ScoringContribution(
                    componentName = "taught_rules_match",
                    signedContribution = clampedTaught,
                    description = "Active Rule match confluence (${if (clampedTaught > 0) "+" else ""}${(clampedTaught * 100).toInt()}%)"
                )
            )
        }

        // 7. momentum_ema_rsi: [-0.05, 0.05]
        var techContrib = trendEval.emaBiasWeight
        rsi?.let {
            if (it <= 30f) techContrib += 0.03f // oversold bounce
            else if (it >= 70f) techContrib -= 0.03f // overbought rejection
        }
        val clampedTech = techContrib.coerceIn(-0.05f, 0.05f)
        if (clampedTech != 0f) {
            contributions.add(
                ScoringContribution(
                    componentName = "momentum_ema_rsi",
                    signedContribution = clampedTech,
                    description = "EMA20/50 & RSI momentum bias (${if (clampedTech > 0) "+" else ""}${(clampedTech * 100).toInt()}%)"
                )
            )
        }

        // Confluence Scoring formula:
        // score = sum(direction_sign * weight * quality)
        // p_up = 1 / (1 + exp(-k * score)); k = 6
        val netScore = contributions.sumOf { it.signedContribution.toDouble() }
        val pUp = (1.0 / (1.0 + exp(-K_FACTOR * netScore))).toFloat()

        // Output rules:
        // signal: UP if p_up >= min_to_signal (0.6); DOWN if (1-p_up) >= min_to_signal; else NEUTRAL
        val rawSignal = when {
            pUp >= MIN_TO_SIGNAL -> "UP"
            (1f - pUp) >= MIN_TO_SIGNAL -> "DOWN"
            else -> "NEUTRAL"
        }

        // confidence: min(max(p_up, 1-p_up), confidence.max_cap) (0.78)
        val rawConfidence = min(max(pUp, 1f - pUp), MAX_CONFIDENCE_CAP)

        // Top 3 contributing components in plain words
        val topContributions = contributions
            .sortedByDescending { abs(it.signedContribution) }
            .take(3)
        val topReasons = topContributions.map { it.description }

        // Check No-Trade Filters
        val filterCheck = checkNoTradeFilters(
            candles = candles,
            atr = atr,
            trendEval = trendEval,
            srZones = srZones,
            higherTfTrend = higherTfTrend,
            candidateSignal = rawSignal,
            hasFakeoutConfirmation = isFakeout,
            consecutiveLosses = consecutiveLosses,
            signalsToday = signalsToday,
            dailyLimit = dailyLimit,
            isNewsWindowFlagged = isNewsWindowFlagged
        )

        val finalSignal = if (filterCheck.isBlocked) "NEUTRAL" else rawSignal
        val finalConfidence = if (filterCheck.isBlocked) 0.50f else rawConfidence

        val suggestedExpiry = when (timeframe.lowercase()) {
            "1m" -> "1m to 3m"
            "5m" -> "5m to 15m"
            "15m" -> "15m to 45m"
            else -> "1 to 3 candles"
        }

        val allReasons = mutableListOf<String>()
        if (filterCheck.isBlocked) {
            allReasons.add(filterCheck.reason ?: "Signal blocked by Risk Filter")
        }
        allReasons.addAll(topReasons)
        if (allReasons.isEmpty()) {
            allReasons.add("Equilibrium state; awaiting market structure confirmation")
        }

        return ChartAnalysisResult(
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight,
            candles = candles,
            swings = swings,
            srZones = srZones,
            trendLines = trendLines,
            trendDirection = trendEval.direction,
            atr14 = atr,
            breakoutHint = breakoutHint,
            detectedPatterns = patterns,
            ema20Points = ema20,
            ema50Points = ema50,
            rsiValue = rsi,
            momentumScore = momentum,
            overallSignal = finalSignal,
            pUp = pUp,
            confidenceScore = finalConfidence,
            reasonList = allReasons,
            topContributingReasons = topReasons,
            matchedRule = contributingRules.firstOrNull(),
            contributingRuleNames = contributingRules,
            suggestedExpiry = suggestedExpiry,
            nearestSupportDist = supportDist,
            nearestResistanceDist = resistanceDist,
            analysisTimeMs = executionDurationMs,
            isBlocked = filterCheck.isBlocked,
            blockedReason = filterCheck.reason,
            timeframe = timeframe
        )
    }
}
