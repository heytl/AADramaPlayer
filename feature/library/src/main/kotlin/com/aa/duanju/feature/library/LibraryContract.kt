package com.aa.duanju.feature.library

import androidx.compose.runtime.Immutable
import com.aa.duanju.core.model.DramaSummary
import com.aa.duanju.core.model.Episode
import com.aa.duanju.core.model.SourceDirectory

@Immutable
data class EpisodeSelection(
    val dramaId: Long,
    val dramaTitle: String,
    val isLoading: Boolean = true,
    val episodes: List<Episode> = emptyList(),
)

@Immutable
data class LibraryUiState(
    val dramas: List<DramaSummary> = emptyList(),
    val sources: List<SourceDirectory> = emptyList(),
    val isScanning: Boolean = false,
    val episodeSelection: EpisodeSelection? = null,
) {
    val recent: DramaSummary?
        get() = dramas.firstOrNull { it.lastEpisode != null }
}

sealed interface LibraryUiEffect {
    data class Message(val text: String) : LibraryUiEffect
}
