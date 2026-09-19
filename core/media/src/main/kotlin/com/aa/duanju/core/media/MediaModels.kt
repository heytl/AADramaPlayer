package com.aa.duanju.core.media

data class ScannedVideo(
    val uri: String,
    val displayName: String,
    val episodeNumber: Int,
    val size: Long,
    val modifiedAt: Long,
) {
    val fingerprint: String = "$uri:$size:$modifiedAt"
}

data class ScannedDrama(
    val documentUri: String,
    val title: String,
    val videos: List<ScannedVideo>,
)

data class SourceSnapshot(val dramas: List<ScannedDrama>)

data class GeneratedCover(
    val path: String,
    val fingerprint: String,
)
