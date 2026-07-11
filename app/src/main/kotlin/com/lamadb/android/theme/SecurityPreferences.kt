package com.lamadb.android.theme

import android.content.Context

class SecurityPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    companion object {
        private const val PREFS_FILE = "lamadb_security"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
