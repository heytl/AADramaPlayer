package com.aa.duanju.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.aa.duanju.R
import com.aa.duanju.db.entity.EpisodeEntity
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

class VideoPagerAdapter(
    private var episodes: List<EpisodeEntity>,
    private val onVideoClick: (Int) -> Unit,
    private val onVideoLongPress: (Boolean) -> Unit
) : RecyclerView.Adapter<VideoPagerAdapter.VideoViewHolder>() {

    fun updateData(newEpisodes: List<EpisodeEntity>) {
        episodes = newEpisodes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val episode = episodes[position]
        
        holder.videoPlayer.setUp(episode.videoPath, true, "")
        
        // 适老定制：隐藏默认控件，我们通过外层控制
        holder.videoPlayer.titleTextView.visibility = View.GONE
        holder.videoPlayer.backButton.visibility = View.GONE
        holder.videoPlayer.fullscreenButton.visibility = View.GONE

        // 使用 AAVideoPlayer 提供的单点击监听
        holder.videoPlayer.onSingleTapListener = {
            onVideoClick(position)
        }

        // 长按加速逻辑
        holder.videoPlayer.setOnLongClickListener {
            onVideoLongPress(true)
            true
        }
        
        // 监听触摸释放以恢复速度
        holder.videoPlayer.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP || 
                event.action == android.view.MotionEvent.ACTION_CANCEL) {
                onVideoLongPress(false)
            }
            false
        }
    }

    override fun getItemCount(): Int = episodes.size

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoPlayer: AAVideoPlayer = itemView.findViewById(R.id.videoPlayer)
    }
}
