package com.example.model

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
    @SerialName("trend_rules") val trendRules: List<TrendRuleJson>,
    @SerialName("support_resistance") val supportResistance: Map<String, JsonElement>,
    val patterns: List<PatternDefJson>,
    @SerialName("confluence_scoring") val confluenceScoring: ConfluenceScoringConfig,
    @SerialName("no_trade_filters") val noTradeFilters: List<String>,
    @SerialName("expiry_suggestion") val expirySuggestion: Map<String, String> = emptyMap(),
    @SerialName("builtin_rules") val builtinRules: List<BuiltinRuleJson>,
    val learning: LearningConfig,
    val validation: ValidationConfig
)

@Serializable
data class ConfidenceConfig(
    val base: Double = 0.5,
    @SerialName("max_cap") val maxCap: Double = 0.78,
    @SerialName("min_to_signal") val minToSignal: Double = 0.6,
    val method: String = "",
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
data class TrendRuleJson(
    val id: String,
    @SerialName("when") val whenCondition: String = "",
    val label: String? = null,
    val weight: Double? = null
)

@Serializable
data class PatternDefJson(
    val id: String,
    val name: String,
    val direction: String,
    val detect: String,
    @SerialName("context_required") val contextRequired: String? = null,
    @SerialName("weight_with_context") val weightWithContext: Double = 0.0,
    @SerialName("weight_without_context") val weightWithoutContext: Double = 0.0,
    val effect: String? = null,
    val notes: String? = null
)

@Serializable
data class ConfluenceScoringConfig(
    val formula: String,
    val quality: String,
    val components: List<ConfluenceComponentConfig>,
    val output: ConfluenceOutputConfig
)

@Serializable
data class ConfluenceComponentConfig(
    val name: String,
    val range: List<Double>
)

@Serializable
data class ConfluenceOutputConfig(
    val signal: String,
    val confidence: String,
    val reasons: String
)

@Serializable
data class BuiltinRuleJson(
    val id: String,
    val name: String,
    val conditions: BuiltinConditionsGroupJson? = null,
    val outcome: String,
    val weight: Double = 0.1,
    @SerialName("prior_strength") val priorStrength: Int = 10,
    val winCount: Int = 0,
    val lossCount: Int = 0,
    val source: String = "builtin"
)

@Serializable
data class BuiltinConditionsGroupJson(
    val all: List<BuiltinConditionClauseJson> = emptyList()
)

@Serializable
data class BuiltinConditionClauseJson(
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

@Serializable
data class SiteProfileConfig(
    @SerialName("timeframe_selector") val timeframeSelector: String = "VERIFY_IN_DEVTOOLS",
    @SerialName("zoom_method") val zoomMethod: String = "wheel_event_on_chart_canvas",
    @SerialName("candle_colors") val candleColors: Map<String, String> = mapOf("up" to "#26A69A", "down" to "#EF5350")
)
