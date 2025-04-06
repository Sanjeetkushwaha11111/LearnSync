package com.example.learnsync

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

class ProfanityFilter(private val context: Context) {
    private val profaneWords = mutableSetOf<String>()

    init {
        loadProfaneWords()
    }

    private fun loadProfaneWords() {
        val gson = Gson()
        
        try {
            // Load from all profanity files
            listOf("eng_prof.json", "hindi.json", "hindi_prof.json").forEach { filename ->
                context.assets.open(filename).use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val type = object : TypeToken<Map<String, Int>>() {}.type
                    val wordMap: Map<String, Int> = gson.fromJson(jsonString, type)
                    profaneWords.addAll(wordMap.filter { it.value == 1 }.keys)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class FilterResult(
        val filteredText: String,
        val containsProfanity: Boolean
    )

    fun filterText(text: String): FilterResult {
        var filteredText = text.lowercase()
        var hasProfanity = false
        
        profaneWords.forEach { word ->
            val regex = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(filteredText)) {
                hasProfanity = true
            }
            filteredText = filteredText.replace(regex, "*".repeat(word.length))
        }
        
        return FilterResult(filteredText, hasProfanity)
    }
}
