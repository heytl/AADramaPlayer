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
}

interface LibrarySyncScheduler {
    fun enqueueSource(sourceId: Long)
    fun enqueueAll()
    fun observeStatus(): Flow<SyncStatus>
}
