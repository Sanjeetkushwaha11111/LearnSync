package com.example.reminder.quiz

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class QuizManager(private val context: Context) {
    private var questions: List<QuizQuestion> = emptyList()
    private var currentQuestionIndex = 0
    
    init {
        loadQuestions()
    }
    
    private fun loadQuestions() {
        try {
            context.assets.open("quiz.json").use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<Map<String, List<QuizQuestion>>>() {}.type
                val data: Map<String, List<QuizQuestion>> = Gson().fromJson(jsonString, type)
                questions = data["questions"] ?: emptyList()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    fun getQuestionForTime(timeInSeconds: Int): QuizQuestion? {
        return questions.firstOrNull { it.timeInSeconds == timeInSeconds }
    }
    
    fun checkAnswer(question: QuizQuestion, selectedAnswer: Int): Boolean {
        return question.correctAnswer == selectedAnswer
    }
}
