package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingWebView(
    url: String,
    isDesktopMode: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Configure Cookie Manager for broker authentication & chart sessions
                val webViewInstance = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webViewInstance, true)
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    // Security hardening: Restrict local file & content sandbox exposure
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    mediaPlaybackRequiresUserGesture = false
                    if (isDesktopMode) {
                        userAgentString = DESKTOP_USER_AGENT
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        url?.let { onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        url?.let { onPageFinished(it) }
                        CookieManager.getInstance().flush()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val reqUrl = request?.url?.toString() ?: return false
                        val uri = Uri.parse(reqUrl)
                        val scheme = uri.scheme?.lowercase() ?: ""

                        // Block malicious or dangerous local schemes
                        if (scheme == "file" || scheme == "content") {
                            return true
                        }

                        // Allow standard web navigation
                        return false
                    }
                }

                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        update = { webView ->
            val targetUserAgent = if (isDesktopMode) DESKTOP_USER_AGENT else null
            val currentAgent = webView.settings.userAgentString
            val agentChanged = if (isDesktopMode) {
                currentAgent != DESKTOP_USER_AGENT
            } else {
                currentAgent != null && currentAgent.contains("Linux x86_64")
            }

            if (agentChanged) {
                webView.settings.userAgentString = targetUserAgent
                if (webView.url != null) {
                    webView.reload()
                }
            }

            if (webView.url != url && url.isNotBlank()) {
                webView.loadUrl(url)
            }
        }
    )
}
