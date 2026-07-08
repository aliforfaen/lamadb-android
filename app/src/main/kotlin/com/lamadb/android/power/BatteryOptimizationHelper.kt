package com.lamadb.android.power

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helpers for dealing with Android battery optimization.
 *
 * Samsung Device Care and similar vendor layers kill background apps aggressively.
 * We cannot force the user to disable optimization, but we can detect it and
 * open the system settings so they can whitelist LamaDB if they choose.
 *
 * @param powerManager Injected for unit tests; defaults to the system service.
 */
class BatteryOptimizationHelper(
    private val context: Context,
    private val powerManager: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager,
    private val manufacturer: String = Build.MANUFACTURER
) {

    /**
     * True when the app is allowed to run in the background unrestricted.
     * minSdk is 31, so the API 23 guard is unnecessary.
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun batteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun openBatteryOptimizationSettings() {
        context.startActivity(batteryOptimizationSettingsIntent())
    }

    /**
     * A rough device-vendor check. Samsung is the primary target because Device Care
     * is known to kill foreground services; the UI copy still applies to most vendors.
     */
    fun isSamsungDevice(): Boolean {
        return manufacturer.equals("samsung", ignoreCase = true)
    }
}
