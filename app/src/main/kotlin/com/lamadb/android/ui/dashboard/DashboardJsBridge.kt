package com.lamadb.android.ui.dashboard

import android.webkit.JavascriptInterface

/**
 * Native side of the WebView ↔ app bridge.
 *
 * The dashboard can call:
 *   LamaDBAndroid.openScanner()
 *   LamaDBAndroid.requestPresence()
 *
 * Native code can call the dashboard back via:
 *   webView.evaluateJavascript("setAuthToken('...')", null)
 *   webView.evaluateJavascript("setTheme(true)", null)
 *   webView.evaluateJavascript("setPresence('home')", null)
 */
class DashboardJsBridge(
    private val onOpenScanner: () -> Unit,
    private val onRequestPresence: () -> String
) {

    @JavascriptInterface
    fun openScanner() {
        onOpenScanner()
    }

    @JavascriptInterface
    fun requestPresence(): String {
        return onRequestPresence()
    }
}
