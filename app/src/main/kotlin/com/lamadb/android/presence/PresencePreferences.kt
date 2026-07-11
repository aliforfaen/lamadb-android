package com.lamadb.android.presence

import android.content.Context
import android.os.Build
import java.util.Locale
import java.util.UUID

/**
 * Plain-text preferences for the WiFi presence sensor.
 *
 * The home SSID is not sensitive; the device id is a stable generated identifier.
 */
class PresencePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var homeSsid: String?
        get() = prefs.getString(KEY_HOME_SSID, null)
        set(value) {
            prefs.edit().putString(KEY_HOME_SSID, value).apply()
        }

    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val generated = generateDeviceId()
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }

    val isSetupComplete: Boolean
        get() = !homeSsid.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun generateDeviceId(): String {
        val base = "lamadb-${Build.MANUFACTURER}-${Build.MODEL}-${UUID.randomUUID()}"
        return base.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9-]"), "-")
    }

    companion object {
        private const val PREFS_FILE = "lamadb_presence"
        private const val KEY_HOME_SSID = "home_ssid"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
