package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.ChartAnalysisResult
import com.example.model.OverlayLayerSettings
import com.example.model.PatternMatchResult
import com.example.model.SupportResistanceZone
import com.example.model.TrendDirection
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-performance transparent Compose Canvas overlay that sits directly above
 * the in-app WebView. Renders detected S/R zones, trendlines, candlestick patterns,
 * indicators, and entry arrows with exact coordinate mapping and interactive touch inspection.
 */
@Composable
fun ChartOverlayCanvas(
    result: ChartAnalysisResult?,
    layerSettings: OverlayLayerSettings,
    modifier: Modifier = Modifier
) {
    if (result == null || result.candles.isEmpty()) return

    // Pulse animation for entry signal arrow & key pattern halos
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_factor"
    )

    // Interactive inspection state on tap
    var inspectedItem by remember { mutableStateOf<String?>(null) }
    var inspectionPos by remember { mutableStateOf<Offset?>(null) }

    Box(modifier = modifier.fillMaxSize().testTag("chart_overlay_canvas_box")) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(result) {
                    detectTapGestures { tapOffset ->
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val scaleX = if (result.bitmapWidth > 0) canvasWidth / result.bitmapWidth else 1f
                        val scaleY = if (result.bitmapHeight > 0) canvasHeight / result.bitmapHeight else 1f

                        // Check if tapped near an S/R zone
                        val tappedZone = result.srZones.firstOrNull { zone ->
                            val zoneY = zone.yLevel * scaleY
                            val thickness = maxOf(14f, zone.thickness * scaleY)
                            abs(tapOffset.y - zoneY) <= (thickness + 16f)
                        }

                        // Check if tapped near a pattern
                        val tappedPattern = result.detectedPatterns.firstOrNull { pat ->
                            val patX = pat.x * scaleX
                            val patY = pat.y * scaleY
                            val dist = sqrt((tapOffset.x - patX) * (tapOffset.x - patX) + (tapOffset.y - patY) * (tapOffset.y - patY))
                            dist <= 40f
                        }

                        if (tappedPattern != null) {
                            inspectedItem = "${tappedPattern.patternName} (${(tappedPattern.confidence * 100).toInt()}%)\n${tappedPattern.description}"
                            inspectionPos = tapOffset
                        } else if (tappedZone != null) {
                            val type = if (tappedZone.isSupport) "SUPPORT FLOOR" else "RESISTANCE CEILING"
                            inspectedItem = "$type • ${tappedZone.touchCount} Touches\nStrength: ${(tappedZone.strengthScore * 100).toInt()}% • Level: ${tappedZone.yLevel.toInt()}px"
                            inspectionPos = tapOffset
                        } else {
                            // Tap outside dismisses inspection tooltip
                            inspectedItem = null
                            inspectionPos = null
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Precise coordinate scaling mapping from captured bitmap pixels to current canvas viewport
            val scaleX = if (result.bitmapWidth > 0) canvasWidth / result.bitmapWidth else 1f
            val scaleY = if (result.bitmapHeight > 0) canvasHeight / result.bitmapHeight else 1f

            val globalAlpha = layerSettings.opacity.coerceIn(0.1f, 1.0f)

            // 1. Draw Support & Resistance Zones
            if (layerSettings.showSRZones) {
                drawSupportResistanceZones(
                    scope = this,
                    zones = result.srZones,
                    canvasWidth = canvasWidth,
                    scaleY = scaleY,
                    globalAlpha = globalAlpha
                )
            }

            // 2. Draw Trendlines & Dynamic Channels
            if (layerSettings.showTrendlines) {
                drawTrendlines(
                    scope = this,
                    lines = result.trendLines,
                    canvasWidth = canvasWidth,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    globalAlpha = globalAlpha
                )
            }

            // 3. Draw Indicators (EMA 9, EMA 21, RSI thresholds)
            if (layerSettings.showIndicators) {
                drawTechnicalIndicators(
                    scope = this,
                    ema9 = result.ema9Points,
                    ema21 = result.ema21Points,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    globalAlpha = globalAlpha
                )
            }

            // 4. Draw Pattern Halos & Identifiers
            if (layerSettings.showPatternLabels) {
                drawPatternMarkers(
                    scope = this,
                    patterns = result.detectedPatterns,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    pulseFactor = pulseFactor,
                    globalAlpha = globalAlpha
                )
            }

            // 5. Draw Entry Crosshair & Signal Direction Arrow
            if (layerSettings.showSignalArrows && result.overallSignal != "NEUTRAL") {
                drawEntrySignal(
                    scope = this,
                    result = result,
                    canvasWidth = canvasWidth,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    pulseFactor = pulseFactor,
                    globalAlpha = globalAlpha
                )
            }

            // 6. Draw Interactive Inspection Tooltip if user tapped an element
            if (inspectedItem != null && inspectionPos != null) {
                drawInspectionTooltip(
                    scope = this,
                    text = inspectedItem!!,
                    position = inspectionPos!!,
                    canvasWidth = canvasWidth
                )
            }
        }
    }
}

/**
 * Renders glowing S/R horizontal zones with vertical gradients, dashed equilibrium lines, and price badges.
 */
private fun drawSupportResistanceZones(
    scope: DrawScope,
    zones: List<SupportResistanceZone>,
    canvasWidth: Float,
    scaleY: Float,
    globalAlpha: Float
) {
    for (zone in zones) {
        val y = zone.yLevel * scaleY
        val thickness = maxOf(12f, zone.thickness * scaleY)
        val zoneColor = if (zone.isSupport) NeonGreen else NeonRed
        val strength = zone.strengthScore.coerceIn(0.4f, 1.0f)

        // Gradient shaded zone band for modern neon look
        val brush = Brush.verticalGradient(
            colors = listOf(
                zoneColor.copy(alpha = 0.05f * globalAlpha * strength),
                zoneColor.copy(alpha = 0.28f * globalAlpha * strength),
                zoneColor.copy(alpha = 0.05f * globalAlpha * strength)
            ),
            startY = y - thickness / 2f,
            endY = y + thickness / 2f
        )

        scope.drawRect(
            brush = brush,
            topLeft = Offset(0f, y - thickness / 2f),
            size = Size(canvasWidth, thickness)
        )

        // Center dashed equilibrium line
        scope.drawLine(
            color = zoneColor.copy(alpha = 0.90f * globalAlpha),
            start = Offset(0f, y),
            end = Offset(canvasWidth, y),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f), 0f)
        )

        // Right-aligned level tag badge
        val badgeText = if (zone.isSupport) "SUPP (${zone.touchCount}x)" else "RES (${zone.touchCount}x)"
        scope.drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 26f
                isAntiAlias = true
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
            }
            val bgPaint = android.graphics.Paint().apply {
                color = if (zone.isSupport) 0xEE008F39.toInt() else 0xEEA0142C.toInt()
                isAntiAlias = true
            }

            val textWidth = textPaint.measureText(badgeText)
            val badgeLeft = canvasWidth - textWidth - 32f
            val badgeTop = y - 20f
            val badgeRight = canvasWidth - 8f
            val badgeBottom = y + 16f

            drawRoundRect(badgeLeft, badgeTop, badgeRight, badgeBottom, 8f, 8f, bgPaint)
            drawText(badgeText, badgeLeft + 12f, y + 8f, textPaint)
        }
    }
}

/**
 * Renders linear regression trendlines with terminal node dots and glowing strokes.
 */
private fun drawTrendlines(
    scope: DrawScope,
    lines: List<com.example.model.TrendLine>,
    canvasWidth: Float,
    scaleX: Float,
    scaleY: Float,
    globalAlpha: Float
) {
    for (line in lines) {
        val start = Offset(line.startX * scaleX, line.startY * scaleY)
        val end = Offset(minOf(canvasWidth, line.endX * scaleX), line.endY * scaleY)
        val lineColor = if (line.isSupport) NeonCyan else NeonYellow

        // Outer glow
        scope.drawLine(
            color = lineColor.copy(alpha = 0.25f * globalAlpha),
            start = start,
            end = end,
            strokeWidth = 8f
        )
        // Solid core
        scope.drawLine(
            color = lineColor.copy(alpha = 0.95f * globalAlpha),
            start = start,
            end = end,
            strokeWidth = 3f
        )

        // Terminal points
        scope.drawCircle(
            color = lineColor.copy(alpha = globalAlpha),
            radius = 5f,
            center = start
        )
        scope.drawCircle(
            color = lineColor.copy(alpha = globalAlpha),
            radius = 6f,
            center = end
        )
    }
}

/**
 * Renders EMA indicator curves across candle closes.
 */
private fun drawTechnicalIndicators(
    scope: DrawScope,
    ema9: List<Pair<Float, Float>>,
    ema21: List<Pair<Float, Float>>,
    scaleX: Float,
    scaleY: Float,
    globalAlpha: Float
) {
    // EMA 9 (Fast - Cyan)
    if (ema9.size >= 2) {
        val path9 = Path().apply {
            val first = ema9.first()
            moveTo(first.first * scaleX, first.second * scaleY)
            for (i in 1 until ema9.size) {
                val pt = ema9[i]
                lineTo(pt.first * scaleX, pt.second * scaleY)
            }
        }
        scope.drawPath(
            path = path9,
            color = NeonCyan.copy(alpha = 0.85f * globalAlpha),
            style = Stroke(width = 2.5f)
        )
    }

    // EMA 21 (Slow - Purple)
    if (ema21.size >= 2) {
        val path21 = Path().apply {
            val first = ema21.first()
            moveTo(first.first * scaleX, first.second * scaleY)
            for (i in 1 until ema21.size) {
                val pt = ema21[i]
                lineTo(pt.first * scaleX, pt.second * scaleY)
            }
        }
        scope.drawPath(
            path = path21,
            color = NeonPurple.copy(alpha = 0.85f * globalAlpha),
            style = Stroke(width = 2.5f)
        )
    }
}

/**
 * Renders pattern halos and tags above/below candles with pulsing animations.
 */
private fun drawPatternMarkers(
    scope: DrawScope,
    patterns: List<PatternMatchResult>,
    scaleX: Float,
    scaleY: Float,
    pulseFactor: Float,
    globalAlpha: Float
) {
    for (pat in patterns) {
        val x = pat.x * scaleX
        val y = pat.y * scaleY
        val isBull = pat.direction == "BULLISH"
        val patColor = if (isBull) NeonGreen else if (pat.direction == "BEARISH") NeonRed else NeonYellow

        // Pulsing outer halo
        scope.drawCircle(
            color = patColor.copy(alpha = 0.25f * globalAlpha),
            radius = 16f * pulseFactor,
            center = Offset(x, y)
        )
        // Core ring
        scope.drawCircle(
            color = patColor.copy(alpha = 0.90f * globalAlpha),
            radius = 16f,
            center = Offset(x, y),
            style = Stroke(width = 2.5f)
        )

        // Label pill
        val label = "${pat.patternName} ${(pat.confidence * 100).toInt()}%"
        val labelY = if (isBull) y + 34f else y - 26f

        scope.drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 22f
                isAntiAlias = true
                typeface = android.graphics.Typeface.MONOSPACE
            }
            val bgPaint = android.graphics.Paint().apply {
                color = if (isBull) 0xE6006424.toInt() else 0xE68A0E25.toInt()
                isAntiAlias = true
            }

            val textWidth = textPaint.measureText(label)
            val left = x - (textWidth / 2f) - 8f
            val right = x + (textWidth / 2f) + 8f
            val top = labelY - 18f
            val bottom = labelY + 8f

            drawRoundRect(left, top, right, bottom, 6f, 6f, bgPaint)
            drawText(label, x - (textWidth / 2f), labelY + 2f, textPaint)
        }
    }
}

/**
 * Draws entry signal arrow with crosshair on latest candle.
 */
private fun drawEntrySignal(
    scope: DrawScope,
    result: ChartAnalysisResult,
    canvasWidth: Float,
    scaleX: Float,
    scaleY: Float,
    pulseFactor: Float,
    globalAlpha: Float
) {
    val lastCandle = result.candles.lastOrNull() ?: return
    val x = lastCandle.centerX * scaleX
    val isUp = result.overallSignal == "UP"
    val signalColor = if (isUp) NeonGreen else NeonRed
    val entryY = (if (isUp) lastCandle.lowY else lastCandle.highY) * scaleY

    // Horizontal Entry Crosshair Line
    scope.drawLine(
        color = signalColor.copy(alpha = 0.50f * globalAlpha),
        start = Offset(0f, entryY),
        end = Offset(canvasWidth, entryY),
        strokeWidth = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )

    // Animated Pulsing Signal Arrow
    val arrowY = if (isUp) entryY + 48f else entryY - 48f
    val arrowSize = 34f * pulseFactor

    val path = Path().apply {
        if (isUp) {
            moveTo(x, arrowY - arrowSize)
            lineTo(x + arrowSize * 0.75f, arrowY + arrowSize * 0.4f)
            lineTo(x + arrowSize * 0.28f, arrowY + arrowSize * 0.4f)
            lineTo(x + arrowSize * 0.28f, arrowY + arrowSize)
            lineTo(x - arrowSize * 0.28f, arrowY + arrowSize)
            lineTo(x - arrowSize * 0.28f, arrowY + arrowSize * 0.4f)
            lineTo(x - arrowSize * 0.75f, arrowY + arrowSize * 0.4f)
            close()
        } else {
            moveTo(x, arrowY + arrowSize)
            lineTo(x + arrowSize * 0.75f, arrowY - arrowSize * 0.4f)
            lineTo(x + arrowSize * 0.28f, arrowY - arrowSize * 0.4f)
            lineTo(x + arrowSize * 0.28f, arrowY - arrowSize)
            lineTo(x - arrowSize * 0.28f, arrowY - arrowSize)
            lineTo(x - arrowSize * 0.28f, arrowY - arrowSize * 0.4f)
            lineTo(x - arrowSize * 0.75f, arrowY - arrowSize * 0.4f)
            close()
        }
    }

    // Outer glow
    scope.drawPath(
        path = path,
        color = signalColor.copy(alpha = 0.35f * globalAlpha),
        style = Stroke(width = 8f)
    )
    // Solid fill
    scope.drawPath(
        path = path,
        color = signalColor.copy(alpha = 0.95f * globalAlpha)
    )
}

/**
 * Draws floating tooltip card when user taps a marker or S/R zone.
 */
private fun drawInspectionTooltip(
    scope: DrawScope,
    text: String,
    position: Offset,
    canvasWidth: Float
) {
    scope.drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val bgPaint = android.graphics.Paint().apply {
            color = 0xF0111927.toInt()
            isAntiAlias = true
        }
        val borderPaint = android.graphics.Paint().apply {
            color = 0xFF00B0FF.toInt()
            strokeWidth = 2f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }

        val lines = text.split("\n")
        val maxTextWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 200f
        val boxWidth = maxTextWidth + 32f
        val boxHeight = (lines.size * 32f) + 24f

        var left = position.x - (boxWidth / 2f)
        if (left < 16f) left = 16f
        if (left + boxWidth > canvasWidth - 16f) left = canvasWidth - boxWidth - 16f
        val top = (position.y - boxHeight - 20f).coerceAtLeast(20f)
        val right = left + boxWidth
        val bottom = top + boxHeight

        drawRoundRect(left, top, right, bottom, 12f, 12f, bgPaint)
        drawRoundRect(left, top, right, bottom, 12f, 12f, borderPaint)

        lines.forEachIndexed { idx, line ->
            drawText(line, left + 16f, top + 32f + (idx * 30f), paint)
        }
    }
}
