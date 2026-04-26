package com.aa.duanju.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aa.duanju.db.AppDatabase
import com.aa.duanju.db.dao.DramaWithProgress
import com.aa.duanju.db.entity.DramaEntity
import com.aa.duanju.db.entity.EpisodeEntity
import com.aa.duanju.db.entity.SourceDirectoryEntity
import com.aa.duanju.utils.LocalDramaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val scanner = LocalDramaScanner(application)

    private val _dramas = MutableStateFlow<List<DramaWithProgress>>(emptyList())
    val dramas: StateFlow<List<DramaWithProgress>> = _dramas.asStateFlow()

    private val _sourceDirectories = MutableStateFlow<List<SourceDirectoryEntity>>(emptyList())
    val sourceDirectories: StateFlow<List<SourceDirectoryEntity>> = _sourceDirectories.asStateFlow()

    private val _recentEpisode = MutableStateFlow<EpisodeEntity?>(null)
    val recentEpisode: StateFlow<EpisodeEntity?> = _recentEpisode.asStateFlow()
    
    private val _recentDrama = MutableStateFlow<DramaEntity?>(null)
    val recentDrama: StateFlow<DramaEntity?> = _recentDrama.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _scanResultMsg = MutableStateFlow<String?>(null)
    val scanResultMsg: StateFlow<String?> = _scanResultMsg.asStateFlow()

    init {
        // 观察剧集列表变化（带进度）
        viewModelScope.launch {
            db.dramaDao().getAllDramasWithProgressFlow().collectLatest { list ->
                _dramas.value = list
            }
        }
        
        // 观察源目录变化
        viewModelScope.launch {
            db.sourceDirectoryDao().getAllFlow().collectLatest { list ->
                _sourceDirectories.value = list
            }
        }

        // 观察最近播放变化 (响应式刷新顶部卡片)
        viewModelScope.launch {
            db.episodeDao().getRecentEpisodesFlow().collectLatest { recents ->
                val lastPlayed = recents.firstOrNull { it.lastPlaybackPosition > 0 } ?: recents.firstOrNull()
                _recentEpisode.value = lastPlayed
                if (lastPlayed != null) {
                    _recentDrama.value = db.dramaDao().getDramaById(lastPlayed.dramaId)
                } else {
                    _recentDrama.value = null
                }
            }
        }
    }

    fun addSourceDirectory(uri: Uri, name: String) {
        viewModelScope.launch {
            val path = uri.toString()
            if (db.sourceDirectoryDao().getByPath(path) == null) {
                db.sourceDirectoryDao().insert(SourceDirectoryEntity(path = path, name = name))
                scanFolder(uri)
            } else {
                _scanResultMsg.value = "目录已存在"
            }
        }
    }

    fun removeSourceDirectory(dir: SourceDirectoryEntity) {
        viewModelScope.launch {
            db.sourceDirectoryDao().deleteById(dir.id)
        }
    }

    fun scanFolder(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResultMsg.value = null
            try {
                val added = scanner.scanAndSave(uri)
                _scanResultMsg.value = "扫描完成！更新了 $added 部短剧"
            } catch (e: Exception) {
                e.printStackTrace()
                _scanResultMsg.value = "扫描失败: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun rescanAll() {
        viewModelScope.launch {
            _isScanning.value = true
            var totalUpdated = 0
            try {
                _sourceDirectories.value.forEach { dir ->
                    totalUpdated += scanner.scanAndSave(Uri.parse(dir.path))
                }
                _scanResultMsg.value = "全量更新完成"
            } catch (e: Exception) {
                _scanResultMsg.value = "更新出错: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    fun clearScanMsg() {
        _scanResultMsg.value = null
    }
}
