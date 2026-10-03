package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.OverlayLayerSettings
import com.example.ui.theme.BackgroundElevated
import com.example.ui.theme.BorderDark
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LayerControlsDialog(
    settings: OverlayLayerSettings,
    onSettingsChanged: (OverlayLayerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OVERLAY LAYERS",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonBlue
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LayerToggleRow("Support & Resistance Zones", settings.showSRZones) {
                    onSettingsChanged(settings.copy(showSRZones = it))
                }
                LayerToggleRow("Trendlines & Channels", settings.showTrendlines) {
                    onSettingsChanged(settings.copy(showTrendlines = it))
                }
                LayerToggleRow("Pattern Labels & Halos", settings.showPatternLabels) {
                    onSettingsChanged(settings.copy(showPatternLabels = it))
                }
                LayerToggleRow("Indicators (EMA 9/21, RSI)", settings.showIndicators) {
                    onSettingsChanged(settings.copy(showIndicators = it))
                }
                LayerToggleRow("Signal Direction Arrows", settings.showSignalArrows) {
                    onSettingsChanged(settings.copy(showSignalArrows = it))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Overlay Opacity: ${(settings.opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Slider(
                    value = settings.opacity,
                    onValueChange = { onSettingsChanged(settings.copy(opacity = it)) },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonBlue,
                        activeTrackColor = NeonBlue
                    )
                )
            }
        }
    }
}

@Composable
private fun LayerToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonBlue,
                checkedTrackColor = NeonBlue.copy(alpha = 0.4f)
            )
        )
    }
}
