package com.lamadb.android.ui.dashboard

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.SslErrorHandler
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lamadb.android.BuildConfig
import com.lamadb.android.R
import com.lamadb.android.logging.AppLogger
import com.lamadb.android.network.ConnectivityObserver
import com.lamadb.android.network.ConnectionState
import com.lamadb.android.presence.PresencePreferences
import com.lamadb.android.theme.ThemeMode

private const val TAG = "LamaDB"
private const val JS_BRIDGE_NAME = "LamaDBAndroid"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    serverUrl: String,
    apiKey: String,
    themeMode: ThemeMode,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val presencePreferences = remember { PresencePreferences(context) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val isDarkMode = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme
    }

    var isRefreshing by remember { mutableStateOf(false) }
    var loadState by remember { mutableStateOf<DashboardLoadState>(DashboardLoadState.Loading) }
    var connectionState by remember { mutableStateOf<ConnectionState>(ConnectionState.Available) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var mainFrameError by remember { mutableStateOf<String?>(null) }

    val connectivityObserver = remember { ConnectivityObserver(context) }
    LaunchedEffect(connectivityObserver) {
        connectivityObserver.observe().collect { state ->
            connectionState = state
            if (state == ConnectionState.Available && loadState is DashboardLoadState.Error) {
                // Auto-retry when connectivity returns.
                webView?.reload()
            }
        }
    }

    val webViewRef = remember {
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

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                    val line = "[DashboardJS] ${message?.sourceId()}:${message?.lineNumber()} ${message?.message()}"
                    when (message?.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> AppLogger.e(TAG, line)
                        ConsoleMessage.MessageLevel.WARNING -> AppLogger.w(TAG, line)
                        else -> AppLogger.d(TAG, line)
                    }
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isRefreshing = true
                    mainFrameError = null
                    loadState = DashboardLoadState.Loading
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isRefreshing = false
                    val error = mainFrameError
                    if (error != null) {
                        loadState = DashboardLoadState.Error(error)
                    } else {
                        injectViewportFix(view)
                        injectWebViewCss(view)


                        loadState = DashboardLoadState.Success
                        injectDashboardBridge(view)
                        injectAuthToken(view, apiKey)
                        injectTheme(view, isDarkMode)
                        injectPresence(view, presencePreferences)
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        isRefreshing = false
                        val reason = error?.description?.toString() ?: "Unknown error"
                        AppLogger.e(TAG, "Dashboard load error: $reason")
                        mainFrameError = reason
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
                    AppLogger.w(TAG, "SSL error for ${error?.url}: proceeding for internal/Tailnet cert")
                    handler?.proceed()
                }
            }

            loadUrl(serverUrl)
        }
    }

    webView = webViewRef

    // Re-inject theme when the dark-mode preference changes while the page is loaded.
    LaunchedEffect(isDarkMode) {
        if (loadState is DashboardLoadState.Success) {
            injectTheme(webView, isDarkMode)
        }
    }

    // Pause/resume JavaScript when the dashboard screen goes to the background so the
    // WebView does not keep doing work while the user is on another tab.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Wire the pull-refresh trigger to the WebView reload.
    val refreshableState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { webView?.reload() }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(refreshableState)
    ) {
        AndroidView(
            factory = { webViewRef },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                // The WebView is retained across recompositions by remember; we only
                // destroy it when the screen is truly removed.
            }
        )

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = refreshableState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )

        ConnectionStatusIndicator(
            state = connectionState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        if (loadState is DashboardLoadState.Error) {
            ErrorOverlay(
                reason = (loadState as DashboardLoadState.Error).reason,
                onRetry = { webView?.reload() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private sealed class DashboardLoadState {
    data object Loading : DashboardLoadState()
    data object Success : DashboardLoadState()
    data class Error(val reason: String) : DashboardLoadState()
}

@Composable
private fun ConnectionStatusIndicator(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        ConnectionState.Available -> MaterialTheme.colorScheme.primary
        ConnectionState.Unavailable -> MaterialTheme.colorScheme.error
    }
    val label = when (state) {
        ConnectionState.Available -> stringResource(R.string.dashboard_status_online)
        ConnectionState.Unavailable -> stringResource(R.string.dashboard_status_offline)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun ErrorOverlay(
    reason: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.error,
                progress = { 1f }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.dashboard_error_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.dashboard_error_retry))
            }
        }
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

/**
 * Fixes a WebView-specific layout bug where CSS `height: 100%` and `100vh` resolve to
 * 0px on the `<html>` element because the WebView's initial containing block has not
 * received its final layout dimensions at CSS-computation time.
 *
 * The script sets explicit pixel-based `minHeight` on `<html>` and `<body>` using
 * `window.innerHeight`, then listens for resize events to re-apply after rotation.
 * This runs before auth/theme/presence injections so the page is visible when the
 * bridge is ready.
 */
private fun injectViewportFix(view: WebView?) {
    view?.evaluateJavascript(
        """
        (function() {
            console.log('[viewportFix] fixing WebView root height collapse');
            function applyFix() {
                var h = window.innerHeight;
                if (h > 0) {
                    document.documentElement.style.height = h + 'px';
                    document.documentElement.style.minHeight = h + 'px';
                    document.body.style.height = h + 'px';
                    document.body.style.minHeight = h + 'px';
                }
            }
            applyFix();
            window.addEventListener('resize', applyFix);
        })();
        """.trimIndent(),
        null
    )
}

/**
 * Injects CSS overrides to hide the web dashboard's own mobile bottom navigation,
 * since the Android app provides a native NavigationBar. Also reclaims the
 * padding that the web layout reserves for the now-hidden mobile nav.
 */
private fun injectWebViewCss(view: WebView?) {
    view?.evaluateJavascript(
        """
        (function() {
            var style = document.createElement('style');
            style.textContent = [
                '.mobile-nav { display: none !important; }',
                '.mobile-bottom-nav { display: none !important; }',
                '.app-main { padding-bottom: var(--space-4) !important; }'
            ].join(' ');
            document.head.appendChild(style);
            console.log('[webViewCss] hid web mobile nav, reset app-main padding');
        })();
        """.trimIndent(),
        null
    )
}

private val String.jsLiteral: String
    get() = "\"${this.jsString}\""

private val String.jsString: String
    get() = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
