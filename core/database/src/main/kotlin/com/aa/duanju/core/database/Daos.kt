package com.aa.duanju.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM source_directories ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM source_directories ORDER BY id")
    suspend fun getAll(): List<SourceEntity>

    @Query("SELECT * FROM source_directories WHERE id = :id")
    suspend fun getById(id: Long): SourceEntity?

    @Query("SELECT * FROM source_directories WHERE treeUri = :treeUri LIMIT 1")
    suspend fun getByTreeUri(treeUri: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: SourceEntity): Long

    @Query("DELETE FROM source_directories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DramaDao {
    @Query(
        """
        SELECT d.id, d.title, d.episodeCount, d.coverPath,
               (SELECT firstEpisode.id FROM episodes firstEpisode
                WHERE firstEpisode.dramaId = d.id
                ORDER BY firstEpisode.episodeNumber, firstEpisode.displayName COLLATE NOCASE
                LIMIT 1) AS firstEpisodeId,
               d.lastWatchedAt,
               e.id AS lastEpisodeId,
               e.episodeNumber AS lastEpisodeNumber,
               e.resumePositionMs AS lastPositionMs,
               e.lastWatchedAt AS lastEpisodeWatchedAt
        FROM dramas d
        LEFT JOIN episodes e ON e.id = d.lastEpisodeId
        ORDER BY d.lastWatchedAt DESC, d.title COLLATE NOCASE
        """,
    )
    fun observeSummaries(): Flow<List<DramaSummaryRow>>

    @Query("SELECT * FROM dramas WHERE id = :id")
    suspend fun getById(id: Long): DramaEntity?

    @Query("SELECT * FROM dramas WHERE documentUri = :documentUri LIMIT 1")
    suspend fun getByDocumentUri(documentUri: String): DramaEntity?

    @Query("SELECT * FROM dramas WHERE sourceId = :sourceId")
    suspend fun getBySource(sourceId: Long): List<DramaEntity>

    @Query("SELECT * FROM dramas ORDER BY lastWatchedAt DESC, title COLLATE NOCASE")
    suspend fun getAllOrdered(): List<DramaEntity>

    @Upsert
    suspend fun upsert(drama: DramaEntity): Long

    @Query("DELETE FROM dramas WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE dramas SET lastEpisodeId = :episodeId, lastWatchedAt = :playedAt WHERE id = :dramaId")
    suspend fun updateLastPlayed(dramaId: Long, episodeId: Long, playedAt: Long)

    @Query("UPDATE dramas SET lastEpisodeId = NULL, lastWatchedAt = 0")
    suspend fun clearLastPlayed()

    @Query("UPDATE dramas SET coverPath = :coverPath, coverFingerprint = :fingerprint WHERE id = :dramaId")
    suspend fun updateCover(dramaId: Long, coverPath: String, fingerprint: String)

    @Query(
        """
        SELECT d.id AS dramaId,
               e.videoUri AS videoUri,
               e.contentFingerprint AS fingerprint,
               d.coverPath AS oldCoverPath
        FROM dramas d
        JOIN episodes e ON e.id = (
            SELECT firstEpisode.id FROM episodes firstEpisode
            WHERE firstEpisode.dramaId = d.id
            ORDER BY firstEpisode.episodeNumber ASC, firstEpisode.displayName COLLATE NOCASE ASC
            LIMIT 1
        )
        WHERE d.coverPath IS NULL OR d.coverFingerprint IS NULL OR d.coverFingerprint != e.contentFingerprint
        """,
    )
    suspend fun getCoverCandidates(): List<CoverCandidateRow>
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE dramaId = :dramaId ORDER BY episodeNumber, displayName COLLATE NOCASE")
    suspend fun getByDrama(dramaId: Long): List<EpisodeEntity>

    @Upsert
    suspend fun upsertAll(episodes: List<EpisodeEntity>): List<Long>

    @Query("DELETE FROM episodes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE episodes SET resumePositionMs = :positionMs, lastWatchedAt = :playedAt WHERE id = :episodeId")
    suspend fun updateProgress(episodeId: Long, positionMs: Long, playedAt: Long)

    @Query("UPDATE episodes SET resumePositionMs = 0, lastWatchedAt = 0")
    suspend fun clearAllProgress()
}
