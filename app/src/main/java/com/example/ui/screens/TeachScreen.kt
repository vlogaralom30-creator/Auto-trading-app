package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RuleEntity
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundElevated
import com.example.ui.theme.BorderDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TeachScreen(
    snapshotBitmap: Bitmap?,
    onSaveRule: (RuleEntity) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ruleName by remember { mutableStateOf("Custom Pattern Strategy") }
    var selectedPattern by remember { mutableStateOf("HAMMER") }
    var selectedTrend by remember { mutableStateOf("ANY") }
    var requireNearSupport by remember { mutableStateOf(false) }
    var requireNearResistance by remember { mutableStateOf(false) }
    var selectedOutcome by remember { mutableStateOf("UP") }
    var ruleWeight by remember { mutableFloatStateOf(0.80f) }
    var notes by remember { mutableStateOf("") }

    // User bounding box coordinates on snapshot
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    val patternsList = listOf(
        "HAMMER", "SHOOTING_STAR", "BULLISH_ENGULFING",
        "BEARISH_ENGULFING", "DOJI", "PIN_BAR",
        "MORNING_STAR", "EVENING_STAR", "INSIDE_BAR", "CUSTOM"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("teach_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TEACH MODE",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Teach custom visual setups & train the inference engine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive snapshot view with drag selection box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = BackgroundElevated),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStart = offset
                                dragEnd = offset
                            },
                            onDrag = { change, _ ->
                                dragEnd = change.position
                            }
                        )
                    }
            ) {
                if (snapshotBitmap != null) {
                    Image(
                        bitmap = snapshotBitmap.asImageBitmap(),
                        contentDescription = "Chart Snapshot",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Freeze",
                            tint = NeonBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Interactive Chart Region Canvas",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Drag a rectangle on the chart below to define pattern bounds",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Draw bounding box
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val start = dragStart
                    val end = dragEnd
                    if (start != null && end != null) {
                        val left = min(start.x, end.x)
                        val top = min(start.y, end.y)
                        val width = abs(end.x - start.x)
                        val height = abs(end.y - start.y)

                        if (width > 5f && height > 5f) {
                            drawRect(
                                color = NeonBlue.copy(alpha = 0.25f),
                                topLeft = Offset(left, top),
                                size = Size(width, height)
                            )
                            drawRect(
                                color = NeonBlue,
                                topLeft = Offset(left, top),
                                size = Size(width, height),
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (dragStart != null && dragEnd != null) "Region Selected" else "Drag to Select Region",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rule Builder Form
        Text(
            text = "RULE SPECIFICATIONS",
            style = MaterialTheme.typography.labelSmall,
            color = NeonBlue
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = ruleName,
            onValueChange = { ruleName = it },
            label = { Text("Strategy Rule Name") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rule_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pattern Selector Chips
        Text(
            text = "Target Pattern Type",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), enabled = false),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Horizontal wrapping row
        }
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            patternsList.forEach { pat ->
                FilterChip(
                    selected = selectedPattern == pat,
                    onClick = { selectedPattern = pat },
                    label = { Text(pat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonBlue,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Required Trend Filter
        Text(
            text = "Required Macro Trend",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ANY", "UP", "DOWN", "SIDEWAYS").forEach { trend ->
                FilterChip(
                    selected = selectedTrend == trend,
                    onClick = { selectedTrend = trend },
                    label = { Text(trend) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonBlue,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Proximity toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Must be near Support Zone", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = requireNearSupport,
                onCheckedChange = { requireNearSupport = it },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(0.4f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Must be near Resistance Zone", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = requireNearResistance,
                onCheckedChange = { requireNearResistance = it },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonRed, checkedTrackColor = NeonRed.copy(0.4f))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Outcome (Direction UP or DOWN)
        Text(
            text = "Expected Direction / Signal Outcome",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { selectedOutcome = "UP" },
                modifier = Modifier.weight(1f).testTag("outcome_up_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOutcome == "UP") NeonGreen else SurfaceCard,
                    contentColor = if (selectedOutcome == "UP") Color.Black else TextSecondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "UP")
                Spacer(modifier = Modifier.width(4.dp))
                Text("CALL (UP)")
            }

            Button(
                onClick = { selectedOutcome = "DOWN" },
                modifier = Modifier.weight(1f).testTag("outcome_down_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOutcome == "DOWN") NeonRed else SurfaceCard,
                    contentColor = if (selectedOutcome == "DOWN") Color.White else TextSecondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "DOWN")
                Spacer(modifier = Modifier.width(4.dp))
                Text("PUT (DOWN)")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rule Initial Weight
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Initial Confidence Weight", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("${(ruleWeight * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = NeonBlue)
        }
        Slider(
            value = ruleWeight,
            onValueChange = { ruleWeight = it },
            valueRange = 0.5f..1.0f,
            colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Strategy Notes & Setup Criteria") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                val newRule = RuleEntity(
                    name = ruleName.ifBlank { "$selectedPattern $selectedOutcome Setup" },
                    patternType = selectedPattern,
                    requiredTrend = selectedTrend,
                    requireNearSupport = requireNearSupport,
                    requireNearResistance = requireNearResistance,
                    outcome = selectedOutcome,
                    weight = ruleWeight,
                    winCount = 1,
                    lossCount = 0,
                    notes = notes
                )
                onSaveRule(newRule)
                saveSuccessMessage = "Rule '${newRule.name}' taught successfully!"
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_taught_rule_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonBlue,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Taught Rule to Brain", style = MaterialTheme.typography.labelLarge)
        }

        saveSuccessMessage?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonGreen.copy(alpha = 0.2f))
                    .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = "Saved", tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }
    }
}
