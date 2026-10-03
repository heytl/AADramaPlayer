package com.aa.duanju.feature.library

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aa.duanju.core.model.SourceDirectory
import com.aa.duanju.domain.AfterDrama
import com.aa.duanju.domain.PlayOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val autoPlay by viewModel.autoPlay.collectAsStateWithLifecycle()
    val playOrder by viewModel.playOrder.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val afterDrama by viewModel.afterDrama.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<SourceDirectory?>(null) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var showAfterDramaDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LibraryUiEffect.Message -> Toast.makeText(context, effect.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val name = SourceDirFormat.dirName(uri.toString(), "")
            viewModel.addSource(uri.toString(), name)
        }.onFailure { Toast.makeText(context, "无法读取目录：${it.message.orEmpty()}", Toast.LENGTH_LONG).show() }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", modifier = Modifier.size(26.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item(key = "sec_play") { SectionHeader("播放") }

            item(key = "autoplay") {
                SettingsRow(
                    icon = Icons.Default.PlayArrow,
                    title = "自动播放",
                    summary = "每次打开自动从上次看到的地方继续播放",
                    trailing = {
                        Switch(checked = autoPlay, onCheckedChange = viewModel::setAutoPlay)
                    },
                )
            }

            item(key = "div_play") {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item(key = "playorder") {
                SettingsRow(
                    icon = Icons.Default.Shuffle,
                    title = "播放顺序",
                    summary = if (playOrder == PlayOrder.SHUFFLE) "随机播放" else "顺序播放",
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { showOrderDialog = true },
                )
            }

            item(key = "div_play2") {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item(key = "keepscreenon") {
                SettingsRow(
                    icon = Icons.Default.BrightnessHigh,
                    title = "播放时屏幕常亮",
                    summary = "播放视频时屏幕不会自动熄灭",
                    trailing = {
                        Switch(checked = keepScreenOn, onCheckedChange = viewModel::setKeepScreenOn)
                    },
                )
            }

            item(key = "div_play3") {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item(key = "afterdrama") {
                SettingsRow(
                    icon = Icons.Default.SkipNext,
                    title = "播完本剧后",
                    summary = if (afterDrama == AfterDrama.STOP) "播完停止" else "自动播下一部",
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { showAfterDramaDialog = true },
                )
            }

            item(key = "sec_dir") { SectionHeader("短剧目录") }

            if (state.sources.isEmpty()) {
                item(key = "dir_empty") {
                    Text(
                        "还没有添加目录，请添加存放短剧视频的文件夹",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(state.sources, key = { it.id }) { source ->
                    SettingsRow(
                        icon = Icons.Default.FolderOpen,
                        title = SourceDirFormat.dirName(source.treeUri, source.displayName),
                        summary = SourceDirFormat.fullPath(source.treeUri, source.displayName),
                        trailing = {
                            IconButton(onClick = { pendingRemoval = source }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "移除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            item(key = "dir_add") {
                TextButton(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("添加短剧目录", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }

            item(key = "sec_other") { SectionHeader("其他") }

            item(key = "clear_history") {
                SettingsRow(
                    icon = Icons.Default.DeleteSweep,
                    title = "清除观看记录",
                    summary = "清除所有短剧的观看进度",
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { showClearConfirm = true },
                )
            }

            item(key = "div_other") {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item(key = "about") {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "关于",
                    summary = "版本 $versionName",
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { showAbout = true },
                )
            }
        }
    }

    if (showOrderDialog) {
        PlayOrderDialog(
            current = playOrder,
            onSelect = {
                viewModel.setPlayOrder(it)
                showOrderDialog = false
            },
            onDismiss = { showOrderDialog = false },
        )
    }

    if (showAfterDramaDialog) {
        AfterDramaDialog(
            current = afterDrama,
            onSelect = {
                viewModel.setAfterDrama(it)
                showAfterDramaDialog = false
            },
            onDismiss = { showAfterDramaDialog = false },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清除观看记录？", fontWeight = FontWeight.Bold) },
            text = { Text("将清除所有短剧的观看进度，下次打开从第一集开始播放。") },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消", fontSize = 17.sp) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearWatchHistory()
                        showClearConfirm = false
                    },
                ) {
                    Text("确认清除", color = MaterialTheme.colorScheme.error, fontSize = 17.sp)
                }
            },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于", fontWeight = FontWeight.Bold) },
            text = { Text("阿阿短剧\n版本 $versionName\n\n简单好用的短剧播放器。") },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("知道了", fontSize = 17.sp) }
            },
        )
    }

    pendingRemoval?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("移除这个目录？", fontWeight = FontWeight.Bold) },
            text = {
                Text("将从应用中移除“${SourceDirFormat.dirName(source.treeUri, source.displayName)}”及播放记录，但不会删除手机里的视频文件。")
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("取消", fontSize = 17.sp) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeSource(source.id)
                        pendingRemoval = null
                    },
                ) {
                    Text("确认移除", color = MaterialTheme.colorScheme.error, fontSize = 17.sp)
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(
                summary,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun PlayOrderDialog(
    current: PlayOrder,
    onSelect: (PlayOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播放顺序", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RadioOption(
                    title = "顺序播放",
                    summary = "按集数从小到大连续播放",
                    selected = current == PlayOrder.SEQUENTIAL,
                    onClick = { onSelect(PlayOrder.SEQUENTIAL) },
                )
                RadioOption(
                    title = "随机播放",
                    summary = "播完一集随机跳到另一集",
                    selected = current == PlayOrder.SHUFFLE,
                    onClick = { onSelect(PlayOrder.SHUFFLE) },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", fontSize = 17.sp) }
        },
    )
}

@Composable
private fun AfterDramaDialog(
    current: AfterDrama,
    onSelect: (AfterDrama) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播完本剧后", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RadioOption(
                    title = "自动播下一部",
                    summary = "播完自动从下一部剧的第一集开始",
                    selected = current == AfterDrama.AUTO_NEXT,
                    onClick = { onSelect(AfterDrama.AUTO_NEXT) },
                )
                RadioOption(
                    title = "播完停止",
                    summary = "播完回到短剧列表",
                    selected = current == AfterDrama.STOP,
                    onClick = { onSelect(AfterDrama.STOP) },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", fontSize = 17.sp) }
        },
    )
}

@Composable
private fun RadioOption(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                summary,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
