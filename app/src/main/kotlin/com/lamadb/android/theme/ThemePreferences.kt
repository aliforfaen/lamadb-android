package com.lamadb.android.theme

import android.content.Context
import android.os.Build

/**
 * User preferences for Material You / dynamic theming and dark mode.
 *
 * Dynamic color is only offered on Android 12+; on older devices the toggle is
 * ignored and the static purple palette is used.
 */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var useDynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, null))
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    val supportsDynamicColor: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    companion object {
        private const val PREFS_FILE = "lamadb_theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.find { it.name == name } ?: SYSTEM
        }
    }
}
