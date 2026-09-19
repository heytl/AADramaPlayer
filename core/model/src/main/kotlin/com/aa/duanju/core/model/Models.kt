package com.aa.duanju.core.model

data class EpisodeResume(
    val episodeId: Long,
    val episodeNumber: Int,
    val positionMs: Long,
    val watchedAt: Long,
)

data class DramaSummary(
    val id: Long,
    val title: String,
    val episodeCount: Int,
    val coverPath: String?,
    val firstEpisodeId: Long?,
    val lastWatchedAt: Long,
    val lastEpisode: EpisodeResume?,
)

data class Episode(
    val id: Long,
    val dramaId: Long,
    val episodeNumber: Int,
    val videoUri: String,
    val durationMs: Long,
    val resumePositionMs: Long,
    val lastWatchedAt: Long,
)

data class SourceDirectory(
    val id: Long,
    val treeUri: String,
    val displayName: String,
)

data class PlaybackQueue(
    val dramaId: Long,
    val dramaTitle: String,
    val episodes: List<Episode>,
    val nextDramaId: Long?,
)

data class SyncStatus(
    val isRunning: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val message: String? = null,
)
