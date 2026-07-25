package com.ytclone.ui.player

import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.ytclone.R

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val videoId = intent.getStringExtra("video_id") ?: return
        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val channelName = intent.getStringExtra("channel_name") ?: ""

        playerView = findViewById(R.id.playerView)
        val txtTitle = findViewById<TextView>(R.id.txtTitle)
        val txtChannel = findViewById<TextView>(R.id.txtChannel)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        txtTitle.text = title
        txtChannel.text = channelName

        btnBack.setOnClickListener { finish() }

        // تهيئة ExoPlayer
        initializePlayer(videoId, videoUrl)
    }

    private fun initializePlayer(videoId: String, videoUrl: String) {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // إنشاء رابط الفيديو
        val url = if (videoUrl.isNotEmpty()) {
            videoUrl
        } else {
            // رابط يوتيوب المباشر
            "https://www.youtube.com/watch?v=$videoId"
        }

        // إنشاء MediaItem وتشغيله
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}
