package com.ytclone.ui.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.ytclone.R
import kotlin.math.abs

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var trackSelector: DefaultTrackSelector
    
    // UI Controls
    private lateinit var btnBack: ImageButton
    private lateinit var txtTitle: TextView
    private lateinit var txtChannel: TextView
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtDuration: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnFullscreen: ImageButton
    private lateinit var btnLike: ImageButton
    private lateinit var btnDislike: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var btnDownload: ImageButton
    private lateinit var progressBar: SeekBar
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var gestureOverlay: View
    private lateinit var volumeIndicator: LinearLayout
    private lateinit var brightnessIndicator: LinearLayout
    private lateinit var volumeProgress: ProgressBar
    private lateinit var brightnessProgress: ProgressBar
    private lateinit var gestureTextView: TextView
    
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideControls() }
    private val hideDelay = 4000L
    
    private var isFullscreen = false
    private var currentBrightness = 0.5f
    private var currentVolume = 0
    private var maxVolume = 0
    private var isSwiping = false
    private var swipeType = "" // "volume" or "brightness"
    private var startPosition = 0f
    
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // إخفاء شريط الحالة وشريط التنقل
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContentView(R.layout.activity_player)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val videoId = intent.getStringExtra("video_id") ?: run { finish(); return }
        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val channelName = intent.getStringExtra("channel_name") ?: ""

        initViews()
        
        txtTitle.text = title
        txtChannel.text = channelName

        btnBack.setOnClickListener { finish() }
        
        setupPlayer(videoId, videoUrl)
        setupControls()
        setupGestures()
        showControls()
    }

    private fun initViews() {
        playerView = findViewById(R.id.playerView)
        btnBack = findViewById(R.id.btnBack)
        txtTitle = findViewById(R.id.txtTitle)
        txtChannel = findViewById(R.id.txtChannel)
        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtDuration = findViewById(R.id.txtDuration)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnForward = findViewById(R.id.btnForward)
        btnRewind = findViewById(R.id.btnRewind)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        btnLike = findViewById(R.id.btnLike)
        btnDislike = findViewById(R.id.btnDislike)
        btnShare = findViewById(R.id.btnShare)
        btnDownload = findViewById(R.id.btnDownload)
        progressBar = findViewById(R.id.progressBar)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        gestureOverlay = findViewById(R.id.gestureOverlay)
        volumeIndicator = findViewById(R.id.volumeIndicator)
        brightnessIndicator = findViewById(R.id.brightnessIndicator)
        volumeProgress = findViewById(R.id.volumeProgress)
        brightnessProgress = findViewById(R.id.brightnessProgress)
        gestureTextView = findViewById(R.id.gestureTextView)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupPlayer(videoId: String, videoUrl: String) {
        trackSelector = DefaultTrackSelector(this)
        
        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                playerView.player = exoPlayer
                playerView.useController = false // سنستخدم controller مخصص
                
                val url = if (videoUrl.isNotEmpty()) videoUrl else "https://www.youtube.com/watch?v=$videoId"
                val mediaItem = MediaItem.fromUri(Uri.parse(url))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                loadingIndicator.visibility = View.VISIBLE
                                btnPlayPause.visibility = View.INVISIBLE
                            }
                            Player.STATE_READY -> {
                                loadingIndicator.visibility = View.GONE
                                btnPlayPause.visibility = View.VISIBLE
                                updateDuration()
                                startProgressUpdate()
                            }
                            Player.STATE_ENDED -> {
                                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                            }
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        btnPlayPause.setImageResource(
                            if (isPlaying) android.R.drawable.ic_media_pause
                            else android.R.drawable.ic_media_play
                        )
                        if (isPlaying) {
                            startProgressUpdate()
                            scheduleHideControls()
                        } else {
                            stopProgressUpdate()
                            showControls()
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        // تحديث واجهة المستخدم عند تغيير التrack
                    }
                })
            }
    }

    private fun setupControls() {
        btnPlayPause.setOnClickListener {
            player?.let { p ->
                if (p.isPlaying) p.pause() else p.play()
            }
        }

        btnForward.setOnClickListener {
            player?.let { p ->
                val newPos = p.currentPosition + 10000
                p.seekTo(newPos.coerceAtMost(p.duration))
            }
        }

        btnRewind.setOnClickListener {
            player?.let { p ->
                val newPos = p.currentPosition - 10000
                p.seekTo(newPos.coerceAtLeast(0))
            }
        }

        btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }

        btnLike.setOnClickListener {
            Toast.makeText(this, "أُعجبت بالفيديو", Toast.LENGTH_SHORT).show()
        }

        btnDislike.setOnClickListener {
            Toast.makeText(this, "لم يعجبني", Toast.LENGTH_SHORT).show()
        }

        btnShare.setOnClickListener {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "شاهد هذا الفيديو: https://www.youtube.com/watch?v=${intent.getStringExtra("video_id")}")
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة الفيديو"))
        }

        btnDownload.setOnClickListener {
            Toast.makeText(this, "التحميل قريباً", Toast.LENGTH_SHORT).show()
        }

        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                }
                txtCurrentTime.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopHideControlsTimer()
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                scheduleHideControls()
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (playerView.visibility == View.VISIBLE) {
                    if (isControlsVisible()) hideControls() else showControls()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val viewWidth = playerView.width
                val x = e.x
                if (x < viewWidth / 3) {
                    // نقر يسار - رجوع 10 ثواني
                    player?.let { p ->
                        val newPos = p.currentPosition - 10000
                        p.seekTo(newPos.coerceAtLeast(0))
                        showGestureIndicator("◀◀ 10 ثوانٍ")
                    }
                } else if (x > viewWidth * 2 / 3) {
                    // نقر يمين - تقديم 10 ثواني
                    player?.let { p ->
                        val newPos = p.currentPosition + 10000
                        p.seekTo(newPos.coerceAtMost(p.duration))
                        showGestureIndicator("▶▶ 10 ثواني")
                    }
                } else {
                    // نقر وسط - تشغيل/إيقاف
                    player?.let { p ->
                        if (p.isPlaying) p.pause() else p.play()
                    }
                }
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (e1 == null) return false
                
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x
                
                if (!isSwiping) {
                    if (abs(deltaY) > abs(deltaX) && abs(deltaY) > 20) {
                        isSwiping = true
                        swipeType = if (e1.x < playerView.width / 2) "brightness" else "volume"
                        startPosition = e1.y
                    }
                }
                
                if (isSwiping) {
                    val delta = deltaY / playerView.height
                    when (swipeType) {
                        "volume" -> {
                            currentVolume = (currentVolume - (delta * maxVolume * 2)).toInt().coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            volumeProgress.progress = currentVolume * 100 / maxVolume
                            gestureTextView.text = "الصوت: ${currentVolume * 100 / maxVolume}%"
                            volumeIndicator.visibility = View.VISIBLE
                            brightnessIndicator.visibility = View.GONE
                        }
                        "brightness" -> {
                            currentBrightness = (currentBrightness - delta).coerceIn(0.05f, 1.0f)
                            val layoutParams = window.attributes
                            layoutParams.screenBrightness = currentBrightness
                            window.attributes = layoutParams
                            brightnessProgress.progress = (currentBrightness * 100).toInt()
                            gestureTextView.text = "السطوع: ${(currentBrightness * 100).toInt()}%"
                            brightnessIndicator.visibility = View.VISIBLE
                            volumeIndicator.visibility = View.GONE
                        }
                    }
                    gestureOverlay.visibility = View.VISIBLE
                }
                return true
            }
        })

        playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isSwiping) {
                        isSwiping = false
                        gestureOverlay.postDelayed({
                            gestureOverlay.visibility = View.GONE
                            volumeIndicator.visibility = View.GONE
                            brightnessIndicator.visibility = View.GONE
                        }, 500)
                    }
                }
            }
            true
        }
    }

    private fun showGestureIndicator(text: String) {
        gestureTextView.text = text
        gestureOverlay.visibility = View.VISIBLE
        gestureOverlay.postDelayed({
            gestureOverlay.visibility = View.GONE
        }, 1000)
    }

    private fun isControlsVisible(): Boolean {
        return btnPlayPause.visibility == View.VISIBLE
    }

    private fun showControls() {
        btnBack.visibility = View.VISIBLE
        btnPlayPause.visibility = View.VISIBLE
        btnForward.visibility = View.VISIBLE
        btnRewind.visibility = View.VISIBLE
        btnFullscreen.visibility = View.VISIBLE
        btnLike.visibility = View.VISIBLE
        btnDislike.visibility = View.VISIBLE
        btnShare.visibility = View.VISIBLE
        btnDownload.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        txtCurrentTime.visibility = View.VISIBLE
        txtDuration.visibility = View.VISIBLE
        txtTitle.visibility = View.VISIBLE
        txtChannel.visibility = View.VISIBLE
        scheduleHideControls()
    }

    private fun hideControls() {
        btnBack.visibility = View.GONE
        btnPlayPause.visibility = View.GONE
        btnForward.visibility = View.GONE
        btnRewind.visibility = View.GONE
        btnFullscreen.visibility = View.GONE
        btnLike.visibility = View.GONE
        btnDislike.visibility = View.GONE
        btnShare.visibility = View.GONE
        btnDownload.visibility = View.GONE
        progressBar.visibility = View.GONE
        txtCurrentTime.visibility = View.GONE
        txtDuration.visibility = View.GONE
        txtTitle.visibility = View.GONE
        txtChannel.visibility = View.GONE
    }

    private fun scheduleHideControls() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, hideDelay)
    }

    private fun stopHideControlsTimer() {
        handler.removeCallbacks(hideRunnable)
    }

    private fun toggleFullscreen() {
        requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        isFullscreen = !isFullscreen
    }

    private fun updateDuration() {
        player?.let { p ->
            val duration = p.duration
            if (duration > 0) {
                progressBar.max = duration.toInt()
                txtDuration.text = formatTime(duration)
            }
        }
    }

    private fun startProgressUpdate() {
        val runnable = object : Runnable {
            override fun run() {
                player?.let { p ->
                    if (p.isPlaying) {
                        progressBar.progress = p.currentPosition.toInt()
                        txtCurrentTime.text = formatTime(p.currentPosition)
                    }
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    private fun stopProgressUpdate() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isFullscreen = true
        } else {
            isFullscreen = false
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
    }
}
