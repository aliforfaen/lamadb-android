package com.lamadb.android.data.events

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class EventDrainWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = EventRepository.createDefault(context)

    override suspend fun doWork(): Result {
        repository.prune()
        repository.drain()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "lamadb_event_drain"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EventDrainWorker>(
                15,
                TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
