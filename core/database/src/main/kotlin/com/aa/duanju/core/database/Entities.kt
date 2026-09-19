package com.aa.duanju.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "source_directories",
    indices = [Index(value = ["treeUri"], unique = true)],
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeUri: String,
    val displayName: String,
)

@Entity(
    tableName = "dramas",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("sourceId"),
        Index(value = ["documentUri"], unique = true),
        Index("lastWatchedAt"),
    ],
)
data class DramaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val title: String,
    val documentUri: String,
    val episodeCount: Int,
    val coverPath: String? = null,
    val coverFingerprint: String? = null,
    val lastEpisodeId: Long? = null,
    val lastWatchedAt: Long = 0,
)

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = DramaEntity::class,
            parentColumns = ["id"],
            childColumns = ["dramaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dramaId"),
        Index(value = ["videoUri"], unique = true),
        Index(value = ["dramaId", "episodeNumber"]),
        Index(value = ["dramaId", "lastWatchedAt"]),
    ],
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dramaId: Long,
    val episodeNumber: Int,
    val displayName: String,
    val videoUri: String,
    val contentFingerprint: String,
    val durationMs: Long = 0,
    val resumePositionMs: Long = 0,
    val lastWatchedAt: Long = 0,
)

data class DramaSummaryRow(
    val id: Long,
    val title: String,
    val episodeCount: Int,
    val coverPath: String?,
    val firstEpisodeId: Long?,
    val lastWatchedAt: Long,
    val lastEpisodeId: Long?,
    val lastEpisodeNumber: Int?,
    val lastPositionMs: Long?,
    val lastEpisodeWatchedAt: Long?,
)

data class CoverCandidateRow(
    val dramaId: Long,
    val videoUri: String,
    val fingerprint: String,
    val oldCoverPath: String?,
)
