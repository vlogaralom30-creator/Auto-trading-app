package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.autotrader.AutoTraderEngine
import com.example.autotrader.RiskManager
import com.example.data.BacktestSummary
import com.example.data.ChartMindRepository
import com.example.data.ContextStat
import com.example.data.entity.BacktestSampleEntity
import com.example.data.entity.JournalEntryEntity
import com.example.data.entity.RuleEntity
import com.example.engine.ChartAnalyzer
import com.example.engine.MultiTimeframeScanner
import com.example.model.AccountMode
import com.example.model.AutoTraderSelectors
import com.example.model.AutoTraderState
import com.example.model.BrowserTab
import com.example.model.ChartAnalysisResult
import com.example.model.ColorCalibration
import com.example.model.DailyRiskStats
import com.example.model.OverlayLayerSettings
import com.example.model.RiskConfig
import com.example.model.SiteTimeframeProfile
import com.example.util.AudioHapticNotifier
import com.example.util.RuleJsonHelper
import com.example.util.ScreenshotCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScreenDestination {
    BROWSER,
    AUTOTRADER,
    TEACH,
    RULES,
    JOURNAL,
    BACKTEST,
    SETTINGS
}

class MainViewModel(
    private val repository: ChartMindRepository
) : ViewModel() {

    // AutoTrader Subsystem
    private val autoTraderEngine = AutoTraderEngine(repository)
    val autoTraderState: StateFlow<AutoTraderState> = autoTraderEngine.state
    val dailyRiskStats: StateFlow<DailyRiskStats> = autoTraderEngine.dailyStats
    val accountMode: StateFlow<AccountMode> = autoTraderEngine.accountMode
    val autoTraderSelectors: StateFlow<AutoTraderSelectors> = autoTraderEngine.selectors
    val lastBlockedReason: StateFlow<String?> = autoTraderEngine.lastBlockedReason

    // Navigation
    private val _currentScreen = MutableStateFlow(ScreenDestination.BROWSER)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Browser Tabs
    private val _tabs = MutableStateFlow(
        listOf(
            BrowserTab(
                title = "TradingView",
                url = "https://www.tradingview.com/chart/"
            )
        )
    )
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _currentTab = MutableStateFlow(_tabs.value.first())
    val currentTab: StateFlow<BrowserTab> = _currentTab.asStateFlow()

    // Snapshot & Analysis
    private val _frozenSnapshot = MutableStateFlow<Bitmap?>(null)
    val frozenSnapshot: StateFlow<Bitmap?> = _frozenSnapshot.asStateFlow()

    private val _analysisResult = MutableStateFlow<ChartAnalysisResult?>(null)
    val analysisResult: StateFlow<ChartAnalysisResult?> = _analysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Settings & Layers
    private val _layerSettings = MutableStateFlow(OverlayLayerSettings())
    val layerSettings: StateFlow<OverlayLayerSettings> = _layerSettings.asStateFlow()

    private val _colorCalibration = MutableStateFlow(ColorCalibration())
    val colorCalibration: StateFlow<ColorCalibration> = _colorCalibration.asStateFlow()

    private val _siteProfile = MutableStateFlow(SiteTimeframeProfile(siteKey = "tradingview"))
    val siteProfile: StateFlow<SiteTimeframeProfile> = _siteProfile.asStateFlow()

    private val _riskConfig = MutableStateFlow(RiskConfig())
    val riskConfig: StateFlow<RiskConfig> = _riskConfig.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _dailyLimit = MutableStateFlow(8)
    val dailyLimit: StateFlow<Int> = _dailyLimit.asStateFlow()

    private val _signalsToday = MutableStateFlow(0)
    val signalsToday: StateFlow<Int> = _signalsToday.asStateFlow()

    private val _consecutiveLosses = MutableStateFlow(0)
    val consecutiveLosses: StateFlow<Int> = _consecutiveLosses.asStateFlow()

    private val _demoSignalCount = MutableStateFlow(0)
    val demoSignalCount: StateFlow<Int> = _demoSignalCount.asStateFlow()

    private val _isSystemReady = MutableStateFlow(false)
    val isSystemReady: StateFlow<Boolean> = _isSystemReady.asStateFlow()

    // Backtest
    private val _isBacktesting = MutableStateFlow(false)
    val isBacktesting: StateFlow<Boolean> = _isBacktesting.asStateFlow()

    private val _lastBacktestSummary = MutableStateFlow<BacktestSummary?>(null)
    val lastBacktestSummary: StateFlow<BacktestSummary?> = _lastBacktestSummary.asStateFlow()

    // Context Stats
    private val _contextStats = MutableStateFlow<List<ContextStat>>(emptyList())
    val contextStats: StateFlow<List<ContextStat>> = _contextStats.asStateFlow()

    // Database Observables
    val rules: StateFlow<List<RuleEntity>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntryEntity>> = repository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backtestSamples: StateFlow<List<BacktestSampleEntity>> = repository.allBacktestSamples
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshRiskCounters()
    }

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    fun selectTab(tab: BrowserTab) {
        _currentTab.value = tab
    }

    fun openNewTab(url: String) {
        val newTab = BrowserTab(url = url, title = "Chart")
        _tabs.value = _tabs.value + newTab
        _currentTab.value = newTab
    }

    fun closeTab(tabId: String) {
        val remaining = _tabs.value.filter { it.id != tabId }
        if (remaining.isNotEmpty()) {
            _tabs.value = remaining
            if (_currentTab.value.id == tabId) {
                _currentTab.value = remaining.first()
            }
        }
    }

    fun toggleDesktopMode(isDesktop: Boolean) {
        val updated = _currentTab.value.copy(isDesktopMode = isDesktop)
        _currentTab.value = updated
        _tabs.value = _tabs.value.map { if (it.id == updated.id) updated else it }
    }

    fun updateLayerSettings(settings: OverlayLayerSettings) {
        _layerSettings.value = settings
    }

    fun updateCalibration(calibration: ColorCalibration) {
        _colorCalibration.value = calibration
    }

    fun updateSiteProfile(profile: SiteTimeframeProfile) {
        _siteProfile.value = profile
    }

    fun updateRiskConfig(config: RiskConfig) {
        _riskConfig.value = config
        _dailyLimit.value = config.maxTradesPerDay
        autoTraderEngine.updateRiskConfig(config)
    }

    fun updateAutoTraderSelectors(selectors: AutoTraderSelectors) {
        autoTraderEngine.updateSelectors(selectors)
    }

    fun setAccountMode(mode: AccountMode) {
        autoTraderEngine.setAccountMode(mode)
    }

    fun toggleSound(enabled: Boolean) {
        _soundEnabled.value = enabled
    }

    fun toggleHaptic(enabled: Boolean) {
        _hapticEnabled.value = enabled
    }

    fun clearAnalysis() {
        _analysisResult.value = null
    }

    fun setDailyLimit(limit: Int) {
        _dailyLimit.value = limit
        updateRiskConfig(_riskConfig.value.copy(maxTradesPerDay = limit))
    }

    fun refreshRiskCounters() {
        viewModelScope.launch {
            _signalsToday.value = repository.getSignalsTodayCount()
            _consecutiveLosses.value = repository.getConsecutiveLossCount()
            _demoSignalCount.value = repository.getDemoSignalCount()
            _isSystemReady.value = repository.isSystemReady()
            _contextStats.value = repository.getPerContextStats()
        }
    }

    // -------------------------------------------------------------
    // AutoTrader Controls
    // -------------------------------------------------------------

    fun startAutoTrader(context: Context, webView: WebView, activity: Activity?) {
        autoTraderEngine.startAutoTrading(
            context = context,
            scope = viewModelScope,
            webView = webView,
            activity = activity,
            calibration = _colorCalibration.value,
            profile = _siteProfile.value
        )
    }

    fun stopAutoTrader() {
        autoTraderEngine.triggerKillSwitch("AutoTrader manually stopped")
    }

    fun triggerKillSwitch() {
        autoTraderEngine.triggerKillSwitch("Emergency kill switch activated")
    }

    fun manualUnlockAutoTrader() {
        autoTraderEngine.manualUnlock()
    }

    fun resetAutoTraderStats() {
        autoTraderEngine.resetDailyStats()
    }

    // -------------------------------------------------------------
    // Manual Chart Analysis
    // -------------------------------------------------------------

    fun runChartAnalysis(context: Context, webView: View, activity: Activity?) {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true

        viewModelScope.launch {
            try {
                val bitmap = ScreenshotCapture.captureView(webView, activity)
                if (bitmap != null) {
                    _frozenSnapshot.value = bitmap
                    val activeRules = repository.getEnabledRules()
                    val result = ChartAnalyzer.analyzeChartBitmap(
                        bitmap = bitmap,
                        calibration = _colorCalibration.value,
                        activeRules = activeRules
                    )
                    _analysisResult.value = result

                    AudioHapticNotifier.notifySignal(
                        context = context,
                        isBullish = result.overallSignal == "UP",
                        isBearish = result.overallSignal == "DOWN",
                        soundEnabled = _soundEnabled.value,
                        hapticEnabled = _hapticEnabled.value
                    )
                }
            } catch (e: Exception) {
                // Ignore capture exception
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun runMultiTimeframeAnalysis(context: Context, webView: WebView, activity: Activity?) {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true

        viewModelScope.launch {
            try {
                val activeRules = repository.getEnabledRules()
                val result = MultiTimeframeScanner.runMultiTimeframeScan(
                    webView = webView,
                    activity = activity,
                    calibration = _colorCalibration.value,
                    activeRules = activeRules,
                    profile = _siteProfile.value,
                    consecutiveLosses = _consecutiveLosses.value,
                    dailySignalsUsed = _signalsToday.value,
                    dailySignalLimit = _dailyLimit.value
                )
                _analysisResult.value = result

                AudioHapticNotifier.notifySignal(
                    context = context,
                    isBullish = result.overallSignal == "UP",
                    isBearish = result.overallSignal == "DOWN",
                    soundEnabled = _soundEnabled.value,
                    hapticEnabled = _hapticEnabled.value
                )
            } catch (e: Exception) {
                // Ignore scan exception
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun freezeForTeachMode(webView: View, activity: Activity?) {
        viewModelScope.launch {
            val bitmap = ScreenshotCapture.captureView(webView, activity)
            if (bitmap != null) {
                _frozenSnapshot.value = bitmap
            }
            _currentScreen.value = ScreenDestination.TEACH
        }
    }

    fun saveTaughtRule(rule: RuleEntity) {
        viewModelScope.launch {
            repository.insertRule(rule)
        }
    }

    fun toggleRule(rule: RuleEntity) {
        viewModelScope.launch {
            repository.updateRule(rule)
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            repository.deleteRule(id)
        }
    }

    fun recordSignalToJournal() {
        val result = _analysisResult.value ?: return
        viewModelScope.launch {
            val isDemo = (accountMode.value == AccountMode.DEMO)
            val entry = JournalEntryEntity(
                url = _currentTab.value.url,
                site = if (_currentTab.value.url.contains("tradingview", true)) "TradingView"
                else if (_currentTab.value.url.contains("quotex", true)) "Quotex"
                else if (_currentTab.value.url.contains("exness", true)) "Exness"
                else "Broker",
                assetPair = "BTC/USDT",
                timeframe = result.timeframe,
                isDemo = isDemo,
                signalDirection = result.overallSignal,
                confidence = result.confidenceScore,
                matchedRuleName = result.matchedRule ?: "Price Action & S/R",
                detectedPatterns = result.detectedPatterns.joinToString(", ") { it.patternName },
                nearestSupport = result.nearestSupportDist ?: 0f,
                nearestResistance = result.nearestResistanceDist ?: 0f,
                expirySuggestion = result.suggestedExpiry,
                outcomeResult = "PENDING"
            )
            repository.insertJournalEntry(entry)
            refreshRiskCounters()
        }
    }

    fun recordLiveSignalFeedback(isWin: Boolean) {
        val result = _analysisResult.value ?: return
        viewModelScope.launch {
            val matchedRule = result.matchedRule ?: ""
            if (matchedRule.isNotBlank()) {
                repository.recordRuleResult(matchedRule, isWin)
            }

            val entry = JournalEntryEntity(
                url = _currentTab.value.url,
                site = "Live Terminal",
                assetPair = "Active Asset",
                timeframe = result.timeframe,
                isDemo = (accountMode.value == AccountMode.DEMO),
                signalDirection = result.overallSignal,
                confidence = result.confidenceScore,
                matchedRuleName = matchedRule,
                detectedPatterns = result.detectedPatterns.joinToString(", ") { it.patternName },
                nearestSupport = result.nearestSupportDist ?: 0f,
                nearestResistance = result.nearestResistanceDist ?: 0f,
                expirySuggestion = result.suggestedExpiry,
                outcomeResult = if (isWin) "WIN" else "LOSS"
            )
            repository.insertJournalEntry(entry)
            refreshRiskCounters()
        }
    }

    fun markJournalResult(entry: JournalEntryEntity, outcome: String) {
        if (entry.outcomeResult == outcome) return
        viewModelScope.launch {
            val isWin = outcome.equals("WIN", ignoreCase = true)
            repository.updateJournalEntry(entry.copy(outcomeResult = outcome))
            if (entry.matchedRuleName.isNotBlank() && entry.outcomeResult == "PENDING") {
                repository.recordRuleResult(entry.matchedRuleName, isWin)
            }
            refreshRiskCounters()
        }
    }

    fun deleteJournalEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteJournalEntry(id)
            refreshRiskCounters()
        }
    }

    fun exportRulesJson(): String {
        return RuleJsonHelper.exportRulesToJson(rules.value)
    }

    fun importRulesJson(json: String) {
        viewModelScope.launch {
            try {
                val parsed = RuleJsonHelper.parseRulesFromJson(json)
                for (rule in parsed) {
                    repository.insertRule(rule)
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    // -------------------------------------------------------------
    // Backtest & Paper Trading Replay
    // -------------------------------------------------------------

    fun runBacktest() {
        if (_isBacktesting.value) return
        _isBacktesting.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val summary = repository.runBacktest(_colorCalibration.value)
                _lastBacktestSummary.value = summary
            } catch (e: Exception) {
                // Ignore backtest exception
            } finally {
                _isBacktesting.value = false
            }
        }
    }

    fun addBacktestSample(label: String, patternName: String) {
        val snapshot = _frozenSnapshot.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val sample = BacktestSampleEntity(
                label = label,
                patternName = patternName,
                timeframe = "1m",
                site = "Saved Capture"
            )
            repository.insertBacktestSample(sample)
        }
    }

    fun deleteBacktestSample(id: Long) {
        viewModelScope.launch {
            repository.deleteBacktestSample(id)
        }
    }
}

class MainViewModelFactory(
    private val repository: ChartMindRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
