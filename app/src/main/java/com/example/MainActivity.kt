package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ChartMindDatabase
import com.example.data.ChartMindRepository
import com.example.model.AccountMode
import com.example.model.AutoTraderState
import com.example.model.RiskConfig
import com.example.ui.screens.AutoTraderDashboardScreen
import com.example.ui.screens.BacktestScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TeachScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import com.example.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val database = ChartMindDatabase.getDatabase(context, coroutineScope)
                val repository = ChartMindRepository(
                    ruleDao = database.ruleDao(),
                    journalDao = database.journalDao(),
                    backtestDao = database.backtestDao()
                )

                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(repository)
                )

                ChartMindApp(
                    viewModel = viewModel,
                    activity = this
                )
            }
        }
    }
}

@Composable
fun ChartMindApp(
    viewModel: MainViewModel,
    activity: ComponentActivity
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val layerSettings by viewModel.layerSettings.collectAsStateWithLifecycle()
    val calibration by viewModel.colorCalibration.collectAsStateWithLifecycle()
    val siteProfile by viewModel.siteProfile.collectAsStateWithLifecycle()
    val riskConfig by viewModel.riskConfig.collectAsStateWithLifecycle()
    val autoTraderSelectors by viewModel.autoTraderSelectors.collectAsStateWithLifecycle()
    val autoTraderState by viewModel.autoTraderState.collectAsStateWithLifecycle()
    val dailyRiskStats by viewModel.dailyRiskStats.collectAsStateWithLifecycle()
    val accountMode by viewModel.accountMode.collectAsStateWithLifecycle()
    val lastBlockedReason by viewModel.lastBlockedReason.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
    val dailyLimit by viewModel.dailyLimit.collectAsStateWithLifecycle()
    val signalsToday by viewModel.signalsToday.collectAsStateWithLifecycle()
    val consecutiveLosses by viewModel.consecutiveLosses.collectAsStateWithLifecycle()
    val demoSignalCount by viewModel.demoSignalCount.collectAsStateWithLifecycle()
    val isSystemReady by viewModel.isSystemReady.collectAsStateWithLifecycle()
    val contextStats by viewModel.contextStats.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val journalEntries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val backtestSamples by viewModel.backtestSamples.collectAsStateWithLifecycle()
    val isBacktesting by viewModel.isBacktesting.collectAsStateWithLifecycle()
    val lastBacktestSummary by viewModel.lastBacktestSummary.collectAsStateWithLifecycle()
    val frozenSnapshot by viewModel.frozenSnapshot.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Handle Android system back button: return to browser if currently on sub-screen
    BackHandler(enabled = currentScreen != ScreenDestination.BROWSER) {
        viewModel.navigateTo(ScreenDestination.BROWSER)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav"),
                containerColor = BackgroundElevated,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.BROWSER,
                    onClick = { viewModel.navigateTo(ScreenDestination.BROWSER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Browser",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("Browser", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        indicatorColor = NeonBlue.copy(alpha = 0.15f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_browser_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.AUTOTRADER,
                    onClick = { viewModel.navigateTo(ScreenDestination.AUTOTRADER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = "AutoTrader",
                            modifier = Modifier.size(20.dp),
                            tint = if (autoTraderState !is AutoTraderState.Idle) NeonGreen else if (currentScreen == ScreenDestination.AUTOTRADER) NeonBlue else TextMuted
                        )
                    },
                    label = { Text("AutoTrader", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        indicatorColor = NeonBlue.copy(alpha = 0.15f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_autotrader_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.RULES,
                    onClick = { viewModel.navigateTo(ScreenDestination.RULES) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Rule,
                            contentDescription = "Rules",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("Rules", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        indicatorColor = NeonBlue.copy(alpha = 0.15f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_rules_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.JOURNAL,
                    onClick = {
                        viewModel.refreshRiskCounters()
                        viewModel.navigateTo(ScreenDestination.JOURNAL)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = "Journal",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("Journal", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        indicatorColor = NeonBlue.copy(alpha = 0.15f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_journal_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.BACKTEST,
                    onClick = { viewModel.navigateTo(ScreenDestination.BACKTEST) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = "Backtest",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("Backtest", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        indicatorColor = NeonBlue.copy(alpha = 0.15f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_backtest_tab")
                )

                NavigationBarItem(
                    selected = currentScreen == ScreenDestination.SETTINGS,
                    onClick = { viewModel.navigateTo(ScreenDestination.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text("Settings", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        indicatorColor = NeonBlue.copy(alpha = 0.15f),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ScreenDestination.BROWSER -> {
                    BrowserScreen(
                        currentTab = currentTab,
                        tabs = tabs,
                        analysisResult = analysisResult,
                        isAnalyzing = isAnalyzing,
                        layerSettings = layerSettings,
                        accountMode = accountMode,
                        autoTraderState = autoTraderState,
                        onTabSelected = { viewModel.selectTab(it) },
                        onNewTab = { viewModel.openNewTab(it) },
                        onCloseTab = { viewModel.closeTab(it) },
                        onDesktopModeToggled = { viewModel.toggleDesktopMode(it) },
                        onAnalyzeRequested = { webView ->
                            viewModel.runChartAnalysis(context, webView, activity)
                        },
                        onMtfScanRequested = { webView ->
                            viewModel.runMultiTimeframeAnalysis(context, webView, activity)
                        },
                        onTeachQuickCapture = { webView ->
                            viewModel.freezeForTeachMode(webView, activity)
                        },
                        onLayerSettingsChanged = { viewModel.updateLayerSettings(it) },
                        onSaveToJournal = { viewModel.recordSignalToJournal() },
                        onMarkWin = { viewModel.recordLiveSignalFeedback(isWin = true) },
                        onMarkLoss = { viewModel.recordLiveSignalFeedback(isWin = false) },
                        onClearOverlay = { viewModel.clearAnalysis() },
                        onKillSwitch = { viewModel.triggerKillSwitch() }
                    )
                }

                ScreenDestination.AUTOTRADER -> {
                    AutoTraderDashboardScreen(
                        state = autoTraderState,
                        dailyStats = dailyRiskStats,
                        accountMode = accountMode,
                        lastBlockedReason = lastBlockedReason,
                        onStartAutoTrading = {
                            viewModel.navigateTo(ScreenDestination.BROWSER)
                        },
                        onStopAutoTrading = { viewModel.stopAutoTrader() },
                        onKillSwitch = { viewModel.triggerKillSwitch() },
                        onManualUnlock = { viewModel.manualUnlockAutoTrader() },
                        onResetStats = { viewModel.resetAutoTraderStats() },
                        onAccountModeChanged = { viewModel.setAccountMode(it) }
                    )
                }

                ScreenDestination.TEACH -> {
                    TeachScreen(
                        snapshotBitmap = frozenSnapshot,
                        onSaveRule = { rule -> viewModel.saveTaughtRule(rule) },
                        onNavigateBack = { viewModel.navigateTo(ScreenDestination.BROWSER) }
                    )
                }

                ScreenDestination.RULES -> {
                    RulesScreen(
                        rules = rules,
                        onToggleRule = { viewModel.toggleRule(it) },
                        onDeleteRule = { viewModel.deleteRule(it) },
                        onExportRules = { viewModel.exportRulesJson() },
                        onImportRules = { viewModel.importRulesJson(it) }
                    )
                }

                ScreenDestination.JOURNAL -> {
                    JournalScreen(
                        entries = journalEntries,
                        signalsTodayCount = signalsToday,
                        consecutiveLossCount = consecutiveLosses,
                        dailySignalLimit = dailyLimit,
                        demoSignalCount = demoSignalCount,
                        contextStats = contextStats,
                        onMarkResult = { entry, outcome ->
                            viewModel.markJournalResult(entry, outcome)
                        },
                        onDeleteEntry = { viewModel.deleteJournalEntry(it) }
                    )
                }

                ScreenDestination.BACKTEST -> {
                    BacktestScreen(
                        samples = backtestSamples,
                        demoSignalCount = demoSignalCount,
                        isReady = isSystemReady,
                        isTesting = isBacktesting,
                        lastSummary = lastBacktestSummary,
                        currentSnapshot = frozenSnapshot,
                        onRunBacktest = { viewModel.runBacktest() },
                        onAddCurrentAsSample = { label, pat ->
                            viewModel.addBacktestSample(label, pat)
                        },
                        onDeleteSample = { viewModel.deleteBacktestSample(it) }
                    )
                }

                ScreenDestination.SETTINGS -> {
                    SettingsScreen(
                        calibration = calibration,
                        profile = siteProfile,
                        riskConfig = riskConfig,
                        selectors = autoTraderSelectors,
                        accountMode = accountMode,
                        soundEnabled = soundEnabled,
                        hapticEnabled = hapticEnabled,
                        onCalibrationChanged = { viewModel.updateCalibration(it) },
                        onProfileChanged = { viewModel.updateSiteProfile(it) },
                        onRiskConfigChanged = { viewModel.updateRiskConfig(it) },
                        onSelectorsChanged = { viewModel.updateAutoTraderSelectors(it) },
                        onAccountModeChanged = { viewModel.setAccountMode(it) },
                        onSoundToggled = { viewModel.toggleSound(it) },
                        onHapticToggled = { viewModel.toggleHaptic(it) },
                        onDailyLimitChanged = { viewModel.setDailyLimit(it) },
                        onResetPresets = {
                            viewModel.updateRiskConfig(RiskConfig())
                        }
                    )
                }
            }
        }
    }
}
