package com.example

import com.example.autotrader.RiskManager
import com.example.model.DailyRiskStats
import com.example.model.RiskConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RiskManagerTest {

    private lateinit var riskManager: RiskManager

    @Before
    fun setup() {
        riskManager = RiskManager(
            config = RiskConfig(
                fixedStake = 1.0,
                maxStakePercent = 10.0,
                dailyLossLimit = 3.0,
                dailyProfitTarget = 2.0,
                maxTradesPerDay = 8,
                consecutiveLossThreshold = 3,
                cooldownDurationMinutes = 30
            )
        )
    }

    @Test
    fun testAllowedTradeUnderNormalConditions() {
        val stats = DailyRiskStats(startingBalance = 100.0, currentBalance = 100.0)
        val result = riskManager.canPlaceTrade(stats, requestedStake = 1.0, currentBalance = 100.0)
        assertTrue("Trade should be allowed under normal conditions", result is RiskManager.RiskEvaluation.Allowed)
    }

    @Test
    fun testRejectStakeExceedingFixedStake_AntiMartingale() {
        val stats = DailyRiskStats(startingBalance = 100.0, currentBalance = 100.0, consecutiveLosses = 1)
        // User/System attempts to double stake to $2.0 after loss -> STRICTLY REJECTED
        val result = riskManager.canPlaceTrade(stats, requestedStake = 2.0, currentBalance = 100.0)
        assertTrue("Martingale / Stake doubling must be rejected", result is RiskManager.RiskEvaluation.Rejected)
    }

    @Test
    fun testRejectStakeExceedingTenPercentBalance() {
        val stats = DailyRiskStats(startingBalance = 8.0, currentBalance = 8.0)
        // 10% of $8.0 is $0.80. A $1.0 stake exceeds 10%
        val result = riskManager.canPlaceTrade(stats, requestedStake = 1.0, currentBalance = 8.0)
        assertTrue("Stake > 10% balance must be rejected", result is RiskManager.RiskEvaluation.Rejected)
    }

    @Test
    fun testConsecutiveLossesTriggerCooldown() {
        val stats = DailyRiskStats(
            startingBalance = 100.0,
            currentBalance = 97.0,
            consecutiveLosses = 3
        )
        val result = riskManager.canPlaceTrade(stats, requestedStake = 1.0, currentBalance = 97.0)
        assertTrue("3 consecutive losses must trigger cooldown", result is RiskManager.RiskEvaluation.CooldownTriggered)
        val cooldown = result as RiskManager.RiskEvaluation.CooldownTriggered
        assertEquals(30, cooldown.durationMinutes)
    }

    @Test
    fun testDailyLossLimitLocksTrading() {
        var stats = DailyRiskStats(startingBalance = 100.0, currentBalance = 100.0)
        // Simulate 3 losses of $1.0 = -$3.0 loss limit hit
        stats = riskManager.processTradeResult(stats, isWin = false, stake = 1.0)
        stats = riskManager.processTradeResult(stats, isWin = false, stake = 1.0)
        stats = riskManager.processTradeResult(stats, isWin = false, stake = 1.0)

        assertTrue("Stats should be locked after -$3.0 loss", stats.isLocked)
        val eval = riskManager.canPlaceTrade(stats, requestedStake = 1.0, currentBalance = stats.currentBalance)
        assertTrue("Cannot place trade when locked", eval is RiskManager.RiskEvaluation.Rejected)
        assertTrue((eval as RiskManager.RiskEvaluation.Rejected).shouldLock)
    }

    @Test
    fun testDailyProfitTargetLocksTrading() {
        var stats = DailyRiskStats(startingBalance = 100.0, currentBalance = 100.0)
        // Simulate 3 wins with 85% payout = 3 * 0.85 = +$2.55 (>= $2.0 target)
        stats = riskManager.processTradeResult(stats, isWin = true, stake = 1.0, payoutRate = 0.85)
        stats = riskManager.processTradeResult(stats, isWin = true, stake = 1.0, payoutRate = 0.85)
        stats = riskManager.processTradeResult(stats, isWin = true, stake = 1.0, payoutRate = 0.85)

        assertTrue("Stats should be locked to protect profits after +$2.0 target hit", stats.isLocked)
        val eval = riskManager.canPlaceTrade(stats, requestedStake = 1.0, currentBalance = stats.currentBalance)
        assertTrue(eval is RiskManager.RiskEvaluation.Rejected)
    }

    @Test
    fun testMaxTradesPerDayLimit() {
        var stats = DailyRiskStats(startingBalance = 100.0, currentBalance = 100.0)
        for (i in 1..8) {
            stats = riskManager.processTradeResult(stats, isWin = (i % 2 == 0), stake = 1.0)
        }
        assertEquals(8, stats.totalTradesToday)
        assertTrue(stats.isLocked)
    }
}
