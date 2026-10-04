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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.ColorCalibration
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

@Composable
fun SettingsScreen(
    calibration: ColorCalibration,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    dailyLimit: Int,
    isNewsWindowActive: Boolean,
    currentSite: String,
    timeframeSelector: String,
    zoomMethod: String,
    onCalibrationChanged: (ColorCalibration) -> Unit,
    onSoundToggled: (Boolean) -> Unit,
    onHapticToggled: (Boolean) -> Unit,
    onDailyLimitChanged: (Int) -> Unit,
    onNewsWindowToggled: (Boolean) -> Unit,
    onSiteProfileChanged: (site: String, selector: String, zoom: String) -> Unit,
    onResetPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSite by remember(currentSite) { mutableStateOf(currentSite) }
    var selectorText by remember(timeframeSelector) { mutableStateOf(timeframeSelector) }
    var activeZoomMethod by remember(zoomMethod) { mutableStateOf(zoomMethod) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "SETTINGS & SITE PROFILES",
            style = MaterialTheme.typography.titleLarge,
            color = NeonBlue,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure multi-timeframe DOM selectors, broker calibration & risk filters",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Multi-Timeframe Site Profiles & Editable DOM Selectors
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = "Site Profiles", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MULTI-TIMEFRAME SITE PROFILES",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Selectors differ per site. Verify in DevTools and configure below (never hardcoded).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Site selection chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TradingView", "Quotex", "Exness").forEach { site ->
                        val isSelected = activeSite.equals(site, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else BackgroundElevated)
                                .border(1.dp, if (isSelected) NeonCyan else BorderDark, RoundedCornerShape(8.dp))
                                .clickable {
                                    activeSite = site
                                    val (defaultSel, defaultZoom) = when (site.lowercase()) {
                                        "quotex" -> Pair("div[data-value='{tf}'], button[data-time='{tf}']", "pinch_gesture")
                                        "exness" -> Pair("button[data-period='{tf}'], div[data-value='{tf}']", "wheel_event_on_chart_canvas")
                                        else -> Pair("button[data-value='{tf}'], div[data-value='{tf}'], [aria-label*='{tf}']", "wheel_event_on_chart_canvas")
                                    }
                                    selectorText = defaultSel
                                    activeZoomMethod = defaultZoom
                                    onSiteProfileChanged(site, defaultSel, defaultZoom)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = site,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Timeframe CSS Selector ({tf} is replaced with 15m, 5m, 1m):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectorText,
                    onValueChange = {
                        selectorText = it
                        onSiteProfileChanged(activeSite, it, activeZoomMethod)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeframe_selector_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Chart Fit Zoom Method:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isWheel = activeZoomMethod == "wheel_event_on_chart_canvas"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isWheel) NeonBlue.copy(alpha = 0.2f) else BackgroundElevated)
                            .border(1.dp, if (isWheel) NeonBlue else BorderDark, RoundedCornerShape(8.dp))
                        .clickable {
                            activeZoomMethod = "wheel_event_on_chart_canvas"
                            onSiteProfileChanged(activeSite, selectorText, activeZoomMethod)
                        }
                        .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Wheel Event Canvas",
                            fontSize = 11.sp,
                            color = if (isWheel) NeonBlue else TextPrimary
                        )
                    }

                    val isPinch = activeZoomMethod == "pinch_gesture"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPinch) NeonBlue.copy(alpha = 0.2f) else BackgroundElevated)
                            .border(1.dp, if (isPinch) NeonBlue else BorderDark, RoundedCornerShape(8.dp))
                            .clickable {
                                activeZoomMethod = "pinch_gesture"
                                onSiteProfileChanged(activeSite, selectorText, activeZoomMethod)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pinch Touch Gesture",
                            fontSize = 11.sp,
                            color = if (isPinch) NeonBlue else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Risk Limits & No-Trade Filters
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = "Risk Guard", tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RISK CONTROLS & NO-TRADE FILTERS",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Economic News release window toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Newspaper, contentDescription = "News", tint = NeonRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("High-Impact News Window", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("Blocks signals during major economic releases", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                    Switch(
                        checked = isNewsWindowActive,
                        onCheckedChange = onNewsWindowToggled,
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonRed, checkedTrackColor = NeonRed.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Daily Limit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Signal Limit:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text("$dailyLimit signals", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
                Slider(
                    value = dailyLimit.toFloat(),
                    onValueChange = { onDailyLimitChanged(it.toInt()) },
                    valueRange = 5f..30f,
                    steps = 24,
                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sound & Haptic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Audio", tint = NeonBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audio Alert Tones", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                    Switch(checked = soundEnabled, onCheckedChange = onSoundToggled)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = "Haptic", tint = NeonBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Haptic Feedback Pulses", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                    Switch(checked = hapticEnabled, onCheckedChange = onHapticToggled)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Color Calibration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = "Calibration", tint = NeonBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHART CANDLE COLOR CALIBRATION",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Starting colors: #26A69A (Up) / #EF5350 (Down). Adjust tolerance for broker themes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pixel Matching Distance Tolerance:", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text("${calibration.tolerance}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = NeonBlue)
                }
                Slider(
                    value = calibration.tolerance.toFloat(),
                    onValueChange = { onCalibrationChanged(calibration.copy(tolerance = it.toInt())) },
                    valueRange = 15f..90f,
                    colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onResetPresets,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundElevated),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESET COLOR CALIBRATION PRESETS", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}
