package com.example.learnsync.quiz

import android.content.Context
import com.example.learnsync.quiz.api.OpenAiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizManager(private val context: Context) {
    private var questions: List<QuizQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private val openAiService = OpenAiService()
    private var currentTopic: String? = null

    fun loadQuestionsForTopic(topic: String, scope: CoroutineScope, onQuestionsLoaded: () -> Unit) {
        if (topic == currentTopic && questions.isNotEmpty()) {
            onQuestionsLoaded()
            return
        }

        scope.launch {
            try {
                questions = withContext(Dispatchers.IO) {
                    try {
                        val apiQuestions = openAiService.generateQuizQuestions(topic)
                        if (apiQuestions.isNotEmpty()) {
                            apiQuestions
                        } else {
                            loadFallbackQuestions()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("QuizManager", "API error: ${e.message}")
                        loadFallbackQuestions()
                    }
                }
                currentTopic = topic
                onQuestionsLoaded()
            } catch (e: Exception) {
                e.printStackTrace()
                questions = emptyList()
                onQuestionsLoaded()
            }
        }
    }

    private fun loadFallbackQuestions(): List<QuizQuestion> {
        return try {
            context.assets.open("fallback_quiz.json").use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<Map<String, List<QuizQuestion>>>() {}.type
                val data: Map<String, List<QuizQuestion>> = Gson().fromJson(jsonString, type)
                data["questions"] ?: emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("QuizManager", "Error loading fallback questions: ${e.message}")
            emptyList()
        }
    }
    
    fun getQuestionForTime(timeInSeconds: Int): QuizQuestion? {
        // Find a question that should be shown within a 5-second window
        return questions.firstOrNull { question ->
            val targetTime = question.timeInSeconds
            timeInSeconds in targetTime..(targetTime + 5)
        }
    }
    
    fun checkAnswer(question: QuizQuestion, selectedAnswer: Int): Boolean {
        return question.correctAnswer == selectedAnswer
    }

    fun hasQuestionsLoaded(): Boolean = questions.isNotEmpty()
}
