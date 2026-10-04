package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: String = "",
    val name: String,
    val patternType: String, // e.g. "HAMMER", "BULLISH_ENGULFING", "BEARISH_ENGULFING", "SHOOTING_STAR", "DOJI", "PIN_BAR", "INSIDE_BAR", "MORNING_STAR", "EVENING_STAR", "TWEEZER_BOTTOM", "TWEEZER_TOP", "MARUBOZU", "THREE_SOLDIERS", "CUSTOM"
    val requiredTrend: String = "ANY", // "UP", "DOWN", "SIDEWAYS", "ANY"
    val requireNearSupport: Boolean = false,
    val requireNearResistance: Boolean = false,
    val minConfidence: Float = 0.60f,
    val outcome: String = "UP", // "UP", "DOWN", "NEUTRAL"
    val weight: Float = 0.10f,
    val priorStrength: Int = 10,
    val priorWeight: Float = 0.10f,
    val winCount: Int = 0,
    val lossCount: Int = 0,
    val source: String = "builtin", // "builtin" | "user"
    val notes: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

val RuleEntity.totalTrades: Int get() = winCount + lossCount
val RuleEntity.winRate: Float get() = if (totalTrades > 0) (winCount.toFloat() / totalTrades.toFloat()) * 100f else 0f

// Knowledge pack formula: (win + 1) / (win + loss + 2)
val RuleEntity.laplaceMeasuredWinRate: Float
    get() = (winCount + 1f) / (winCount + lossCount + 2f)

// Blend: effective_win_rate = (prior_win_rate * prior_strength + measured_win_rate * n) / (prior_strength + n)
fun RuleEntity.calculateEffectiveWinRate(): Float {
    val n = totalTrades
    val priorWinRate = 0.5f + (priorWeight * 2f).coerceIn(-0.4f, 0.4f)
    return ((priorWinRate * priorStrength) + (laplaceMeasuredWinRate * n)) / (priorStrength + n)
}

// weight = (effective_win_rate - 0.5) * 0.5
fun RuleEntity.calculateUpdatedWeight(): Float {
    val effectiveWinRate = calculateEffectiveWinRate()
    return ((effectiveWinRate - 0.5f) * 0.5f).coerceIn(0.01f, 0.50f)
}

// disable_rule: if n >= 30 and measured_win_rate < 0.48, disable rule and tell the user
val RuleEntity.shouldBeDisabled: Boolean
    get() = totalTrades >= 30 && laplaceMeasuredWinRate < 0.48f

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val url: String = "",
    val site: String = "TradingView",
    val assetPair: String = "BTC/USDT",
    val timeframe: String = "1m",
    val session: String = "New York", // "London", "New York", "Asian", "General"
    val signalDirection: String = "UP", // "UP", "DOWN", "NEUTRAL"
    val confidence: Float = 0.60f,
    val matchedRuleName: String = "",
    val contributingRuleIds: String = "", // comma-separated IDs
    val detectedPatterns: String = "",
    val nearestSupport: Float = 0f,
    val nearestResistance: Float = 0f,
    val expirySuggestion: String = "1m to 3m",
    val outcomeResult: String = "PENDING", // "PENDING", "WIN", "LOSS"
    val notes: String = "",
    val screenshotPath: String = "",
    val isDemo: Boolean = true,
    val blockedReason: String = ""
)
