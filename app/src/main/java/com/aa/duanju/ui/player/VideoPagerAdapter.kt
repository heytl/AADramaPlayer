package com.aa.duanju.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aa.duanju.R
import com.aa.duanju.db.entity.EpisodeEntity

class VideoPagerAdapter(
    private var episodes: List<EpisodeEntity>,
    private val onVideoClick: (Int) -> Unit
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
        
        holder.videoPlayer.titleTextView.visibility = View.GONE
        holder.videoPlayer.backButton.visibility = View.GONE
        holder.videoPlayer.fullscreenButton.visibility = View.GONE

        // 单击逻辑
        holder.videoPlayer.onSingleTapListener = {
            onVideoClick(position)
        }
    }

    override fun getItemCount(): Int = episodes.size

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoPlayer: AAVideoPlayer = itemView.findViewById(R.id.videoPlayer)
    }
}
