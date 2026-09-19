package com.aa.duanju.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aa.duanju.core.model.DramaSummary
import com.aa.duanju.domain.LibraryRepository
import com.aa.duanju.domain.LibrarySyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val syncScheduler: LibrarySyncScheduler,
) : ViewModel() {
    private val showSourceDialog = MutableStateFlow(false)
    private val episodeSelection = MutableStateFlow<EpisodeSelection?>(null)
    private val effectChannel = Channel<LibraryUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val uiState = combine(
        repository.observeLibrary(),
        repository.observeSources(),
        syncScheduler.observeStatus(),
        showSourceDialog,
        episodeSelection,
    ) { dramas, sources, sync, showSources, selection ->
        LibraryUiState(
            dramas = dramas,
            sources = sources,
            isScanning = sync.isRunning,
            showSourceDialog = showSources,
            episodeSelection = selection,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch {
            var wasRunning = false
            syncScheduler.observeStatus().collect { status ->
                status.message?.let { effectChannel.send(LibraryUiEffect.Message(it)) }
                if (wasRunning && !status.isRunning && status.message == null) {
                    effectChannel.send(LibraryUiEffect.Message("更新完成"))
                }
                wasRunning = status.isRunning
            }
        }
    }

    fun setSourceDialogVisible(visible: Boolean) {
        showSourceDialog.value = visible
    }

    fun addSource(treeUri: String, displayName: String) {
        viewModelScope.launch {
            if (uiState.value.sources.any { it.treeUri == treeUri }) {
                effectChannel.send(LibraryUiEffect.Message("目录已存在"))
                return@launch
            }
            runCatching { repository.addSource(treeUri, displayName) }
                .onSuccess(syncScheduler::enqueueSource)
                .onFailure { effectChannel.send(LibraryUiEffect.Message("添加目录失败：${it.message.orEmpty()}")) }
        }
    }

    fun removeSource(sourceId: Long) {
        viewModelScope.launch {
            runCatching { repository.removeSource(sourceId) }
                .onFailure { effectChannel.send(LibraryUiEffect.Message("移除失败：${it.message.orEmpty()}")) }
        }
    }

    fun rescanAll() = syncScheduler.enqueueAll()

    fun openEpisodes(drama: DramaSummary) {
        episodeSelection.value = EpisodeSelection(drama.id, drama.title)
        viewModelScope.launch {
            runCatching { repository.getEpisodes(drama.id) }
                .onSuccess { episodes ->
                    if (episodeSelection.value?.dramaId == drama.id) {
                        episodeSelection.value = EpisodeSelection(drama.id, drama.title, false, episodes)
                    }
                }
                .onFailure {
                    episodeSelection.value = null
                    effectChannel.send(LibraryUiEffect.Message("选集加载失败"))
                }
        }
    }

    fun closeEpisodes() {
        episodeSelection.value = null
    }
}
