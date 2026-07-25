package com.ytclone.api

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

    private fun buildUrl(endpoint: String, vararg params: Pair<String, String>): String {
        val allParams = mutableListOf("key" to API_KEY)
        allParams.addAll(params)
        val query = allParams.joinToString("&") { "${it.first}=${java.net.URLEncoder.encode(it.second, "UTF-8")}" }
        return "$BASE_URL/$endpoint?$query"
    }

    private fun executeRequest(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "VideoPlus/1.0")
                .build()
            client.newCall(request).execute().body?.string()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseVideos(jsonStr: String?): List<VideoItem> {
        if (jsonStr == null) return emptyList()
        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject
            val items = json.getAsJsonArray("items") ?: return emptyList()
            val results = mutableListOf<VideoItem>()

            for (item in items) {
                val id = item.getAsJsonObject("id") ?: continue
                val videoId = id.get("videoId")?.asString ?: continue
                val snippet = item.getAsJsonObject("snippet") ?: continue

                val title = snippet.get("title")?.asString ?: ""
                val channelName = snippet.get("channelTitle")?.asString ?: ""
                val channelId = snippet.get("channelId")?.asString ?: ""
                val thumbnails = snippet.getAsJsonObject("thumbnails")
                val highThumb = thumbnails?.getAsJsonObject("high")?.get("url")?.asString
                    ?: thumbnails?.getAsJsonObject("medium")?.get("url")?.asString
                    ?: thumbnails?.getAsJsonObject("default")?.get("url")?.asString ?: ""
                val defaultThumb = thumbnails?.getAsJsonObject("default")?.get("url")?.asString ?: ""
                val publishedAt = snippet.get("publishedAt")?.asString ?: ""
                val description = snippet.get("description")?.asString ?: ""

                results.add(VideoItem(
                    videoId = videoId,
                    title = title,
                    channelName = channelName,
                    channelId = channelId,
                    channelAvatarUrl = defaultThumb,
                    thumbnailUrl = highThumb,
                    duration = null,
                    viewCount = "",
                    publishedTime = publishedAt
                ))
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun search(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        val url = buildUrl("search",
            "part" to "snippet",
            "q" to query,
            "maxResults" to "20",
            "type" to "video"
        )
        parseVideos(executeRequest(url))
    }

    suspend fun getHomeFeed(query: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        val searchQuery = query ?: "trending music videos"
        val url = buildUrl("search",
            "part" to "snippet",
            "q" to searchQuery,
            "maxResults" to "20",
            "type" to "video",
            "order" to "viewCount"
        )
        parseVideos(executeRequest(url))
    }

    suspend fun getSubscriptions(oauthToken: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        emptyList()
    }
}
