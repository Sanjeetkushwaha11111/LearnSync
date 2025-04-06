package com.example.learnsync.quiz.api

import com.example.learnsync.quiz.QuizQuestion
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAiApi {
    @Headers(
        "Content-Type: application/json"
    )
    @POST("v1/chat/completions")
    suspend fun generateQuestions(@Body request: OpenAiRequest): OpenAiResponse
}

class OpenAiService {
    private val apiKey = "sk-0WC8zGwx_ueo3VuWKSDRYw"
    private val baseUrl = "https://api.rabbithole.cred.club/"
    private val api: OpenAiApi

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()


        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(OpenAiApi::class.java)
    }

    suspend fun generateQuizQuestions(topic: String): List<QuizQuestion> {
        return try {
            val request = OpenAiRequest(
                messages = listOf(
                    Message(
                        role = "user",
                        content = createQuizPrompt(QuizGenerationRequest(topic))
                    )
                )
            )

            val response = api.generateQuestions(request)
            val jsonResponse = response.choices.firstOrNull()?.message?.content ?: return emptyList()
            
            try {
                val jsonElement = JsonParser.parseString(jsonResponse)
                val questionsArray = jsonElement.asJsonObject.getAsJsonArray("questions")
                val gson = Gson()
                questionsArray.map { element -> 
                    gson.fromJson<QuizQuestion>(element, QuizQuestion::class.java)
                }
            } catch (e: Exception) {
                android.util.Log.e("OpenAiService", "Error parsing response: ${e.message}")
                android.util.Log.e("OpenAiService", "Response content: $jsonResponse")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("OpenAiService", "Error making API request: ${e.message}")
            android.util.Log.e("OpenAiService", "Stack trace: ${android.util.Log.getStackTraceString(e)}")
            emptyList()
        }
    }
}
