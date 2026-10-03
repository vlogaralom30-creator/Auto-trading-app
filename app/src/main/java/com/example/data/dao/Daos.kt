package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BacktestSampleEntity
import com.example.data.entity.JournalEntryEntity
import com.example.data.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY weight DESC, createdAt DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1")
    suspend fun getEnabledRulesSync(): List<RuleEntity>

    @Query("SELECT * FROM rules")
    suspend fun getAllRulesSync(): List<RuleEntity>

    @Query("SELECT COUNT(*) FROM rules")
    suspend fun getRuleCount(): Int

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Long): RuleEntity?

    @Query("SELECT * FROM rules WHERE name = :name LIMIT 1")
    suspend fun getRuleByName(name: String): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RuleEntity>)

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM rules")
    suspend fun deleteAllRules()
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE outcomeResult != 'PENDING'")
    suspend fun getCompletedEntriesSync(): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT COUNT(*) FROM journal_entries WHERE timestamp >= :sinceTimestamp")
    suspend fun getCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun getTotalJournalCount(): Int

    @Query("SELECT COUNT(*) FROM journal_entries WHERE isDemo = 1")
    suspend fun getDemoSignalCount(): Int

    @Query("SELECT outcomeResult FROM journal_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOutcomes(limit: Int): List<String>
}

@Dao
interface BacktestDao {
    @Query("SELECT * FROM backtest_samples ORDER BY timestamp DESC")
    fun getAllSamples(): Flow<List<BacktestSampleEntity>>

    @Query("SELECT * FROM backtest_samples ORDER BY timestamp DESC")
    suspend fun getAllSamplesSync(): List<BacktestSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: BacktestSampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<BacktestSampleEntity>)

    @Update
    suspend fun updateSample(sample: BacktestSampleEntity)

    @Query("DELETE FROM backtest_samples WHERE id = :id")
    suspend fun deleteSampleById(id: Long)

    @Query("SELECT COUNT(*) FROM backtest_samples")
    suspend fun getSampleCount(): Int
}
