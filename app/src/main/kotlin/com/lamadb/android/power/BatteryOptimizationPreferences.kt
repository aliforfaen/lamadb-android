package com.lamadb.android.power

import android.content.Context

/**
 * Tracks whether the user has seen/dismissed the battery-optimization guidance.
 */
class BatteryOptimizationPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var guidanceDismissed: Boolean
        get() = prefs.getBoolean(KEY_GUIDANCE_DISMISSED, false)
        set(value) = prefs.edit().putBoolean(KEY_GUIDANCE_DISMISSED, value).apply()

    companion object {
        private const val PREFS_FILE = "lamadb_battery_opt"
        private const val KEY_GUIDANCE_DISMISSED = "guidance_dismissed"
    }
}
