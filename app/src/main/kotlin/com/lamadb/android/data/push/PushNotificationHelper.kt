package com.lamadb.android.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lamadb.android.MainActivity
import com.lamadb.android.R

/**
 * Creates notification channels and displays ntfy push notifications.
 *
 * Channels map to ntfy priority levels so users can configure critical vs.
 * low-priority notifications independently in Android settings.
 */
class PushNotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
        as NotificationManager

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (PushPreferences(context).channelsCreated) return

        val channels = listOf(
            buildChannel(
                CHANNEL_CRITICAL,
                R.string.push_channel_critical_name,
                R.string.push_channel_critical_description,
                NotificationManager.IMPORTANCE_HIGH
            ),
            buildChannel(
                CHANNEL_HIGH,
                R.string.push_channel_high_name,
                R.string.push_channel_high_description,
                NotificationManager.IMPORTANCE_HIGH
            ),
            buildChannel(
                CHANNEL_DEFAULT,
                R.string.push_channel_default_name,
                R.string.push_channel_default_description,
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            buildChannel(
                CHANNEL_LOW,
                R.string.push_channel_low_name,
                R.string.push_channel_low_description,
                NotificationManager.IMPORTANCE_LOW
            )
        )
        notificationManager.createNotificationChannels(channels)
        PushPreferences(context).channelsCreated = true
    }

    /**
     * Show a notification for an ntfy message. Newer messages with the same
     * [topic] replace older ones so the shade does not fill up with duplicates.
     */
    fun show(message: NtfyMessage) {
        ensureChannels()

        val channelId = channelForPriority(message.priority)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_PUSH, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = message.title?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.push_notification_default_title)
        val text = message.message

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(notificationPriority(channelId))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)

        if (message.priority >= 5) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300))
        }

        val notificationId = message.topic.hashCode()
        notificationManager.notify(notificationId, builder.build())
    }

    private fun buildChannel(
        id: String,
        nameRes: Int,
        descriptionRes: Int,
        importance: Int
    ): NotificationChannel {
        return NotificationChannel(id, context.getString(nameRes), importance).apply {
            description = context.getString(descriptionRes)
        }
    }

    private fun channelForPriority(priority: Int): String = when {
        priority >= 5 -> CHANNEL_CRITICAL
        priority >= 4 -> CHANNEL_HIGH
        priority >= 3 -> CHANNEL_DEFAULT
        else -> CHANNEL_LOW
    }

    private fun notificationPriority(channelId: String): Int = when (channelId) {
        CHANNEL_CRITICAL -> NotificationCompat.PRIORITY_HIGH
        CHANNEL_HIGH -> NotificationCompat.PRIORITY_HIGH
        CHANNEL_DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
        else -> NotificationCompat.PRIORITY_LOW
    }

    companion object {
        const val CHANNEL_CRITICAL = "lamadb_push_critical"
        const val CHANNEL_HIGH = "lamadb_push_high"
        const val CHANNEL_DEFAULT = "lamadb_push_default"
        const val CHANNEL_LOW = "lamadb_push_low"

        const val EXTRA_FROM_PUSH = "from_push"
    }
}
