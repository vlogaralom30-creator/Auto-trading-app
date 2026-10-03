package com.example.model

/**
 * Represents a detected candlestick's pixel geometry and price action properties.
 * Formulas strictly aligned with ChartMind Knowledge Pack schema.
 */
data class Candle(
    val index: Int,
    val openY: Float,
    val closeY: Float,
    val highY: Float,
    val lowY: Float,
    val centerX: Float,
    val bodyLeft: Float,
    val bodyRight: Float,
    val isBullish: Boolean
) {
    // In screen coordinates: smaller Y = higher price
    val bodyTop: Float get() = minOf(openY, closeY)
    val bodyBottom: Float get() = maxOf(openY, closeY)
    val body: Float get() = maxOf(0.1f, bodyBottom - bodyTop)
    val upperWick: Float get() = maxOf(0f, bodyTop - highY)
    val lowerWick: Float get() = maxOf(0f, lowY - bodyBottom)
    val range: Float get() = maxOf(1f, lowY - highY)

    // Candlestick anatomy ratios
    val bodyRatio: Float get() = body / range
    val upperWickRatio: Float get() = upperWick / range
    val lowerWickRatio: Float get() = lowerWick / range

    // Quick shape classifiers from Knowledge Pack
    val isDoji: Boolean get() = bodyRatio <= 0.10f
    val isHammer: Boolean get() = lowerWick >= (2f * body) && upperWick <= (0.15f * range) && bodyRatio <= 0.35f
    val isShootingStar: Boolean get() = upperWick >= (2f * body) && lowerWick <= (0.15f * range) && bodyRatio <= 0.35f
    val isPinBarBull: Boolean get() = lowerWick >= (0.66f * range) && bodyBottom <= highY + (0.34f * range)
    val isPinBarBear: Boolean get() = upperWick >= (0.66f * range) && bodyTop >= lowY - (0.34f * range)
    val isMarubozu: Boolean get() = bodyRatio >= 0.85f
}

enum class TrendDirection {
    UPTREND,
    DOWNTREND,
    SIDEWAYS
}

data class SwingPoint(
    val candleIndex: Int,
    val x: Float,
    val y: Float,
    val isHigh: Boolean,
    val strength: Int = 3
)

data class SupportResistanceZone(
    val yLevel: Float,
    val thickness: Float = 6f,
    val isSupport: Boolean,
    val touchCount: Int,
    val strengthScore: Float,
    val rejectionsCount: Int = 0
)

data class TrendLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val slope: Float,
    val isSupport: Boolean,
    val label: String
)

data class PatternMatchResult(
    val patternName: String,
    val candleIndex: Int,
    val x: Float,
    val y: Float,
    val direction: String, // "UP", "DOWN", "NEUTRAL", "BREAKOUT", "WITH_CANDLE"
    val confidence: Float,
    val weight: Float,
    val description: String,
    val hasContext: Boolean = true
)

data class ChartAnalysisResult(
    val bitmapWidth: Int = 0,
    val bitmapHeight: Int = 0,
    val candles: List<Candle> = emptyList(),
    val swings: List<SwingPoint> = emptyList(),
    val srZones: List<SupportResistanceZone> = emptyList(),
    val trendLines: List<TrendLine> = emptyList(),
    val trendDirection: TrendDirection = TrendDirection.SIDEWAYS,
    val breakoutHint: String? = null,
    val detectedPatterns: List<PatternMatchResult> = emptyList(),
    val ema9Points: List<Pair<Float, Float>> = emptyList(),
    val ema21Points: List<Pair<Float, Float>> = emptyList(),
    val rsiValue: Float? = null,
    val atr: Float = 0f,
    val momentumScore: Float = 0f,
    val overallSignal: String = "NEUTRAL", // "UP", "DOWN", "NEUTRAL"
    val confidenceScore: Float = 0f,
    val pUp: Float = 0.5f,
    val isBlockedByFilter: Boolean = false,
    val blockReason: String? = null,
    val reasonList: List<String> = emptyList(),
    val matchedRule: String? = null,
    val suggestedExpiry: String = "1m to 3m",
    val nearestSupportDist: Float? = null,
    val nearestResistanceDist: Float? = null,
    val timeframe: String = "1m",
    val higherTfTrend: String = "ANY",
    val analysisTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class OverlayLayerSettings(
    val showCandles: Boolean = true,
    val showSRZones: Boolean = true,
    val showTrendlines: Boolean = true,
    val showPatternLabels: Boolean = true,
    val showIndicators: Boolean = true,
    val showSignalArrows: Boolean = true,
    val opacity: Float = 0.85f
)

data class ColorCalibration(
    val bullishHueMin: Float = 70f,
    val bullishHueMax: Float = 170f,
    val bearishHueMin: Float = 340f,
    val bearishHueMax: Float = 25f,
    val minSaturation: Float = 0.35f,
    val minValue: Float = 0.35f
)

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "TradingView",
    val url: String = "https://www.tradingview.com/chart/",
    val isDesktopMode: Boolean = false
)

data class SiteTimeframeProfile(
    val siteKey: String, // "tradingview", "quotex", "exness", "custom"
    val timeframe1mSelector: String = "[data-value='1m'], button:contains('1m')",
    val timeframe5mSelector: String = "[data-value='5m'], button:contains('5m')",
    val timeframe15mSelector: String = "[data-value='15m'], button:contains('15m')",
    val zoomMethod: String = "wheel_event_on_chart_canvas"
)
