package com.lamadb.android.debug

import android.content.Context

/**
 * Debug-only preferences for the Android client.
 *
 * These are stored separately from release preferences and are only exposed in
 * debug builds via the settings screen.
 */
class DebugPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    /**
     * When true, the web dashboard's own debug overlay (e.g. the red state bar
     * showing class/notes/activity counts) is shown. When false, Android injects
     * CSS to hide it.
     */
    var dashboardDebugOverlay: Boolean
        get() = prefs.getBoolean(KEY_DASHBOARD_DEBUG_OVERLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_DASHBOARD_DEBUG_OVERLAY, value).apply()

    companion object {
        private const val PREFS_FILE = "lamadb_debug"
        private const val KEY_DASHBOARD_DEBUG_OVERLAY = "dashboard_debug_overlay"
    }
}
