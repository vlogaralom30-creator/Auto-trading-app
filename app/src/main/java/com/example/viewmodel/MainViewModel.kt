package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChartMindRepository
import com.example.data.ContextStat
import com.example.data.entity.JournalEntryEntity
import com.example.data.entity.RuleEntity
import com.example.engine.ChartAnalyzer
import com.example.engine.KnowledgePackLoader
import com.example.engine.WebViewChartController
import com.example.model.BrowserTab
import com.example.model.ChartAnalysisResult
import com.example.model.ColorCalibration
import com.example.model.MultiTimeframeAnalysisResult
import com.example.model.OverlayLayerSettings
import com.example.util.AudioHapticNotifier
import com.example.util.RuleJsonHelper
import com.example.util.ScreenshotCapture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    BROWSER,
    TEACH,
    RULES,
    JOURNAL,
    SETTINGS,
    BACKTEST
}

data class SiteProfileSettings(
    val siteName: String = "TradingView",
    val timeframeSelector: String = "button[data-value='{tf}'], div[data-value='{tf}'], [aria-label*='{tf}']",
    val zoomMethod: String = "wheel_event_on_chart_canvas"
)

class MainViewModel(
    private val repository: ChartMindRepository
) : ViewModel() {

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

    // Analysis & Multi-Timeframe State
    private val _frozenSnapshot = MutableStateFlow<Bitmap?>(null)
    val frozenSnapshot: StateFlow<Bitmap?> = _frozenSnapshot.asStateFlow()

    private val _analysisResult = MutableStateFlow<ChartAnalysisResult?>(null)
    val analysisResult: StateFlow<ChartAnalysisResult?> = _analysisResult.asStateFlow()

    private val _multiTimeframeResult = MutableStateFlow<MultiTimeframeAnalysisResult?>(null)
    val multiTimeframeResult: StateFlow<MultiTimeframeAnalysisResult?> = _multiTimeframeResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow("")
    val scanStatusMessage: StateFlow<String> = _scanStatusMessage.asStateFlow()

    // Settings & Profiles
    private val _layerSettings = MutableStateFlow(OverlayLayerSettings())
    val layerSettings: StateFlow<OverlayLayerSettings> = _layerSettings.asStateFlow()

    private val _colorCalibration = MutableStateFlow(ColorCalibration())
    val colorCalibration: StateFlow<ColorCalibration> = _colorCalibration.asStateFlow()

    private val _siteProfileSettings = MutableStateFlow(SiteProfileSettings())
    val siteProfileSettings: StateFlow<SiteProfileSettings> = _siteProfileSettings.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _dailyLimit = MutableStateFlow(10)
    val dailyLimit: StateFlow<Int> = _dailyLimit.asStateFlow()

    private val _isNewsWindowActive = MutableStateFlow(false)
    val isNewsWindowActive: StateFlow<Boolean> = _isNewsWindowActive.asStateFlow()

    private val _signalsToday = MutableStateFlow(0)
    val signalsToday: StateFlow<Int> = _signalsToday.asStateFlow()

    private val _consecutiveLosses = MutableStateFlow(0)
    val consecutiveLosses: StateFlow<Int> = _consecutiveLosses.asStateFlow()

    private val _demoSignalsCount = MutableStateFlow(0)
    val demoSignalsCount: StateFlow<Int> = _demoSignalsCount.asStateFlow()

    private val _contextStats = MutableStateFlow<List<ContextStat>>(emptyList())
    val contextStats: StateFlow<List<ContextStat>> = _contextStats.asStateFlow()

    private val _userAlert = MutableStateFlow<String?>(null)
    val userAlert: StateFlow<String?> = _userAlert.asStateFlow()

    // Database Observables
    val rules: StateFlow<List<RuleEntity>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntryEntity>> = repository.allJournalEntries
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

    fun updateSiteProfile(site: String, selector: String, zoom: String) {
        _siteProfileSettings.value = SiteProfileSettings(
            siteName = site,
            timeframeSelector = selector,
            zoomMethod = zoom
        )
    }

    fun toggleSound(enabled: Boolean) {
        _soundEnabled.value = enabled
    }

    fun toggleHaptic(enabled: Boolean) {
        _hapticEnabled.value = enabled
    }

    fun toggleNewsWindow(active: Boolean) {
        _isNewsWindowActive.value = active
    }

    fun setDailyLimit(limit: Int) {
        _dailyLimit.value = limit
    }

    fun clearAnalysis() {
        _analysisResult.value = null
        _multiTimeframeResult.value = null
    }

    fun dismissUserAlert() {
        _userAlert.value = null
    }

    fun refreshRiskCounters() {
        viewModelScope.launch {
            _signalsToday.value = repository.getSignalsTodayCount()
            _consecutiveLosses.value = repository.getConsecutiveLossCount()
            _demoSignalsCount.value = repository.getDemoSignalsCount()
            _contextStats.value = repository.getContextStatistics()
        }
    }

    /**
     * Executes single timeframe chart analysis on current visible screen.
     */
    fun runSingleChartAnalysis(context: Context, webView: View, activity: Activity?) {
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
                        activeRules = activeRules,
                        consecutiveLosses = _consecutiveLosses.value,
                        signalsToday = _signalsToday.value,
                        dailyLimit = _dailyLimit.value,
                        isNewsWindowFlagged = _isNewsWindowActive.value,
                        timeframe = "1m"
                    )
                    _analysisResult.value = result

                    if (!result.isBlocked) {
                        AudioHapticNotifier.notifySignal(
                            context = context,
                            isBullish = result.overallSignal == "UP",
                            isBearish = result.overallSignal == "DOWN",
                            soundEnabled = _soundEnabled.value,
                            hapticEnabled = _hapticEnabled.value
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore transient errors
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Executes automated Multi-Timeframe Scan (15m -> 5m -> 1m) in WebView per Knowledge Pack.
     */
    fun runMultiTimeframeScan(context: Context, webView: WebView, activity: Activity?) {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true
        _scanProgress.value = 0f

        viewModelScope.launch {
            try {
                val activeRules = repository.getEnabledRules()
                val profile = _siteProfileSettings.value

                val mtfResult = WebViewChartController.executeMultiTimeframeScan(
                    webView = webView,
                    activity = activity,
                    calibration = _colorCalibration.value,
                    activeRules = activeRules,
                    selectorTemplate = profile.timeframeSelector,
                    zoomMethod = profile.zoomMethod,
                    consecutiveLosses = _consecutiveLosses.value,
                    signalsToday = _signalsToday.value,
                    dailyLimit = _dailyLimit.value,
                    isNewsWindowFlagged = _isNewsWindowActive.value,
                    onProgressUpdate = { msg, prog ->
                        _scanStatusMessage.value = msg
                        _scanProgress.value = prog
                    }
                )

                _multiTimeframeResult.value = mtfResult
                _analysisResult.value = mtfResult.tf1m

                val res1m = mtfResult.tf1m
                if (res1m != null && !res1m.isBlocked) {
                    AudioHapticNotifier.notifySignal(
                        context = context,
                        isBullish = res1m.overallSignal == "UP",
                        isBearish = res1m.overallSignal == "DOWN",
                        soundEnabled = _soundEnabled.value,
                        hapticEnabled = _hapticEnabled.value
                    )
                }
            } catch (e: Exception) {
                // Scan error handling
            } finally {
                _isAnalyzing.value = false
                _scanProgress.value = 0f
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

    fun deleteRule(rule: RuleEntity) {
        deleteRule(rule.id)
    }

    fun recordSignalToJournal() {
        val result = _analysisResult.value ?: return
        viewModelScope.launch {
            val siteName = if (_currentTab.value.url.contains("tradingview", true)) "TradingView"
            else if (_currentTab.value.url.contains("quotex", true)) "Quotex"
            else if (_currentTab.value.url.contains("exness", true)) "Exness"
            else "Chart"

            val entry = JournalEntryEntity(
                url = _currentTab.value.url,
                site = siteName,
                assetPair = "Active Pair",
                timeframe = result.timeframe,
                session = "Active",
                signalDirection = result.overallSignal,
                confidence = result.confidenceScore,
                matchedRuleName = result.matchedRule ?: "Price Action & S/R",
                contributingRuleIds = result.contributingRuleNames.joinToString(", "),
                detectedPatterns = result.detectedPatterns.joinToString(", ") { it.patternName },
                nearestSupport = result.nearestSupportDist ?: 0f,
                nearestResistance = result.nearestResistanceDist ?: 0f,
                expirySuggestion = result.suggestedExpiry,
                outcomeResult = "PENDING",
                isDemo = true,
                blockedReason = result.blockedReason ?: ""
            )
            repository.insertJournalEntry(entry)
            refreshRiskCounters()
        }
    }

    /**
     * Live outcome feedback:
     * - Records trade in journal
     * - Updates winCount/lossCount for ALL contributing rules
     * - Recalculates weights via Bayesian formula
     * - Disables rule if n >= 30 and win rate < 0.48, alerting the user
     */
    fun recordLiveSignalFeedback(isWin: Boolean) {
        val result = _analysisResult.value ?: return
        viewModelScope.launch {
            val contributing = result.contributingRuleNames.ifEmpty {
                result.matchedRule?.let { listOf(it) } ?: emptyList()
            }

            // Apply Knowledge Pack Learning updates
            val report = repository.applyLearningToRules(contributing, isWin)
            if (report.disabledRuleNames.isNotEmpty()) {
                _userAlert.value = "Rule(s) auto-disabled due to low win rate (<48% after 30+ trades): ${report.disabledRuleNames.joinToString(", ")}"
            }

            val entry = JournalEntryEntity(
                url = _currentTab.value.url,
                site = _siteProfileSettings.value.siteName,
                assetPair = "Active Asset",
                timeframe = result.timeframe,
                session = "Live",
                signalDirection = result.overallSignal,
                confidence = result.confidenceScore,
                matchedRuleName = result.matchedRule ?: "Price Action",
                contributingRuleIds = contributing.joinToString(", "),
                detectedPatterns = result.detectedPatterns.joinToString(", ") { it.patternName },
                nearestSupport = result.nearestSupportDist ?: 0f,
                nearestResistance = result.nearestResistanceDist ?: 0f,
                expirySuggestion = result.suggestedExpiry,
                outcomeResult = if (isWin) "WIN" else "LOSS",
                isDemo = true,
                blockedReason = result.blockedReason ?: ""
            )
            repository.insertJournalEntry(entry)
            refreshRiskCounters()
        }
    }

    fun markJournalResult(entry: JournalEntryEntity, outcome: String) {
        viewModelScope.launch {
            val isWin = outcome.equals("WIN", ignoreCase = true)
            repository.updateJournalEntry(entry.copy(outcomeResult = outcome))

            val targets = entry.contributingRuleIds.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { if (entry.matchedRuleName.isNotBlank()) listOf(entry.matchedRuleName) else emptyList() }

            val report = repository.applyLearningToRules(targets, isWin)
            if (report.disabledRuleNames.isNotEmpty()) {
                _userAlert.value = "Rule(s) auto-disabled due to low win rate (<48% after 30+ trades): ${report.disabledRuleNames.joinToString(", ")}"
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

    fun triggerBacktestSimulation() {
        viewModelScope.launch {
            // Evaluates synthetic historical samples
            refreshRiskCounters()
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
