package com.aa.duanju.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "source_directories")
data class SourceDirectoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,       // 根目录 URI 字符串
    val name: String        // 根目录显示名称
)
