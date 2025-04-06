package com.example.learnsync.utils

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApi {
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("id") videoId: String,
        @Query("part") part: String = "snippet",
        @Query("key") apiKey: String
    ): Response<VideoResponse>
}

data class VideoResponse(
    @SerializedName("items") val items: List<VideoItem>
)

data class VideoItem(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: VideoSnippet
)

data class VideoSnippet(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("thumbnails") val thumbnails: Thumbnails
)

data class Thumbnails(
    @SerializedName("default") val default: Thumbnail,
    @SerializedName("medium") val medium: Thumbnail,
    @SerializedName("high") val high: Thumbnail
)

data class Thumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

class YouTubeService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(YouTubeApi::class.java)

    suspend fun getVideoDetails(videoId: String, apiKey: String): VideoItem? {
        return try {
            val response = api.getVideoDetails(videoId, "snippet", apiKey)
            if (response.isSuccessful && response.body()?.items?.isNotEmpty() == true) {
                response.body()?.items?.first()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun extractVideoId(url: String): String? {
            val patterns = listOf(
                "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*",
                "(?<=shorts/)[^#\\&\\?\\n]*"
            )

            return patterns.firstNotNullOfOrNull { pattern ->
                Regex(pattern).find(url)?.value
            }
        }
    }
}
