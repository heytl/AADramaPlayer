package com.aa.duanju.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class VideoCoverGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun generate(videoUri: String, fingerprint: String): GeneratedCover? = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "covers").apply { mkdirs() }
        val name = fingerprint.sha256()
        val destination = File(directory, "$name.webp")
        if (destination.isFile && destination.length() > 0) {
            return@withContext GeneratedCover(destination.absolutePath, fingerprint)
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(videoUri))
            val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    1_000_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    480,
                    480,
                )
            } else {
                retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } ?: return@withContext null

            val square = frame.centerCrop(360)
            destination.outputStream().buffered().use { output ->
                @Suppress("DEPRECATION")
                square.compress(Bitmap.CompressFormat.WEBP, 80, output)
            }
            if (square !== frame) square.recycle()
            frame.recycle()
            GeneratedCover(destination.absolutePath, fingerprint)
        } catch (_: Exception) {
            destination.delete()
            null
        } finally {
            retriever.release()
        }
    }

    fun delete(path: String?) {
        path?.let(::File)?.takeIf { it.parentFile?.name == "covers" }?.delete()
    }

    private fun Bitmap.centerCrop(targetSize: Int): Bitmap {
        val side = minOf(width, height)
        val cropped = Bitmap.createBitmap(this, (width - side) / 2, (height - side) / 2, side, side)
        if (side == targetSize) return cropped
        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (scaled !== cropped && cropped !== this) cropped.recycle()
        return scaled
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
}
