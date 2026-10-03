package com.example.data

import android.graphics.BitmapFactory
import com.example.data.dao.BacktestDao
import com.example.data.dao.JournalDao
import com.example.data.dao.RuleDao
import com.example.data.entity.BacktestSampleEntity
import com.example.data.entity.JournalEntryEntity
import com.example.data.entity.RuleEntity
import com.example.engine.ChartAnalyzer
import com.example.model.ColorCalibration
import kotlinx.coroutines.flow.Flow
import java.io.File
import kotlin.math.abs

data class ContextStat(
    val contextKey: String,
    val type: String, // "Site", "Pair", "Timeframe", "Session"
    val totalTrades: Int,
    val wins: Int,
    val winRate: Float
)

data class BacktestSummary(
    val totalTested: Int,
    val correctPredictions: Int,
    val accuracy: Float,
    val perPatternAccuracy: Map<String, Pair<Int, Int>> // Pattern -> (Wins, Total)
)

class ChartMindRepository(
    private val ruleDao: RuleDao,
    private val journalDao: JournalDao,
    private val backtestDao: BacktestDao
) {
    val allRules: Flow<List<RuleEntity>> = ruleDao.getAllRules()
    val allJournalEntries: Flow<List<JournalEntryEntity>> = journalDao.getAllEntries()
    val allBacktestSamples: Flow<List<BacktestSampleEntity>> = backtestDao.getAllSamples()

    suspend fun getEnabledRules(): List<RuleEntity> = ruleDao.getEnabledRulesSync()

    suspend fun insertRule(rule: RuleEntity): Long = ruleDao.insertRule(rule)

    suspend fun updateRule(rule: RuleEntity) = ruleDao.updateRule(rule)

    suspend fun deleteRule(id: Long) = ruleDao.deleteRuleById(id)

    /**
     * Learning Engine:
     * - measured_win_rate = (win + 1) / (win + loss + 2)
     * - blend: effective_win_rate = (prior_win_rate * prior_strength + measured_win_rate * n) / (prior_strength + n)
     * - weight = (effective_win_rate - 0.5) * 0.5 + 0.10
     * - overfit_guard: clamp weight delta <= 0.02
     * - disable_rule: if n >= 30 and measured_win_rate < 0.48, disable rule
     */
    suspend fun recordRuleResult(ruleName: String, isWin: Boolean): String? {
        if (ruleName.isBlank()) return null
        val rule = ruleDao.getRuleByName(ruleName) ?: return null

        val newWin = if (isWin) rule.winCount + 1 else rule.winCount
        val newLoss = if (!isWin) rule.lossCount + 1 else rule.lossCount
        val n = newWin + newLoss

        // Measured win rate with Laplace smoothing
        val measuredWinRate = (newWin + 1f) / (newWin + newLoss + 2f)

        // Bayesian blend with prior strength
        val effectiveWinRate = ((rule.priorWinRate * rule.priorStrength) + (measuredWinRate * n)) / (rule.priorStrength + n)

        // Raw weight calculation
        val targetWeight = (((effectiveWinRate - 0.5f) * 0.5f) + 0.10f).coerceIn(0.02f, 0.20f)

        // Overfit guard: never change weight by more than 0.02 per single sample
        val delta = (targetWeight - rule.weight).coerceIn(-0.02f, 0.02f)
        val finalWeight = (rule.weight + delta).coerceIn(0.02f, 0.20f)

        // Disable rule check: if n >= 30 and measured_win_rate < 0.48
        var shouldDisable = false
        var disableMessage: String? = null
        if (n >= 30 && measuredWinRate < 0.48f && rule.isEnabled) {
            shouldDisable = true
            disableMessage = "Rule '${rule.name}' disabled: Win rate ${(measuredWinRate * 100).toInt()}% fell below 48% over $n trades"
        }

        val updatedRule = rule.copy(
            winCount = newWin,
            lossCount = newLoss,
            weight = finalWeight,
            isEnabled = if (shouldDisable) false else rule.isEnabled
        )
        ruleDao.updateRule(updatedRule)

        return disableMessage
    }

    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long = journalDao.insertEntry(entry)

    suspend fun updateJournalEntry(entry: JournalEntryEntity) = journalDao.updateEntry(entry)

    suspend fun deleteJournalEntry(id: Long) = journalDao.deleteEntryById(id)

    suspend fun getSignalsTodayCount(): Int {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return journalDao.getCountSince(calendar.timeInMillis)
    }

    suspend fun getDemoSignalCount(): Int = journalDao.getDemoSignalCount()

    suspend fun isSystemReady(): Boolean = getDemoSignalCount() >= 150

    suspend fun getConsecutiveLossCount(): Int {
        val recent = journalDao.getRecentOutcomes(5)
        var losses = 0
        for (outcome in recent) {
            if (outcome.equals("LOSS", ignoreCase = true)) {
                losses++
            } else if (outcome.equals("WIN", ignoreCase = true)) {
                break
            }
        }
        return losses
    }

    /**
     * Aggregates win rate per context: (Site, Pair, Timeframe, Session)
     */
    suspend fun getPerContextStats(): List<ContextStat> {
        val completed = journalDao.getCompletedEntriesSync()
        if (completed.isEmpty()) return emptyList()

        val stats = mutableListOf<ContextStat>()

        fun computeGroup(type: String, groupSelector: (JournalEntryEntity) -> String) {
            val grouped = completed.groupBy(groupSelector)
            for ((key, entries) in grouped) {
                if (key.isBlank()) continue
                val total = entries.size
                val wins = entries.count { it.outcomeResult == "WIN" }
                val wr = (wins.toFloat() / total.toFloat()) * 100f
                stats.add(ContextStat(contextKey = key, type = type, totalTrades = total, wins = wins, winRate = wr))
            }
        }

        computeGroup("Site") { it.site }
        computeGroup("Pair") { it.assetPair }
        computeGroup("Timeframe") { it.timeframe }
        computeGroup("Session") { it.session }

        return stats.sortedByDescending { it.totalTrades }
    }

    // -------------------------------------------------------------
    // Backtest Operations
    // -------------------------------------------------------------

    suspend fun insertBacktestSample(sample: BacktestSampleEntity): Long =
        backtestDao.insertSample(sample)

    suspend fun deleteBacktestSample(id: Long) =
        backtestDao.deleteSampleById(id)

    suspend fun runBacktest(calibration: ColorCalibration): BacktestSummary {
        val samples = backtestDao.getAllSamplesSync()
        val rules = ruleDao.getEnabledRulesSync()

        var correct = 0
        var totalTested = 0
        val perPatternCounts = mutableMapOf<String, Pair<Int, Int>>() // Pattern -> (Wins, Total)

        for (sample in samples) {
            val file = File(sample.screenshotPath)
            if (!file.exists()) continue

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
            val result = ChartAnalyzer.analyzeChartBitmap(
                bitmap = bitmap,
                calibration = calibration,
                activeRules = rules,
                timeframe = sample.timeframe
            )

            totalTested++
            val isMatch = result.overallSignal.equals(sample.label, ignoreCase = true)
            if (isMatch) correct++

            val pat = sample.patternName.ifBlank { "GENERAL" }
            val existing = perPatternCounts[pat] ?: Pair(0, 0)
            perPatternCounts[pat] = Pair(
                existing.first + (if (isMatch) 1 else 0),
                existing.second + 1
            )

            // Update sample test outcome in db
            val testOutcome = if (isMatch) "WIN" else if (result.overallSignal == "NEUTRAL") "NO_SIGNAL" else "LOSS"
            backtestDao.updateSample(
                sample.copy(
                    lastTestResult = testOutcome,
                    lastTestConfidence = result.confidenceScore
                )
            )
        }

        val accuracy = if (totalTested > 0) (correct.toFloat() / totalTested.toFloat()) * 100f else 0f
        return BacktestSummary(
            totalTested = totalTested,
            correctPredictions = correct,
            accuracy = accuracy,
            perPatternAccuracy = perPatternCounts
        )
    }
}
