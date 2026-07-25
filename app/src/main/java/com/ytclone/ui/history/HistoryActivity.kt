package com.ytclone.ui.history

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ytclone.R
import com.ytclone.adapters.VideoAdapter
import com.ytclone.api.YouTubeApi
import com.ytclone.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerHistory: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var txtEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerHistory = findViewById(R.id.recyclerHistory)
        txtEmpty = findViewById(R.id.txtEmpty)
        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        videoAdapter = VideoAdapter(
            onVideoClick = { video ->
                startActivity(Intent(this, PlayerActivity::class.java).apply {
                    putExtra("video_id", video.videoId)
                    putExtra("title", video.title)
                    putExtra("channel_name", video.channelName)
                })
            }
        )

        recyclerHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = videoAdapter
        }

        loadHistory()
    }

    private fun loadHistory() {
        if (YouTubeApi.authCookies.isEmpty()) {
            txtEmpty.text = "يرجى تسجيل الدخول لعرض السجل"
            txtEmpty.visibility = android.view.View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                val videos = YouTubeApi.getHistory()
                if (videos.isEmpty()) {
                    txtEmpty.text = "سجل المشاهدة فارغ"
                    txtEmpty.visibility = android.view.View.VISIBLE
                } else {
                    txtEmpty.visibility = android.view.View.GONE
                    videoAdapter.submitList(videos)
                }
            } catch (e: Exception) {
                txtEmpty.text = "خطأ في تحميل السجل"
                txtEmpty.visibility = android.view.View.VISIBLE
                Toast.makeText(this@HistoryActivity, "خطأ في تحميل السجل", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
