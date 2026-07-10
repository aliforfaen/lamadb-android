package com.lamadb.android.data.wiki

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
 * Periodically syncs the LamaDB wiki page index to the on-device cache.
 */
class WikiSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = WikiRepository.createDefault(applicationContext)
        return try {
            repository.sync()
                .onSuccess { count ->
                    AppLogger.d(TAG, "Wiki sync complete: $count pages cached")
                }
                .onFailure { error ->
                    if (error is WikiRepository.NotAuthenticatedException) {
                        AppLogger.d(TAG, "Wiki sync skipped: not authenticated")
                    } else {
                        AppLogger.w(TAG, "Wiki sync failed: ${error.message}")
                    }
                }
            Result.success()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Wiki sync error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WikiSync"
        private const val WORK_NAME = "lamadb_wiki_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WikiSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            AppLogger.d(TAG, "Scheduled wiki sync worker")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
