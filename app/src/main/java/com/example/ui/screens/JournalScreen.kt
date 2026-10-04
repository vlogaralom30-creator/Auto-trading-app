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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Insights
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

@Composable
fun JournalScreen(
    entries: List<JournalEntryEntity>,
    signalsTodayCount: Int,
    consecutiveLossCount: Int,
    dailySignalLimit: Int = 10,
    demoSignalsCount: Int = 0,
    contextStats: List<ContextStat> = emptyList(),
    onMarkResult: (JournalEntryEntity, String) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTrades = entries.count { it.outcomeResult != "PENDING" }
    val wins = entries.count { it.outcomeResult == "WIN" }
    val winRate = if (totalTrades > 0) (wins.toFloat() / totalTrades.toFloat()) * 100f else 0f

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
    val isReady = demoSignalsCount >= 150

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
                    text = "TRADE JOURNAL & LEARNING",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Adaptive weights updated on Win/Loss",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Ready Badge / Demo Phase indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isReady) NeonGreen.copy(alpha = 0.15f) else NeonYellow.copy(alpha = 0.15f))
                    .border(1.dp, if (isReady) NeonGreen else NeonYellow, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isReady) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                        contentDescription = "Badge",
                        tint = if (isReady) NeonGreen else NeonYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isReady) "READY" else "DEMO $demoSignalsCount/150",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) NeonGreen else NeonYellow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Risk & Cooldown Banner if triggered
        if (isCooldownActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonRed, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Cooldown", tint = NeonRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Risk Cooldown: 3 consecutive losses hit. 30-min pause recommended.",
                        color = NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "WIN RATE",
                value = "${winRate.toInt()}%",
                subtitle = "$wins W / ${totalTrades - wins} L",
                accentColor = if (winRate >= 56f) NeonGreen else NeonYellow,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "CURRENT STREAK",
                value = if (currentStreak > 0) "$currentStreak ${if (isWinStreak) "W" else "L"}" else "-",
                subtitle = if (isWinStreak) "Active run" else "Drawdown",
                accentColor = if (isWinStreak) NeonGreen else NeonRed,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "TODAY'S SIGNALS",
                value = "$signalsTodayCount",
                subtitle = "Limit: $dailySignalLimit",
                accentColor = if (signalsTodayCount >= dailySignalLimit) NeonRed else NeonCyan,
                modifier = Modifier.weight(1f)
            )
        }

        // Context Performance section (Best / Worst contexts)
        if (contextStats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Insights, contentDescription = "Context Stats", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PER-CONTEXT WIN RATE (Site, Pair, Timeframe, Session)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        contextStats.take(3).forEach { stat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(BackgroundElevated, RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text("${stat.category}: ${stat.name}", fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                                    Text("${stat.winRate.toInt()}% (${stat.wins}/${stat.total})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (stat.winRate >= 56f) NeonGreen else NeonRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Entries List
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trades logged yet. Run chart analysis to log signals.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    JournalItemCard(
                        entry = entry,
                        onMarkResult = onMarkResult,
                        onDeleteEntry = onDeleteEntry
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun JournalItemCard(
    entry: JournalEntryEntity,
    onMarkResult: (JournalEntryEntity, String) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val isUp = entry.signalDirection == "UP"
    val isDown = entry.signalDirection == "DOWN"
    val isPending = entry.outcomeResult == "PENDING"
    val isWin = entry.outcomeResult == "WIN"
    val isBlocked = entry.blockedReason.isNotBlank()

    val dirColor = when {
        isBlocked -> NeonYellow
        isUp -> NeonGreen
        isDown -> NeonRed
        else -> TextSecondary
    }

    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(entry.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isBlocked) Icons.Default.Block else if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Direction",
                        tint = dirColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBlocked) "BLOCKED" else entry.signalDirection,
                        fontWeight = FontWeight.Bold,
                        color = dirColor,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.site} (${entry.timeframe})",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = dateString,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isBlocked) {
                Text(
                    text = entry.blockedReason,
                    fontSize = 12.sp,
                    color = NeonYellow,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Rule: ${entry.matchedRuleName.ifEmpty { "Price Action & S/R" }} (${(entry.confidence * 100).toInt()}% conf)",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                if (entry.detectedPatterns.isNotBlank()) {
                    Text(
                        text = "Patterns: ${entry.detectedPatterns}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPending) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ElevatedButton(
                            onClick = { onMarkResult(entry, "WIN") },
                            colors = ButtonDefaults.elevatedButtonColors(containerColor = NeonGreen.copy(alpha = 0.2f)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Win", tint = NeonGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WIN", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        ElevatedButton(
                            onClick = { onMarkResult(entry, "LOSS") },
                            colors = ButtonDefaults.elevatedButtonColors(containerColor = NeonRed.copy(alpha = 0.2f)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Loss", tint = NeonRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LOSS", color = NeonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isWin) NeonGreen.copy(alpha = 0.2f) else NeonRed.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = entry.outcomeResult,
                            color = if (isWin) NeonGreen else NeonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = { onDeleteEntry(entry.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
