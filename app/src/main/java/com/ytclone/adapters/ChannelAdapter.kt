package com.ytclone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ytclone.R
import com.ytclone.models.ChannelItem

class ChannelAdapter(
    private var channels: MutableList<ChannelItem> = mutableListOf(),
    private val onClick: (ChannelItem) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    fun submitList(newList: List<ChannelItem>) {
        channels.clear()
        channels.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(channels[position])
    }

    override fun getItemCount() = channels.size

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgThumb: ImageView = itemView.findViewById(R.id.imgChannelAvatar)
        private val txtName: TextView = itemView.findViewById(R.id.txtChannelName)

        fun bind(channel: ChannelItem) {
            txtName.text = channel.title
            Glide.with(itemView.context)
                .load(channel.thumbnailUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_account)
                .into(imgThumb)
            itemView.setOnClickListener { onClick(channel) }
        }
    }
}
