package com.lamadb.android.data.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lamadb.android.logging.AppLogger

/**
 * Re-schedules the ntfy push worker after the device boots.
 *
 * WorkManager persisted jobs normally survive reboots, but this receiver
 * ensures the channel setup and schedule are refreshed promptly.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AppLogger.d("BootReceiver", "Device booted; syncing push schedule")
        NtfyPushWorker.syncSchedule(context.applicationContext)
    }
}
