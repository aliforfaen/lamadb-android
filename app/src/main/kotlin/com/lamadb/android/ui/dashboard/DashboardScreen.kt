package com.lamadb.android.ui.dashboard

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.lamadb.android.BuildConfig
import com.lamadb.android.presence.PresencePreferences

private const val TAG = "LamaDB"
private const val JS_BRIDGE_NAME = "LamaDBAndroid"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    serverUrl: String,
    apiKey: String,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val presencePreferences = remember { PresencePreferences(context) }
    val isSystemInDarkTheme = isSystemInDarkTheme()

    var isRefreshing by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            val jsBridge = DashboardJsBridge(
                onOpenScanner = onOpenQrScanner,
                onRequestPresence = { currentPresenceJson(presencePreferences) }
            )
            addJavascriptInterface(jsBridge, JS_BRIDGE_NAME)

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isRefreshing = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isRefreshing = false
                    injectDashboardBridge(view)
                    injectAuthToken(view, apiKey)
                    injectTheme(view, isSystemInDarkTheme)
                    injectPresence(view, presencePreferences)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        isRefreshing = false
                        Log.e(TAG, "Dashboard load error: ${error?.description}")
                    }
                }

                @SuppressLint("WebViewClientOnReceivedSslError")
                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: android.net.http.SslError?
                ) {
                    // Tailnet / internal certificates are not signed by public CAs.
                    // Proceed for the configured LamaDB URL only.
                    Log.w(TAG, "SSL error for ${error?.url}: proceeding for internal/Tailnet cert")
                    handler?.proceed()
                }
            }

            loadUrl(serverUrl)
        }
    }

    // Wire the pull-refresh trigger to the WebView reload.
    val refreshableState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { webView.reload() }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(refreshableState)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = refreshableState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

private fun injectDashboardBridge(view: WebView?) {
    view?.evaluateJavascript(
        """
        (function() {
            if (window.LamaDBAndroidBridgeReady) return;
            window.openScanner = function() { $JS_BRIDGE_NAME.openScanner(); };
            window.requestPresence = function() { return $JS_BRIDGE_NAME.requestPresence(); };
            window.LamaDBAndroidBridgeReady = true;
        })();
        """.trimIndent(),
        null
    )
}

private fun injectAuthToken(view: WebView?, apiKey: String) {
    val js = "if (window.setAuthToken) { window.setAuthToken(${apiKey.jsLiteral}); }"
    view?.evaluateJavascript(js, null)
}

private fun injectTheme(view: WebView?, isDark: Boolean) {
    val js = "if (window.setTheme) { window.setTheme(${isDark}); }"
    view?.evaluateJavascript(js, null)
}

private fun injectPresence(view: WebView?, presencePreferences: PresencePreferences) {
    val json = currentPresenceJson(presencePreferences)
    val js = "if (window.setPresence) { window.setPresence(${json.jsLiteral}); }"
    view?.evaluateJavascript(js, null)
}

private fun currentPresenceJson(presencePreferences: PresencePreferences): String {
    // The service keeps the authoritative state in memory; the dashboard can call
    // requestPresence() when it needs a fresher value. For the initial page load we
    // report unknown because the foreground service state is not persisted.
    return """{"state":"unknown","home_ssid":"${presencePreferences.homeSsid.orEmpty().jsString}","device_id":"${presencePreferences.deviceId.jsString}"}"""
}

private val String.jsLiteral: String
    get() = "\"${this.jsString}\""

private val String.jsString: String
    get() = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
