package com.aa.duanju.data

import androidx.room.withTransaction
import com.aa.duanju.core.database.AADramaDatabase
import com.aa.duanju.core.database.SourceEntity
import com.aa.duanju.core.media.VideoCoverGenerator
import com.aa.duanju.core.model.DramaSummary
import com.aa.duanju.core.model.Episode
import com.aa.duanju.core.model.PlaybackQueue
import com.aa.duanju.core.model.SourceDirectory
import com.aa.duanju.domain.LibraryRepository
import com.aa.duanju.domain.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineLibraryRepository @Inject constructor(
    private val database: AADramaDatabase,
    private val coverGenerator: VideoCoverGenerator,
) : LibraryRepository {
    override fun observeLibrary(): Flow<List<DramaSummary>> =
        database.dramaDao().observeSummaries().map { rows -> rows.map { it.asModel() } }

    override fun observeSources(): Flow<List<SourceDirectory>> =
        database.sourceDao().observeAll().map { rows -> rows.map { it.asModel() } }

    override suspend fun getEpisodes(dramaId: Long): List<Episode> =
        database.episodeDao().getByDrama(dramaId).map { it.asModel() }

    override suspend fun addSource(treeUri: String, displayName: String): Long {
        database.sourceDao().getByTreeUri(treeUri)?.let { return it.id }
        return database.sourceDao().insert(SourceEntity(treeUri = treeUri, displayName = displayName))
    }

    override suspend fun removeSource(sourceId: Long) {
        val covers = database.dramaDao().getBySource(sourceId).mapNotNull { it.coverPath }
        database.sourceDao().delete(sourceId)
        covers.forEach(coverGenerator::delete)
    }
}

@Singleton
class OfflinePlaybackRepository @Inject constructor(
    private val database: AADramaDatabase,
) : PlaybackRepository {
    override suspend fun getPlaybackQueue(dramaId: Long): PlaybackQueue {
        val drama = requireNotNull(database.dramaDao().getById(dramaId)) { "短剧不存在" }
        val ordered = database.dramaDao().getAllOrdered()
        val currentIndex = ordered.indexOfFirst { it.id == dramaId }
        val nextId = when {
            ordered.size <= 1 -> null
            currentIndex in 0 until ordered.lastIndex -> ordered[currentIndex + 1].id
            currentIndex == ordered.lastIndex -> ordered.first().id
            else -> null
        }
        return PlaybackQueue(
            dramaId = drama.id,
            dramaTitle = drama.title,
            episodes = database.episodeDao().getByDrama(drama.id).map { it.asModel() },
            nextDramaId = nextId,
        )
    }

    override suspend fun saveProgress(episodeId: Long, positionMs: Long, playedAt: Long) {
        database.withTransaction {
            val episode = database.episodeDao().getById(episodeId) ?: return@withTransaction
            database.episodeDao().updateProgress(episodeId, positionMs, playedAt)
            database.dramaDao().updateLastPlayed(episode.dramaId, episodeId, playedAt)
        }
    }

    override suspend fun clearAllProgress() {
        database.withTransaction {
            database.episodeDao().clearAllProgress()
            database.dramaDao().clearLastPlayed()
        }
    }
}
