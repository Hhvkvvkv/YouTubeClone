package com.ytclone.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
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

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerResults: RecyclerView
    private lateinit var videoAdapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etSearch = findViewById(R.id.etSearch)
        recyclerResults = findViewById(R.id.recyclerResults)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        videoAdapter = VideoAdapter(
            onVideoClick = { video ->
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("video_id", video.videoId)
                    putExtra("title", video.title)
                    putExtra("channel_name", video.channelName)
                }
                startActivity(intent)
            }
        )

        recyclerResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = videoAdapter
        }

        etSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = textView.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else false
        }

        // Auto-focus
        etSearch.requestFocus()
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            try {
                val results = YouTubeApi.search(query)
                videoAdapter.submitList(results)
            } catch (e: Exception) {
                Toast.makeText(this@SearchActivity, "خطأ في البحث", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
