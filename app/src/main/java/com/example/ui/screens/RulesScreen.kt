package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.RuleEntity
import com.example.data.entity.totalTrades
import com.example.data.entity.winRate
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundElevated
import com.example.ui.theme.BorderDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun RulesScreen(
    rules: List<RuleEntity>,
    onToggleRule: (RuleEntity) -> Unit,
    onDeleteRule: (RuleEntity) -> Unit,
    onExportRules: () -> String = { "" },
    onImportRules: (String) -> Unit = {},
    onAddRule: (RuleEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    if (showAddDialog) {
        AddRuleDialog(
            onSave = {
                onAddRule(it)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IMPORT RULES JSON",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Paste JSON array of rules") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showImportDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (importJsonText.isNotBlank()) {
                                    onImportRules(importJsonText)
                                    Toast.makeText(context, "Rules imported successfully", Toast.LENGTH_SHORT).show()
                                    showImportDialog = false
                                    importJsonText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("Import", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("rules_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STRATEGY RULES & LEARNING",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active rules feed confluence scoring. Win/loss updates adjust weights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: Export & Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val exported = onExportRules()
                        if (exported.isNotBlank()) {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("ChartMind Rules", exported)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "Rules JSON copied to clipboard (${rules.size} rules)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export JSON", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Import", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import JSON", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No rules configured. Tap + to add or import rules.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleItemCard(
                            rule = rule,
                            onToggle = { onToggleRule(rule) },
                            onDelete = { onDeleteRule(rule) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_rule_fab"),
            containerColor = NeonCyan,
            contentColor = BackgroundDark
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Rule")
        }
    }
}

@Composable
private fun RuleItemCard(
    rule: RuleEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (rule.isEnabled) BorderDark else Color(0x33EF4444), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction Badge
                val isUp = rule.outcome.equals("UP", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isUp) NeonGreen.copy(alpha = 0.2f) else NeonRed.copy(alpha = 0.2f))
                        .border(1.dp, if (isUp) NeonGreen else NeonRed, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = rule.outcome,
                            tint = if (isUp) NeonGreen else NeonRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = rule.outcome,
                            color = if (isUp) NeonGreen else NeonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Source badge: Builtin vs User
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BackgroundElevated)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = rule.source.uppercase(Locale.US),
                        fontSize = 9.sp,
                        color = if (rule.source == "builtin") NeonCyan else NeonBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Enabled Toggle Switch
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BackgroundElevated
                    )
                )

                if (rule.source != "builtin") {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = NeonRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rule Name
            Text(
                text = rule.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (rule.isEnabled) TextPrimary else TextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Context badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContextChip(text = "Pattern: ${rule.patternType}")
                if (rule.requiredTrend != "ANY") {
                    ContextChip(text = "Trend: ${rule.requiredTrend}")
                }
                if (rule.requireNearSupport) {
                    ContextChip(text = "@ Support", color = NeonGreen)
                }
                if (rule.requireNearResistance) {
                    ContextChip(text = "@ Resistance", color = NeonRed)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Learning & Performance Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundElevated)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("WEIGHT", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.2f", rule.weight),
                        fontSize = 13.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("WIN RATE", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (rule.totalTrades > 0) String.format(Locale.US, "%.1f%%", rule.winRate) else "N/A",
                        fontSize = 13.sp,
                        color = if (rule.winRate >= 50f) NeonGreen else NeonRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("RECORD (W/L)", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${rule.winCount}W / ${rule.lossCount}L",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("TRADES", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${rule.totalTrades}",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextChip(text: String, color: Color = NeonBlue) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AddRuleDialog(
    onSave: (RuleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var patternType by remember { mutableStateOf("HAMMER") }
    var requiredTrend by remember { mutableStateOf("ANY") }
    var outcome by remember { mutableStateOf("UP") }
    var requireSupport by remember { mutableStateOf(false) }
    var requireResistance by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Custom Strategy Rule",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = patternType,
                    onValueChange = { patternType = it },
                    label = { Text("Pattern (HAMMER, ENGULFING, DOJI, PIN_BAR, CUSTOM)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = outcome,
                    onValueChange = { outcome = it },
                    label = { Text("Signal Outcome (UP / DOWN)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = requireSupport, onCheckedChange = { requireSupport = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Requires Support Zone Touch", fontSize = 12.sp, color = TextPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = requireResistance, onCheckedChange = { requireResistance = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Requires Resistance Zone Touch", fontSize = 12.sp, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    RuleEntity(
                                        ruleId = "user_${System.currentTimeMillis()}",
                                        name = name,
                                        patternType = patternType.uppercase(),
                                        requiredTrend = requiredTrend.uppercase(),
                                        requireNearSupport = requireSupport,
                                        requireNearResistance = requireResistance,
                                        outcome = outcome.uppercase(),
                                        weight = 0.10f,
                                        priorStrength = 10,
                                        priorWeight = 0.10f,
                                        source = "user"
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Save Rule", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
