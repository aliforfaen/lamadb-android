package com.lamadb.android.data.push

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lamadb.android.logging.AppLogger
import java.util.concurrent.TimeUnit

/**
 * Polls the configured ntfy topic and surfaces new messages as notifications.
 *
 * ntfy is used as a self-hosted, Google-free push gateway: the LamaDB backend
 * posts critical notifications to the topic, and this worker polls it
 * periodically so messages arrive even when the app is in the background.
 */
class NtfyPushWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = PushPreferences(context)
        if (!prefs.pushEnabled) {
            return Result.success()
        }

        val helper = PushNotificationHelper(context)
        helper.ensureChannels()

        val client = NtfyApiClient(prefs.ntfyUrl, prefs.ntfyTopic)
        val lastShown = prefs.lastMessageTime

        return try {
            val messages = client.poll(since = "15m")
            var newest = lastShown
            var shown = 0

            for (message in messages) {
                if (message.time > lastShown) {
                    helper.show(message)
                    shown++
                }
                if (message.time > newest) {
                    newest = message.time
                }
            }

            if (newest > lastShown) {
                prefs.lastMessageTime = newest
            }
            AppLogger.d(TAG, "Poll complete: $shown new messages")
            Result.success()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Push poll failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "NtfyPush"
        private const val WORK_NAME = "lamadb_ntfy_push"

        /**
         * Schedules the periodic ntfy poll if push is enabled, or cancels it
         * when disabled.
         */
        fun syncSchedule(context: Context) {
            val prefs = PushPreferences(context)
            val workManager = WorkManager.getInstance(context)

            if (!prefs.pushEnabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<NtfyPushWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            PushNotificationHelper(context).ensureChannels()
            AppLogger.d(TAG, "Scheduled ntfy push worker")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
