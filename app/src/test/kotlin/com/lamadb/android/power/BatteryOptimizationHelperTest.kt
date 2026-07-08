package com.lamadb.android.power

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class BatteryOptimizationHelperTest {

    private val context = RuntimeEnvironment.getApplication() as Context

    @Test
    fun `isIgnoringBatteryOptimizations returns true when whitelisted`() {
        val powerManager = mockk<PowerManager> {
            every { isIgnoringBatteryOptimizations(context.packageName) } returns true
        }
        val helper = BatteryOptimizationHelper(context, powerManager)

        assertTrue(helper.isIgnoringBatteryOptimizations())
    }

    @Test
    fun `isIgnoringBatteryOptimizations returns false when optimized`() {
        val powerManager = mockk<PowerManager> {
            every { isIgnoringBatteryOptimizations(context.packageName) } returns false
        }
        val helper = BatteryOptimizationHelper(context, powerManager)

        assertFalse(helper.isIgnoringBatteryOptimizations())
    }

    @Test
    fun `batteryOptimizationSettingsIntent targets correct settings page`() {
        val helper = BatteryOptimizationHelper(context)

        val intent = helper.batteryOptimizationSettingsIntent()

        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun `isSamsungDevice returns true for samsung manufacturer`() {
        val helper = BatteryOptimizationHelper(context, manufacturer = "samsung")
        assertTrue(helper.isSamsungDevice())
    }

    @Test
    fun `isSamsungDevice returns false for other manufacturers`() {
        val helper = BatteryOptimizationHelper(context, manufacturer = "Google")
        assertFalse(helper.isSamsungDevice())
    }
}
