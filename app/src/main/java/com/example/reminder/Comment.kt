package com.example.reminder

data class Comment(
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
