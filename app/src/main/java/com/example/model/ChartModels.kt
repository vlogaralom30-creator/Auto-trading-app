package com.example.model

/**
 * Represents a detected candlestick's pixel geometry and price action properties.
 * Screen pixel coordinates: smaller Y = higher price, larger Y = lower price.
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
    // Screen geometry
    val bodyTop: Float get() = minOf(openY, closeY)
    val bodyBottom: Float get() = maxOf(openY, closeY)
    val bodyHeight: Float get() = maxOf(1f, bodyBottom - bodyTop)
    val upperWickHeight: Float get() = maxOf(0f, bodyTop - highY)
    val lowerWickHeight: Float get() = maxOf(0f, lowY - bodyBottom)
    val totalRange: Float get() = maxOf(1f, lowY - highY)

    // Knowledge pack feature definitions:
    // range: high - low
    // body: abs(close - open)
    // body_ratio: body / range
    // upper_wick: high - max(open, close)
    // lower_wick: min(open, close) - low
    val range: Float get() = totalRange
    val body: Float get() = bodyHeight
    val bodyRatio: Float get() = body / range
    val upperWick: Float get() = upperWickHeight
    val lowerWick: Float get() = lowerWickHeight

    val upperWickRatio: Float get() = upperWick / range
    val lowerWickRatio: Float get() = lowerWick / range

    // Quick shape classifiers
    val isDoji: Boolean get() = bodyRatio <= 0.10f
    val isHammer: Boolean get() = lowerWick >= 2f * body && upperWick <= 0.15f * range && bodyRatio <= 0.35f
    val isShootingStar: Boolean get() = upperWick >= 2f * body && lowerWick <= 0.15f * range && bodyRatio <= 0.35f
    val isPinBar: Boolean get() = isHammer || isShootingStar
}

enum class TrendDirection {
    UPTREND,
    DOWNTREND,
    SIDEWAYS
}

data class SwingPoint(
    val candleIndex: Int,
    val x: Float,
    val y: Float, // screen Y
    val isHigh: Boolean,
    val strength: Int = 1
)

data class SupportResistanceZone(
    val yLevel: Float,
    val halfHeight: Float = 4f,
    val isSupport: Boolean,
    val touchCount: Int,
    val rejectionCount: Int = 0,
    val strengthScore: Float
) {
    val thickness: Float get() = halfHeight * 2f
    val topY: Float get() = yLevel - halfHeight
    val bottomY: Float get() = yLevel + halfHeight

    fun containsPrice(priceY: Float): Boolean {
        return priceY in (yLevel - halfHeight)..(yLevel + halfHeight)
    }
}

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
    val patternId: String,
    val patternName: String,
    val candleIndex: Int,
    val x: Float,
    val y: Float,
    val direction: String, // "UP", "DOWN", "NEUTRAL"
    val confidence: Float,
    val weight: Float = 0.10f,
    val quality: Float = 1.0f,
    val hasContext: Boolean = false,
    val description: String
)

data class ScoringContribution(
    val componentName: String,
    val signedContribution: Float,
    val description: String
)

data class ChartAnalysisResult(
    val bitmapWidth: Int = 0,
    val bitmapHeight: Int = 0,
    val candles: List<Candle> = emptyList(),
    val swings: List<SwingPoint> = emptyList(),
    val srZones: List<SupportResistanceZone> = emptyList(),
    val trendLines: List<TrendLine> = emptyList(),
    val trendDirection: TrendDirection = TrendDirection.SIDEWAYS,
    val atr14: Float = 0f,
    val breakoutHint: String? = null,
    val detectedPatterns: List<PatternMatchResult> = emptyList(),
    val ema20Points: List<Pair<Float, Float>> = emptyList(),
    val ema50Points: List<Pair<Float, Float>> = emptyList(),
    val rsiValue: Float? = null,
    val momentumScore: Float = 0f,
    val overallSignal: String = "NEUTRAL", // "UP", "DOWN", "NEUTRAL"
    val pUp: Float = 0.5f,
    val confidenceScore: Float = 0.5f,
    val reasonList: List<String> = emptyList(),
    val topContributingReasons: List<String> = emptyList(),
    val matchedRule: String? = null,
    val contributingRuleNames: List<String> = emptyList(),
    val suggestedExpiry: String = "1m to 3m",
    val nearestSupportDist: Float? = null,
    val nearestResistanceDist: Float? = null,
    val analysisTimeMs: Long = 0L,
    val isBlocked: Boolean = false,
    val blockedReason: String? = null,
    val timeframe: String = "1m",
    val timestamp: Long = System.currentTimeMillis()
) {
    val ema9Points: List<Pair<Float, Float>> get() = ema20Points
    val ema21Points: List<Pair<Float, Float>> get() = ema50Points
}

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "TradingView",
    val url: String = "https://www.tradingview.com/chart/",
    val isDesktopMode: Boolean = true
)

data class ColorCalibration(
    val minSaturation: Float = 0.35f,
    val minValue: Float = 0.30f,
    val bullishHueMin: Float = 70f,
    val bullishHueMax: Float = 175f,
    val bearishHueMin: Float = 335f,
    val bearishHueMax: Float = 25f,
    val bullishRed: Int = 0x26,
    val bullishGreen: Int = 0xA6,
    val bullishBlue: Int = 0x9A,
    val bearishRed: Int = 0xEF,
    val bearishGreen: Int = 0x53,
    val bearishBlue: Int = 0x50,
    val tolerance: Int = 45
)

data class OverlayLayerSettings(
    val showCandles: Boolean = true,
    val showCandleBoxes: Boolean = showCandles,
    val showSRZones: Boolean = true,
    val showSupportResistance: Boolean = showSRZones,
    val showTrendlines: Boolean = true,
    val showPatternLabels: Boolean = true,
    val showPatterns: Boolean = showPatternLabels,
    val showIndicators: Boolean = true,
    val showEma: Boolean = showIndicators,
    val showSignalArrows: Boolean = true,
    val opacity: Float = 0.90f
)

data class MultiTimeframeAnalysisResult(
    val tf15m: ChartAnalysisResult? = null,
    val tf5m: ChartAnalysisResult? = null,
    val tf1m: ChartAnalysisResult? = null,
    val finalSignal: String = "NEUTRAL",
    val finalConfidence: Float = 0.5f,
    val isAgreement: Boolean = false,
    val agreementNote: String = "",
    val topReasons: List<String> = emptyList()
)
