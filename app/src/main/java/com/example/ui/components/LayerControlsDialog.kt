package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.OverlayLayerSettings

@Composable
fun LayerControlsDialog(
    currentSettings: OverlayLayerSettings,
    onSettingsChanged: (OverlayLayerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Overlay Layer Controls",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                LayerSwitchRow("Candlesticks", currentSettings.showCandles) {
                    onSettingsChanged(currentSettings.copy(showCandles = it))
                }

                LayerSwitchRow("Support / Resistance Zones", currentSettings.showSRZones) {
                    onSettingsChanged(currentSettings.copy(showSRZones = it))
                }

                LayerSwitchRow("Trendlines", currentSettings.showTrendlines) {
                    onSettingsChanged(currentSettings.copy(showTrendlines = it))
                }

                LayerSwitchRow("Pattern Recognition Labels", currentSettings.showPatternLabels) {
                    onSettingsChanged(currentSettings.copy(showPatternLabels = it))
                }

                LayerSwitchRow("Signal Arrows", currentSettings.showSignalArrows) {
                    onSettingsChanged(currentSettings.copy(showSignalArrows = it))
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun LayerSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
