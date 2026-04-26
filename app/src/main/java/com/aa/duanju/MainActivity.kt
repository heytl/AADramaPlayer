package com.aa.duanju

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aa.duanju.ui.library.LibraryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LibraryScreen(
                        onNavigateToPlayer = { dramaId, episodeId ->
                            com.aa.duanju.ui.player.PlayerActivity.start(
                                this@MainActivity,
                                dramaId,
                                episodeId
                            )
                        }
                    )
                }
            }
        }
    }
}
