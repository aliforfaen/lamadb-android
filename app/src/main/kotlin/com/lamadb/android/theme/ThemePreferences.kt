package com.lamadb.android.theme

import android.content.Context
import android.os.Build

/**
 * User preferences for Material You / dynamic theming.
 *
 * Dynamic color is only offered on Android 12+; on older devices the toggle is
 * ignored and the static purple palette is used.
 */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var useDynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    val supportsDynamicColor: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    companion object {
        private const val PREFS_FILE = "lamadb_theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}
