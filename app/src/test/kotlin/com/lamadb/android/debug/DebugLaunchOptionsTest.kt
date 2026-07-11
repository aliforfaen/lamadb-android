package com.lamadb.android.debug

import android.content.Intent
import com.lamadb.android.ui.main.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.lamadb.android.BuildConfig
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class DebugLaunchOptionsTest {

    @Test
    fun parseDebugLaunchOptions_readsStateInjectionExtras() {
        assumeTrue(BuildConfig.DEBUG)

        val options = Intent()
            .putExtra(EXTRA_SKIP_ONBOARDING, true)
            .putExtra(EXTRA_SKIP_PRESENCE_SETUP, true)
            .putExtra(EXTRA_START_SCREEN, "wiki")
            .putExtra(EXTRA_USE_TEST_ACCOUNT, true)
            .putExtra(EXTRA_SEED_DATA, true)
            .putExtra(EXTRA_RESET_FIRST_LAUNCH, true)
            .putExtra(EXTRA_QUEUE_EVENT_COUNT, 7)
            .putExtra(EXTRA_WIKI_PAGE_COUNT, 3)
            .putExtra(EXTRA_PRESENCE_STATE, "away")
            .putExtra(EXTRA_AUTH_EXPIRED, true)
            .parseDebugLaunchOptions()

        assertTrue(options.skipOnboarding)
        assertTrue(options.skipPresenceSetup)
        assertEquals(AppDestination.Wiki, options.startScreen)
        assertTrue(options.useTestAccount)
        assertTrue(options.seedData)
        assertTrue(options.resetFirstLaunch)
        assertEquals(7, options.queueEventCount)
        assertEquals(3, options.wikiPageCount)
        assertEquals("away", options.presenceState)
        assertTrue(options.authExpired)
    }

    @Test
    fun parseDebugLaunchOptions_defaultsStateInjectionExtras() {
        val options = Intent().parseDebugLaunchOptions()

        assertEquals(0, options.queueEventCount)
        assertEquals(0, options.wikiPageCount)
        assertNull(options.presenceState)
        assertFalse(options.authExpired)
    }
}
