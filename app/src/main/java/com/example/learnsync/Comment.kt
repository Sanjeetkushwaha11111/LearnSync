package com.example.learnsync

data class Comment(
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
