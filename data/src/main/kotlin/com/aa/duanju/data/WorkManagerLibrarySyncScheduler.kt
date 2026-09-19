package com.aa.duanju.data

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aa.duanju.core.database.AADramaDatabase
import com.aa.duanju.core.model.SyncStatus
import com.aa.duanju.data.work.GenerateCoversWorker
import com.aa.duanju.data.work.KEY_SOURCE_ID
import com.aa.duanju.data.work.ScanSourceWorker
import com.aa.duanju.data.work.TAG_LIBRARY_SYNC
import com.aa.duanju.domain.LibrarySyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerLibrarySyncScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val database: AADramaDatabase,
) : LibrarySyncScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val trackedWorkIds = MutableStateFlow<Set<java.util.UUID>>(emptySet())

    override fun enqueueSource(sourceId: Long) {
        val constraints = Constraints.Builder().setRequiresStorageNotLow(true).build()
        val scan = OneTimeWorkRequestBuilder<ScanSourceWorker>()
            .setInputData(androidx.work.workDataOf(KEY_SOURCE_ID to sourceId))
            .setConstraints(constraints)
            .addTag(TAG_LIBRARY_SYNC)
            .build()
        val covers = OneTimeWorkRequestBuilder<GenerateCoversWorker>()
            .setConstraints(constraints)
            .addTag(TAG_LIBRARY_SYNC)
            .build()
        trackedWorkIds.update { it + scan.id + covers.id }
        workManager.beginUniqueWork(
            "scan-source-$sourceId",
            ExistingWorkPolicy.KEEP,
            scan,
        ).then(covers).enqueue()
    }

    override fun enqueueAll() {
        scope.launch {
            database.sourceDao().getAll().forEach { enqueueSource(it.id) }
        }
    }

    override fun observeStatus(): Flow<SyncStatus> = combine(
        workManager.getWorkInfosByTagFlow(TAG_LIBRARY_SYNC),
        trackedWorkIds,
    ) { infos, trackedIds ->
                val active = infos.filterNot { it.state.isFinished }
                val tracked = infos.filter { it.id in trackedIds }
                val trackedFinished = trackedIds.isNotEmpty() && tracked.size == trackedIds.size &&
                    tracked.all { it.state.isFinished }
                ObservedSyncStatus(
                    status = SyncStatus(
                    isRunning = active.isNotEmpty(),
                    completed = tracked.count { it.state == WorkInfo.State.SUCCEEDED },
                    total = trackedIds.size,
                    message = if (trackedFinished && tracked.any { it.state == WorkInfo.State.FAILED }) {
                        "部分目录扫描失败，请检查目录授权"
                    } else null,
                    ),
                    trackedFinished = trackedFinished,
                )
            }
            .onEach { observed ->
                if (observed.trackedFinished) {
                    trackedWorkIds.value = emptySet()
                }
            }
            .map { it.status }
            .distinctUntilChanged()

    private data class ObservedSyncStatus(
        val status: SyncStatus,
        val trackedFinished: Boolean,
    )
}
