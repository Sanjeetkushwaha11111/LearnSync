package com.example.reminder

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.example.reminder.quiz.QuizManager
import com.example.reminder.quiz.QuizBottomSheetDialog

class VideoPlayerActivity : AppCompatActivity(), Player.Listener {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var etComment: EditText
    private lateinit var btnSend: Button
    private lateinit var rvComments: RecyclerView
    private lateinit var profanityFilter: ProfanityFilter
    private lateinit var quizManager: QuizManager
    private val handler = Handler(Looper.getMainLooper())
    private var lastQuizTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        profanityFilter = ProfanityFilter(this)
        quizManager = QuizManager(this)
        playerView = findViewById(R.id.playerView)
        etComment = findViewById(R.id.etComment)
        btnSend = findViewById(R.id.btnSend)
        rvComments = findViewById(R.id.rvComments)

        setupPlayer()
        setupComments()
    }

    private val checkQuizRunnable = object : Runnable {
        override fun run() {
            player?.let { exoPlayer ->
                val currentPosition = exoPlayer.currentPosition / 1000 // Convert to seconds
                val question = quizManager.getQuestionForTime(currentPosition.toInt())
                
                if (question != null && currentPosition - lastQuizTime >= 10) { // Show quiz every 10 seconds
                    lastQuizTime = currentPosition
                    QuizBottomSheetDialog.newInstance(question) { isCorrect ->
                        val message = if (isCorrect) "Correct answer!" else "Wrong answer!"
                        Toast.makeText(this@VideoPlayerActivity, message, Toast.LENGTH_SHORT).show()
                    }.show(supportFragmentManager, "quiz")
                }
                
                handler.postDelayed(this, 1000) // Check every second
            }
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(this@VideoPlayerActivity)
        }
        playerView.player = player

        // Example URL - replace with your video URL
        val mediaItem = MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
        
        handler.post(checkQuizRunnable)
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
        player?.playWhenReady = true
        handler.post(checkQuizRunnable)
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
        handler.removeCallbacks(checkQuizRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkQuizRunnable)
        player?.release()
        player = null
    }
}
