package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RuleEntity
import com.example.engine.CandleExtractor
import com.example.engine.ColorCalibrator
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TeachScreen(
    snapshotBitmap: Bitmap? = null,
    onSaveRule: (RuleEntity) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var ruleName by remember { mutableStateOf("") }
    var patternType by remember { mutableStateOf("HAMMER") }
    var requiredTrend by remember { mutableStateOf("ANY") }
    var outcome by remember { mutableStateOf("UP") }
    var notes by remember { mutableStateOf("") }
    var detectedInsight by remember { mutableStateOf<String?>(null) }

    // Auto-extract features from snapshot if present
    LaunchedEffect(snapshotBitmap) {
        if (snapshotBitmap != null) {
            val candles = CandleExtractor.extractCandles(snapshotBitmap)
            if (candles.isNotEmpty()) {
                val lastCandle = candles.last()
                val isBull = lastCandle.isBullish
                outcome = if (isBull) "UP" else "DOWN"
                
                if (lastCandle.lowerWick > lastCandle.bodyHeight * 1.8f) {
                    patternType = "HAMMER"
                    ruleName = "Support Hammer Reversal"
                    detectedInsight = "Detected long lower rejection wick (Hammer Setup)"
                } else if (lastCandle.upperWick > lastCandle.bodyHeight * 1.8f) {
                    patternType = "SHOOTING_STAR"
                    ruleName = "Resistance Shooting Star Reversal"
                    outcome = "DOWN"
                    detectedInsight = "Detected long upper rejection wick (Shooting Star Setup)"
                } else if (lastCandle.bodyRatio > 0.75f) {
                    patternType = "ENGULFING"
                    ruleName = if (isBull) "Bullish Momentum Engulfing" else "Bearish Momentum Engulfing"
                    detectedInsight = "Detected strong momentum body (${(lastCandle.bodyRatio * 100).toInt()}% body ratio)"
                } else {
                    patternType = "CUSTOM"
                    ruleName = "Price Action Reaction"
                    detectedInsight = "Captured ${candles.size} candles from chart viewport"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("teach_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "TEACH / ANNOTATE RULE",
                style = MaterialTheme.typography.titleLarge,
                color = NeonBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (snapshotBitmap != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Image(
                    bitmap = snapshotBitmap.asImageBitmap(),
                    contentDescription = "Captured Chart Snapshot",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            if (detectedInsight != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Insight",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = detectedInsight ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "New Rule Definition",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Rule Name (e.g. Morning Pin Bar Support Bounce)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = patternType,
                    onValueChange = { patternType = it },
                    label = { Text("Pattern Type (HAMMER, ENGULFING, PIN_BAR, CUSTOM)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = requiredTrend,
                    onValueChange = { requiredTrend = it },
                    label = { Text("Trend Context (UP, DOWN, ANY)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

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

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observation Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (ruleName.isNotBlank()) {
                            val newRule = RuleEntity(
                                ruleId = "user_${System.currentTimeMillis()}",
                                name = ruleName,
                                patternType = patternType.uppercase(),
                                requiredTrend = requiredTrend.uppercase(),
                                requireNearSupport = patternType.contains("HAMMER", true) || outcome == "UP",
                                requireNearResistance = outcome == "DOWN",
                                outcome = outcome.uppercase(),
                                weight = 0.10f,
                                priorStrength = 10,
                                priorWeight = 0.10f,
                                source = "user",
                                notes = notes
                            )
                            onSaveRule(newRule)
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_taught_rule_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = BackgroundDark)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Save Rule to Knowledge Base", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
