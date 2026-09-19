package com.aa.duanju.core.media

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class SafMediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeParser: EpisodeParser,
) {
    suspend fun scan(treeUriValue: String): SourceSnapshot = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriValue)
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val pending = ArrayDeque<Pair<String, String>>()
        pending.add(rootId to queryName(treeUri, rootId))
        val dramas = mutableListOf<ScannedDrama>()

        while (pending.isNotEmpty()) {
            coroutineContext.ensureActive()
            val (parentId, parentName) = pending.removeFirst()
            val videos = mutableListOf<ScannedVideo>()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex) ?: continue
                    val mime = cursor.getString(mimeIndex)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.add(id to name)
                    } else if (episodeParser.isVideo(name)) {
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        videos += ScannedVideo(
                            uri = documentUri.toString(),
                            displayName = name,
                            episodeNumber = episodeParser.parse(name),
                            size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0,
                            modifiedAt = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else 0,
                        )
                    }
                }
            }

            if (videos.isNotEmpty()) {
                dramas += ScannedDrama(
                    documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentId).toString(),
                    title = parentName,
                    videos = videos.sortedWith(compareBy(ScannedVideo::episodeNumber, ScannedVideo::displayName)),
                )
            }
        }

        SourceSnapshot(dramas.sortedBy { it.title.lowercase() })
    }

    private fun queryName(treeUri: Uri, documentId: String): String {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        return context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "未命名目录"
    }
}
