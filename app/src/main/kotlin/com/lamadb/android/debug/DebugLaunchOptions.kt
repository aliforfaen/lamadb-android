package com.lamadb.android.debug

import android.content.Intent
import com.lamadb.android.BuildConfig
import com.lamadb.android.ui.main.AppDestination

/**
 * Debug-only launch options for agent smoke tests.
 *
 * These are read from intent extras when MainActivity starts. In release builds
 * the parser always returns the default (empty) options and every backdoor is
 * unreachable.
 */
data class DebugLaunchOptions(
    val skipOnboarding: Boolean = false,
    val skipPresenceSetup: Boolean = false,
    val startScreen: AppDestination? = null,
    val useTestAccount: Boolean = false,
    val seedData: Boolean = false,
    val resetFirstLaunch: Boolean = false
)

/**
 * Parse debug launch options from the intent that started MainActivity.
 * Returns defaults immediately for release builds.
 */
fun Intent.parseDebugLaunchOptions(): DebugLaunchOptions {
    if (!BuildConfig.DEBUG) return DebugLaunchOptions()

    return DebugLaunchOptions(
        skipOnboarding = getBooleanExtra(EXTRA_SKIP_ONBOARDING, false),
        skipPresenceSetup = getBooleanExtra(EXTRA_SKIP_PRESENCE_SETUP, false),
        startScreen = getStringExtra(EXTRA_START_SCREEN)?.toAppDestination(),
        useTestAccount = getBooleanExtra(EXTRA_USE_TEST_ACCOUNT, false),
        seedData = getBooleanExtra(EXTRA_SEED_DATA, false),
        resetFirstLaunch = getBooleanExtra(EXTRA_RESET_FIRST_LAUNCH, false)
    )
}

private fun String.toAppDestination(): AppDestination? =
    when (lowercase()) {
        "dashboard" -> AppDestination.Dashboard
        "wiki" -> AppDestination.Wiki
        "tasks" -> AppDestination.Tasks
        "health" -> AppDestination.Health
        "settings" -> AppDestination.Settings
        else -> null
    }

const val EXTRA_SKIP_ONBOARDING = "SKIP_ONBOARDING"
const val EXTRA_SKIP_PRESENCE_SETUP = "SKIP_PRESENCE_SETUP"
const val EXTRA_START_SCREEN = "START_SCREEN"
const val EXTRA_USE_TEST_ACCOUNT = "USE_TEST_ACCOUNT"
const val EXTRA_SEED_DATA = "SEED_DATA"
const val EXTRA_RESET_FIRST_LAUNCH = "RESET_FIRST_LAUNCH"
