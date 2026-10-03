package com.example.autotrader

import com.example.model.DailyRiskStats
import com.example.model.RiskConfig
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Risk Manager Engine.
 * Enforces hard trading rules with strict anti-Martingale safety.
 */
class RiskManager(
    var config: RiskConfig = RiskConfig()
) {

    sealed class RiskEvaluation {
        object Allowed : RiskEvaluation()
        data class Rejected(val reason: String, val shouldLock: Boolean = false) : RiskEvaluation()
        data class CooldownTriggered(val reason: String, val durationMinutes: Int) : RiskEvaluation()
    }

    /**
     * Validates whether a trade is permitted given current daily risk state and balance.
     */
    fun canPlaceTrade(
        stats: DailyRiskStats,
        requestedStake: Double,
        currentBalance: Double
    ): RiskEvaluation {
        // 1. Check if already locked
        if (stats.isLocked) {
            return RiskEvaluation.Rejected(
                reason = "Trading is LOCKED: ${stats.lockReason ?: "Daily safety boundary reached"}",
                shouldLock = true
            )
        }

        // 2. Validate balance presence & validity
        if (currentBalance <= 0.0) {
            return RiskEvaluation.Rejected(
                reason = "Invalid or zero balance ($$currentBalance). Page balance read may have failed.",
                shouldLock = true
            )
        }

        // 3. Max trades per day limit
        if (stats.totalTradesToday >= config.maxTradesPerDay) {
            return RiskEvaluation.Rejected(
                reason = "Daily trade limit reached (${stats.totalTradesToday}/${config.maxTradesPerDay} trades)",
                shouldLock = true
            )
        }

        // 4. Daily loss limit
        if (stats.netPnlToday <= -config.dailyLossLimit) {
            return RiskEvaluation.Rejected(
                reason = "Daily loss limit reached ($${String.format("%.2f", stats.netPnlToday)} <= -$${config.dailyLossLimit})",
                shouldLock = true
            )
        }

        // 5. Daily profit target
        if (stats.netPnlToday >= config.dailyProfitTarget) {
            return RiskEvaluation.Rejected(
                reason = "Daily profit target reached ($${String.format("%.2f", stats.netPnlToday)} >= +$${config.dailyProfitTarget}) - capital secured",
                shouldLock = true
            )
        }

        // 6. Max 10% balance rule
        val maxAllowedStake = (currentBalance * (config.maxStakePercent / 100.0))
        if (requestedStake > maxAllowedStake) {
            return RiskEvaluation.Rejected(
                reason = "Stake $$requestedStake exceeds ${config.maxStakePercent.toInt()}% of balance (Max allowed: $$${String.format("%.2f", maxAllowedStake)})",
                shouldLock = false
            )
        }

        // 7. Fixed stake enforcement (Strictly no Martingale / stake increases after loss)
        if (requestedStake > config.fixedStake) {
            return RiskEvaluation.Rejected(
                reason = "Stake $$requestedStake exceeds configured fixed stake of $$${config.fixedStake}. Martingale/Stake-increase is strictly prohibited.",
                shouldLock = false
            )
        }

        // 8. Consecutive losses cooldown check
        if (stats.consecutiveLosses >= config.consecutiveLossThreshold) {
            return RiskEvaluation.CooldownTriggered(
                reason = "${stats.consecutiveLosses} consecutive losses detected. 30-minute cooldown enforced to prevent revenge trading.",
                durationMinutes = config.cooldownDurationMinutes
            )
        }

        return RiskEvaluation.Allowed
    }

    /**
     * Updates daily risk statistics following a trade outcome.
     */
    fun processTradeResult(
        currentStats: DailyRiskStats,
        isWin: Boolean,
        stake: Double,
        payoutRate: Double = 0.85
    ): DailyRiskStats {
        val profitLoss = if (isWin) (stake * payoutRate) else -stake
        val newBalance = currentStats.currentBalance + profitLoss
        val newNetPnl = currentStats.netPnlToday + profitLoss
        val newTotalTrades = currentStats.totalTradesToday + 1
        val newWins = if (isWin) currentStats.winsToday + 1 else currentStats.winsToday
        val newLosses = if (!isWin) currentStats.lossesToday + 1 else currentStats.lossesToday
        val newConsecutiveLosses = if (isWin) 0 else currentStats.consecutiveLosses + 1

        val newPeakBalance = max(currentStats.peakBalanceToday, newBalance)
        val currentDrawdown = max(0.0, newPeakBalance - newBalance)
        val maxDrawdown = max(currentStats.maxDrawdownToday, currentDrawdown)

        var isLocked = currentStats.isLocked
        var lockReason = currentStats.lockReason

        if (newNetPnl <= -config.dailyLossLimit) {
            isLocked = true
            lockReason = "Daily loss limit reached ($${String.format("%.2f", newNetPnl)} <= -$${config.dailyLossLimit})"
        } else if (newNetPnl >= config.dailyProfitTarget) {
            isLocked = true
            lockReason = "Daily profit target secured (+$${String.format("%.2f", newNetPnl)} >= +$${config.dailyProfitTarget})"
        } else if (newTotalTrades >= config.maxTradesPerDay) {
            isLocked = true
            lockReason = "Daily trade limit completed ($newTotalTrades/${config.maxTradesPerDay} trades)"
        }

        val updatedPoints = currentStats.equityCurvePoints + Pair(System.currentTimeMillis(), newBalance)

        return currentStats.copy(
            currentBalance = newBalance,
            totalTradesToday = newTotalTrades,
            winsToday = newWins,
            lossesToday = newLosses,
            netPnlToday = newNetPnl,
            consecutiveLosses = newConsecutiveLosses,
            peakBalanceToday = newPeakBalance,
            maxDrawdownToday = maxDrawdown,
            isLocked = isLocked,
            lockReason = lockReason,
            equityCurvePoints = updatedPoints
        )
    }
}
