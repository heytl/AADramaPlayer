package com.aa.duanju.data

import androidx.room.withTransaction
import com.aa.duanju.core.database.AADramaDatabase
import com.aa.duanju.core.database.DramaEntity
import com.aa.duanju.core.database.EpisodeEntity
import com.aa.duanju.core.media.SafMediaScanner
import com.aa.duanju.core.media.VideoCoverGenerator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibrarySyncEngine @Inject constructor(
    private val database: AADramaDatabase,
    private val scanner: SafMediaScanner,
    private val coverGenerator: VideoCoverGenerator,
) {
    suspend fun syncSource(sourceId: Long): Int {
        val source = database.sourceDao().getById(sourceId) ?: return 0
        val snapshot = scanner.scan(source.treeUri)
        val obsoleteCovers = mutableSetOf<String>()

        database.withTransaction {
            val existingDramas = database.dramaDao().getBySource(sourceId).associateBy { it.documentUri }
            val seenDramaIds = mutableSetOf<Long>()

            snapshot.dramas.forEach { scannedDrama ->
                val existing = existingDramas[scannedDrama.documentUri]
                val firstFingerprint = scannedDrama.videos.firstOrNull()?.fingerprint
                val coverIsCurrent = existing != null && existing.coverFingerprint == firstFingerprint &&
                    existing.coverPath?.let { java.io.File(it).isFile } == true
                if (!coverIsCurrent) existing?.coverPath?.let(obsoleteCovers::add)

                val existingEpisodes = existing?.let { database.episodeDao().getByDrama(it.id) }.orEmpty()
                val existingEpisodeByUri = existingEpisodes.associateBy { it.videoUri }
                val retainedEpisodeIds = scannedDrama.videos.mapNotNull { existingEpisodeByUri[it.uri]?.id }.toSet()
                val retainedLastEpisode = existing?.lastEpisodeId?.takeIf(retainedEpisodeIds::contains)

                val draft = DramaEntity(
                    id = existing?.id ?: 0,
                    sourceId = sourceId,
                    title = scannedDrama.title,
                    documentUri = scannedDrama.documentUri,
                    episodeCount = scannedDrama.videos.size,
                    coverPath = existing?.coverPath?.takeIf { coverIsCurrent },
                    coverFingerprint = existing?.coverFingerprint?.takeIf { coverIsCurrent },
                    lastEpisodeId = retainedLastEpisode,
                    lastWatchedAt = existing?.lastWatchedAt?.takeIf { retainedLastEpisode != null } ?: 0,
                )
                val upsertedId = database.dramaDao().upsert(draft)
                val dramaId = existing?.id ?: upsertedId
                seenDramaIds += dramaId

                val episodes = scannedDrama.videos.map { video ->
                    val old = existingEpisodeByUri[video.uri]
                    EpisodeEntity(
                        id = old?.id ?: 0,
                        dramaId = dramaId,
                        episodeNumber = video.episodeNumber,
                        displayName = video.displayName,
                        videoUri = video.uri,
                        contentFingerprint = video.fingerprint,
                        durationMs = old?.durationMs ?: 0,
                        resumePositionMs = old?.resumePositionMs ?: 0,
                        lastWatchedAt = old?.lastWatchedAt ?: 0,
                    )
                }
                database.episodeDao().upsertAll(episodes)

                val staleEpisodeIds = existingEpisodes.map(EpisodeEntity::id).filterNot(retainedEpisodeIds::contains)
                if (staleEpisodeIds.isNotEmpty()) database.episodeDao().deleteByIds(staleEpisodeIds)
            }

            val staleDramas = existingDramas.values.filterNot { it.id in seenDramaIds }
            staleDramas.mapNotNullTo(obsoleteCovers) { it.coverPath }
            if (staleDramas.isNotEmpty()) database.dramaDao().deleteByIds(staleDramas.map { it.id })
        }

        obsoleteCovers.forEach(coverGenerator::delete)
        return snapshot.dramas.size
    }

    suspend fun generatePendingCovers(): Int {
        val candidates = database.dramaDao().getCoverCandidates()
        val generated = buildList {
            candidates.chunked(2).forEach { pair ->
                addAll(coroutineScope {
                    pair.map { candidate ->
                        async {
                            val cover = coverGenerator.generate(candidate.videoUri, candidate.fingerprint)
                            Triple(candidate, cover, candidate.oldCoverPath)
                        }
                    }.map { it.await() }
                })
            }
        }

        database.withTransaction {
            generated.forEach { (candidate, cover, _) ->
                if (cover != null) {
                    database.dramaDao().updateCover(candidate.dramaId, cover.path, cover.fingerprint)
                }
            }
        }
        generated.forEach { (_, cover, oldPath) ->
            if (cover != null && oldPath != cover.path) coverGenerator.delete(oldPath)
        }
        return generated.count { it.second != null }
    }
}
