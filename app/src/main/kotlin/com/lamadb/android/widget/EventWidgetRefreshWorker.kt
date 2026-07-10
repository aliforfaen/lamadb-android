package com.lamadb.android.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Periodically tells the AppWidgetManager to refresh the event ticker data.
 *
 * The actual data fetch happens on-demand inside [EventWidgetRemoteViewsFactory]
 * when the widget list is redrawn.
 */
class EventWidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        EventWidgetProvider.requestUpdate(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "lamadb_widget_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EventWidgetRefreshWorker>(
                30,
                TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
