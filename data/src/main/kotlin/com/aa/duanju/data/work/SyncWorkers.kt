package com.aa.duanju.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.aa.duanju.data.LibrarySyncEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal const val KEY_SOURCE_ID = "source_id"
internal const val TAG_LIBRARY_SYNC = "library_sync"

@HiltWorker
class ScanSourceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncEngine: LibrarySyncEngine,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val sourceId = inputData.getLong(KEY_SOURCE_ID, -1L)
        if (sourceId <= 0) return Result.failure()
        return runCatching {
            val count = syncEngine.syncSource(sourceId)
            Result.success(Data.Builder().putInt("drama_count", count).build())
        }.getOrElse { error ->
            if (runAttemptCount < 2) Result.retry()
            else Result.failure(Data.Builder().putString("error", error.message).build())
        }
    }
}

@HiltWorker
class GenerateCoversWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncEngine: LibrarySyncEngine,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val count = syncEngine.generatePendingCovers()
        Result.success(Data.Builder().putInt("cover_count", count).build())
    }.getOrElse { error ->
        if (runAttemptCount < 2) Result.retry()
        else Result.failure(Data.Builder().putString("error", error.message).build())
    }
}
