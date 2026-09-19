package com.aa.duanju.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibrarySummaryDaoTest {
    private lateinit var database: AADramaDatabase

    @Before fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AADramaDatabase::class.java).build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun summaryQueryReturnsOneRowPerDramaForTenThousandEpisodes() = runBlocking {
        val sourceId = database.sourceDao().insert(SourceEntity(treeUri = "content://test", displayName = "test"))
        repeat(200) { dramaIndex ->
            val dramaId = database.dramaDao().upsert(
                DramaEntity(
                    sourceId = sourceId,
                    title = "Drama $dramaIndex",
                    documentUri = "content://test/drama/$dramaIndex",
                    episodeCount = 50,
                ),
            )
            database.episodeDao().upsertAll(
                List(50) { episodeIndex ->
                    EpisodeEntity(
                        dramaId = dramaId,
                        episodeNumber = episodeIndex + 1,
                        displayName = "${episodeIndex + 1}.mp4",
                        videoUri = "content://test/drama/$dramaIndex/$episodeIndex",
                        contentFingerprint = "$dramaIndex:$episodeIndex",
                    )
                },
            )
        }

        val summaries = database.dramaDao().observeSummaries().first()
        assertEquals(200, summaries.size)
        assertEquals(50, summaries.first().episodeCount)
    }
}
