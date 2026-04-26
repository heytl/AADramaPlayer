package com.aa.duanju.db.dao

import androidx.room.*
import com.aa.duanju.db.entity.DramaEntity
import com.aa.duanju.db.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

data class DramaWithProgress(
    @Embedded val drama: DramaEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dramaId"
    )
    val episodes: List<EpisodeEntity>
) {
    // 逻辑属性：获取最后观看的那一集
    val lastWatchedEpisode: EpisodeEntity?
        get() = episodes.filter { it.lastWatchedTime > 0 }
            .maxByOrNull { it.lastWatchedTime }
}

@Dao
interface DramaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrama(drama: DramaEntity): Long

    @Update
    suspend fun updateDrama(drama: DramaEntity)

    @Transaction
    @Query("SELECT * FROM dramas ORDER BY lastWatchedTime DESC")
    fun getAllDramasWithProgressFlow(): Flow<List<DramaWithProgress>>

    @Query("SELECT * FROM dramas ORDER BY lastWatchedTime DESC")
    suspend fun getAllDramas(): List<DramaEntity>

    @Query("SELECT * FROM dramas WHERE id = :dramaId")
    suspend fun getDramaById(dramaId: Long): DramaEntity?

    @Query("SELECT * FROM dramas WHERE folderPath = :path LIMIT 1")
    suspend fun getDramaByPath(path: String): DramaEntity?
}

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)

    @Query("UPDATE episodes SET lastPlaybackPosition = :position, lastWatchedTime = :time WHERE id = :episodeId")
    suspend fun updatePlaybackPosition(episodeId: Long, position: Long, time: Long)

    @Query("SELECT * FROM episodes WHERE dramaId = :dramaId ORDER BY episodeNumber ASC")
    fun getEpisodesByDramaIdFlow(dramaId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE dramaId = :dramaId ORDER BY episodeNumber ASC")
    suspend fun getEpisodesByDramaId(dramaId: Long): List<EpisodeEntity>

    @Query("SELECT * FROM episodes ORDER BY lastWatchedTime DESC LIMIT 10")
    fun getRecentEpisodesFlow(): Flow<List<EpisodeEntity>>
}
