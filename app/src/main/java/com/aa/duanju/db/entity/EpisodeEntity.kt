package com.aa.duanju.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = DramaEntity::class,
            parentColumns = ["id"],
            childColumns = ["dramaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dramaId", "episodeNumber"], unique = true)]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dramaId: Long,             // 所属短剧ID
    val episodeNumber: Int,        // 集数
    val videoPath: String,         // 视频物理路径
    val duration: Long = 0,        
    val lastPlaybackPosition: Long = 0,
    val lastWatchedTime: Long = 0  // 新增：上次观看的时间戳
)
