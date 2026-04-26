package com.aa.duanju.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aa.duanju.db.dao.DramaDao
import com.aa.duanju.db.dao.EpisodeDao
import com.aa.duanju.db.dao.SourceDirectoryDao
import com.aa.duanju.db.entity.DramaEntity
import com.aa.duanju.db.entity.EpisodeEntity
import com.aa.duanju.db.entity.SourceDirectoryEntity

@Database(
    entities = [DramaEntity::class, EpisodeEntity::class, SourceDirectoryEntity::class],
    version = 3, // 升级到版本 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dramaDao(): DramaDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun sourceDirectoryDao(): SourceDirectoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drama_player_database"
                )
                .fallbackToDestructiveMigration() // 重新启用，解决迁移报错问题
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
