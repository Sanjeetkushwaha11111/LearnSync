package com.example.learnsync

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.example.learnsync.quiz.QuizManager
import com.example.learnsync.quiz.QuizBottomSheetDialog
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideoPlayerActivity : AppCompatActivity(), Player.Listener {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var youtubePlayerView: YouTubePlayerView
    private var youTubePlayer: YouTubePlayer? = null
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var etComment: EditText
    private lateinit var btnSend: Button
    private lateinit var rvComments: RecyclerView
    private lateinit var tvVideoTitle: TextView
    private lateinit var tvStartChat: TextView
    private lateinit var profanityFilter: ProfanityFilter
    private lateinit var quizManager: QuizManager
    private val handler = Handler(Looper.getMainLooper())
    private var lastQuizTime = 0L
    private var currentVideoTopic: String = ""
    private var isQuizEnabled = false
    private var isYouTubeVideo = false

    companion object {
        const val EXTRA_VIDEO_TOPIC = "video_topic"
        const val EXTRA_VIDEO_URL = "video_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        currentVideoTopic = intent.getStringExtra(EXTRA_VIDEO_TOPIC) ?: "Unknown Topic"
        
        profanityFilter = ProfanityFilter(this)
        quizManager = QuizManager(this)
        playerView = findViewById(R.id.playerView)
        youtubePlayerView = findViewById(R.id.youtubePlayerView)
        lifecycle.addObserver(youtubePlayerView)
        etComment = findViewById(R.id.etComment)
        btnSend = findViewById(R.id.btnSend)
        rvComments = findViewById(R.id.rvComments)
        tvVideoTitle = findViewById(R.id.tvVideoTitle)
        tvStartChat = findViewById(R.id.tvStartChat)

        setupChatVisibility()
        setupPlayer()
        setupComments()
        loadQuizQuestions()
    }

    private fun loadQuizQuestions() {
        quizManager.loadQuestionsForTopic(currentVideoTopic, lifecycleScope) {
            isQuizEnabled = quizManager.hasQuestionsLoaded()
            if (!isQuizEnabled) {
                Toast.makeText(this, "Failed to load quiz questions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkForQuiz(currentPosition: Long) {
        if (isQuizEnabled) {
            val question = quizManager.getQuestionForTime(currentPosition.toInt())
            if (question != null && currentPosition - lastQuizTime >= 10) {
                lastQuizTime = currentPosition
                QuizBottomSheetDialog.newInstance(question) { isCorrect ->
                    val message = if (isCorrect) "Correct answer!" else "Wrong answer!"
                    Toast.makeText(this@VideoPlayerActivity, message, Toast.LENGTH_SHORT).show()
                }.show(supportFragmentManager, "quiz")
            }
        }
    }

    private val checkQuizRunnable = object : Runnable {
        override fun run() {
            if (!isYouTubeVideo) {
                player?.let { exoPlayer ->
                    val currentPosition = exoPlayer.currentPosition / 1000 // Convert to seconds
                    checkForQuiz(currentPosition)
                }
            }
            handler.postDelayed(this, 1000) // Check every second
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(this@VideoPlayerActivity)
        }
        playerView.player = player

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) 
            ?: "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TOPIC) ?: "Big Buck Bunny"
        tvVideoTitle.text = videoTitle

        if (videoUrl.contains("youtube.com")) {
            isYouTubeVideo = true
            playerView.visibility = View.GONE
            youtubePlayerView.visibility = View.VISIBLE
            
            // Extract video ID from YouTube URL
            val videoId = videoUrl.split("v=")[1]
            youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(player: YouTubePlayer) {
                    youTubePlayer = player
                    player.loadVideo(videoId, 0f)
                }

                override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                    checkForQuiz(second.toLong())
                }
            })
        } else {
            isYouTubeVideo = false
            playerView.visibility = View.VISIBLE
            youtubePlayerView.visibility = View.GONE
            
            val mediaItem = MediaItem.fromUri(videoUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
        }
        
        handler.post(checkQuizRunnable)
    }

    private fun setupChatVisibility() {
        tvStartChat.setOnClickListener {
            tvStartChat.visibility = View.GONE
            rvComments.visibility = View.VISIBLE
            rvComments.alpha = 0f
            rvComments.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }

        etComment.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up))
            }
        }
    }

    private fun setupComments() {
        commentAdapter = CommentAdapter()
        rvComments.apply {
            layoutManager = LinearLayoutManager(this@VideoPlayerActivity)
            adapter = commentAdapter
        }

        btnSend.setOnClickListener {
            val commentText = etComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                val filterResult = profanityFilter.filterText(commentText)
                if (filterResult.containsProfanity) {
                    Toast.makeText(this, "Warning: Your message contains inappropriate words", Toast.LENGTH_SHORT).show()
                }
                commentAdapter.addComment(Comment(filterResult.filteredText))
                etComment.text.clear()
                rvComments.smoothScrollToPosition(commentAdapter.itemCount - 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isYouTubeVideo) {
            player?.playWhenReady = true
        }
        handler.post(checkQuizRunnable)
    }

    override fun onPause() {
        super.onPause()
        if (!isYouTubeVideo) {
            player?.playWhenReady = false
        }
        handler.removeCallbacks(checkQuizRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkQuizRunnable)
        if (!isYouTubeVideo) {
            player?.release()
            player = null
        }
    }
}
