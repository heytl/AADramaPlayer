package com.aa.duanju.ui.library

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.aa.duanju.db.dao.DramaWithProgress
import com.aa.duanju.db.entity.DramaEntity
import com.aa.duanju.db.entity.EpisodeEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libViewModel: LibraryViewModel = viewModel(),
    onNavigateToPlayer: (dramaId: Long, startEpisodeId: Long) -> Unit
) {
    val context = LocalContext.current
    
    // 全局共享的 ImageLoader，开启硬件加速和 Crossfade 提升滑动性能
    val sharedImageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .allowHardware(true)
            .build()
    }

    val dramasWithProgress by libViewModel.dramas.collectAsState()
    val sourceDirs by libViewModel.sourceDirectories.collectAsState()
    val recentEpisode by libViewModel.recentEpisode.collectAsState()
    val recentDrama by libViewModel.recentDrama.collectAsState()
    val isScanning by libViewModel.isScanning.collectAsState()
    val scanResultMsg by libViewModel.scanResultMsg.collectAsState()

    var selectedDramaId by remember { mutableStateOf<Long?>(null) }
    var showEpisodeSelection by remember { mutableStateOf(false) }
    var showDirManagement by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val currentSelectedWithProgress = remember(selectedDramaId, dramasWithProgress) {
        dramasWithProgress.find { it.drama.id == selectedDramaId }
    }
    
    val gridState = rememberLazyGridState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                val dirName = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, it)?.name ?: "新目录"
                libViewModel.addSourceDirectory(it, dirName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(scanResultMsg) {
        scanResultMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            libViewModel.clearScanMsg()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // 使用完全透明的背景，并减小高度感的视觉偏移
            TopAppBar(
                title = { 
                    Text(
                        "阿阿短剧", 
                        fontSize = 22.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 4.dp) // 微调标题高度
                    ) 
                },
                actions = {
                    IconButton(onClick = { showDirManagement = true }) {
                        Icon(Icons.Default.Add, contentDescription = "目录管理", modifier = Modifier.size(26.dp))
                    }
                    IconButton(onClick = { libViewModel.rescanAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // 彻底透明，实现一体化
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets.statusBars // 仅处理状态栏，不增加额外 padding
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = padding.calculateTopPadding() - 8.dp) // 进一步压缩顶部间距，让内容上移
        ) {
            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 移除多余的 Spacer，让内容更贴近顶栏
            // Spacer(modifier = Modifier.height(8.dp))

            if (recentEpisode != null && recentDrama != null) {
                Surface(
                    onClick = { onNavigateToPlayer(recentDrama!!.id, recentEpisode!!.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF2E7D32), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "播放",
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "上次看到这里",
                                fontSize = 16.sp,
                                color = Color(0xFF2E7D32).copy(alpha = 0.7f)
                            )
                            Text(
                                recentDrama?.title ?: "未知剧集",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 24.sp
                            )
                            Text(
                                "第 ${recentEpisode?.episodeNumber} 集",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("共有 ${dramasWithProgress.size} 部短剧", fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = dramasWithProgress,
                    key = { it.drama.id },
                    contentType = { "drama" }
                ) { item ->
                    DramaCard(
                        drama = item.drama,
                        lastEp = item.lastWatchedEpisode,
                        imageLoader = sharedImageLoader,
                        onClick = {
                            if (item.episodes.isNotEmpty()) {
                                val target = item.lastWatchedEpisode ?: item.episodes.first()
                                onNavigateToPlayer(item.drama.id, target.id)
                            }
                        },
                        onMenuClick = {
                            selectedDramaId = item.drama.id
                            showEpisodeSelection = true
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showEpisodeSelection && currentSelectedWithProgress != null) {
        val episodes = currentSelectedWithProgress.episodes
        val lastWatchedIndex = remember(episodes) {
            val lastEp = episodes.filter { it.lastWatchedTime > 0 }.maxByOrNull { it.lastWatchedTime }
            if (lastEp != null) episodes.indexOf(lastEp) else -1
        }

        LaunchedEffect(showEpisodeSelection) {
            if (lastWatchedIndex >= 0) {
                gridState.scrollToItem(lastWatchedIndex)
            }
        }

        AlertDialog(
            onDismissRequest = { showEpisodeSelection = false },
            title = {
                Text(
                    text = "${currentSelectedWithProgress.drama.title} - 选集",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    itemsIndexed(
                        items = episodes,
                        key = { _, ep -> ep.id }, // 关键优化：指定 Key 提升列表复用性能
                        contentType = { _, _ -> "episode" }
                    ) { index, ep ->
                        val isCurrent = index == lastWatchedIndex
                        EpisodeItem(
                            episodeNumber = ep.episodeNumber,
                            isCurrent = isCurrent,
                            onClick = {
                                showEpisodeSelection = false
                                onNavigateToPlayer(currentSelectedWithProgress.drama.id, ep.id)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEpisodeSelection = false }) {
                    Text("关闭", fontSize = 22.sp)
                }
            }
        )
    }

    if (showDirManagement) {
        AlertDialog(
            onDismissRequest = { showDirManagement = false },
            title = { Text("目录管理", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("已选中的根目录：", fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sourceDirs.isEmpty()) {
                        Text("暂无目录，请点击下方按钮添加", fontSize = 16.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(
                                items = sourceDirs,
                                key = { it.id }
                            ) { dir ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dir.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text(dir.path, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { libViewModel.removeSourceDirectory(dir) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "移除", tint = Color.Red)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { folderPickerLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text("添加新目录")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDirManagement = false }) {
                    Text("完成", fontSize = 18.sp)
                }
            }
        )
    }
}

@Composable
fun EpisodeItem(episodeNumber: Int, isCurrent: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = episodeNumber.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (isCurrent) {
                Text(
                    "播放中", 
                    fontSize = 12.sp, 
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DramaCard(drama: DramaEntity, lastEp: EpisodeEntity?, imageLoader: ImageLoader, onClick: () -> Unit, onMenuClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp), // 减低阴影，更扁平一体
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 使用普通 surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (drama.coverImagePath.isNotEmpty()) {
                AsyncImage(
                    model = drama.coverImagePath,
                    imageLoader = imageLoader,
                    contentDescription = "封面",
                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = drama.title.take(1), fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = drama.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                
                if (lastEp != null) {
                    Text(
                        text = "上次看到第 ${lastEp.episodeNumber} 集",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "未开始播放",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                Text(text = "共 ${drama.totalEpisodes} 集", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }

            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "选集", modifier = Modifier.size(24.dp))
            }
        }
    }
}
