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
import com.ytclone.adapters.VideoAdapter
import com.ytclone.api.YouTubeApi
import com.ytclone.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class SubscriptionsFragment : Fragment() {

    private lateinit var recyclerSubscriptions: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var txtNoSubs: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_subscriptions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerSubscriptions = view.findViewById(R.id.recyclerSubscriptions)
        txtNoSubs = view.findViewById(R.id.txtNoSubs)

        videoAdapter = VideoAdapter(
            onVideoClick = { video ->
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("video_id", video.videoId)
                    putExtra("title", video.title)
                    putExtra("channel_name", video.channelName)
                }
                startActivity(intent)
            }
        )

        recyclerSubscriptions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }

        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val videos = YouTubeApi.getSubscriptions()
                if (videos.isEmpty()) {
                    txtNoSubs.visibility = View.VISIBLE
                    recyclerSubscriptions.visibility = View.GONE
                } else {
                    txtNoSubs.visibility = View.GONE
                    recyclerSubscriptions.visibility = View.VISIBLE
                    videoAdapter.submitList(videos)
                }
            } catch (e: Exception) {
                txtNoSubs.visibility = View.VISIBLE
                recyclerSubscriptions.visibility = View.GONE
            }
        }
    }

    companion object {
        fun newInstance() = SubscriptionsFragment()
    }
}
