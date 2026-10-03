package com.example.autotrader

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import com.example.data.ChartMindRepository
import com.example.data.entity.JournalEntryEntity
import com.example.engine.ChartAnalyzer
import com.example.engine.MultiTimeframeScanner
import com.example.model.AccountMode
import com.example.model.AutoTraderSelectors
import com.example.model.AutoTraderState
import com.example.model.ChartAnalysisResult
import com.example.model.ColorCalibration
import com.example.model.DailyRiskStats
import com.example.model.RiskConfig
import com.example.model.SiteTimeframeProfile
import com.example.util.AudioHapticNotifier
import com.example.util.ScreenshotCapture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Core AutoTrader Engine.
 * Manages state machine: IDLE -> SCANNING -> WAITING_ENTRY -> PLACING -> IN_TRADE -> RESULT -> COOLDOWN/LOCKED.
 * Strictly executes only ONE trade at a time.
 */
class AutoTraderEngine(
    private val repository: ChartMindRepository,
    private val riskManager: RiskManager = RiskManager()
) {

    private val _state = MutableStateFlow<AutoTraderState>(AutoTraderState.Idle)
    val state: StateFlow<AutoTraderState> = _state.asStateFlow()

    private val _dailyStats = MutableStateFlow(DailyRiskStats())
    val dailyStats: StateFlow<DailyRiskStats> = _dailyStats.asStateFlow()

    private val _accountMode = MutableStateFlow(AccountMode.DEMO)
    val accountMode: StateFlow<AccountMode> = _accountMode.asStateFlow()

    private val _selectors = MutableStateFlow(AutoTraderSelectors())
    val selectors: StateFlow<AutoTraderSelectors> = _selectors.asStateFlow()

    private val _lastBlockedReason = MutableStateFlow<String?>(null)
    val lastBlockedReason: StateFlow<String?> = _lastBlockedReason.asStateFlow()

    private var executionJob: Job? = null
    private var countdownJob: Job? = null

    fun setAccountMode(mode: AccountMode) {
        _accountMode.value = mode
    }

    fun updateRiskConfig(config: RiskConfig) {
        riskManager.config = config
    }

    fun updateSelectors(selectors: AutoTraderSelectors) {
        _selectors.value = selectors
    }

    fun manualUnlock() {
        _dailyStats.value = _dailyStats.value.copy(
            isLocked = false,
            lockReason = null,
            consecutiveLosses = 0
        )
        _state.value = AutoTraderState.Idle
    }

    fun resetDailyStats() {
        _dailyStats.value = DailyRiskStats(
            dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
            startingBalance = _dailyStats.value.currentBalance,
            currentBalance = _dailyStats.value.currentBalance
        )
        _state.value = AutoTraderState.Idle
    }

    /**
     * Emergency Kill Switch. Immediately halts all background execution and returns to Idle.
     */
    fun triggerKillSwitch(reason: String = "Kill switch engaged by user") {
        executionJob?.cancel()
        countdownJob?.cancel()
        executionJob = null
        countdownJob = null
        _state.value = AutoTraderState.Idle
        _lastBlockedReason.value = reason
    }

    /**
     * Starts the automated scan & trade cycle.
     */
    fun startAutoTrading(
        context: Context,
        scope: CoroutineScope,
        webView: WebView,
        activity: Activity?,
        calibration: ColorCalibration,
        profile: SiteTimeframeProfile
    ) {
        if (_state.value !is AutoTraderState.Idle && _state.value !is AutoTraderState.Cooldown) {
            return
        }

        executionJob?.cancel()
        executionJob = scope.launch(Dispatchers.Main) {
            try {
                runTradingLoop(context, webView, activity, calibration, profile)
            } catch (e: CancellationException) {
                _state.value = AutoTraderState.Idle
            } catch (e: Exception) {
                _state.value = AutoTraderState.Locked(
                    lockReason = "AutoTrader error: ${e.message ?: "Unexpected exception"}",
                    isManualUnlockRequired = true
                )
            }
        }
    }

    private suspend fun runTradingLoop(
        context: Context,
        webView: WebView,
        activity: Activity?,
        calibration: ColorCalibration,
        profile: SiteTimeframeProfile
    ) {
        while (coroutineContext.isActive) {
            // 1. Check Risk Manager permissions
            val currentBalance = readBalanceFromWebView(webView)
            if (currentBalance > 0.0) {
                _dailyStats.value = _dailyStats.value.copy(currentBalance = currentBalance)
            }

            val riskCheck = riskManager.canPlaceTrade(
                stats = _dailyStats.value,
                requestedStake = riskManager.config.fixedStake,
                currentBalance = _dailyStats.value.currentBalance
            )

            when (riskCheck) {
                is RiskManager.RiskEvaluation.Rejected -> {
                    if (riskCheck.shouldLock) {
                        _state.value = AutoTraderState.Locked(riskCheck.reason)
                        _dailyStats.value = _dailyStats.value.copy(isLocked = true, lockReason = riskCheck.reason)
                        return
                    } else {
                        _lastBlockedReason.value = riskCheck.reason
                        _state.value = AutoTraderState.Idle
                        delay(5000)
                        continue
                    }
                }
                is RiskManager.RiskEvaluation.CooldownTriggered -> {
                    _state.value = AutoTraderState.Cooldown(
                        reason = riskCheck.reason,
                        remainingCooldownSec = riskCheck.durationMinutes * 60
                    )
                    var remaining = riskCheck.durationMinutes * 60
                    while (remaining > 0 && coroutineContext.isActive) {
                        delay(1000)
                        remaining--
                        _state.value = AutoTraderState.Cooldown(riskCheck.reason, remaining)
                    }
                    _dailyStats.value = _dailyStats.value.copy(consecutiveLosses = 0)
                    _state.value = AutoTraderState.Idle
                    continue
                }
                is RiskManager.RiskEvaluation.Allowed -> {
                    // Proceed to scan
                }
            }

            // 2. State: SCANNING
            _state.value = AutoTraderState.Scanning("Scanning Multi-Timeframe (15m -> 5m -> 1m)...", 10)
            val activeRules = repository.getEnabledRules()

            val analysisResult: ChartAnalysisResult
            try {
                analysisResult = MultiTimeframeScanner.runMultiTimeframeScan(
                    webView = webView,
                    activity = activity,
                    calibration = calibration,
                    activeRules = activeRules,
                    profile = profile,
                    consecutiveLosses = _dailyStats.value.consecutiveLosses,
                    dailySignalsUsed = _dailyStats.value.totalTradesToday,
                    dailySignalLimit = riskManager.config.maxTradesPerDay,
                    onProgress = { msg ->
                        _state.value = AutoTraderState.Scanning(msg, 50)
                    }
                )
            } catch (e: Exception) {
                _state.value = AutoTraderState.Locked("Scanning failed: ${e.message}. Check DOM selectors in Settings.")
                return
            }

            // 3. Evaluate Signal Validity
            if (analysisResult.isBlockedByFilter) {
                _lastBlockedReason.value = analysisResult.blockReason ?: "Filtered by risk rule"
                _state.value = AutoTraderState.Idle
                delay(6000) // Wait before re-scanning
                continue
            }

            if (analysisResult.overallSignal != "UP" && analysisResult.overallSignal != "DOWN") {
                _lastBlockedReason.value = "Market in neutral consolidation - no high-probability confluence setup"
                _state.value = AutoTraderState.Idle
                delay(5000)
                continue
            }

            // 4. State: WAITING_ENTRY (Wait for candle close -> Enter at opening of next bar)
            val secondsUntilNextBar = calculateSecondsUntilNextCandle(analysisResult.timeframe)
            _state.value = AutoTraderState.WaitingEntry(
                signal = analysisResult.overallSignal,
                confidence = analysisResult.confidenceScore,
                matchedRule = analysisResult.matchedRule ?: "Price Action Confluence",
                reasons = analysisResult.reasonList,
                secondsUntilNextCandle = secondsUntilNextBar,
                suggestedExpiry = analysisResult.suggestedExpiry
            )

            // Countdown to candle close
            var waitSec = secondsUntilNextBar
            while (waitSec > 0 && coroutineContext.isActive) {
                delay(1000)
                waitSec--
                val curr = _state.value
                if (curr is AutoTraderState.WaitingEntry) {
                    _state.value = curr.copy(secondsUntilNextCandle = waitSec)
                }
            }

            // 5. State: PLACING
            val stake = riskManager.config.fixedStake
            val direction = analysisResult.overallSignal
            val expiry = analysisResult.suggestedExpiry

            _state.value = AutoTraderState.Placing(direction, stake, expiry, "Injecting $direction order ($$stake)...")

            // Execute in WebView
            val orderPlaced = executeTradeInWebView(
                webView = webView,
                direction = direction,
                stake = stake,
                selectors = _selectors.value
            )

            if (!orderPlaced) {
                _state.value = AutoTraderState.Locked(
                    lockReason = "Order placement could not be verified on page. Trading paused to prevent duplicate orders. Check selectors in Settings.",
                    isManualUnlockRequired = true
                )
                return
            }

            // Haptic & sound cue
            AudioHapticNotifier.notifySignal(
                context = context,
                isBullish = direction == "UP",
                isBearish = direction == "DOWN"
            )

            // 6. State: IN_TRADE (Duration: 60 seconds for 1m binary trade)
            val tradeDurationSec = 60
            val tradeId = "T-${System.currentTimeMillis() % 100000}"
            _state.value = AutoTraderState.InTrade(
                tradeId = tradeId,
                direction = direction,
                stake = stake,
                entryTime = System.currentTimeMillis(),
                expiryDurationSec = tradeDurationSec,
                remainingSec = tradeDurationSec,
                pair = "BTC/USDT"
            )

            var remainingSec = tradeDurationSec
            while (remainingSec > 0 && coroutineContext.isActive) {
                delay(1000)
                remainingSec--
                val inTradeState = _state.value
                if (inTradeState is AutoTraderState.InTrade) {
                    _state.value = inTradeState.copy(remainingSec = remainingSec)
                }
            }

            // 7. State: RESULT
            delay(2000) // Brief pause for broker trade settlement
            val tradeOutcome = verifyTradeResult(webView, _selectors.value)
            val isWin = tradeOutcome.isWin

            // Update Risk Manager & Stats
            _dailyStats.value = riskManager.processTradeResult(
                currentStats = _dailyStats.value,
                isWin = isWin,
                stake = stake
            )

            // Update Learning Block (Bayesian weight adjustment)
            val matchedRule = analysisResult.matchedRule ?: ""
            if (matchedRule.isNotBlank()) {
                repository.recordRuleResult(matchedRule, isWin)
            }

            // Log to Room Journal
            val journalEntry = JournalEntryEntity(
                url = webView.url ?: "Broker Platform",
                site = "AutoTrader",
                assetPair = "BTC/USDT",
                timeframe = analysisResult.timeframe,
                isDemo = (_accountMode.value == AccountMode.DEMO),
                signalDirection = direction,
                confidence = analysisResult.confidenceScore,
                matchedRuleName = matchedRule,
                detectedPatterns = analysisResult.detectedPatterns.joinToString(", ") { it.patternName },
                nearestSupport = analysisResult.nearestSupportDist ?: 0f,
                nearestResistance = analysisResult.nearestResistanceDist ?: 0f,
                expirySuggestion = expiry,
                outcomeResult = if (isWin) "WIN" else "LOSS",
                notes = "AutoTrader execution [${if (_accountMode.value == AccountMode.DEMO) "DEMO" else "REAL"}] • Stake: $$stake • Balance: $${String.format("%.2f", _dailyStats.value.currentBalance)}"
            )
            repository.insertJournalEntry(journalEntry)

            val pnl = if (isWin) stake * 0.85 else -stake
            _state.value = AutoTraderState.Result(
                isWin = isWin,
                profitLoss = pnl,
                newBalance = _dailyStats.value.currentBalance,
                message = if (isWin) "Trade WON (+$$pnl)" else "Trade LOST (-$$stake)"
            )

            delay(4000) // Display result before returning to Idle / next scan
            _state.value = AutoTraderState.Idle
        }
    }

    private fun calculateSecondsUntilNextCandle(timeframe: String): Int {
        val nowSec = (System.currentTimeMillis() / 1000) % 60
        return (60 - nowSec.toInt()).coerceAtLeast(1)
    }

    /**
     * Executes order injection via JavaScript in the WebView.
     */
    private suspend fun executeTradeInWebView(
        webView: WebView,
        direction: String,
        stake: Double,
        selectors: AutoTraderSelectors
    ): Boolean = suspendCoroutine { cont ->
        val btnSelector = if (direction == "UP") selectors.upButtonSelector else selectors.downButtonSelector
        val escapedBtn = btnSelector.replace("'", "\\'")
        val escapedStakeInput = selectors.stakeInputSelector.replace("'", "\\'")
        val escapedVerifier = selectors.activeTradeVerifierSelector.replace("'", "\\'")

        val js = """
            (function() {
                try {
                    // 1. Set stake amount if input found
                    var stakeInput = document.querySelector('$escapedStakeInput');
                    if (stakeInput) {
                        stakeInput.value = '$stake';
                        stakeInput.dispatchEvent(new Event('input', { bubbles: true }));
                        stakeInput.dispatchEvent(new Event('change', { bubbles: true }));
                    }

                    // 2. Find and click directional button
                    var btn = document.querySelector('$escapedBtn');
                    if (!btn) {
                        var buttons = document.querySelectorAll('button, div[role="button"]');
                        for (var i = 0; i < buttons.length; i++) {
                            var text = (buttons[i].innerText || '').toLowerCase();
                            var target = '${direction.lowercase()}';
                            if (text.includes(target) || (target === 'up' && (text.includes('call') || text.includes('higher'))) || (target === 'down' && (text.includes('put') || text.includes('lower')))) {
                                btn = buttons[i];
                                break;
                            }
                        }
                    }

                    if (btn) {
                        btn.click();
                        return 'CLICKED_ORDER';
                    }
                    return 'BUTTON_NOT_FOUND';
                } catch(e) {
                    return 'ERROR: ' + e.message;
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { res ->
            val success = res != null && res.contains("CLICKED_ORDER")
            cont.resume(success)
        }
    }

    private suspend fun readBalanceFromWebView(webView: WebView): Double = suspendCoroutine { cont ->
        val selector = _selectors.value.balanceSelector.replace("'", "\\'")
        val js = """
            (function() {
                try {
                    var el = document.querySelector('$selector');
                    if (el) {
                        var txt = el.innerText.replace(/[^0-9.]/g, '');
                        return parseFloat(txt) || 0;
                    }
                    return 0;
                } catch(e) {
                    return 0;
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { res ->
            val parsed = res?.replace("\"", "")?.toDoubleOrNull() ?: 0.0
            cont.resume(parsed)
        }
    }

    private data class TradeResultInfo(val isWin: Boolean, val payout: Double)

    private suspend fun verifyTradeResult(webView: WebView, selectors: AutoTraderSelectors): TradeResultInfo = suspendCoroutine { cont ->
        val selector = selectors.resultSelector.replace("'", "\\'")
        val js = """
            (function() {
                try {
                    var el = document.querySelector('$selector');
                    if (el) {
                        var txt = (el.innerText || '').toLowerCase();
                        if (txt.includes('win') || txt.includes('+') || txt.includes('profit')) return 'WIN';
                        if (txt.includes('loss') || txt.includes('-')) return 'LOSS';
                    }
                    return 'UNKNOWN';
                } catch(e) {
                    return 'ERROR';
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { res ->
            val isWin = res != null && res.contains("WIN")
            // Default to realistic probabilistic payout if DOM text not explicit
            cont.resume(TradeResultInfo(isWin = isWin, payout = 0.85))
        }
    }
}
