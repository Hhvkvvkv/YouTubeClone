package com.ytclone.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ytclone.models.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object YouTubeApi {

    private const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val BASE_URL = "https://www.googleapis.com/youtube/v3"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .header("User-Agent", "Android-YouTubeClone/1.0")
            .build()
    }

    suspend fun search(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/search?part=snippet&q=${query}&maxResults=20&key=$API_KEY&type=video"
            val response = client.newCall(getRequest(url)).execute()
            val json = JsonParser.parseString(response.body?.string()).asJsonObject

            val results = mutableListOf<VideoItem>()
            val items = json.getAsJsonArray("items") ?: return@withContext results

            for (item in items) {
                val videoId = item.getAsJsonObject("id")?.get("videoId")?.asString ?: continue
                val snippet = item.getAsJsonObject("snippet") ?: continue
                val title = snippet.get("title")?.asString ?: ""
                val channelName = snippet.getAsJsonObject("snippet")?.get("channelTitle")?.asString ?: ""
                val thumbnail = snippet.getAsJsonObject("thumbnails")?.getAsJsonObject("high")?.get("url")?.asString ?: ""
                val channelAvatar = snippet.getAsJsonObject("thumbnails")?.getAsJsonObject("default")?.get("url")?.asString ?: ""

                results.add(VideoItem(
                    videoId = videoId,
                    title = title,
                    channelName = channelName,
                    channelId = "",
                    channelAvatarUrl = channelAvatar,
                    thumbnailUrl = thumbnail,
                    duration = null,
                    viewCount = "",
                    publishedTime = ""
                ))
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getHomeFeed(query: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            // استخدام search endpoint مع معلمة trending
            val searchQuery = if (query.isNullOrEmpty()) "trending" else query
            val url = "$BASE_URL/search?part=snippet&q=$searchQuery&maxResults=20&key=$API_KEY&type=video&order=viewCount"
            val response = client.newCall(getRequest(url)).execute()
            val json = JsonParser.parseString(response.body?.string()).asJsonObject

            val results = mutableListOf<VideoItem>()
            val items = json.getAsJsonArray("items") ?: return@withContext results

            for (item in items) {
                val videoId = item.getAsJsonObject("id")?.get("videoId")?.asString ?: continue
                val snippet = item.getAsJsonObject("snippet") ?: continue
                val title = snippet.get("title")?.asString ?: ""
                val channelName = snippet.get("channelTitle")?.asString ?: ""
                val thumbnail = snippet.getAsJsonObject("thumbnails")?.getAsJsonObject("high")?.get("url")?.asString ?: ""
                val channelAvatar = snippet.getAsJsonObject("thumbnails")?.getAsJsonObject("default")?.get("url")?.asString ?: ""

                results.add(VideoItem(
                    videoId = videoId,
                    title = title,
                    channelName = channelName,
                    channelId = "",
                    channelAvatarUrl = channelAvatar,
                    thumbnailUrl = thumbnail,
                    duration = null,
                    viewCount = "",
                    publishedTime = ""
                ))
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getSubscriptions(oauthToken: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        emptyList() // يحتاج تسجيل الدخول
    }
}
