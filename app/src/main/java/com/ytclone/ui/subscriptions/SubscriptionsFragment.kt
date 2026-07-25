package com.ytclone.ui.subscriptions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ytclone.R
import com.ytclone.adapters.ChannelAdapter
import com.ytclone.adapters.VideoAdapter
import com.ytclone.api.YouTubeApi
import com.ytclone.models.ChannelItem
import com.ytclone.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class SubscriptionsFragment : Fragment() {

    private lateinit var recyclerVideos: RecyclerView
    private lateinit var recyclerChannels: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var txtNoSubs: TextView
    private lateinit var txtNoVideos: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_subscriptions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerVideos = view.findViewById(R.id.recyclerSubscriptions)
        recyclerChannels = view.findViewById(R.id.recyclerChannels)
        txtNoSubs = view.findViewById(R.id.txtNoSubs)
        txtNoVideos = view.findViewById(R.id.txtNoVideos)

        // محول الفيديوهات
        videoAdapter = VideoAdapter(
            onVideoClick = { video ->
                startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("video_id", video.videoId)
                    putExtra("title", video.title)
                    putExtra("channel_name", video.channelName)
                })
            }
        )

        recyclerVideos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }

        // محول القنوات (أفقي)
        channelAdapter = ChannelAdapter { channel ->
            Toast.makeText(requireContext(), channel.title, Toast.LENGTH_SHORT).show()
        }

        recyclerChannels.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = channelAdapter
        }

        loadData()
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // تحميل القنوات
                if (YouTubeApi.authCookies.isNotEmpty()) {
                    val channels = YouTubeApi.getSubscribedChannels()
                    if (channels.isEmpty()) {
                        recyclerChannels.visibility = View.GONE
                        txtNoSubs.visibility = View.VISIBLE
                    } else {
                        recyclerChannels.visibility = View.VISIBLE
                        txtNoSubs.visibility = View.GONE
                        channelAdapter.submitList(channels)
                    }
                } else {
                    recyclerChannels.visibility = View.GONE
                    txtNoSubs.visibility = View.VISIBLE
                    txtNoSubs.text = "سجل الدخول لعرض اشتراكاتك"
                }

                // تحميل الفيديوهات
                val videos = YouTubeApi.getSubscriptions()
                if (videos.isEmpty()) {
                    txtNoVideos.visibility = View.VISIBLE
                    recyclerVideos.visibility = View.GONE
                } else {
                    txtNoVideos.visibility = View.GONE
                    recyclerVideos.visibility = View.VISIBLE
                    videoAdapter.submitList(videos)
                }
            } catch (e: Exception) {
                txtNoSubs.text = "خطأ في تحميل البيانات"
                txtNoSubs.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        fun newInstance() = SubscriptionsFragment()
    }
}
