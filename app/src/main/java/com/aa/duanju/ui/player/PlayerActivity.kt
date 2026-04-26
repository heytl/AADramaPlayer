package com.aa.duanju.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        
        viewPager = findViewById(R.id.viewPager)
        tvTitle = findViewById(R.id.tvTitle)
        tvSpeedTip = findViewById(R.id.tvSpeedTip)
        btnBack = findViewById(R.id.btnBack)
        
        btnBack.setOnClickListener { finish() }
        
        adapter = VideoPagerAdapter(
            episodes = emptyList(),
            onVideoClick = { position -> togglePlayPause(position) },
            onVideoLongPress = { isPressed -> handleSpeed(isPressed) }
        )
        viewPager.adapter = adapter
        
        setupViewPager()
        loadData()
    }

    private fun playNextDrama() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allDramas = db.dramaDao().getAllDramas()
            val currentIndex = allDramas.indexOfFirst { it.id == currentDrama?.id }
            
            // 找到下一个短剧（如果到头了就从第一个开始循环）
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

    private fun handleSpeed(isPressed: Boolean) {
        val recyclerView = viewPager.getChildAt(0) as androidx.recyclerview.widget.RecyclerView
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(currentPlayingPosition) as? VideoPagerAdapter.VideoViewHolder
        
        viewHolder?.videoPlayer?.let { player ->
            if (isPressed) {
                player.setSpeed(2.0f, true)
                tvSpeedTip.visibility = View.VISIBLE
            } else {
                player.setSpeed(1.0f, true)
                tvSpeedTip.visibility = View.GONE
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
                
                // 定位到请求的集数
                val targetIndex = episodes.indexOfFirst { it.id == episodeId }
                if (targetIndex >= 0) {
                    viewPager.setCurrentItem(targetIndex, false)
                    // 如果等于 0 的话，onPageSelected 会立刻触发，无需手动 play 
                    if (targetIndex == 0) {
                        playPosition(0)
                    }
                }
            }
        }
    }

    private fun setupViewPager() {
        // 利用 ViewPager2 回调处理滑动自动播放与释放上一集
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // 每次翻页，如果旧的等于当前的，忽略
                if (currentPlayingPosition == position) return
                
                // 保存并释放上一个视频
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
        // 初始播放时隐藏标题和返回按钮
        tvTitle.visibility = View.GONE
        btnBack.visibility = View.GONE
        
        // 确保 ViewPager 切换完成后再寻找 ViewHolder
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
                            // 最后一集播放完，尝试跳转到下一个短剧
                            playNextDrama()
                        }
                    }
                })
                
                viewHolder.videoPlayer.startPlayLogic()
                if (episode.lastPlaybackPosition > 0) {
                    viewHolder.videoPlayer.seekTo(episode.lastPlaybackPosition)
                }
            } else {
                // 如果 ViewHolder 还没准备好，稍后再试一次
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
                player.showUi() // 暂停时直接显示进度条和按钮
            } else {
                player.onVideoResume()
                tvTitle.visibility = View.GONE
                btnBack.visibility = View.GONE
                player.hideUi() // 播放时隐藏
            }
        }
    }

    private fun saveProgress(position: Int) {
        if (position < 0 || position >= episodes.size) return
        
        val viewHolder = (viewPager.getChildAt(0) as androidx.recyclerview.widget.RecyclerView)
                              .findViewHolderForAdapterPosition(position) as? VideoPagerAdapter.VideoViewHolder
        
        viewHolder?.videoPlayer?.let { player ->
            val curPos = player.currentPositionWhenPlaying
            if (curPos > 0) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val episode = episodes[position]
                    val now = System.currentTimeMillis()
                    db.episodeDao().updatePlaybackPosition(episode.id, curPos.toLong(), now)
                    currentDrama?.let { drama ->
                        // 更新电视剧最近观看时间
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
