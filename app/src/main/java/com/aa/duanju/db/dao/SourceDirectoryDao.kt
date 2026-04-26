package com.aa.duanju.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aa.duanju.db.entity.SourceDirectoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDirectoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(directory: SourceDirectoryEntity): Long

    @Query("SELECT * FROM source_directories")
    fun getAllFlow(): Flow<List<SourceDirectoryEntity>>

    @Query("DELETE FROM source_directories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM source_directories WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): SourceDirectoryEntity?
}
