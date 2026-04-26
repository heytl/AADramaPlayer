package com.aa.duanju.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.aa.duanju.R
import com.aa.duanju.db.AppDatabase
import com.aa.duanju.db.entity.DramaEntity
import com.aa.duanju.db.entity.EpisodeEntity
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_DRAMA_ID = "drama_id"
        private const val EXTRA_EPISODE_ID = "episode_id"

        fun start(context: Context, dramaId: Long, startEpisodeId: Long) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_DRAMA_ID, dramaId)
                putExtra(EXTRA_EPISODE_ID, startEpisodeId)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var tvTitle: TextView
    private lateinit var tvSpeedTip: TextView
    private lateinit var btnBack: View
    
    private lateinit var adapter: VideoPagerAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }
    
    private var currentDrama: DramaEntity? = null
    private var episodes: List<EpisodeEntity> = emptyList()
    
    private var currentPlayingPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        // 播放器开启全透明沉浸，由于是播放视频，状态栏设为全透明
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        
        viewPager = findViewById(R.id.viewPager)
        tvTitle = findViewById(R.id.tvTitle)
        tvSpeedTip = findViewById(R.id.tvSpeedTip)
        btnBack = findViewById(R.id.btnBack)
        
        btnBack.setOnClickListener { finish() }
        
        adapter = VideoPagerAdapter(
            episodes = emptyList(),
            onVideoClick = { position -> togglePlayPause(position) }
        )
        viewPager.adapter = adapter
        
        setupViewPager()
        loadData()
    }

    private fun playNextDrama() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allDramas = db.dramaDao().getAllDramas()
            val currentIndex = allDramas.indexOfFirst { it.id == currentDrama?.id }
            
            val nextDrama = if (currentIndex != -1 && currentIndex < allDramas.size - 1) {
                allDramas[currentIndex + 1]
            } else {
                allDramas.firstOrNull()
            }

            if (nextDrama != null) {
                val nextEpisodes = db.episodeDao().getEpisodesByDramaId(nextDrama.id)
                if (nextEpisodes.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        currentDrama = nextDrama
                        episodes = nextEpisodes
                        adapter.updateData(episodes)
                        viewPager.setCurrentItem(0, false)
                        playPosition(0)
                        
                        android.widget.Toast.makeText(
                            this@PlayerActivity, 
                            "即将播放：${nextDrama.title}", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) { finish() }
                }
            } else {
                withContext(Dispatchers.Main) { finish() }
            }
        }
    }

    private fun loadData() {
        val dramaId = intent.getLongExtra(EXTRA_DRAMA_ID, -1L)
        val episodeId = intent.getLongExtra(EXTRA_EPISODE_ID, -1L)
        if (dramaId == -1L) return

        lifecycleScope.launch(Dispatchers.IO) {
            currentDrama = db.dramaDao().getDramaById(dramaId)
            episodes = db.episodeDao().getEpisodesByDramaId(dramaId)
            
            withContext(Dispatchers.Main) {
                adapter.updateData(episodes)
                
                val targetIndex = episodes.indexOfFirst { it.id == episodeId }
                if (targetIndex >= 0) {
                    viewPager.setCurrentItem(targetIndex, false)
                    if (targetIndex == 0) {
                        playPosition(0)
                    }
                }
            }
        }
    }

    private fun setupViewPager() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (currentPlayingPosition == position) return
                saveProgress(currentPlayingPosition)
                GSYVideoManager.releaseAllVideos()
                playPosition(position)
            }
        })
    }

    private fun playPosition(position: Int) {
        if (position < 0 || position >= episodes.size) return
        currentPlayingPosition = position
        val episode = episodes[position]
        
        tvTitle.text = "第 ${episode.episodeNumber} 集 - ${currentDrama?.title}"
        tvTitle.visibility = View.GONE
        btnBack.visibility = View.GONE
        
        viewPager.post {
            val recyclerView = viewPager.getChildAt(0) as androidx.recyclerview.widget.RecyclerView
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? VideoPagerAdapter.VideoViewHolder
            
            if (viewHolder != null) {
                viewHolder.videoPlayer.setVideoAllCallBack(object : GSYSampleCallBack() {
                    override fun onAutoComplete(url: String?, vararg objects: Any?) {
                        super.onAutoComplete(url, *objects)
                        if (currentPlayingPosition < episodes.size - 1) {
                            viewPager.setCurrentItem(currentPlayingPosition + 1, true)
                        } else {
                            playNextDrama()
                        }
                    }
                })
                
                viewHolder.videoPlayer.startPlayLogic()
                if (episode.lastPlaybackPosition > 0) {
                    viewHolder.videoPlayer.seekTo(episode.lastPlaybackPosition)
                }
            } else {
                viewPager.postDelayed({ playPosition(position) }, 100)
            }
        }
    }

    private fun togglePlayPause(position: Int) {
        val recyclerView = viewPager.getChildAt(0) as androidx.recyclerview.widget.RecyclerView
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? VideoPagerAdapter.VideoViewHolder
        
        viewHolder?.let { holder ->
            val player = holder.videoPlayer
            
            if (player.currentState == com.shuyu.gsyvideoplayer.video.base.GSYVideoView.CURRENT_STATE_PLAYING) {
                player.onVideoPause()
                tvTitle.visibility = View.VISIBLE
                btnBack.visibility = View.VISIBLE
                player.showUi() 
            } else {
                player.onVideoResume()
                tvTitle.visibility = View.GONE
                btnBack.visibility = View.GONE
                player.hideUi()
            }
        }
    }

    private fun saveProgress(position: Int) {
        if (position < 0 || position >= episodes.size) return
        
        val recyclerView = viewPager.getChildAt(0) as androidx.recyclerview.widget.RecyclerView
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? VideoPagerAdapter.VideoViewHolder
        
        viewHolder?.videoPlayer?.let { player ->
            val curPos = player.currentPositionWhenPlaying
            if (curPos > 0) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val episode = episodes[position]
                    val now = System.currentTimeMillis()
                    db.episodeDao().updatePlaybackPosition(episode.id, curPos.toLong(), now)
                    currentDrama?.let { drama ->
                        db.dramaDao().updateDrama(drama.copy(lastWatchedTime = now))
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        GSYVideoManager.onPause()
        saveProgress(currentPlayingPosition)
    }

    override fun onResume() {
        super.onResume()
        GSYVideoManager.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveProgress(currentPlayingPosition)
        GSYVideoManager.releaseAllVideos()
    }
}
