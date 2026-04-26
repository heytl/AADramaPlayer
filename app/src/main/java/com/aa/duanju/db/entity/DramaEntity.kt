package com.aa.duanju.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "dramas",
    indices = [Index(value = ["folderPath"], unique = true)]
)
data class DramaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,          // 短剧名称（如文件夹名称）
    val folderPath: String,     // 该短剧所在的根目录物理路径或 Uri
    val totalEpisodes: Int = 0, // 总集数
    val coverImagePath: String = "", // 封面图路径（可选，通常取第一集的第一帧）
    val lastWatchedTime: Long = 0 // 上次观看的时间戳，用于在首页排序
)
