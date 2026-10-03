# ChartMind - Trading-Focused Browser with On-Device AI/CV Chart Analysis

ChartMind is an Android browser and on-device chart intelligence terminal built with Kotlin, Jetpack Compose, and Material 3. It allows traders to browse any charting platform (TradingView, Quotex, Exness, Binance, etc.), capture the chart viewport, run on-device computer vision to detect candlesticks and price action geometry, and teach the engine custom pattern rules that continually improve via a win/loss feedback loop.

---

## Key Features

1. **Integrated In-App Browser**
   - Full multi-tab browsing powered by hardware-accelerated Android `WebView`.
   - Desktop-mode toggle with desktop Chrome user-agent override for complex trading terminals.
   - Persistent cookies and session management via `CookieManager`.
   - Quick-access bookmark chips for TradingView, Quotex, Exness, Binance, and PocketOption.
   - SSL security lock badge, forward/back, reload, and responsive address bar.

2. **On-Device Computer Vision & Technical Structure Analysis**
   - Pure Kotlin high-performance pixel segmentation (<100ms) with zero heavy native `.so` dependencies.
   - RGB/HSV color calibration configurable per broker/dark-mode theme.
   - Chronological candlestick extraction: High, Low, Open, Close, body geometry, upper/lower wicks.
   - Swing High / Swing Low peak and trough identification.
   - Support & Resistance (S/R) horizontal cluster zones with touch-count detection.
   - Linear regression trendlines, channel bounds, and breakout/breakdown alerts.
   - Synthetic indicators: EMA 9, EMA 21, and 14-period RSI approximation.

3. **Modular Candlestick Pattern Recognition Engine**
   - `PatternDetector` modular architecture supporting:
     - **Hammer & Inverted Hammer** (bullish rejection with long lower shadow)
     - **Shooting Star** (bearish rejection with long upper shadow)
     - **Bullish & Bearish Engulfing** (dominant momentum body envelope)
     - **Doji** (indecision & contraction)
     - **Pin Bar** (sharp directional rejection wick)
     - **Morning Star & Evening Star** (three-bar high-probability reversals)
     - **Inside Bar** (volatility compression breakout setups)

4. **Teach Mode & Rule Builder Engine**
   - Freeze chart snapshot and drag an interactive rectangle bounding box to select regions.
   - Define custom rules: Target pattern + Macro trend requirement + Proximity to Support/Resistance.
   - Assign expected outcome (CALL/UP or PUT/DOWN) and initial confidence weight.
   - Saved locally in Room database (`RuleEntity`).
   - Import & export rules in JSON format.

5. **Self-Optimizing Learning Loop**
   - Record signals to the persistent Trade Journal.
   - Mark signals as "Win" or "Loss"; the engine automatically increments/decrements rule weights and tracks per-rule win rates.

6. **Risk Management & Probability Governance**
   - Clear probability warning notices ("Signals are probabilities, not guarantees").
   - Configurable daily signal ceiling (e.g. 10 signals/day).
   - Automated cool-down lockout after 3 consecutive losses to mitigate emotional trading.

7. **Pixel-Aligned Compose Overlay Canvas**
   - Interactive multi-layer canvas overlaying the WebView:
     - S/R zones with price levels and touch counts.
     - Regression trendlines.
     - EMA lines and RSI status tags.
     - Candlestick halo highlights and pattern labels.
     - Glowing CALL/PUT signal direction arrows.
   - Layer toggles and opacity slider (0.2 to 1.0).

---

## Architecture

- **UI Framework:** 100% Jetpack Compose with Material Design 3.
- **State Management:** MVVM with `StateFlow` and `collectAsStateWithLifecycle`.
- **Local Persistence:** Room Database (`ChartMindDatabase`) using KSP (Kotlin Symbol Processing).
- **Asynchronous Execution:** Kotlin Coroutines (`Dispatchers.Default` for vision, `Dispatchers.IO` for Room DB).
- **Haptics & Audio:** `VibratorManager` waveform pulses and `ToneGenerator` chimes.

---

## Build & Test Instructions

### Requirements
- JDK 17
- Android SDK with compileSdk 36 (minSdk 26)

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

### Compile & Build Debug APK
```bash
gradle :app:assembleDebug
```
