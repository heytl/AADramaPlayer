package com.aa.duanju

import androidx.room.withTransaction
import com.aa.duanju.core.database.AADramaDatabase
import com.aa.duanju.core.database.DramaEntity
import com.aa.duanju.core.database.EpisodeEntity
import com.aa.duanju.core.database.SourceEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BenchmarkSeeder @Inject constructor(
    private val database: AADramaDatabase,
) {
    suspend fun seed() {
        database.withTransaction {
            database.sourceDao().getAll().forEach { database.sourceDao().delete(it.id) }
            val sourceId = database.sourceDao().insert(
                SourceEntity(treeUri = "content://benchmark", displayName = "性能测试资源"),
            )
            repeat(200) { dramaIndex ->
                val dramaId = database.dramaDao().upsert(
                    DramaEntity(
                        sourceId = sourceId,
                        title = "性能测试短剧 ${dramaIndex + 1}",
                        documentUri = "content://benchmark/drama/$dramaIndex",
                        episodeCount = 50,
                        lastWatchedAt = 200L - dramaIndex,
                    ),
                )
                database.episodeDao().upsertAll(
                    List(50) { episodeIndex ->
                        EpisodeEntity(
                            dramaId = dramaId,
                            episodeNumber = episodeIndex + 1,
                            displayName = "第${episodeIndex + 1}集.mp4",
                            videoUri = "content://benchmark/drama/$dramaIndex/$episodeIndex",
                            contentFingerprint = "$dramaIndex:$episodeIndex",
                        )
                    },
                )
            }
        }
    }
}
