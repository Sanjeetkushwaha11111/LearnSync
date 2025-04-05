package com.example.reminder

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class VideoPlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var etComment: EditText
    private lateinit var btnSend: Button
    private lateinit var rvComments: RecyclerView
    private lateinit var profanityFilter: ProfanityFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        profanityFilter = ProfanityFilter(this)
        playerView = findViewById(R.id.playerView)
        etComment = findViewById(R.id.etComment)
        btnSend = findViewById(R.id.btnSend)
        rvComments = findViewById(R.id.rvComments)

        setupPlayer()
        setupComments()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Example URL - replace with your video URL
        val mediaItem = MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
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
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
