package com.example.data

import com.example.data.dao.JournalDao
import com.example.data.dao.RuleDao
import com.example.data.entity.JournalEntryEntity
import com.example.data.entity.RuleEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

data class ContextStat(
    val category: String, // "Site", "Pair", "Timeframe", "Session"
    val name: String,
    val total: Int,
    val wins: Int,
    val winRate: Float
)

data class RuleLearningReport(
    val updatedRules: List<RuleEntity>,
    val disabledRuleNames: List<String>
)

class ChartMindRepository(
    private val ruleDao: RuleDao,
    private val journalDao: JournalDao
) {
    val allRules: Flow<List<RuleEntity>> = ruleDao.getAllRules()
    val allJournalEntries: Flow<List<JournalEntryEntity>> = journalDao.getAllEntries()

    suspend fun getEnabledRules(): List<RuleEntity> = ruleDao.getEnabledRulesSync()

    suspend fun insertRule(rule: RuleEntity): Long = ruleDao.insertRule(rule)

    suspend fun updateRule(rule: RuleEntity) = ruleDao.updateRule(rule)

    suspend fun deleteRule(id: Long) = ruleDao.deleteRuleById(id)

    /**
     * Updates all rules that contributed to the signal per Knowledge Pack learning formulas.
     * Enforces:
     * - Bayesian effective win rate blend
     * - Overfit guard (weight delta <= 0.02 per sample)
     * - Auto-disable rule if n >= 30 and measured_win_rate < 0.48
     */
    suspend fun applyLearningToRules(
        contributingRuleNamesOrIds: List<String>,
        isWin: Boolean
    ): RuleLearningReport {
        if (contributingRuleNamesOrIds.isEmpty()) return RuleLearningReport(emptyList(), emptyList())

        val disabledNames = mutableListOf<String>()
        val updated = mutableListOf<RuleEntity>()
        val allCurrentRules = ruleDao.getEnabledRulesSync()

        for (target in contributingRuleNamesOrIds.map { it.trim() }.filter { it.isNotBlank() }) {
            val rule = allCurrentRules.find {
                it.ruleId.equals(target, ignoreCase = true) ||
                        it.name.equals(target, ignoreCase = true) ||
                        it.patternType.equals(target, ignoreCase = true)
            } ?: ruleDao.getRuleByName(target) ?: ruleDao.getRuleByRuleId(target)

            if (rule != null) {
                val newWinCount = if (isWin) rule.winCount + 1 else rule.winCount
                val newLossCount = if (!isWin) rule.lossCount + 1 else rule.lossCount
                val n = newWinCount + newLossCount

                // measured_win_rate = (win + 1) / (win + loss + 2)
                val measuredWinRate = (newWinCount + 1f) / (newWinCount + newLossCount + 2f)

                // effective_win_rate = (prior_win_rate * prior_strength + measured_win_rate * n) / (prior_strength + n)
                val priorWinRate = 0.5f + (rule.priorWeight * 2f).coerceIn(-0.4f, 0.4f)
                val effectiveWinRate = ((priorWinRate * rule.priorStrength) + (measuredWinRate * n)) / (rule.priorStrength + n)

                // weight = (effective_win_rate - 0.5) * 0.5
                val targetWeight = ((effectiveWinRate - 0.5f) * 0.5f).coerceIn(0.01f, 0.50f)

                // overfit_guard: never let a single user sample change a weight by more than 0.02
                val weightDelta = (targetWeight - rule.weight).coerceIn(-0.02f, 0.02f)
                val newWeight = (rule.weight + weightDelta).coerceIn(0.01f, 0.50f)

                // disable_rule: if n >= 30 and measured_win_rate < 0.48, disable rule and tell the user
                val shouldDisable = n >= 30 && measuredWinRate < 0.48f
                val newIsEnabled = if (shouldDisable) false else rule.isEnabled

                if (shouldDisable && rule.isEnabled) {
                    disabledNames.add(rule.name)
                }

                val updatedRule = rule.copy(
                    winCount = newWinCount,
                    lossCount = newLossCount,
                    weight = newWeight,
                    isEnabled = newIsEnabled
                )
                ruleDao.updateRule(updatedRule)
                updated.add(updatedRule)
            }
        }
        return RuleLearningReport(updated, disabledNames)
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

    suspend fun getDemoSignalsCount(): Int = journalDao.getDemoSignalCount()

    suspend fun getContextStatistics(): List<ContextStat> {
        val completed = journalDao.getCompletedTrades()
        if (completed.isEmpty()) return emptyList()

        val results = mutableListOf<ContextStat>()

        // Sites
        completed.groupBy { it.site }.forEach { (site, list) ->
            val wins = list.count { it.outcomeResult == "WIN" }
            results.add(ContextStat("Site", site, list.size, wins, (wins.toFloat() / list.size) * 100f))
        }

        // Pairs
        completed.groupBy { it.assetPair }.forEach { (pair, list) ->
            val wins = list.count { it.outcomeResult == "WIN" }
            results.add(ContextStat("Pair", pair, list.size, wins, (wins.toFloat() / list.size) * 100f))
        }

        // Timeframes
        completed.groupBy { it.timeframe }.forEach { (tf, list) ->
            val wins = list.count { it.outcomeResult == "WIN" }
            results.add(ContextStat("Timeframe", tf, list.size, wins, (wins.toFloat() / list.size) * 100f))
        }

        // Sessions
        completed.groupBy { it.session }.forEach { (session, list) ->
            val wins = list.count { it.outcomeResult == "WIN" }
            results.add(ContextStat("Session", session, list.size, wins, (wins.toFloat() / list.size) * 100f))
        }

        return results.sortedByDescending { it.winRate }
    }
}
