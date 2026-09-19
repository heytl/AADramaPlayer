package com.aa.duanju.core.media

import javax.inject.Inject

class EpisodeParser @Inject constructor() {
    private val patterns = listOf(
        Regex("第\\s*(\\d+)\\s*(集|部分|话)"),
        Regex("(?i)ep\\s*(\\d+)"),
        Regex("(?i)episode\\s*(\\d+)"),
        Regex("[-_\\s](\\d+)(?=\\.[a-zA-Z0-9]+$)"),
        Regex("^(\\d+)(?=\\.[a-zA-Z0-9]+$)"),
    )

    fun parse(fileName: String): Int {
        patterns.forEach { pattern ->
            pattern.find(fileName)?.groups?.get(1)?.value?.toIntOrNull()?.let { return it }
        }
        val baseName = fileName.substringBeforeLast('.', fileName)
        return Regex("(\\d+)").findAll(baseName).lastOrNull()?.value?.toIntOrNull() ?: 1
    }

    fun isVideo(fileName: String): Boolean = fileName.substringAfterLast('.', "").lowercase() in
        setOf("mp4", "mkv", "avi", "flv", "mov", "webm", "m4v")
}
