package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.JournalDao
import com.example.data.dao.RuleDao
import com.example.data.entity.JournalEntryEntity
import com.example.data.entity.RuleEntity
import com.example.engine.KnowledgePackLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [RuleEntity::class, JournalEntryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ChartMindDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: ChartMindDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ChartMindDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    ChartMindDatabase::class.java,
                    "chartmind_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback(appContext, scope))
                    .build()
                INSTANCE = instance
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
                        seedFromKnowledgePack(context, database.ruleDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // If table is empty on reopen (e.g. migration), re-seed
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        if (database.ruleDao().countRules() == 0) {
                            seedFromKnowledgePack(context, database.ruleDao())
                        }
                    }
                }
            }

            private suspend fun seedFromKnowledgePack(context: Context, ruleDao: RuleDao) {
                try {
                    val pack = KnowledgePackLoader.load(context)
                    val ruleEntities = KnowledgePackLoader.toRuleEntities(pack.builtinRules)
                    ruleDao.insertAll(ruleEntities)
                } catch (e: Exception) {
                    // Fallback baseline rule if asset fails to load
                    ruleDao.insertRule(
                        RuleEntity(
                            ruleId = "r1",
                            name = "Hammer at support after downtrend",
                            patternType = "HAMMER",
                            requiredTrend = "DOWN",
                            requireNearSupport = true,
                            requireNearResistance = false,
                            minConfidence = 0.60f,
                            outcome = "UP",
                            weight = 0.10f,
                            priorStrength = 10,
                            source = "builtin",
                            notes = "Baseline hammer bounce rule"
                        )
                    )
                }
            }
        }
    }
}
