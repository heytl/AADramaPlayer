package com.aa.duanju.domain

import com.aa.duanju.core.model.DramaSummary
import com.aa.duanju.core.model.Episode
import com.aa.duanju.core.model.PlaybackQueue
import com.aa.duanju.core.model.SourceDirectory
import com.aa.duanju.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibrary(): Flow<List<DramaSummary>>
    fun observeSources(): Flow<List<SourceDirectory>>
    suspend fun getEpisodes(dramaId: Long): List<Episode>
    suspend fun addSource(treeUri: String, displayName: String): Long
    suspend fun removeSource(sourceId: Long)
}

interface PlaybackRepository {
    suspend fun getPlaybackQueue(dramaId: Long): PlaybackQueue
    suspend fun saveProgress(episodeId: Long, positionMs: Long, playedAt: Long)
    suspend fun clearAllProgress()
}

interface LibrarySyncScheduler {
    fun enqueueSource(sourceId: Long)
    fun enqueueAll()
    fun observeStatus(): Flow<SyncStatus>
}

interface SettingsRepository {
    fun observeAutoPlay(): Flow<Boolean>
    suspend fun setAutoPlay(enabled: Boolean)
    fun observePlayOrder(): Flow<PlayOrder>
    suspend fun setPlayOrder(order: PlayOrder)
    fun observeKeepScreenOn(): Flow<Boolean>
    suspend fun setKeepScreenOn(enabled: Boolean)
    fun observeAfterDrama(): Flow<AfterDrama>
    suspend fun setAfterDrama(after: AfterDrama)
}

enum class PlayOrder {
    SEQUENTIAL,
    SHUFFLE,
}

enum class AfterDrama {
    AUTO_NEXT,
    STOP,
}
