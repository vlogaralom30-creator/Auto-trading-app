package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
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
                    allowContentAccess = true
                    allowFileAccess = true
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
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }
                }

                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        update = { webView ->
            if (isDesktopMode) {
                if (webView.settings.userAgentString != DESKTOP_USER_AGENT) {
                    webView.settings.userAgentString = DESKTOP_USER_AGENT
                }
            } else {
                webView.settings.userAgentString = null
            }
            if (webView.url != url && url.isNotBlank()) {
                webView.loadUrl(url)
            }
        }
    )
}
