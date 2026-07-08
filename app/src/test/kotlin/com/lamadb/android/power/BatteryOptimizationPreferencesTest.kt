package com.lamadb.android.power

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class BatteryOptimizationPreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: BatteryOptimizationPreferences

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        // Use a fresh prefs file name so tests do not interfere with each other.
        context.getSharedPreferences("lamadb_battery_opt_test", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        prefs = BatteryOptimizationPreferences(context)
    }

    @Test
    fun `guidanceDismissed defaults to false`() {
        assertFalse(prefs.guidanceDismissed)
    }

    @Test
    fun `guidanceDismissed persists true`() {
        prefs.guidanceDismissed = true
        assertTrue(prefs.guidanceDismissed)
    }

    @Test
    fun `guidanceDismissed can be reset to false`() {
        prefs.guidanceDismissed = true
        assertTrue(prefs.guidanceDismissed)

        prefs.guidanceDismissed = false
        assertFalse(prefs.guidanceDismissed)
    }
}
