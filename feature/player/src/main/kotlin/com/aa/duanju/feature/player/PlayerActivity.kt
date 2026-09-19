package com.aa.duanju.feature.player

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.aa.duanju.core.model.PlaybackQueue
import com.aa.duanju.domain.PlaybackRepository
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
    @Inject lateinit var playbackRepository: PlaybackRepository

    private lateinit var viewPager: ViewPager2
    private lateinit var recyclerView: RecyclerView
    private lateinit var title: TextView
    private lateinit var back: View
    private lateinit var adapter: VideoPagerAdapter

    private var queue: PlaybackQueue? = null
    private var currentPosition = -1
    private var pendingPosition = -1
    private var lastSavedEpisodeId = -1L
    private var lastSavedPosition = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        viewPager = findViewById(R.id.viewPager)
        recyclerView = viewPager.getChildAt(0) as RecyclerView
        title = findViewById(R.id.tvTitle)
        back = findViewById(R.id.btnBack)
        back.setOnClickListener { finish() }

        adapter = VideoPagerAdapter(::togglePlayPause)
        viewPager.adapter = adapter
        recyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                val holder = recyclerView.getChildViewHolder(view) as? VideoPagerAdapter.VideoViewHolder ?: return
                if (holder.bindingAdapterPosition == pendingPosition) startHolder(pendingPosition, holder)
            }

            override fun onChildViewDetachedFromWindow(view: View) = Unit
        })
        setupPager()
        loadQueue(
            dramaId = intent.getLongExtra(EXTRA_DRAMA_ID, -1L),
            targetEpisodeId = intent.getLongExtra(EXTRA_EPISODE_ID, -1L),
        )
    }

    private fun setupPager() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position !in adapter.currentList.indices || currentPosition == position) return
                saveProgress(currentPosition)
                GSYVideoManager.releaseAllVideos()
                currentPosition = position
                playPosition(position)
            }
        })
    }

    private fun loadQueue(dramaId: Long, targetEpisodeId: Long = -1L) {
        if (dramaId <= 0) {
            finish()
            return
        }
        lifecycleScope.launch {
            val loaded = runCatching {
                withContext(Dispatchers.IO) { playbackRepository.getPlaybackQueue(dramaId) }
            }.getOrElse {
                Toast.makeText(this@PlayerActivity, "视频加载失败", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            if (loaded.episodes.isEmpty()) {
                finish()
                return@launch
            }
            queue = loaded
            currentPosition = -1
            val target = loaded.episodes.indexOfFirst { it.id == targetEpisodeId }.takeIf { it >= 0 } ?: 0
            adapter.submitList(loaded.episodes) {
                viewPager.setCurrentItem(target, false)
                currentPosition = target
                playPosition(target)
            }
        }
    }

    private fun playPosition(position: Int) {
        val episode = adapter.currentList.getOrNull(position) ?: return
        pendingPosition = position
        title.text = getString(R.string.episode_title, episode.episodeNumber, queue?.dramaTitle.orEmpty())
        title.visibility = View.GONE
        back.visibility = View.GONE
        viewPager.post {
            findHolder(position)?.let { startHolder(position, it) }
        }
    }

    private fun startHolder(position: Int, holder: VideoPagerAdapter.VideoViewHolder) {
        if (position != pendingPosition) return
        val episode = adapter.currentList.getOrNull(position) ?: return
        pendingPosition = -1
        holder.videoPlayer.setVideoAllCallBack(object : GSYSampleCallBack() {
            override fun onAutoComplete(url: String?, vararg objects: Any?) {
                if (currentPosition < adapter.currentList.lastIndex) {
                    viewPager.setCurrentItem(currentPosition + 1, true)
                } else {
                    playNextDrama()
                }
            }
        })
        holder.videoPlayer.startPlayLogic()
        if (episode.resumePositionMs > 0) holder.videoPlayer.seekTo(episode.resumePositionMs)
    }

    private fun playNextDrama() {
        saveProgress(currentPosition)
        val nextDramaId = queue?.nextDramaId
        if (nextDramaId == null) finish() else loadQueue(nextDramaId)
    }

    private fun togglePlayPause(position: Int) {
        val holder = findHolder(position) ?: return
        val player = holder.videoPlayer
        if (player.currentState == com.shuyu.gsyvideoplayer.video.base.GSYVideoView.CURRENT_STATE_PLAYING) {
            player.onVideoPause()
            title.visibility = View.VISIBLE
            back.visibility = View.VISIBLE
            player.showUi()
        } else {
            player.onVideoResume()
            title.visibility = View.GONE
            back.visibility = View.GONE
            player.hideUi()
        }
    }

    private fun saveProgress(position: Int) {
        val episode = adapter.currentList.getOrNull(position) ?: return
        val currentMs = findHolder(position)?.videoPlayer?.currentPositionWhenPlaying?.toLong() ?: return
        if (currentMs <= 0) return
        if (lastSavedEpisodeId == episode.id && kotlin.math.abs(currentMs - lastSavedPosition) < 1_000) return
        lastSavedEpisodeId = episode.id
        lastSavedPosition = currentMs
        lifecycleScope.launch(Dispatchers.IO) {
            playbackRepository.saveProgress(episode.id, currentMs, System.currentTimeMillis())
        }
    }

    private fun findHolder(position: Int): VideoPagerAdapter.VideoViewHolder? =
        recyclerView.findViewHolderForAdapterPosition(position) as? VideoPagerAdapter.VideoViewHolder

    override fun onPause() {
        saveProgress(currentPosition)
        GSYVideoManager.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        GSYVideoManager.onResume()
    }

    override fun onDestroy() {
        GSYVideoManager.releaseAllVideos()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_DRAMA_ID = "drama_id"
        private const val EXTRA_EPISODE_ID = "episode_id"

        fun start(context: Context, dramaId: Long, episodeId: Long) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putExtra(EXTRA_DRAMA_ID, dramaId)
                    .putExtra(EXTRA_EPISODE_ID, episodeId),
            )
        }
    }
}
