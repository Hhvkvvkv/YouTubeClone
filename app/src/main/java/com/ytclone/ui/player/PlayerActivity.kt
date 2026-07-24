package com.ytclone.ui.player

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.ytclone.R

class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val videoId = intent.getStringExtra("video_id") ?: return
        val title = intent.getStringExtra("title") ?: ""
        val channelName = intent.getStringExtra("channel_name") ?: ""

        val youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtubePlayerView)
        val txtTitle = findViewById<TextView>(R.id.txtTitle)
        val txtChannel = findViewById<TextView>(R.id.txtChannel)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        txtTitle.text = title
        txtChannel.text = channelName

        btnBack.setOnClickListener { finish() }

        lifecycle.addObserver(youTubePlayerView)
        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadVideo(videoId, 0f)
            }
        })
    }
}
