package com.aa.duanju.data

import com.aa.duanju.core.database.DramaSummaryRow
import com.aa.duanju.core.database.EpisodeEntity
import com.aa.duanju.core.database.SourceEntity
import com.aa.duanju.core.model.DramaSummary
import com.aa.duanju.core.model.Episode
import com.aa.duanju.core.model.EpisodeResume
import com.aa.duanju.core.model.SourceDirectory

internal fun DramaSummaryRow.asModel() = DramaSummary(
    id = id,
    title = title,
    episodeCount = episodeCount,
    coverPath = coverPath,
    firstEpisodeId = firstEpisodeId,
    lastWatchedAt = lastWatchedAt,
    lastEpisode = lastEpisodeId?.let { episodeId ->
        EpisodeResume(
            episodeId = episodeId,
            episodeNumber = lastEpisodeNumber ?: 1,
            positionMs = lastPositionMs ?: 0,
            watchedAt = lastEpisodeWatchedAt ?: 0,
        )
    },
)

internal fun EpisodeEntity.asModel() = Episode(
    id = id,
    dramaId = dramaId,
    episodeNumber = episodeNumber,
    videoUri = videoUri,
    durationMs = durationMs,
    resumePositionMs = resumePositionMs,
    lastWatchedAt = lastWatchedAt,
)

internal fun SourceEntity.asModel() = SourceDirectory(id, treeUri, displayName)
