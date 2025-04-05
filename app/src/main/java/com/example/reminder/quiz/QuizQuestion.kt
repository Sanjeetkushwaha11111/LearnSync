package com.example.reminder.quiz

data class QuizQuestion(
    val timeInSeconds: Int,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int
)
