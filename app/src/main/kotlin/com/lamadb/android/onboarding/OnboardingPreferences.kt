package com.lamadb.android.onboarding

import android.content.Context

/**
 * Tracks whether the user has completed the first-run onboarding flow.
 */
class OnboardingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    companion object {
        private const val PREFS_FILE = "lamadb_onboarding"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
