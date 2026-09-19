package com.aa.duanju.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SourceEntity::class, DramaEntity::class, EpisodeEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AADramaDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun dramaDao(): DramaDao
    abstract fun episodeDao(): EpisodeDao
}
