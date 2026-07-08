package com.lamadb.android.logging

import android.content.Context

/**
 * Preferences controlling in-app debug logging.
 *
 * Logging is disabled by default for privacy. When enabled, recent logs are
 * written to a rotating file in the app's private storage and can be viewed
 * or shared from Settings.
 */
class LogPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var loggingEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOGGING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGGING_ENABLED, value).apply()

    companion object {
        private const val PREFS_FILE = "lamadb_logs"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"
    }
}
