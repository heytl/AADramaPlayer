package com.aa.duanju.feature.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aa.duanju.core.model.Episode

class VideoPagerAdapter(
    private val onVideoClick: (Int) -> Unit,
) : ListAdapter<Episode, VideoPagerAdapter.VideoViewHolder>(EpisodeDiff) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val episode = getItem(position)
        holder.videoPlayer.setUp(episode.videoUri, true, "")
        holder.videoPlayer.titleTextView.visibility = View.GONE
        holder.videoPlayer.backButton.visibility = View.GONE
        holder.videoPlayer.fullscreenButton.visibility = View.GONE
        holder.videoPlayer.onSingleTapListener = {
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(onVideoClick)
        }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        holder.videoPlayer.onSingleTapListener = null
        super.onViewRecycled(holder)
    }

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoPlayer: AAVideoPlayer = itemView.findViewById(R.id.videoPlayer)
    }

    private object EpisodeDiff : DiffUtil.ItemCallback<Episode>() {
        override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean = oldItem == newItem
    }
}
