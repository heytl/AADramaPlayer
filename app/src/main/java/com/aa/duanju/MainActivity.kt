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
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.aa.duanju.core.designsystem.AADramaTheme
import com.aa.duanju.feature.library.LibraryScreen
import com.aa.duanju.feature.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var benchmarkSeeder: BenchmarkSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装官方启动页方案，确保启动瞬间也具备沉浸式底色
        installSplashScreen()

        super.onCreate(savedInstanceState)

        if (BuildConfig.BENCHMARK && intent.getBooleanExtra("seed_benchmark_data", false)) {
            lifecycleScope.launch(Dispatchers.IO) { benchmarkSeeder.seed() }
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
                    LibraryScreen(onNavigateToPlayer = { dramaId, episodeId ->
                        PlayerActivity.start(
                            this@MainActivity,
                            dramaId,
                            episodeId
                        )
                    })
                }
            }
        }
    }
}
