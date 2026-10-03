package com.example.knowledge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class KnowledgePack(
    @SerialName("schema_version") val schemaVersion: String,
    val name: String,
    val disclaimer: String,
    val confidence: ConfidenceConfig,
    @SerialName("multi_timeframe") val multiTimeframe: MultiTimeframeConfig,
    @SerialName("site_profiles") val siteProfiles: Map<String, JsonElement>,
    val features: Map<String, JsonElement>,
    @SerialName("trend_rules") val trendRules: List<TrendRuleConfig>,
    @SerialName("support_resistance") val supportResistance: Map<String, JsonElement>,
    val patterns: List<PatternConfig>,
    @SerialName("confluence_scoring") val confluenceScoring: ConfluenceScoringConfig,
    @SerialName("no_trade_filters") val noTradeFilters: List<String>,
    @SerialName("expiry_suggestion") val expirySuggestion: Map<String, String>,
    @SerialName("builtin_rules") val builtinRules: List<BuiltinRuleConfig>,
    val learning: LearningConfig,
    val validation: ValidationConfig
)

@Serializable
data class ConfidenceConfig(
    val base: Double = 0.5,
    @SerialName("max_cap") val maxCap: Double = 0.78,
    @SerialName("min_to_signal") val minToSignal: Double = 0.6,
    val method: String = "logistic(sum(signed_weight_i * quality_i))",
    val calibration: String = ""
)

@Serializable
data class MultiTimeframeConfig(
    @SerialName("scan_order") val scanOrder: List<String> = listOf("15m", "5m", "1m"),
    val roles: Map<String, String> = emptyMap(),
    @SerialName("min_timeframes_agree") val minTimeframesAgree: Int = 2,
    val rule: String = "",
    @SerialName("visible_candles_target") val visibleCandlesTarget: List<Int> = listOf(60, 90),
    @SerialName("auto_view") val autoView: AutoViewConfig? = null
)

@Serializable
data class AutoViewConfig(
    val enabled: Boolean = true,
    val steps: List<String> = emptyList(),
    val methods: List<String> = emptyList(),
    val note: String = ""
)

@Serializable
data class TrendRuleConfig(
    val id: String,
    val whenCondition: String? = null,
    val label: String? = null,
    val weight: Double = 0.05
)

@Serializable
data class PatternConfig(
    val id: String,
    val name: String,
    val direction: String,
    val detect: String,
    @SerialName("context_required") val contextRequired: String? = null,
    @SerialName("weight_with_context") val weightWithContext: Double = 0.1,
    @SerialName("weight_without_context") val weightWithoutContext: Double = 0.0,
    val effect: String? = null,
    val notes: String? = null
)

@Serializable
data class ConfluenceScoringConfig(
    val formula: String,
    val quality: String,
    val components: List<ConfluenceComponentConfig> = emptyList(),
    val output: Map<String, String> = emptyMap()
)

@Serializable
data class ConfluenceComponentConfig(
    val name: String,
    val range: List<Double>
)

@Serializable
data class BuiltinRuleConfig(
    val id: String,
    val name: String,
    val conditions: RuleConditionsConfig,
    val outcome: String,
    val weight: Double
)

@Serializable
data class RuleConditionsConfig(
    val all: List<ConditionItemConfig> = emptyList()
)

@Serializable
data class ConditionItemConfig(
    val feature: String,
    val op: String,
    val value: JsonElement
)

@Serializable
data class LearningConfig(
    @SerialName("on_result") val onResult: String = "",
    @SerialName("measured_win_rate") val measuredWinRate: String = "",
    val blend: String = "",
    @SerialName("weight_from_win_rate") val weightFromWinRate: String = "",
    @SerialName("disable_rule") val disableRule: String = "",
    @SerialName("per_context_stats") val perContextStats: String = "",
    @SerialName("overfit_guard") val overfitGuard: String = ""
)

@Serializable
data class ValidationConfig(
    val backtest: String = "",
    @SerialName("demo_phase") val demoPhase: String = "",
    @SerialName("breakeven_note") val breakevenNote: String = ""
)
