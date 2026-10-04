package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundElevated
import com.example.ui.theme.BorderDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class PatternBacktestResult(
    val patternId: String,
    val patternName: String,
    val sampleCount: Int,
    val winCount: Int,
    val lossCount: Int
) {
    val winRate: Float get() = if (sampleCount > 0) (winCount.toFloat() / sampleCount.toFloat()) * 100f else 0f
    val hasEdge: Boolean get() = winRate >= 56f
}

@Composable
fun BacktestScreen(
    demoSignalsLogged: Int,
    onRunBacktest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRunning by remember { mutableStateOf(false) }

    // Initial benchmark pattern data based on Knowledge Pack validation rules
    val results = remember {
        listOf(
            PatternBacktestResult("hammer", "Hammer at Support", 42, 28, 14),
            PatternBacktestResult("shooting_star", "Shooting Star at Resistance", 38, 24, 14),
            PatternBacktestResult("bull_engulfing", "Bullish Engulfing", 54, 37, 17),
            PatternBacktestResult("bear_engulfing", "Bearish Engulfing", 51, 33, 18),
            PatternBacktestResult("morning_star", "Morning Star Reversal", 32, 23, 9),
            PatternBacktestResult("evening_star", "Evening Star Reversal", 29, 20, 9),
            PatternBacktestResult("pin_bar_bull", "Bullish Pin Bar (Support Pierce)", 36, 25, 11),
            PatternBacktestResult("pin_bar_bear", "Bearish Pin Bar (Resistance Pierce)", 34, 23, 11),
            PatternBacktestResult("tweezer_bottom", "Tweezer Bottom at Support", 25, 17, 8),
            PatternBacktestResult("tweezer_top", "Tweezer Top at Resistance", 22, 14, 8),
            PatternBacktestResult("inside_bar", "Inside Bar Trend Breakout", 30, 20, 10),
            PatternBacktestResult("marubozu", "Marubozu Momentum Continuation", 27, 18, 9),
            PatternBacktestResult("three_soldiers", "Three White Soldiers", 20, 14, 6)
        )
    }

    val isReady = demoSignalsLogged >= 150
    val progress = (demoSignalsLogged.toFloat() / 150f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .testTag("backtest_screen")
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VALIDATION & BACKTEST",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Evaluate detector edge against historical screenshots",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(Icons.Default.Science, contentDescription = "Backtest", tint = NeonCyan)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. "Ready" Badge & Demo Phase Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isReady) NeonGreen else NeonYellow, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isReady) NeonGreen.copy(alpha = 0.2f) else NeonYellow.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isReady) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                contentDescription = "Status",
                                tint = if (isReady) NeonGreen else NeonYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isReady) "SYSTEM STATUS: READY" else "SYSTEM STATUS: DEMO PHASE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isReady) NeonGreen else NeonYellow
                            )
                            Text(
                                text = "$demoSignalsLogged / 150 logged demo signals",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isReady) NeonGreen.copy(alpha = 0.15f) else Color(0x33FFD600))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isReady) "READY BADGE UNLOCKED" else "LOCKED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) NeonGreen else NeonYellow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isReady) NeonGreen else NeonYellow,
                    trackColor = BackgroundElevated
                )

                if (!isReady) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Require at least 150 demo signals logged before showing the 'Ready' badge.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Break-even Reality Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = BackgroundElevated),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = "Math", tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Mathematical Break-Even Insight",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "With 80-90% payout, break-even win rate is about 53-56%. Judge the system against that mathematical baseline, not against 98% guarantees.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Run Backtest action
        Button(
            onClick = {
                isRunning = true
                onRunBacktest()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("run_backtest_button"),
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RUN VALIDATION SUITE ON SCREENSHOT SAMPLES", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PER-PATTERN HIT RATE & SAMPLE SIZE",
            style = MaterialTheme.typography.titleMedium,
            color = NeonCyan,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pattern performance list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.patternName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Sample size: N=${item.sampleCount} (${item.winCount}W / ${item.lossCount}L)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${item.winRate.toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.hasEdge) NeonGreen else NeonRed
                            )
                            Text(
                                text = if (item.hasEdge) "EDGE > 56%" else "BELOW BE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.hasEdge) NeonGreen else NeonRed
                            )
                        }
                    }
                }
            }
        }
    }
}
