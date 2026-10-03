package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

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
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var rendererRecoveryKey by remember { mutableIntStateOf(0) }

    DisposableEffect(rendererRecoveryKey) {
        onDispose {
            try {
                webViewInstance?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    (parent as? ViewGroup)?.removeView(this)
                    destroy()
                }
            } catch (e: Exception) {
                // Ignore cleanup error
            }
            webViewInstance = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        key(rendererRecoveryKey) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewInstance = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Hardware acceleration layer
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                            // Manage Desktop vs Mobile user agent
                            userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else null
                        }

                        // Cookie persistence across sessions
                        val currentWebView = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(currentWebView, true)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let { onPageStarted(it) }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let { onPageFinished(it) }
                                CookieManager.getInstance().flush()
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val requestUrl = request?.url?.toString() ?: return false
                                return !(requestUrl.startsWith("http://") || requestUrl.startsWith("https://"))
                            }

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.cancel()
                            }

                            /**
                             * CRITICAL: Handles renderer process crash gracefully without killing host application.
                             */
                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                try {
                                    view?.let {
                                        (it.parent as? ViewGroup)?.removeView(it)
                                        it.destroy()
                                    }
                                } catch (e: Exception) {
                                    // Ignore cleanup exception
                                }
                                // Increment key to seamlessly re-instantiate clean WebView
                                rendererRecoveryKey++
                                return true // Return true: Host application handles crash and must NOT be killed
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                onProgressChanged(newProgress)
                            }
                        }

                        loadUrl(url)
                        onWebViewCreated(this)
                    }
                },
                update = { webView ->
                    val targetUa = if (isDesktopMode) DESKTOP_USER_AGENT else null
                    if (webView.settings.userAgentString != targetUa) {
                        webView.settings.userAgentString = targetUa
                        webView.reload()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
