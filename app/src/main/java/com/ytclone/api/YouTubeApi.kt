package com.ytclone.api

import com.google.gson.JsonParser
import com.ytclone.models.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object YouTubeApi {

    private const val INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val BASE_URL = "https://www.youtube.com/youtubei/v1"
    private const val CLIENT_VERSION = "2.20240701.00.00"
    private const val CLIENT_NAME = "WEB"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    // سجل الدخول - لحفظ cookies بعد تسجيل الدخول عبر WebView
    var authCookies: String = ""

    private fun buildContext(): String {
        return """
        {
            "client": {
                "clientName": "$CLIENT_NAME",
                "clientVersion": "$CLIENT_VERSION",
                "hl": "ar",
                "gl": "EG"
            }
        }
        """.trimIndent()
    }

    private fun executeInnertube(endpoint: String, bodyJson: String): String? {
        return try {
            val url = "$BASE_URL/$endpoint?key=$INNERTUBE_KEY"
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Origin", "https://www.youtube.com")
                .header("Content-Type", "application/json")
                .post(bodyJson.toRequestBody(jsonMediaType))

            if (authCookies.isNotEmpty()) {
                requestBuilder.header("Cookie", authCookies)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            response.body?.string()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSearchResults(jsonStr: String?): List<VideoItem> {
        if (jsonStr == null) return emptyList()
        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject
            val contents = json.getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnSearchResultsRenderer")
                ?.getAsJsonObject("primaryContents")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            val results = mutableListOf<VideoItem>()

            for (section in contents) {
                val items = section.asJsonObject
                    .getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents") ?: continue

                for (item in items) {
                    val videoRenderer = item.asJsonObject.getAsJsonObject("videoRenderer") ?: continue
                    val videoId = videoRenderer.get("videoId")?.asString ?: continue

                    // Title
                    val title = extractText(videoRenderer, "title")

                    // Channel
                    val channelName = extractRunsText(videoRenderer, "ownerText")
                    val channelId = videoRenderer.get("channelId")?.asString ?: ""

                    // Duration
                    val duration = extractSimpleText(videoRenderer, "lengthText")

                    // Views
                    val views = extractSimpleText(videoRenderer, "viewCountText") ?: ""

                    // Published time
                    val publishedTime = extractSimpleText(videoRenderer, "publishedTimeText") ?: ""

                    // Thumbnail
                    val thumbnails = videoRenderer.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")
                    val thumbUrl = if (thumbnails != null && thumbnails.size() > 0) {
                        thumbnails[thumbnails.size() - 1].asJsonObject.get("url")?.asString ?: ""
                    } else ""

                    // Channel avatar
                    val channelThumbnails = videoRenderer.getAsJsonObject("channelThumbnail")
                        ?.getAsJsonArray("thumbnails")
                    val avatarUrl = if (channelThumbnails != null && channelThumbnails.size() > 0) {
                        channelThumbnails[channelThumbnails.size() - 1].asJsonObject.get("url")?.asString ?: ""
                    } else ""

                    results.add(VideoItem(
                        videoId = videoId,
                        title = title,
                        channelName = channelName,
                        channelId = channelId,
                        channelAvatarUrl = avatarUrl,
                        thumbnailUrl = thumbUrl,
                        duration = duration,
                        viewCount = views,
                        publishedTime = publishedTime
                    ))
                }
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun extractText(obj: com.google.gson.JsonObject, key: String): String {
        val runs = obj.getAsJsonObject(key)?.getAsJsonArray("runs")
        if (runs != null && runs.size() > 0) {
            return runs[0].asJsonObject.get("text")?.asString ?: ""
        }
        return obj.getAsJsonObject(key)?.get("simpleText")?.asString ?: ""
    }

    private fun extractRunsText(obj: com.google.gson.JsonObject, key: String): String {
        val runs = obj.getAsJsonObject(key)?.getAsJsonArray("runs")
        if (runs != null && runs.size() > 0) {
            return runs[0].asJsonObject.get("text")?.asString ?: ""
        }
        return ""
    }

    private fun extractSimpleText(obj: com.google.gson.JsonObject, key: String): String? {
        return obj.getAsJsonObject(key)?.get("simpleText")?.asString
    }

    suspend fun search(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        val body = """{"context":${buildContext()},"query":"$query"}"""
        val result = executeInnertube("search", body)
        parseSearchResults(result)
    }

    suspend fun getHomeFeed(category: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        val searchQuery = when (category) {
            "music" -> "new music 2024"
            "podcast" -> "podcast"
            "mixes" -> "music mix"
            "live" -> "live stream"
            "gaming" -> "gaming"
            "news" -> "news"
            "sports" -> "sports"
            "learning" -> "learning"
            "fashion" -> "fashion"
            else -> "trending"
        }
        val body = """{"context":${buildContext()},"query":"$searchQuery"}"""
        val result = executeInnertube("search", body)
        parseSearchResults(result)
    }

    suspend fun getShorts(): List<VideoItem> = withContext(Dispatchers.IO) {
        val body = """{"context":${buildContext()},"query":"#shorts"}"""
        val result = executeInnertube("search", body)
        parseSearchResults(result)
    }

    suspend fun getSubscriptions(): List<VideoItem> = withContext(Dispatchers.IO) {
        // بدون تسجيل دخول، نعرض فيديوهات عامة
        getHomeFeed("trending")
    }
}
