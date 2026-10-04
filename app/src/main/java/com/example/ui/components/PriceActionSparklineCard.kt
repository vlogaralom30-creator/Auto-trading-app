package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Candle
import com.example.model.PatternMatchResult
import com.example.model.TrendDirection
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundElevated
import com.example.ui.theme.BorderDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * Compact, interactive sparkline visualization component for the main dashboard.
 * Displays recent price action trajectory, baseline gradient fill, and badges highlighting
 * actively detected candlestick patterns.
 */
@Composable
fun PriceActionSparklineCard(
    candles: List<Candle>,
    patterns: List<PatternMatchResult>,
    trendDirection: TrendDirection = TrendDirection.SIDEWAYS,
    timeframe: String = "1m",
    modifier: Modifier = Modifier,
    onPatternSelected: ((PatternMatchResult) -> Unit)? = null
) {
    if (candles.isEmpty()) return

    var isExpanded by remember { mutableStateOf(true) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val recentCandles = remember(candles) {
        if (candles.size > 35) candles.takeLast(35) else candles
    }

    val startIndexOffset = remember(candles, recentCandles) {
        candles.size - recentCandles.size
    }

    // Identify patterns occurring within the visible recent sparkline segment
    val visiblePatterns = remember(patterns, recentCandles, startIndexOffset) {
        patterns.mapNotNull { p ->
            val relIdx = p.candleIndex - startIndexOffset
            if (relIdx in recentCandles.indices) {
                Pair(relIdx, p)
            } else null
        }
    }

    val trendColor = when (trendDirection) {
        TrendDirection.UPTREND -> NeonGreen
        TrendDirection.DOWNTREND -> NeonRed
        TrendDirection.SIDEWAYS -> NeonCyan
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .border(1.dp, NeonBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .testTag("price_action_sparkline_card"),
        colors = CardDefaults.cardColors(containerColor = BackgroundElevated.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Title, Trend Badge, and Expand/Collapse Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(NeonBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Sparkline",
                            tint = NeonBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECENT PRICE ACTION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Text(
                            text = timeframe.uppercase(),
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Trend Pill
                    Surface(
                        color = trendColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, trendColor.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = when (trendDirection) {
                                    TrendDirection.UPTREND -> Icons.Default.TrendingUp
                                    TrendDirection.DOWNTREND -> Icons.Default.TrendingDown
                                    TrendDirection.SIDEWAYS -> Icons.Default.TrendingFlat
                                },
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = trendDirection.name,
                                color = trendColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sparkline Canvas Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark.copy(alpha = 0.8f))
                            .border(1.dp, BorderDark.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(recentCandles) {
                                    detectTapGestures { offset ->
                                        val step = size.width / (recentCandles.size - 1).coerceAtLeast(1)
                                        val idx = (offset.x / step).toInt().coerceIn(recentCandles.indices)
                                        selectedPointIndex = if (selectedPointIndex == idx) null else idx
                                    }
                                }
                        ) {
                            drawSparklinePriceAction(
                                candles = recentCandles,
                                patterns = visiblePatterns,
                                trendColor = trendColor,
                                selectedIdx = selectedPointIndex
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Row: Highlighted Active Pattern Pills
                    if (visiblePatterns.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PATTERNS:",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )

                            visiblePatterns.take(3).forEach { (_, pat) ->
                                val patColor = when (pat.direction) {
                                    "UP" -> NeonGreen
                                    "DOWN" -> NeonRed
                                    else -> NeonCyan
                                }
                                Surface(
                                    color = patColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, patColor.copy(alpha = 0.6f)),
                                    modifier = Modifier.clickable { onPatternSelected?.invoke(pat) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = patColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${pat.patternName} (${(pat.confidence * 100).toInt()}%)",
                                            color = patColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Consolidating price structure • Tap chart to inspect points",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "${recentCandles.size} Candles",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Inspected point details HUD
                    selectedPointIndex?.let { idx ->
                        if (idx in recentCandles.indices) {
                            val c = recentCandles[idx]
                            val matchedPat = visiblePatterns.find { it.first == idx }?.second
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = SurfaceCard,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Candle #${c.index + 1} (${if (c.isBullish) "Bullish ▲" else "Bearish ▼"})",
                                        color = if (c.isBullish) NeonGreen else NeonRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = matchedPat?.let { "${it.patternName} [${it.direction}]" }
                                            ?: "Body: ${(c.bodyRatio * 100).toInt()}% • Wick: ${(c.lowerWickRatio * 100).toInt()}%",
                                        color = if (matchedPat != null) NeonCyan else TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Canvas drawing logic for sparkline curve, gradient underlay, reference grid,
 * and highlighted pattern badges.
 */
private fun DrawScope.drawSparklinePriceAction(
    candles: List<Candle>,
    patterns: List<Pair<Int, PatternMatchResult>>,
    trendColor: Color,
    selectedIdx: Int?
) {
    if (candles.size < 2) return

    val w = size.width
    val h = size.height

    // In screen coordinates: smaller Y = higher price. We invert so high price is near top.
    val minPriceY = candles.minOf { it.highY }
    val maxPriceY = candles.maxOf { it.lowY }
    val priceRange = (maxPriceY - minPriceY).coerceAtLeast(1f)

    val stepX = w / (candles.size - 1).toFloat()
    val paddingY = h * 0.12f
    val availableH = h - (paddingY * 2f)

    // Map each candle close price to sparkline (x, y) coordinates
    val points = candles.mapIndexed { idx, c ->
        val x = idx * stepX
        // Price inversion: higher price (lower highY) maps to higher in canvas (smaller y)
        val normalizedY = (c.closeY - minPriceY) / priceRange
        val y = paddingY + (normalizedY * availableH)
        Offset(x, y)
    }

    // 1. Draw subtle background horizontal reference grid lines
    val gridCount = 3
    for (i in 0..gridCount) {
        val gy = paddingY + (i.toFloat() / gridCount) * availableH
        drawLine(
            color = Color(0x253B82F6),
            start = Offset(0f, gy),
            end = Offset(w, gy),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }

    // 2. Build Smooth Sparkline Path & Gradient Fill Path
    val linePath = Path()
    val fillPath = Path()

    linePath.moveTo(points.first().x, points.first().y)
    fillPath.moveTo(points.first().x, h)
    fillPath.lineTo(points.first().x, points.first().y)

    for (i in 1 until points.size) {
        val p0 = points[i - 1]
        val p1 = points[i]
        val midX = (p0.x + p1.x) / 2f
        linePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
        fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
    }

    fillPath.lineTo(points.last().x, h)
    fillPath.close()

    // Draw Gradient Area under sparkline
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                trendColor.copy(alpha = 0.28f),
                trendColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h
        ),
        style = Fill
    )

    // Draw Main Glow Stroke & Line Stroke
    drawPath(
        path = linePath,
        color = trendColor.copy(alpha = 0.35f),
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = linePath,
        color = trendColor,
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 3. Draw Candle Dots along the sparkline
    points.forEachIndexed { idx, pt ->
        val c = candles[idx]
        val isUp = c.isBullish
        val dotColor = if (isUp) NeonGreen else NeonRed
        drawCircle(
            color = dotColor.copy(alpha = 0.6f),
            radius = 2.dp.toPx(),
            center = pt
        )
    }

    // 4. Highlight Active Detected Patterns with Glowing Badges
    patterns.forEach { (idx, pat) ->
        if (idx in points.indices) {
            val pt = points[idx]
            val patColor = when (pat.direction) {
                "UP" -> NeonGreen
                "DOWN" -> NeonRed
                else -> NeonCyan
            }

            // Outer glowing ring
            drawCircle(
                color = patColor.copy(alpha = 0.30f),
                radius = 10.dp.toPx(),
                center = pt
            )
            // Accent border ring
            drawCircle(
                color = patColor,
                radius = 6.dp.toPx(),
                center = pt,
                style = Stroke(width = 2.dp.toPx())
            )
            // Solid center core
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = pt
            )

            // Vertical indicator drop-line to baseline
            drawLine(
                color = patColor.copy(alpha = 0.5f),
                start = pt,
                end = Offset(pt.x, h),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
        }
    }

    // 5. Selected Scrubber Point Indicator
    selectedIdx?.let { idx ->
        if (idx in points.indices) {
            val pt = points[idx]
            drawLine(
                color = NeonCyan.copy(alpha = 0.8f),
                start = Offset(pt.x, 0f),
                end = Offset(pt.x, h),
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(
                color = NeonCyan,
                radius = 6.dp.toPx(),
                center = pt,
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = pt
            )
        }
    }
}
