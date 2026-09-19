package com.aa.duanju.feature.library

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aa.duanju.core.model.DramaSummary
import com.aa.duanju.core.model.Episode
import com.aa.duanju.core.model.SourceDirectory
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlayer: (dramaId: Long, episodeId: Long) -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val treeId = DocumentsContract.getTreeDocumentId(uri)
            val name = treeId.substringAfterLast('/').ifBlank { "新目录" }
            viewModel.addSource(uri.toString(), name)
        }.onFailure { Toast.makeText(context, "无法读取目录：${it.message.orEmpty()}", Toast.LENGTH_LONG).show() }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LibraryUiEffect.Message -> Toast.makeText(context, effect.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阿阿短剧", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) },
                actions = {
                    TextButton(onClick = { viewModel.setSourceDialogVisible(true) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("目录", fontSize = 16.sp)
                    }
                    TextButton(
                        onClick = viewModel::rescanAll,
                        enabled = !state.isScanning && state.sources.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("刷新", fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
        ) {
            if (state.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 16.dp),
                )
            }

            state.recent?.let { recent ->
                Box(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
                    ContinueWatchingCard(recent) {
                        recent.lastEpisode?.let { onNavigateToPlayer(recent.id, it.episodeId) }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .testTag("library_list"),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "count", contentType = "header") {
                    Text(
                        text = "共有 ${state.dramas.size} 部短剧",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.dramas.isEmpty() && !state.isScanning) {
                    item(key = "empty", contentType = "empty") {
                        EmptyLibrary(onAddSource = { folderPicker.launch(null) })
                    }
                }

                items(
                    items = state.dramas,
                    key = { it.id },
                    contentType = { "drama" },
                ) { drama ->
                    DramaCard(
                        drama = drama,
                        onClick = {
                            val targetEpisodeId = drama.lastEpisode?.episodeId ?: drama.firstEpisodeId
                            if (targetEpisodeId != null) onNavigateToPlayer(drama.id, targetEpisodeId)
                            else viewModel.openEpisodes(drama)
                        },
                        onMenuClick = { viewModel.openEpisodes(drama) },
                    )
                }
            }
        }
    }

    state.episodeSelection?.let { selection ->
        key(selection.dramaId) {
            EpisodeDialog(
                selection = selection,
                onDismiss = viewModel::closeEpisodes,
                onEpisodeClick = { episode ->
                    viewModel.closeEpisodes()
                    onNavigateToPlayer(selection.dramaId, episode.id)
                },
            )
        }
    }

    if (state.showSourceDialog) {
        SourceDialog(
            sources = state.sources,
            onDismiss = { viewModel.setSourceDialogVisible(false) },
            onAdd = { folderPicker.launch(null) },
            onRemove = viewModel::removeSource,
        )
    }
}

@Composable
private fun ContinueWatchingCard(drama: DramaSummary, onClick: () -> Unit) {
    val episode = drama.lastEpisode ?: return
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("上次看到这里", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .7f))
                Text(drama.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("第 ${episode.episodeNumber} 集 · 点击继续播放", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun DramaCard(
    drama: DramaSummary,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = .5.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val cover = remember(drama.coverPath) { drama.coverPath?.let(::File)?.takeIf(File::isFile) }
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = "${drama.title}封面",
                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(drama.title.take(1), fontSize = 36.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(drama.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                drama.lastEpisode?.let {
                    Text("上次看到第 ${it.episodeNumber} 集", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                } ?: Text("未开始播放", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                Text("共 ${drama.episodeCount} 集", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "选集")
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onAddSource: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("还没有短剧", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("添加包含视频的根目录即可开始扫描", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAddSource) { Text("添加目录") }
    }
}

@Composable
private fun EpisodeDialog(
    selection: EpisodeSelection,
    onDismiss: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val currentIndex = remember(selection.episodes) {
        selection.episodes.indices.maxByOrNull { selection.episodes[it].lastWatchedAt }?.takeIf {
            selection.episodes[it].lastWatchedAt > 0
        } ?: -1
    }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) gridState.scrollToItem(currentIndex)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${selection.dramaTitle} - 选集", fontWeight = FontWeight.Bold) },
        text = {
            if (selection.isLoading) {
                Box(Modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(selection.episodes, key = { it.id }, contentType = { "episode" }) { episode ->
                        val selected = selection.episodes.getOrNull(currentIndex)?.id == episode.id
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { onEpisodeClick(episode) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                episode.episodeNumber.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun SourceDialog(
    sources: List<SourceDirectory>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    var pendingRemoval by remember { mutableStateOf<SourceDirectory?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目录管理", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (sources.isEmpty()) {
                    Text("暂无目录，请添加短剧根目录")
                } else {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(sources, key = { it.id }) { source ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                    Text(source.displayName, fontWeight = FontWeight.Bold)
                                    Text(source.treeUri, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                }
                                IconButton(onClick = { pendingRemoval = source }) {
                                    Icon(Icons.Default.Delete, contentDescription = "移除${source.displayName}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("添加短剧目录", fontSize = 17.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )

    pendingRemoval?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("移除这个目录？", fontWeight = FontWeight.Bold) },
            text = {
                Text("将从应用中移除“${source.displayName}”及播放记录，但不会删除手机里的视频文件。")
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("取消", fontSize = 17.sp) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(source.id)
                        pendingRemoval = null
                    },
                ) {
                    Text("确认移除", color = MaterialTheme.colorScheme.error, fontSize = 17.sp)
                }
            },
        )
    }
}
