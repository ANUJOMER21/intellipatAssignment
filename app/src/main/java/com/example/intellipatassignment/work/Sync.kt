package com.example.intellipatassignment.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.intellipatassignment.core.toErrorKind
import com.example.intellipatassignment.domain.repository.CourseRepository
import com.example.intellipatassignment.domain.repository.SyncScheduler
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: CourseRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d(TAG, "Sync started (attempt ${runAttemptCount + 1}/$MAX_ATTEMPTS)")
        return repository.sync().fold(
            onSuccess = {
                Log.d(TAG, "Sync finished successfully")
                Result.success()
            },
            onFailure = { cause ->
                Log.w(TAG, "Sync failed (attempt ${runAttemptCount + 1}/$MAX_ATTEMPTS)", cause)
                when {
                    runAttemptCount + 1 >= MAX_ATTEMPTS -> Result.failure(failureData(cause))
                    else -> Result.retry()
                }
            },
        )
    }

    private fun failureData(cause: Throwable) = workDataOf(
        KEY_ERROR_TYPE to cause::class.java.simpleName,
        KEY_ERROR_KIND to cause.toErrorKind().name,
    )

    companion object {
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_ERROR_KIND = "error_kind"
        private const val TAG = "SyncWorker"
        private const val MAX_ATTEMPTS = 5
    }
}

class WorkManagerSyncScheduler(context: Context) : SyncScheduler {
    private val workManager = WorkManager.getInstance(context)

    private val onlineOnly = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun syncWhenOnline() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(onlineOnly)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(ONE_TIME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        Log.d(TAG, "Queued one-time sync (runs when a connection is available)")
    }

    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_MINUTES, TimeUnit.MINUTES)
            .setInitialDelay(PERIODIC_MINUTES, TimeUnit.MINUTES)
            .setConstraints(onlineOnly)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        Log.d(TAG, "Periodic sync ensured (every $PERIODIC_MINUTES min)")
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(ONE_TIME)
        workManager.cancelUniqueWork(PERIODIC)
        Log.d(TAG, "Cancelled scheduled sync work")
    }

    private companion object {
        const val TAG = "SyncScheduler"
        const val ONE_TIME = "course_sync_once"
        const val PERIODIC = "course_sync_periodic"
        const val PERIODIC_MINUTES = 15L
        const val BACKOFF_SECONDS = 30L
    }
}
