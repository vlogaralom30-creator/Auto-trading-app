package com.example.engine

import android.content.Context
import com.example.data.entity.RuleEntity
import com.example.model.BuiltinRuleJson
import com.example.model.KnowledgePack
import com.example.model.SiteProfileConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object KnowledgePackLoader {

    private const val ASSET_FILE_NAME = "chartmind_knowledge_pack.json"
    private const val EXPECTED_SCHEMA_VERSION = "1.0"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    private var cachedPack: KnowledgePack? = null

    fun load(context: Context): KnowledgePack {
        cachedPack?.let { return it }

        val jsonString = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        val pack = json.decodeFromString<KnowledgePack>(jsonString)

        if (pack.schemaVersion != EXPECTED_SCHEMA_VERSION) {
            throw IllegalStateException(
                "Unsupported Knowledge Pack schema version: ${pack.schemaVersion}. Expected: $EXPECTED_SCHEMA_VERSION"
            )
        }

        cachedPack = pack
        return pack
    }

    fun getOrNull(): KnowledgePack? = cachedPack

    fun toRuleEntities(builtinRules: List<BuiltinRuleJson>): List<RuleEntity> {
        return builtinRules.map { br ->
            var pattern = "CUSTOM"
            var trend = "ANY"
            var nearSupport = false
            var nearResistance = false

            br.conditions?.all?.forEach { clause ->
                val feat = clause.feature
                val valueStr = clause.value.jsonPrimitive.content
                when (feat) {
                    "pattern" -> pattern = valueStr
                    "trend" -> trend = valueStr
                    "near_support" -> nearSupport = valueStr.equals("true", ignoreCase = true)
                    "near_resistance" -> nearResistance = valueStr.equals("true", ignoreCase = true)
                    "breakout_up" -> {
                        pattern = "BREAKOUT"
                        nearResistance = true
                    }
                    "breakout_down" -> {
                        pattern = "BREAKOUT"
                        nearSupport = true
                    }
                    "fakeout_up" -> {
                        pattern = "FAKEOUT"
                        nearResistance = true
                    }
                    "fakeout_down" -> {
                        pattern = "FAKEOUT"
                        nearSupport = true
                    }
                    "price_at_ema20" -> {
                        if (pattern == "CUSTOM") pattern = "EMA_PULLBACK"
                    }
                }
            }

            RuleEntity(
                ruleId = br.id,
                name = br.name,
                patternType = pattern,
                requiredTrend = trend,
                requireNearSupport = nearSupport,
                requireNearResistance = nearResistance,
                minConfidence = 0.60f,
                outcome = br.outcome,
                weight = br.weight.toFloat(),
                priorStrength = br.priorStrength,
                winCount = br.winCount,
                lossCount = br.lossCount,
                source = "builtin",
                notes = "Built-in rule from Knowledge Pack (${br.id})",
                isEnabled = true
            )
        }
    }

    fun parseSiteProfiles(pack: KnowledgePack): Map<String, SiteProfileConfig> {
        val result = mutableMapOf<String, SiteProfileConfig>()
        pack.siteProfiles.forEach { (key, element) ->
            if (element.toString().startsWith("{")) {
                try {
                    val config = json.decodeFromJsonElement<SiteProfileConfig>(element)
                    result[key] = config
                } catch (_: Exception) {
                }
            }
        }
        return result
    }
}
