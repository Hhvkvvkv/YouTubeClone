package com.ytclone.ui.shorts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class ShortsFragment : Fragment() {

    private lateinit var recyclerShorts: RecyclerView
    private lateinit var videoAdapter: VideoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shorts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerShorts = view.findViewById(R.id.recyclerShorts)

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

        recyclerShorts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
        }

        loadShorts()
    }

    private fun loadShorts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val shorts = YouTubeApi.getShorts()
                videoAdapter.submitList(shorts)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "خطأ في تحميل Shorts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun newInstance() = ShortsFragment()
    }
}
