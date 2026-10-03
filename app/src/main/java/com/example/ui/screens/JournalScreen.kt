package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ContextStat
import com.example.data.entity.JournalEntryEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    entries: List<JournalEntryEntity>,
    signalsTodayCount: Int,
    consecutiveLossCount: Int,
    dailySignalLimit: Int = 10,
    demoSignalCount: Int = 0,
    contextStats: List<ContextStat> = emptyList(),
    onMarkResult: (JournalEntryEntity, String) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTrades = entries.count { it.outcomeResult != "PENDING" }
    val wins = entries.count { it.outcomeResult == "WIN" }
    val winRate = if (totalTrades > 0) (wins.toFloat() / totalTrades.toFloat()) * 100f else 0f

    // Calculate current streak
    var currentStreak = 0
    var isWinStreak = true
    for (entry in entries) {
        if (entry.outcomeResult == "PENDING") continue
        if (currentStreak == 0) {
            isWinStreak = entry.outcomeResult == "WIN"
            currentStreak = 1
        } else {
            if ((entry.outcomeResult == "WIN") == isWinStreak) {
                currentStreak++
            } else {
                break
            }
        }
    }

    val isCooldownActive = consecutiveLossCount >= 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .testTag("journal_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRADE JOURNAL & STATS",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Performance history & probability governance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Risk Panel & Cooldown Alert
        RiskGovernanceBanner(
            signalsToday = signalsTodayCount,
            dailyLimit = dailySignalLimit,
            consecutiveLosses = consecutiveLossCount,
            isCooldownActive = isCooldownActive
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stats Highlights Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "WIN RATE",
                value = "${winRate.toInt()}%",
                subtitle = "$wins/$totalTrades Won",
                valueColor = if (winRate >= 60f) NeonGreen else if (winRate >= 45f) NeonYellow else NeonRed,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "STREAK",
                value = if (currentStreak > 0) "$currentStreak ${if (isWinStreak) "W" else "L"}" else "0",
                subtitle = if (isWinStreak) "Winning Run" else "Drawdown",
                valueColor = if (isWinStreak && currentStreak > 0) NeonGreen else NeonRed,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "TODAY",
                value = "$signalsTodayCount/$dailySignalLimit",
                subtitle = "Signals Used",
                valueColor = if (signalsTodayCount >= dailySignalLimit) NeonRed else NeonBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mini Equity / Performance Canvas Curve
        if (entries.isNotEmpty()) {
            PerformanceCurveCard(entries)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Per-Context Stats (Site, Pair, Timeframe, Session) per Knowledge Pack
        if (contextStats.isNotEmpty()) {
            Text(
                text = "PER-CONTEXT PERFORMANCE (SITE, PAIR, TIMEFRAME, SESSION)",
                style = MaterialTheme.typography.labelSmall,
                color = NeonBlue
            )
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                contextStats.take(8).forEach { stat ->
                    val color = if (stat.winRate >= 56f) NeonGreen else if (stat.winRate >= 48f) NeonYellow else NeonRed
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${stat.type}: ${stat.contextKey} (${stat.winRate.toInt()}% WR • ${stat.totalTrades}T)",
                            fontSize = 10.sp,
                            color = color
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Journal Entries List
        Text(
            text = "RECORDED SIGNAL AUDIT TRAIL",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trading signals logged yet. Run Analyze on any chart and tap 'Journal' to record setups.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    JournalItemCard(
                        entry = entry,
                        onMarkWin = { onMarkResult(entry, "WIN") },
                        onMarkLoss = { onMarkResult(entry, "LOSS") },
                        onDelete = { onDeleteEntry(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskGovernanceBanner(
    signalsToday: Int,
    dailyLimit: Int,
    consecutiveLosses: Int,
    isCooldownActive: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCooldownActive) NeonRed else BorderDark,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCooldownActive) NeonRed.copy(alpha = 0.15f) else BackgroundElevated
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCooldownActive) Icons.Default.Warning else Icons.Default.Shield,
                    contentDescription = "Risk",
                    tint = if (isCooldownActive) NeonRed else NeonYellow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCooldownActive) "COOLDOWN ACTIVATED: 3 CONSECUTIVE LOSSES" else "RISK & PROBABILITY GOVERNANCE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCooldownActive) NeonRed else NeonYellow
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isCooldownActive)
                    "Engine enforces a mandatory cool-down pause to prevent revenge trading. Clear head before next analysis."
                else
                    "Signals are algorithmic probabilities, not financial guarantees. Capital preservation is priority #1.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, BorderDark, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun PerformanceCurveCard(entries: List<JournalEntryEntity>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "CUMULATIVE PERFORMANCE TRAJECTORY",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height

                val completed = entries.filter { it.outcomeResult != "PENDING" }.reversed()
                if (completed.isEmpty()) {
                    drawLine(
                        color = Color.DarkGray,
                        start = Offset(0f, chartHeight / 2f),
                        end = Offset(chartWidth, chartHeight / 2f),
                        strokeWidth = 2f
                    )
                    return@Canvas
                }

                var score = 0
                val scores = mutableListOf(0)
                for (entry in completed) {
                    if (entry.outcomeResult == "WIN") score++ else score--
                    scores.add(score)
                }

                val minScore = scores.minOrNull() ?: 0
                val maxScore = scores.maxOrNull() ?: 1
                val range = maxOf(1, maxScore - minScore).toFloat()

                val path = Path()
                scores.forEachIndexed { index, s ->
                    val x = (index.toFloat() / (scores.size - 1).coerceAtLeast(1)) * chartWidth
                    val normalizedY = 1f - ((s - minScore) / range)
                    val y = normalizedY * (chartHeight - 16f) + 8f

                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = NeonCyan,
                    style = Stroke(width = 3.5f)
                )
            }
        }
    }
}

@Composable
private fun JournalItemCard(
    entry: JournalEntryEntity,
    onMarkWin: () -> Unit,
    onMarkLoss: () -> Unit,
    onDelete: () -> Unit
) {
    val isUp = entry.signalDirection.equals("UP", ignoreCase = true)
    val dirColor = if (isUp) NeonGreen else NeonRed
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(entry.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(dirColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = entry.signalDirection,
                            tint = dirColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "${entry.site} • ${entry.timeframe}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Status Badge
                val (statusBg, statusColor) = when (entry.outcomeResult) {
                    "WIN" -> Pair(NeonGreen.copy(0.2f), NeonGreen)
                    "LOSS" -> Pair(NeonRed.copy(0.2f), NeonRed)
                    else -> Pair(NeonYellow.copy(0.2f), NeonYellow)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entry.outcomeResult,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (entry.matchedRuleName.isNotBlank()) {
                Text(
                    text = "Rule: ${entry.matchedRuleName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonBlue
                )
            }

            if (entry.detectedPatterns.isNotBlank()) {
                Text(
                    text = "Patterns: ${entry.detectedPatterns}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Win / Loss resolution buttons if PENDING
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conf: ${(entry.confidence * 100).toInt()}% • Exp: ${entry.expirySuggestion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.outcomeResult == "PENDING") {
                        ElevatedButton(
                            onClick = onMarkWin,
                            colors = ButtonDefaults.elevatedButtonColors(containerColor = NeonGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Win", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Win", style = MaterialTheme.typography.labelSmall)
                        }

                        ElevatedButton(
                            onClick = onMarkLoss,
                            colors = ButtonDefaults.elevatedButtonColors(containerColor = NeonRed, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Loss", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Loss", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
