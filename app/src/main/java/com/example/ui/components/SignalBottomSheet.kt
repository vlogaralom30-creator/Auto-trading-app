package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChartAnalysisResult
import com.example.ui.theme.BackgroundElevated
import com.example.ui.theme.BorderDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenMuted
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedMuted
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignalBottomSheet(
    result: ChartAnalysisResult,
    onSaveToJournal: () -> Unit,
    onMarkWin: () -> Unit,
    onMarkLoss: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUp = result.overallSignal == "UP"
    val isDown = result.overallSignal == "DOWN"
    val isBlocked = result.isBlocked

    val signalColor = when {
        isBlocked -> NeonYellow
        isUp -> NeonGreen
        isDown -> NeonRed
        else -> NeonCyan
    }

    val signalBg = when {
        isBlocked -> Color(0x22FFD600)
        isUp -> NeonGreenMuted
        isDown -> NeonRedMuted
        else -> Color(0x2200E5FF)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = result.confidenceScore,
        animationSpec = tween(durationMillis = 800),
        label = "confidence_progress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .testTag("signal_bottom_sheet"),
        color = BackgroundElevated,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header drag bar & close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(TextMuted)
                )

                Text(
                    text = "CONFLUENCE SCORING ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonBlue
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).testTag("close_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Blocked Banner if No-Trade Filter triggered
            if (isBlocked && result.blockedReason != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonYellow, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = NeonYellow.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = "Blocked", tint = NeonYellow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SIGNAL BLOCKED BY NO-TRADE FILTER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeonYellow
                            )
                            Text(
                                text = result.blockedReason,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Main Signal Banner & Animated Confidence Gauge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(signalBg)
                    .border(1.dp, signalColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Signal Direction & Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(signalColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isBlocked -> Icons.Default.Shield
                                    isUp -> Icons.Default.ArrowUpward
                                    isDown -> Icons.Default.ArrowDownward
                                    else -> Icons.Default.Remove
                                },
                                contentDescription = result.overallSignal,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = when {
                                    isBlocked -> "FILTER BLOCKED"
                                    isUp -> "SIGNAL: UP (CALL)"
                                    isDown -> "SIGNAL: DOWN (PUT)"
                                    else -> "NEUTRAL / HOLD"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = signalColor
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Expiry",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Suggested Expiry: ${result.suggestedExpiry}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    if (result.matchedRule != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rule: ${result.matchedRule}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Confidence Gauge (Cap 78%, min_to_signal 60%)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(64.dp),
                        color = Color(0x33FFFFFF),
                        strokeWidth = 5.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(64.dp),
                        color = signalColor,
                        strokeWidth = 5.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(result.confidenceScore * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBlocked) "Blocked" else "Conf",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TOP 3 CONTRIBUTING REASONS
            Text(
                text = "TOP 3 CONFLUENCE FACTORS",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val displayReasons = if (result.topContributingReasons.isNotEmpty()) {
                result.topContributingReasons
            } else {
                result.reasonList.take(3)
            }

            displayReasons.forEachIndexed { idx, reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${idx + 1}", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions & Outcome Feedback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSaveToJournal,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan)),
                    modifier = Modifier.testTag("save_journal_btn")
                ) {
                    Icon(Icons.Default.History, contentDescription = "Journal", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LOG TO JOURNAL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(
                        onClick = onMarkWin,
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = NeonGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.testTag("quick_win_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Win", tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WIN", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    ElevatedButton(
                        onClick = onMarkLoss,
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = NeonRed.copy(alpha = 0.2f)),
                        modifier = Modifier.testTag("quick_loss_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Loss", tint = NeonRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LOSS", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
