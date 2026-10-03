package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BacktestDao
import com.example.data.dao.JournalDao
import com.example.data.dao.RuleDao
import com.example.data.entity.BacktestSampleEntity
import com.example.data.entity.JournalEntryEntity
import com.example.data.entity.RuleEntity
import com.example.knowledge.KnowledgePackLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [RuleEntity::class, JournalEntryEntity::class, BacktestSampleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ChartMindDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun journalDao(): JournalDao
    abstract fun backtestDao(): BacktestDao

    companion object {
        @Volatile
        private var INSTANCE: ChartMindDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ChartMindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChartMindDatabase::class.java,
                    "chartmind_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context.applicationContext, scope))
                    .build()
                INSTANCE = instance

                // Double check seed in case onCreate already fired in a previous version
                scope.launch(Dispatchers.IO) {
                    if (instance.ruleDao().getRuleCount() == 0) {
                        seedBuiltinRules(context.applicationContext, instance.ruleDao())
                    }
                }

                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedBuiltinRules(context, database.ruleDao())
                    }
                }
            }
        }

        suspend fun seedBuiltinRules(context: Context, ruleDao: RuleDao) {
            try {
                val pack = KnowledgePackLoader.loadKnowledgePack(context)
                val entities = pack.builtinRules.map { br ->
                    var pattern = "CUSTOM"
                    var reqTrend = "ANY"
                    var nearSupp = false
                    var nearRes = false

                    for (c in br.conditions.all) {
                        val strVal = c.value.toString().replace("\"", "")
                        when (c.feature) {
                            "pattern" -> pattern = strVal.uppercase()
                            "trend" -> reqTrend = strVal.uppercase()
                            "near_support" -> nearSupp = strVal.toBoolean()
                            "near_resistance" -> nearRes = strVal.toBoolean()
                            "breakout_up" -> { pattern = "BREAKOUT"; reqTrend = "UP" }
                            "breakout_down" -> { pattern = "BREAKDOWN"; reqTrend = "DOWN" }
                            "fakeout_up" -> { pattern = "FAKEOUT_RESISTANCE"; nearRes = true }
                            "fakeout_down" -> { pattern = "FAKEOUT_SUPPORT"; nearSupp = true }
                            "price_at_ema20" -> { pattern = "PULLBACK_EMA20" }
                        }
                    }

                    RuleEntity(
                        ruleKey = br.id,
                        name = br.name,
                        patternType = pattern,
                        requiredTrend = reqTrend,
                        requireNearSupport = nearSupp,
                        requireNearResistance = nearRes,
                        minConfidence = 0.60f,
                        outcome = br.outcome.uppercase(),
                        weight = br.weight.toFloat(),
                        priorStrength = 10,
                        priorWinRate = 0.50f,
                        winCount = 0,
                        lossCount = 0,
                        notes = "Built-in rule from Knowledge Pack (${br.id})",
                        isEnabled = true,
                        source = "builtin"
                    )
                }
                ruleDao.insertAll(entities)
            } catch (e: Exception) {
                // Fallback manual default presets if pack reading fails
                val fallbackPresets = listOf(
                    RuleEntity(
                        ruleKey = "r1",
                        name = "Hammer at support after downtrend",
                        patternType = "HAMMER",
                        requiredTrend = "DOWN",
                        requireNearSupport = true,
                        outcome = "UP",
                        weight = 0.10f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r2",
                        name = "Shooting star at resistance after uptrend",
                        patternType = "SHOOTING_STAR",
                        requiredTrend = "UP",
                        requireNearResistance = true,
                        outcome = "DOWN",
                        weight = 0.10f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r3",
                        name = "Bullish engulfing at support",
                        patternType = "BULL_ENGULFING",
                        requiredTrend = "ANY",
                        requireNearSupport = true,
                        outcome = "UP",
                        weight = 0.12f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r4",
                        name = "Bearish engulfing at resistance",
                        patternType = "BEAR_ENGULFING",
                        requiredTrend = "ANY",
                        requireNearResistance = true,
                        outcome = "DOWN",
                        weight = 0.12f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r5",
                        name = "Breakout above resistance with retest holding",
                        patternType = "BREAKOUT",
                        requiredTrend = "UP",
                        requireNearResistance = true,
                        outcome = "UP",
                        weight = 0.10f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r6",
                        name = "Breakdown below support with retest failing",
                        patternType = "BREAKDOWN",
                        requiredTrend = "DOWN",
                        requireNearSupport = true,
                        outcome = "DOWN",
                        weight = 0.10f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r7",
                        name = "Fakeout above resistance (rejection)",
                        patternType = "FAKEOUT_RESISTANCE",
                        requiredTrend = "ANY",
                        requireNearResistance = true,
                        outcome = "DOWN",
                        weight = 0.11f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r8",
                        name = "Fakeout below support (rejection)",
                        patternType = "FAKEOUT_SUPPORT",
                        requiredTrend = "ANY",
                        requireNearSupport = true,
                        outcome = "UP",
                        weight = 0.11f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r9",
                        name = "Trend pullback to EMA20 in uptrend",
                        patternType = "PULLBACK_EMA20",
                        requiredTrend = "UP",
                        outcome = "UP",
                        weight = 0.10f,
                        source = "builtin"
                    ),
                    RuleEntity(
                        ruleKey = "r10",
                        name = "Trend pullback to EMA20 in downtrend",
                        patternType = "PULLBACK_EMA20",
                        requiredTrend = "DOWN",
                        outcome = "DOWN",
                        weight = 0.10f,
                        source = "builtin"
                    )
                )
                ruleDao.insertAll(fallbackPresets)
            }
        }
    }
}
