package com.aa.duanju

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.aa.duanju.core.designsystem.AADramaTheme
import com.aa.duanju.domain.LibraryRepository
import com.aa.duanju.domain.SettingsRepository
import com.aa.duanju.feature.library.LibraryScreen
import com.aa.duanju.feature.library.SettingsScreen
import com.aa.duanju.feature.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var benchmarkSeeder: BenchmarkSeeder
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var libraryRepository: LibraryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装官方启动页方案，确保启动瞬间也具备沉浸式底色
        installSplashScreen()

        super.onCreate(savedInstanceState)

        if (BuildConfig.BENCHMARK && intent.getBooleanExtra("seed_benchmark_data", false)) {
            lifecycleScope.launch(Dispatchers.IO) { benchmarkSeeder.seed() }
        }

        // 打开自动播放：每次冷启动时，若开关开启且有上次观看记录，直接进入播放器
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val target = withTimeoutOrNull(12_000) {
                    combine(
                        settingsRepository.observeAutoPlay(),
                        libraryRepository.observeLibrary(),
                    ) { autoPlay, dramas ->
                        if (!autoPlay) null
                        else dramas.firstOrNull { it.lastEpisode != null }
                            ?.let { drama -> drama.lastEpisode?.let { drama.id to it.episodeId } }
                    }.first { it != null }
                }
                if (target != null) {
                    PlayerActivity.start(this@MainActivity, target.first, target.second)
                }
            }
        }

        // 开启全屏沉浸模式，针对现代 Android (包括华为 HarmonyOS) 优化
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            AADramaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSettings by rememberSaveable { mutableStateOf(false) }
                    if (showSettings) {
                        SettingsScreen(onBack = { showSettings = false })
                    } else {
                        LibraryScreen(
                            onNavigateToPlayer = { dramaId, episodeId ->
                                PlayerActivity.start(
                                    this@MainActivity,
                                    dramaId,
                                    episodeId
                                )
                            },
                            onNavigateToSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }
}
