package com.lamadb.android.ui.health

import android.content.Context

/**
 * Lightweight preferences for the (future) health data integration.
 *
 * Real Samsung Health / Health Connect sync will need the Health Connect SDK and
 * appropriate permissions; for now this stores the user's opt-in preference.
 */
class HealthPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var syncEnabled: Boolean
        get() = prefs.getBoolean(KEY_SYNC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_ENABLED, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    companion object {
        private const val PREFS_FILE = "lamadb_health"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }
}
