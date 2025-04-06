package com.example.learnsync

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.learnsync.utils.YouTubeService
import com.example.learnsync.utils.Config
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Start background animation
        val rootView = findViewById<ScrollView>(R.id.rootScrollView)
        val animationDrawable = rootView.background as android.graphics.drawable.AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(4000)
        animationDrawable.start()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etVideoUrl = findViewById<TextInputEditText>(R.id.etVideoUrl)
        val tilVideoUrl = findViewById<TextInputLayout>(R.id.tilVideoUrl)
        val cardVideoPreview = findViewById<CardView>(R.id.cardVideoPreview)
        val ivThumbnail = findViewById<ImageView>(R.id.ivThumbnail)
        val tvVideoTitle = findViewById<TextView>(R.id.tvVideoTitle)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val youTubeService = YouTubeService()
        var currentVideoId: String? = null
        var currentVideoTitle: String? = null

        etVideoUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = etVideoUrl.text?.toString()
                if (!url.isNullOrEmpty()) {
                    val videoId = YouTubeService.extractVideoId(url)
                    if (videoId != null && videoId != currentVideoId) {
                        currentVideoId = videoId
                        progressBar.visibility = View.VISIBLE
                        cardVideoPreview.visibility = View.GONE
                        
                        lifecycleScope.launch {
                            try {
                                val videoDetails = youTubeService.getVideoDetails(videoId, Config.YOUTUBE_API_KEY)
                                if (videoDetails != null) {
                                    currentVideoTitle = videoDetails.snippet.title
                                    tvVideoTitle.text = currentVideoTitle
                                    Glide.with(this@MainActivity)
                                        .load(videoDetails.snippet.thumbnails.high.url)
                                        .into(ivThumbnail)
                                    cardVideoPreview.visibility = View.VISIBLE
                                } else {
                                    Toast.makeText(this@MainActivity, "Could not fetch video details", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }

        // Debug button with hardcoded video
        findViewById<Button>(R.id.btnDebug).setOnClickListener {
            val debugUrl = "https://www.youtube.com/watch?v=GhFeWFLL4lQ"
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra(VideoPlayerActivity.EXTRA_VIDEO_TOPIC, "Class 12 Physics | Electric Charges and Fields | Introduction to Charge")
                putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, debugUrl)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnPlayVideo).setOnClickListener {
            val videoUrl = etVideoUrl.text?.toString()?.trim() ?: ""
            val videoId = YouTubeService.extractVideoId(videoUrl)

            if (videoUrl.isEmpty()) {
                tilVideoUrl.error = "Please enter video URL"
                return@setOnClickListener
            }

            if (videoId == null) {
                tilVideoUrl.error = "Please enter a valid YouTube URL"
                return@setOnClickListener
            }

            tilVideoUrl.error = null
            if (currentVideoId != null && currentVideoTitle != null) {
                val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra(VideoPlayerActivity.EXTRA_VIDEO_TOPIC, currentVideoTitle)
                    putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl)
                }
                startActivity(intent)
            }
        }
    }
}
