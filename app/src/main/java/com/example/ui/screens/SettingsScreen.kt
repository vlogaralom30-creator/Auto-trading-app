package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.AccountMode
import com.example.model.AutoTraderSelectors
import com.example.model.ColorCalibration
import com.example.model.RiskConfig
import com.example.model.SiteTimeframeProfile
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

@Composable
fun SettingsScreen(
    calibration: ColorCalibration,
    profile: SiteTimeframeProfile,
    riskConfig: RiskConfig = RiskConfig(),
    selectors: AutoTraderSelectors = AutoTraderSelectors(),
    accountMode: AccountMode = AccountMode.DEMO,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    onCalibrationChanged: (ColorCalibration) -> Unit,
    onProfileChanged: (SiteTimeframeProfile) -> Unit,
    onRiskConfigChanged: (RiskConfig) -> Unit = {},
    onSelectorsChanged: (AutoTraderSelectors) -> Unit = {},
    onAccountModeChanged: (AccountMode) -> Unit = {},
    onSoundToggled: (Boolean) -> Unit,
    onHapticToggled: (Boolean) -> Unit,
    onDailyLimitChanged: (Int) -> Unit = {},
    onResetPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRealConfirmDialog by remember { mutableStateOf(false) }

    var fixedStake by remember(riskConfig.fixedStake) { mutableDoubleStateOf(riskConfig.fixedStake) }
    var dailyLossLimit by remember(riskConfig.dailyLossLimit) { mutableDoubleStateOf(riskConfig.dailyLossLimit) }
    var dailyProfitTarget by remember(riskConfig.dailyProfitTarget) { mutableDoubleStateOf(riskConfig.dailyProfitTarget) }
    var maxTradesPerDay by remember(riskConfig.maxTradesPerDay) { mutableIntStateOf(riskConfig.maxTradesPerDay) }

    var stakeInputSel by remember(selectors.stakeInputSelector) { mutableStateOf(selectors.stakeInputSelector) }
    var upBtnSel by remember(selectors.upButtonSelector) { mutableStateOf(selectors.upButtonSelector) }
    var downBtnSel by remember(selectors.downButtonSelector) { mutableStateOf(selectors.downButtonSelector) }
    var balanceSel by remember(selectors.balanceSelector) { mutableStateOf(selectors.balanceSelector) }
    var resultSel by remember(selectors.resultSelector) { mutableStateOf(selectors.resultSelector) }

    var sel1m by remember(profile.timeframe1mSelector) { mutableStateOf(profile.timeframe1mSelector) }
    var sel5m by remember(profile.timeframe5mSelector) { mutableStateOf(profile.timeframe5mSelector) }
    var sel15m by remember(profile.timeframe15mSelector) { mutableStateOf(profile.timeframe15mSelector) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "SETTINGS & RISK CONTROLS",
            style = MaterialTheme.typography.titleLarge,
            color = NeonBlue,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Account mode, risk boundaries, DOM selectors & calibration",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Account Mode (DEMO vs REAL)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (accountMode == AccountMode.REAL) NeonRed else BorderDark, RoundedCornerShape(12.dp)),
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
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Account Mode",
                            tint = if (accountMode == AccountMode.REAL) NeonRed else NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXECUTION ACCOUNT MODE",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(BackgroundElevated)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (accountMode == AccountMode.DEMO) NeonGreen.copy(0.2f) else Color.Transparent)
                                .clickable { onAccountModeChanged(AccountMode.DEMO) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("DEMO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (accountMode == AccountMode.DEMO) NeonGreen else TextMuted)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (accountMode == AccountMode.REAL) NeonRed.copy(0.25f) else Color.Transparent)
                                .clickable { showRealConfirmDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("REAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (accountMode == AccountMode.REAL) NeonRed else TextMuted)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Risk Manager Constraints
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "Risk", tint = NeonYellow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RISK GOVERNANCE (ANTI-MARTINGALE)", style = MaterialTheme.typography.titleSmall, color = NeonYellow)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fixed Stake
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Fixed Stake per Trade", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("Never increased after a loss (No Martingale)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Text("$$fixedStake", style = MaterialTheme.typography.titleMedium, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = fixedStake.toFloat(),
                    onValueChange = {
                        fixedStake = Math.round(it * 10.0) / 10.0
                        onRiskConfigChanged(riskConfig.copy(fixedStake = fixedStake))
                    },
                    valueRange = 0.5f..10.0f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Daily Loss Limit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Loss Limit", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("Locks trading immediately when reached", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Text("$$dailyLossLimit", style = MaterialTheme.typography.titleMedium, color = NeonRed, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = dailyLossLimit.toFloat(),
                    onValueChange = {
                        dailyLossLimit = Math.round(it * 10.0) / 10.0
                        onRiskConfigChanged(riskConfig.copy(dailyLossLimit = dailyLossLimit))
                    },
                    valueRange = 1.0f..20.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRed)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Daily Profit Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Profit Target", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("Locks trading to secure profits", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Text("$$dailyProfitTarget", style = MaterialTheme.typography.titleMedium, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = dailyProfitTarget.toFloat(),
                    onValueChange = {
                        dailyProfitTarget = Math.round(it * 10.0) / 10.0
                        onRiskConfigChanged(riskConfig.copy(dailyProfitTarget = dailyProfitTarget))
                    },
                    valueRange = 1.0f..20.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Max Trades Per Day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Max Trades Per Day", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("Enforces trading discipline", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Text("$maxTradesPerDay", style = MaterialTheme.typography.titleMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = maxTradesPerDay.toFloat(),
                    onValueChange = {
                        maxTradesPerDay = it.toInt()
                        onRiskConfigChanged(riskConfig.copy(maxTradesPerDay = maxTradesPerDay))
                        onDailyLimitChanged(maxTradesPerDay)
                    },
                    valueRange = 2f..20f,
                    steps = 17,
                    colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. AutoTrader Platform DOM Selectors
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("AUTOTRADER DOM SELECTORS", style = MaterialTheme.typography.titleSmall, color = NeonCyan)
                Text("Used for automated stake setting, clicking & balance reading", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stakeInputSel,
                    onValueChange = {
                        stakeInputSel = it
                        onSelectorsChanged(selectors.copy(stakeInputSelector = it))
                    },
                    label = { Text("Stake Input Selector") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = BorderDark)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = upBtnSel,
                    onValueChange = {
                        upBtnSel = it
                        onSelectorsChanged(selectors.copy(upButtonSelector = it))
                    },
                    label = { Text("CALL / UP Button Selector") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderDark)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = downBtnSel,
                    onValueChange = {
                        downBtnSel = it
                        onSelectorsChanged(selectors.copy(downButtonSelector = it))
                    },
                    label = { Text("PUT / DOWN Button Selector") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonRed, unfocusedBorderColor = BorderDark)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = balanceSel,
                    onValueChange = {
                        balanceSel = it
                        onSelectorsChanged(selectors.copy(balanceSelector = it))
                    },
                    label = { Text("Account Balance Selector") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = BorderDark)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = resultSel,
                    onValueChange = {
                        resultSel = it
                        onSelectorsChanged(selectors.copy(resultSelector = it))
                    },
                    label = { Text("Trade Result / Payout Selector") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonYellow, unfocusedBorderColor = BorderDark)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Timeframe Selectors
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("TIMEFRAME SELECTORS", style = MaterialTheme.typography.titleSmall, color = NeonBlue)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sel1m,
                    onValueChange = {
                        sel1m = it
                        onProfileChanged(profile.copy(timeframe1mSelector = it))
                    },
                    label = { Text("1m Selector") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = sel5m,
                    onValueChange = {
                        sel5m = it
                        onProfileChanged(profile.copy(timeframe5mSelector = it))
                    },
                    label = { Text("5m Selector") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = sel15m,
                    onValueChange = {
                        sel15m = it
                        onProfileChanged(profile.copy(timeframe15mSelector = it))
                    },
                    label = { Text("15m Selector") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Audio & Haptics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound Chimes", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = soundEnabled, onCheckedChange = onSoundToggled)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Haptic Feedback", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = hapticEnabled, onCheckedChange = onHapticToggled)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onResetPresets,
            colors = ButtonDefaults.buttonColors(containerColor = BackgroundElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = TextSecondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("RESTORE DEFAULT PRESETS & SELECTORS", color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // REAL Account Confirmation Dialog
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
                            "• Hard stop loss limits ($$dailyLossLimit max daily loss) and max 1 trade at a time are enforced.\n" +
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
