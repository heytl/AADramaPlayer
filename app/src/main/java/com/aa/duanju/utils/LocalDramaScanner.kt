package com.aa.duanju.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.aa.duanju.db.AppDatabase
import com.aa.duanju.db.entity.DramaEntity
import com.aa.duanju.db.entity.EpisodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDramaScanner(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    suspend fun scanAndSave(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val dramaFoldersFound = mutableListOf<DramaFolder>()
        
        fastRecursiveSearch(treeUri, rootId, dramaFoldersFound)

        var dramasAddedOrUpdated = 0
        for (folder in dramaFoldersFound) {
            if (processDramaFolder(treeUri, folder)) {
                dramasAddedOrUpdated++
            }
        }
        return@withContext dramasAddedOrUpdated
    }

    private fun fastRecursiveSearch(treeUri: Uri, parentId: String, result: MutableList<DramaFolder>) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        val videoFiles = mutableListOf<VideoFile>()
        val subFolderIds = mutableListOf<Pair<String, String>>()

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex)
                val mime = cursor.getString(mimeIndex)

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    subFolderIds.add(id to name)
                } else if (EpisodeParser.isVideoFile(name)) {
                    val videoUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    videoFiles.add(VideoFile(videoUri, name))
                }
            }
        }

        if (videoFiles.isNotEmpty()) {
            val folderName = getFolderNameById(treeUri, parentId)
            result.add(DramaFolder(parentId, folderName, videoFiles))
        }

        for (subFolder in subFolderIds) {
            fastRecursiveSearch(treeUri, subFolder.first, result)
        }
    }

    private fun getFolderNameById(treeUri: Uri, docId: String): String {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) return it.getString(0)
        }
        return "未知剧集"
    }

    private suspend fun processDramaFolder(treeUri: Uri, folder: DramaFolder): Boolean {
        try {
            // 使用完整的 URI 作为路径，确保与之前存储的路径一致，防止重复
            val folderUriString = DocumentsContract.buildDocumentUriUsingTree(treeUri, folder.id).toString()
            val sortedVideos = folder.videos.sortedBy { it.name }

            val existingDrama = db.dramaDao().getDramaByPath(folderUriString)
            val dramaToUse = if (existingDrama == null) {
                val entity = DramaEntity(
                    title = folder.name,
                    folderPath = folderUriString,
                    totalEpisodes = sortedVideos.size,
                    coverImagePath = sortedVideos.firstOrNull()?.uri?.toString() ?: ""
                )
                val newId = db.dramaDao().insertDrama(entity)
                entity.copy(id = newId)
            } else {
                val updated = existingDrama.copy(
                    title = folder.name,
                    totalEpisodes = sortedVideos.size,
                    coverImagePath = existingDrama.coverImagePath.ifEmpty { 
                        sortedVideos.firstOrNull()?.uri?.toString() ?: "" 
                    }
                )
                db.dramaDao().updateDrama(updated)
                updated
            }

            // 获取该剧集在库里的现有进度
            val existingEpisodes = db.episodeDao().getEpisodesByDramaId(dramaToUse.id).associateBy { it.videoPath }
            
            val parsedEpisodes = sortedVideos.map { video ->
                val videoPath = video.uri.toString()
                val oldEp = existingEpisodes[videoPath]
                
                EpisodeEntity(
                    id = oldEp?.id ?: 0L,
                    dramaId = dramaToUse.id,
                    episodeNumber = EpisodeParser.parseEpisodeNumber(video.name),
                    videoPath = videoPath,
                    duration = oldEp?.duration ?: 0L,
                    // 核心：保留播放进度和观看时间！
                    lastPlaybackPosition = oldEp?.lastPlaybackPosition ?: 0L,
                    lastWatchedTime = oldEp?.lastWatchedTime ?: 0L
                )
            }
            db.episodeDao().insertEpisodes(parsedEpisodes)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private data class DramaFolder(val id: String, val name: String, val videos: List<VideoFile>)
    private data class VideoFile(val uri: Uri, val name: String)
}
