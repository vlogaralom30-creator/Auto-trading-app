package com.example.ui.screens

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccountMode
import com.example.model.AutoTraderState
import com.example.model.BrowserTab
import com.example.model.ChartAnalysisResult
import com.example.model.OverlayLayerSettings
import com.example.ui.components.ChartOverlayCanvas
import com.example.ui.components.LayerControlsDialog
import com.example.ui.components.SignalBottomSheet
import com.example.ui.components.TradingWebView
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    currentTab: BrowserTab,
    tabs: List<BrowserTab>,
    analysisResult: ChartAnalysisResult?,
    isAnalyzing: Boolean,
    layerSettings: OverlayLayerSettings,
    accountMode: AccountMode = AccountMode.DEMO,
    autoTraderState: AutoTraderState = AutoTraderState.Idle,
    onTabSelected: (BrowserTab) -> Unit,
    onNewTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onDesktopModeToggled: (Boolean) -> Unit,
    onAnalyzeRequested: (WebView) -> Unit,
    onMtfScanRequested: (WebView) -> Unit = {},
    onTeachQuickCapture: (WebView) -> Unit,
    onLayerSettingsChanged: (OverlayLayerSettings) -> Unit,
    onSaveToJournal: () -> Unit,
    onMarkWin: () -> Unit,
    onMarkLoss: () -> Unit,
    onClearOverlay: () -> Unit = {},
    onKillSwitch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var urlInput by remember(currentTab.url) { mutableStateOf(currentTab.url) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showTabManager by remember { mutableStateOf(false) }
    var showSignalSheet by remember(analysisResult) { mutableStateOf(analysisResult != null) }

    val focusManager = LocalFocusManager.current

    // Draggable floating action button offsets
    var floatingOffsetX by remember { mutableFloatStateOf(0f) }
    var floatingOffsetY by remember { mutableFloatStateOf(0f) }

    val bookmarks = listOf(
        Pair("TradingView", "https://www.tradingview.com/chart/"),
        Pair("Quotex", "https://quotex.com/"),
        Pair("Exness", "https://www.exness.com/"),
        Pair("Binance", "https://www.binance.com/en/trade/BTC_USDT"),
        Pair("PocketOption", "https://pocketoption.com/")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("browser_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Slim Top Navigation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BackgroundElevated,
                tonalElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back
                        IconButton(
                            onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                            modifier = Modifier.size(32.dp).testTag("browser_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Forward
                        IconButton(
                            onClick = { webViewRef?.let { if (it.canGoForward()) it.goForward() } },
                            modifier = Modifier.size(32.dp).testTag("browser_forward_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Refresh
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(32.dp).testTag("browser_reload_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // URL Bar
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("browser_url_input"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "SSL",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    focusManager.clearFocus()
                                    val formatted = if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                                        "https://$urlInput"
                                    } else urlInput
                                    webViewRef?.loadUrl(formatted)
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Account Mode Tag (REAL / DEMO)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (accountMode == AccountMode.REAL) NeonRed.copy(0.2f) else NeonGreen.copy(0.2f))
                                .border(1.dp, if (accountMode == AccountMode.REAL) NeonRed else NeonGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (accountMode == AccountMode.REAL) "REAL" else "DEMO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (accountMode == AccountMode.REAL) NeonRed else NeonGreen
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Desktop Mode Toggle
                        IconButton(
                            onClick = { onDesktopModeToggled(!currentTab.isDesktopMode) },
                            modifier = Modifier.size(32.dp).testTag("desktop_mode_btn")
                        ) {
                            Icon(
                                imageVector = if (currentTab.isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                                contentDescription = "Desktop Mode",
                                tint = if (currentTab.isDesktopMode) NeonBlue else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Tab Manager Button
                        IconButton(
                            onClick = { showTabManager = true },
                            modifier = Modifier.size(32.dp).testTag("tab_manager_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tab,
                                    contentDescription = "Tabs",
                                    tint = NeonBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${tabs.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Loading Progress Indicator
                    if (pageProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { pageProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = NeonBlue,
                            trackColor = Color.Transparent
                        )
                    }

                    // Bookmarks Quick-Access Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bookmarks.forEach { (name, url) ->
                            val isSelected = currentTab.url.contains(name.lowercase())
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NeonBlue.copy(0.2f) else SurfaceCard)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonBlue else BorderDark,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        urlInput = url
                                        webViewRef?.loadUrl(url)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = if (isSelected) NeonBlue else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Central Chart Area: WebView + Pixel-Aligned Overlay Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // In-App Browser WebView
                TradingWebView(
                    url = currentTab.url,
                    isDesktopMode = currentTab.isDesktopMode,
                    onWebViewCreated = { webViewRef = it },
                    onPageStarted = { urlInput = it },
                    onPageFinished = { urlInput = it },
                    onProgressChanged = { pageProgress = it }
                )

                // Transparent Overlay Canvas drawn on top of WebView
                ChartOverlayCanvas(
                    result = analysisResult,
                    layerSettings = layerSettings
                )

                // Overlay active status pill with clear button
                if (analysisResult != null && analysisResult.candles.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xDD090D16))
                            .border(1.dp, NeonBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OVERLAY ACTIVE (${analysisResult.srZones.size} S/R • ${analysisResult.detectedPatterns.size} PATTERNS)",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = NeonBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Overlay",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onClearOverlay() }
                            )
                        }
                    }
                }

                // AutoTrader Status Pill overlay on WebView if running
                if (autoTraderState !is AutoTraderState.Idle) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xDD090D16))
                            .border(1.dp, NeonGreen.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = NeonGreen,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTOTRADER: ${autoTraderState.javaClass.simpleName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Draggable Floating Action Controls Pill
        Box(
            modifier = Modifier
                .offset { IntOffset(floatingOffsetX.roundToInt(), floatingOffsetY.roundToInt()) }
                .align(Alignment.BottomEnd)
                .padding(bottom = if (showSignalSheet) 240.dp else 16.dp, end = 16.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        floatingOffsetX += dragAmount.x
                        floatingOffsetY += dragAmount.y
                    }
                }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = BackgroundElevated.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Floating "ANALYZE" Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(NeonBlue)
                            .clickable(enabled = !isAnalyzing) {
                                webViewRef?.let { onAnalyzeRequested(it) }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("floating_analyze_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Analyze",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAnalyzing) "SCANNING..." else "ANALYZE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }

                    // Floating "MTF SCAN" Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan, RoundedCornerShape(18.dp))
                            .clickable(enabled = !isAnalyzing) {
                                webViewRef?.let { onMtfScanRequested(it) }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("floating_mtf_scan_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MTF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    // Floating "LAYERS" Button
                    IconButton(
                        onClick = { showLayersDialog = true },
                        modifier = Modifier.size(34.dp).testTag("floating_layers_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Layers",
                            tint = NeonBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Floating "TEACH" Quick-Capture Button
                    IconButton(
                        onClick = { webViewRef?.let { onTeachQuickCapture(it) } },
                        modifier = Modifier.size(34.dp).testTag("floating_teach_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Teach Capture",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Floating Emergency Kill Switch if AutoTrader active
                    if (autoTraderState !is AutoTraderState.Idle) {
                        IconButton(
                            onClick = onKillSwitch,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonRed)
                                .testTag("floating_kill_switch_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Emergency Kill Switch",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Collapsible Signal Bottom Sheet
        AnimatedVisibility(
            visible = showSignalSheet && analysisResult != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            analysisResult?.let { res ->
                SignalBottomSheet(
                    result = res,
                    onSaveToJournal = onSaveToJournal,
                    onMarkWin = onMarkWin,
                    onMarkLoss = onMarkLoss,
                    onDismiss = { showSignalSheet = false }
                )
            }
        }
    }

    // Layer Controls Dialog
    if (showLayersDialog) {
        LayerControlsDialog(
            settings = layerSettings,
            onSettingsChanged = onLayerSettingsChanged,
            onDismiss = { showLayersDialog = false }
        )
    }

    // Tabs Manager Sheet
    if (showTabManager) {
        ModalBottomSheet(
            onDismissRequest = { showTabManager = false },
            containerColor = BackgroundElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BROWSER TABS (${tabs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonBlue
                    )

                    IconButton(onClick = {
                        onNewTab("https://www.tradingview.com/chart/")
                        showTabManager = false
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab", tint = NeonGreen)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                for (tab in tabs) {
                    val isCurrent = tab.id == currentTab.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                1.dp,
                                if (isCurrent) NeonBlue else BorderDark,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onTabSelected(tab)
                                showTabManager = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) SurfaceCard else BackgroundDark
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isCurrent) NeonBlue else TextPrimary
                                )
                                Text(
                                    text = tab.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }

                            if (tabs.size > 1) {
                                IconButton(onClick = { onCloseTab(tab.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
