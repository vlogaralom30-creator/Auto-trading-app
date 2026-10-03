package com.example.model

/**
 * Represents the operating mode of the trading account.
 */
enum class AccountMode {
    DEMO,
    REAL
}

/**
 * State machine for the Automated Trader module.
 */
sealed class AutoTraderState {
    object Idle : AutoTraderState()

    data class Scanning(
        val step: String,
        val progressPercent: Int = 0
    ) : AutoTraderState()

    data class WaitingEntry(
        val signal: String, // "UP" | "DOWN"
        val confidence: Float,
        val matchedRule: String,
        val reasons: List<String>,
        val secondsUntilNextCandle: Int = 10,
        val suggestedExpiry: String = "1m"
    ) : AutoTraderState()

    data class Placing(
        val direction: String,
        val stake: Double,
        val expiry: String,
        val statusMessage: String = "Injecting order into platform..."
    ) : AutoTraderState()

    data class InTrade(
        val tradeId: String,
        val direction: String,
        val stake: Double,
        val entryTime: Long,
        val expiryDurationSec: Int,
        val remainingSec: Int,
        val pair: String = "BTC/USDT"
    ) : AutoTraderState()

    data class Result(
        val isWin: Boolean,
        val profitLoss: Double,
        val newBalance: Double,
        val message: String
    ) : AutoTraderState()

    data class Cooldown(
        val reason: String,
        val remainingCooldownSec: Int
    ) : AutoTraderState()

    data class Locked(
        val lockReason: String,
        val isManualUnlockRequired: Boolean = true
    ) : AutoTraderState()
}

/**
 * Risk management configuration with safe defaults and hard safety bounds.
 * STRICTLY NO Martingale or stake-increase-after-loss logic.
 */
data class RiskConfig(
    val fixedStake: Double = 1.0,           // Fixed currency amount (default $1.0)
    val maxStakePercent: Double = 10.0,     // Max stake <= 10% of balance
    val dailyLossLimit: Double = 3.0,       // Max daily loss ($3.0) -> LOCKED
    val dailyProfitTarget: Double = 2.0,    // Daily profit target ($2.0) -> LOCKED
    val maxTradesPerDay: Int = 8,           // Max trades per day (8) -> LOCKED
    val consecutiveLossThreshold: Int = 3,  // 3 consecutive losses -> 30 min cooldown
    val cooldownDurationMinutes: Int = 30   // 30 min cooldown
)

/**
 * DOM selectors for AutoTrader platform execution & DOM scraping.
 */
data class AutoTraderSelectors(
    val siteKey: String = "tradingview",
    val stakeInputSelector: String = "input[name='amount'], input.stake-input, .input-control__input",
    val expirySelector: String = "button.expiry-btn, .time-select, [data-test='expiration']",
    val upButtonSelector: String = "button.call-btn, button.btn-call, button:contains('Call'), button:contains('Higher'), .btn-up",
    val downButtonSelector: String = "button.put-btn, button.btn-put, button:contains('Put'), button:contains('Lower'), .btn-down",
    val activeTradeVerifierSelector: String = ".active-deals, .open-trades, .deal-item, .position-open, .trade-history-item",
    val balanceSelector: String = ".user-balance, .balance-value, .account-balance, [data-test='balance']",
    val resultSelector: String = ".deal-result, .last-trade-profit, .deal-item__payout, .closed-position"
)

/**
 * Daily statistics for risk monitoring & dashboard equity curve.
 */
data class DailyRiskStats(
    val dateString: String = "",
    val startingBalance: Double = 100.0,
    val currentBalance: Double = 100.0,
    val totalTradesToday: Int = 0,
    val winsToday: Int = 0,
    val lossesToday: Int = 0,
    val netPnlToday: Double = 0.0,
    val consecutiveLosses: Int = 0,
    val maxDrawdownToday: Double = 0.0,
    val peakBalanceToday: Double = 100.0,
    val isLocked: Boolean = false,
    val lockReason: String? = null,
    val equityCurvePoints: List<Pair<Long, Double>> = emptyList()
) {
    val winRate: Float
        get() = if (totalTradesToday > 0) (winsToday.toFloat() / totalTradesToday.toFloat()) * 100f else 0f
}
