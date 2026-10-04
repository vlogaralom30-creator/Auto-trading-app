package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.model.ChartAnalysisResult
import com.example.model.OverlayLayerSettings

@Composable
fun ChartOverlayCanvas(
    analysisResult: ChartAnalysisResult?,
    layerSettings: OverlayLayerSettings,
    modifier: Modifier = Modifier
) {
    if (analysisResult == null) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val scaleX = if (analysisResult.bitmapWidth > 0) width / analysisResult.bitmapWidth else 1f
        val scaleY = if (analysisResult.bitmapHeight > 0) height / analysisResult.bitmapHeight else 1f

        // 1. Candlestick boxes
        if (layerSettings.showCandles) {
            analysisResult.candles.forEach { c ->
                val left = c.bodyLeft * scaleX
                val top = c.bodyTop * scaleY
                val bodyW = c.body * scaleX
                val bodyH = maxOf(2f, (c.bodyBottom - c.bodyTop) * scaleY)
                val color = if (c.isBullish) Color(0x6622C55E) else Color(0x66EF4444)

                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(bodyW, bodyH)
                )

                // Wick
                drawLine(
                    color = if (c.isBullish) Color(0xFF22C55E) else Color(0xFFEF4444),
                    start = Offset(c.centerX * scaleX, c.highY * scaleY),
                    end = Offset(c.centerX * scaleX, c.lowY * scaleY),
                    strokeWidth = 2f
                )
            }
        }

        // 2. S/R Zones
        if (layerSettings.showSRZones) {
            analysisResult.srZones.forEach { zone ->
                val y = zone.yLevel * scaleY
                val color = if (zone.isSupport) Color(0x5522C55E) else Color(0x55EF4444)
                drawRect(
                    color = color,
                    topLeft = Offset(0f, y - 4f),
                    size = Size(width, 8f)
                )
            }
        }

        // 3. Trendlines
        if (layerSettings.showTrendlines) {
            analysisResult.trendLines.forEach { tl ->
                drawLine(
                    color = if (tl.isSupport) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                    start = Offset(tl.startX * scaleX, tl.startY * scaleY),
                    end = Offset(tl.endX * scaleX, tl.endY * scaleY),
                    strokeWidth = 3f
                )
            }
        }

        // 4. Pattern Markers
        if (layerSettings.showPatternLabels) {
            analysisResult.detectedPatterns.forEach { p ->
                val px = p.x * scaleX
                val py = p.y * scaleY
                val color = if (p.direction == "BULLISH") Color(0xFF22C55E) else Color(0xFFEF4444)

                drawCircle(
                    color = color,
                    radius = 8f,
                    center = Offset(px, py)
                )
            }
        }

        // 5. Signal Arrow
        if (layerSettings.showSignalArrows && analysisResult.overallSignal != "NEUTRAL") {
            val isCall = analysisResult.overallSignal == "CALL"
            val arrowColor = if (isCall) Color(0xFF22C55E) else Color(0xFFEF4444)
            val path = Path().apply {
                if (isCall) {
                    moveTo(width - 80f, height - 120f)
                    lineTo(width - 50f, height - 170f)
                    lineTo(width - 20f, height - 120f)
                    close()
                } else {
                    moveTo(width - 80f, height - 170f)
                    lineTo(width - 50f, height - 120f)
                    lineTo(width - 20f, height - 170f)
                    close()
                }
            }
            drawPath(path = path, color = arrowColor)
        }
    }
}
