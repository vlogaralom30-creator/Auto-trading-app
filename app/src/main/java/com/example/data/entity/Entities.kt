package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleKey: String = "", // e.g. "r1", "r2", or custom
    val name: String,
    val patternType: String, // e.g. "HAMMER", "BULLISH_ENGULFING", "BEARISH_ENGULFING", "SHOOTING_STAR", "DOJI", "PIN_BAR", "INSIDE_BAR", "MORNING_STAR", "EVENING_STAR", "TWEEZER_TOP", "TWEEZER_BOTTOM", "MARUBOZU", "THREE_SOLDIERS", "CUSTOM"
    val requiredTrend: String = "ANY", // "UP", "DOWN", "SIDEWAYS", "ANY"
    val requireNearSupport: Boolean = false,
    val requireNearResistance: Boolean = false,
    val minConfidence: Float = 0.60f,
    val outcome: String = "UP", // "UP", "DOWN", "NEUTRAL"
    val weight: Float = 0.10f, // Confluence weight from knowledge pack (e.g. 0.08 - 0.20)
    val priorStrength: Int = 10,
    val priorWinRate: Float = 0.50f,
    val winCount: Int = 0,
    val lossCount: Int = 0,
    val notes: String = "",
    val isEnabled: Boolean = true,
    val source: String = "builtin", // "builtin" | "user"
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalTrades: Int get() = winCount + lossCount
    val measuredWinRate: Float get() = (winCount + 1f) / (winCount + lossCount + 2f)
    val effectiveWinRate: Float get() {
        val n = totalTrades
        return ((priorWinRate * priorStrength) + (measuredWinRate * n)) / (priorStrength + n)
    }
    val winRate: Float get() = if (totalTrades > 0) (winCount.toFloat() / totalTrades.toFloat()) * 100f else 0f
}

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val url: String = "",
    val site: String = "TradingView",
    val assetPair: String = "BTC/USDT",
    val timeframe: String = "1m",
    val session: String = "London/NY",
    val isDemo: Boolean = true,
    val signalDirection: String = "UP", // "UP", "DOWN", "NEUTRAL"
    val confidence: Float = 0.70f,
    val matchedRuleName: String = "",
    val detectedPatterns: String = "",
    val nearestSupport: Float = 0f,
    val nearestResistance: Float = 0f,
    val expirySuggestion: String = "1m to 3m",
    val outcomeResult: String = "PENDING", // "PENDING", "WIN", "LOSS"
    val notes: String = ""
)

@Entity(tableName = "backtest_samples")
data class BacktestSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val screenshotPath: String = "",
    val label: String = "UP", // Known actual outcome: "UP" or "DOWN"
    val site: String = "TradingView",
    val pair: String = "BTC/USDT",
    val timeframe: String = "1m",
    val patternName: String = "HAMMER",
    val notes: String = "",
    val lastTestResult: String? = null, // "WIN", "LOSS", "NO_SIGNAL"
    val lastTestConfidence: Float = 0f
)
