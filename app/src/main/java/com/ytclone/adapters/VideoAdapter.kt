package com.ytclone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ytclone.R
import com.ytclone.models.VideoItem

class VideoAdapter(
    private var videos: MutableList<VideoItem> = mutableListOf(),
    private val onVideoClick: (VideoItem) -> Unit,
    private val onMoreClick: (VideoItem) -> Unit = {}
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    fun submitList(newVideos: List<VideoItem>) {
        videos.clear()
        videos.addAll(newVideos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount() = videos.size

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgThumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        private val txtChannelName: TextView = itemView.findViewById(R.id.txtChannelName)
        private val txtViews: TextView = itemView.findViewById(R.id.txtViews)
        private val txtDuration: TextView = itemView.findViewById(R.id.txtDuration)
        private val imgChannelAvatar: ImageView = itemView.findViewById(R.id.imgChannelAvatar)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)

        fun bind(video: VideoItem) {
            txtTitle.text = video.title
            txtChannelName.text = video.channelName
            txtViews.text = "${video.viewCount} · ${video.publishedTime}"

            if (!video.duration.isNullOrEmpty()) {
                txtDuration.visibility = View.VISIBLE
                txtDuration.text = video.duration
            } else {
                txtDuration.visibility = View.GONE
            }

            Glide.with(itemView.context)
                .load(video.thumbnailUrl)
                .centerCrop()
                .into(imgThumbnail)

            Glide.with(itemView.context)
                .load(video.channelAvatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_account)
                .into(imgChannelAvatar)

            itemView.setOnClickListener { onVideoClick(video) }
            btnMore.setOnClickListener { onMoreClick(video) }
        }
    }
}
