package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccountMode
import com.example.model.AutoTraderState
import com.example.model.DailyRiskStats
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundElevated
import com.example.ui.theme.BorderDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AutoTraderDashboardScreen(
    state: AutoTraderState,
    dailyStats: DailyRiskStats,
    accountMode: AccountMode,
    lastBlockedReason: String?,
    onStartAutoTrading: () -> Unit,
    onStopAutoTrading: () -> Unit,
    onKillSwitch: () -> Unit,
    onManualUnlock: () -> Unit,
    onResetStats: () -> Unit,
    onAccountModeChanged: (AccountMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRealConfirmDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("autotrader_dashboard_screen")
    ) {
        // Top Header with Account Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AUTOTRADER COCKPIT",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Automated execution, state machine & risk boundaries",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Account Mode Badge / Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(BackgroundElevated)
                    .border(1.dp, if (accountMode == AccountMode.REAL) NeonRed else NeonGreen, RoundedCornerShape(20.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (accountMode == AccountMode.DEMO) NeonGreen.copy(0.2f) else Color.Transparent)
                        .clickable { onAccountModeChanged(AccountMode.DEMO) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DEMO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (accountMode == AccountMode.DEMO) NeonGreen else TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (accountMode == AccountMode.REAL) NeonRed.copy(0.25f) else Color.Transparent)
                        .clickable { showRealConfirmDialog = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "REAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (accountMode == AccountMode.REAL) NeonRed else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // REAL Mode Active Safety Banner
        if (accountMode == AccountMode.REAL) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NeonRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Real Account", tint = NeonRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE CAPITAL ACTIVE — Strictly 1 trade at a time, hard stop-loss limits enabled.",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Active State Machine Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    when (state) {
                        is AutoTraderState.Idle -> NeonCyan.copy(0.5f)
                        is AutoTraderState.Scanning -> NeonBlue
                        is AutoTraderState.WaitingEntry -> NeonYellow
                        is AutoTraderState.Placing -> NeonPurple
                        is AutoTraderState.InTrade -> if (state.direction == "UP") NeonGreen else NeonRed
                        is AutoTraderState.Result -> if (state.isWin) NeonGreen else NeonRed
                        is AutoTraderState.Cooldown -> NeonYellow
                        is AutoTraderState.Locked -> NeonRed
                    },
                    RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = BackgroundElevated),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when (state) {
                                        is AutoTraderState.Idle -> NeonCyan
                                        is AutoTraderState.Scanning -> NeonBlue
                                        is AutoTraderState.WaitingEntry -> NeonYellow
                                        is AutoTraderState.Placing -> NeonPurple
                                        is AutoTraderState.InTrade -> NeonGreen
                                        is AutoTraderState.Result -> if (state.isWin) NeonGreen else NeonRed
                                        is AutoTraderState.Cooldown -> NeonYellow
                                        is AutoTraderState.Locked -> NeonRed
                                    }.copy(alpha = if (state !is AutoTraderState.Idle) pulseAlpha else 1f)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STATE: ${state.javaClass.simpleName.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    // Emergency Kill Switch Button
                    OutlinedButton(
                        onClick = onKillSwitch,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed),
                        modifier = Modifier.testTag("kill_switch_btn")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = "Kill Switch", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("KILL SWITCH", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Contextual State Details
                when (state) {
                    is AutoTraderState.Idle -> {
                        Text(
                            text = "Engine is IDLE. Press Start to initiate automatic multi-timeframe analysis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    is AutoTraderState.Scanning -> {
                        Text(text = state.step, style = MaterialTheme.typography.bodyMedium, color = NeonBlue)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeonBlue)
                    }
                    is AutoTraderState.WaitingEntry -> {
                        Text(
                            text = "Confluence Found: ${state.signal} (${(state.confidence * 100).toInt()}% conf) • ${state.matchedRule}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonYellow,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Waiting for candle close: ${state.secondsUntilNextCandle}s remaining (entering next bar)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                    is AutoTraderState.Placing -> {
                        Text(text = state.statusMessage, style = MaterialTheme.typography.bodyMedium, color = NeonPurple)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeonPurple)
                    }
                    is AutoTraderState.InTrade -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ACTIVE POSITION: ${state.direction} ($${state.stake})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (state.direction == "UP") NeonGreen else NeonRed
                            )
                            Text(
                                text = "Expiry in ${state.remainingSec}s",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = NeonYellow
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (state.expiryDurationSec - state.remainingSec).toFloat() / state.expiryDurationSec.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (state.direction == "UP") NeonGreen else NeonRed
                        )
                    }
                    is AutoTraderState.Result -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isWin) NeonGreen else NeonRed
                        )
                    }
                    is AutoTraderState.Cooldown -> {
                        Text(text = "COOLDOWN ENFORCED: ${state.reason}", style = MaterialTheme.typography.bodyMedium, color = NeonYellow)
                        Text(text = "Remaining: ${state.remainingCooldownSec}s", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    is AutoTraderState.Locked -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = NeonRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "TRADING LOCKED", style = MaterialTheme.typography.titleSmall, color = NeonRed, fontWeight = FontWeight.Bold)
                                Text(text = state.lockReason, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onManualUnlock,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Unlock", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MANUAL SAFETY OVERRIDE UNLOCK")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Daily Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardMetricCard(
                title = "BALANCE",
                value = "$${String.format("%.2f", dailyStats.currentBalance)}",
                subtitle = "P&L: ${if (dailyStats.netPnlToday >= 0) "+$" else "-$"}${String.format("%.2f", Math.abs(dailyStats.netPnlToday))}",
                valueColor = if (dailyStats.netPnlToday >= 0) NeonGreen else NeonRed,
                modifier = Modifier.weight(1f)
            )

            DashboardMetricCard(
                title = "WIN RATE",
                value = "${(dailyStats.winRate).toInt()}%",
                subtitle = "${dailyStats.winsToday}W / ${dailyStats.lossesToday}L (${dailyStats.totalTradesToday} Total)",
                valueColor = if (dailyStats.winRate >= 56f) NeonGreen else NeonYellow,
                modifier = Modifier.weight(1f)
            )

            DashboardMetricCard(
                title = "DRAWDOWN",
                value = "$${String.format("%.2f", dailyStats.maxDrawdownToday)}",
                subtitle = "Peak: $${String.format("%.2f", dailyStats.peakBalanceToday)}",
                valueColor = if (dailyStats.maxDrawdownToday > 2.0) NeonRed else TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Equity Curve Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "INTRA-DAY EQUITY TRAJECTORY",
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonBlue
                )
                Spacer(modifier = Modifier.height(8.dp))

                val points = dailyStats.equityCurvePoints
                if (points.size >= 2) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        val minBal = points.minOf { it.second } - 1.0
                        val maxBal = points.maxOf { it.second } + 1.0
                        val span = (maxBal - minBal).coerceAtLeast(0.5)

                        val path = Path()
                        val stepX = size.width / (points.size - 1)

                        points.forEachIndexed { i, p ->
                            val x = i * stepX
                            val y = (size.height - ((p.second - minBal) / span * size.height)).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = if (dailyStats.netPnlToday >= 0) NeonGreen else NeonRed,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Equity curve will render as trades are executed today",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Last Filtered / Blocked Reason Card
        if (lastBlockedReason != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BackgroundElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "Shield", tint = NeonBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("LAST SAFETY FILTER EVENT", style = MaterialTheme.typography.labelSmall, color = NeonBlue)
                        Text(lastBlockedReason, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Action Controls (START / STOP / RESET)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isRunning = state !is AutoTraderState.Idle && state !is AutoTraderState.Locked
            Button(
                onClick = { if (isRunning) onStopAutoTrading() else onStartAutoTrading() },
                colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) NeonRed else NeonGreen),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("start_stop_autotrader_btn")
            ) {
                Icon(if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "Toggle")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "STOP AUTOTRADER" else "START AUTOTRADER",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            OutlinedButton(
                onClick = onResetStats,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // REAL Account Mode Confirmation Dialog
    if (showRealConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRealConfirmDialog = false },
            title = {
                Text(
                    text = "SWITCH TO REAL CAPITAL MODE?",
                    color = NeonRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You are activating REAL capital execution on your broker account.\n\n" +
                            "• All automated trades will execute with REAL funds.\n" +
                            "• Hard stop loss limits ($3.0 max daily loss) and max 1 trade at a time are enforced.\n" +
                            "• Signals are statistical probabilities, never guarantees.\n\n" +
                            "Do you understand the financial risks and wish to proceed?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAccountModeChanged(AccountMode.REAL)
                        showRealConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) {
                    Text("I UNDERSTAND THE RISK • ENABLE REAL", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRealConfirmDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = BackgroundElevated
        )
    }
}

@Composable
fun DashboardMetricCard(
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
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
        }
    }
}
