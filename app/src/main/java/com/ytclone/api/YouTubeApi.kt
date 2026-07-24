package com.ytclone.api

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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

    private const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val BASE_URL = "https://www.youtube.com/youtubei/v1"
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var visitorData: String = ""

    init {
        fetchVisitorData()
    }

    private fun fetchVisitorData() {
        try {
            val request = Request.Builder()
                .url("https://www.youtube.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val regex = Regex(""""VISITOR_DATA":"([^"]+)"""")
            regex.find(body)?.let {
                visitorData = it.groupValues[1]
            }
        } catch (e: Exception) {
            visitorData = ""
        }
    }

    private fun getContext(): JsonObject {
        val clientObj = JsonObject().apply {
            addProperty("clientName", "WEB")
            addProperty("clientVersion", "2.20260715.04.00")
            addProperty("hl", "ar")
            addProperty("gl", "EG")
            if (visitorData.isNotEmpty()) {
                addProperty("visitorData", visitorData)
            }
        }
        val context = JsonObject()
        context.add("client", clientObj)
        return context
    }

    private fun buildRequest(endpoint: String, body: JsonObject, oauthToken: String? = null): Request.Builder {
        val request = Request.Builder()
            .url("$BASE_URL/$endpoint?key=$API_KEY&prettyPrint=false")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
            .header("Origin", "https://www.youtube.com")
            .header("Referer", "https://www.youtube.com/")
            .header("X-Youtube-Client-Name", "1")
            .header("X-Youtube-Client-Version", "2.20260715.04.00")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (!oauthToken.isNullOrEmpty()) {
            request.header("Authorization", "Bearer $oauthToken")
        }
        return request
    }

    suspend fun search(query: String, oauthToken: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                add("context", getContext())
                addProperty("query", query)
                addProperty("params", "none")
            }
            val request = buildRequest("search", body, oauthToken).build()
            val response = client.newCall(request).execute()
            val json = JsonParser.parseString(response.body?.string()).asJsonObject

            val results = mutableListOf<VideoItem>()
            val contents = json.getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnSearchResultsRenderer")
                ?.getAsJsonObject("primaryContents")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return@withContext results

            for (section in contents) {
                val items = section.asJsonObject
                    ?.getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents") ?: continue
                for (item in items) {
                    val videoRenderer = item.asJsonObject?.getAsJsonObject("videoRenderer") ?: continue
                    results.add(parseVideoRenderer(videoRenderer))
                }
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getHomeFeed(categoryParams: String? = null, oauthToken: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                add("context", getContext())
                if (categoryParams != null) {
                    addProperty("params", categoryParams)
                } else {
                    addProperty("browseId", "FEtrending")
                }
            }
            val request = buildRequest("browse", body, oauthToken).build()
            val response = client.newCall(request).execute()
            val json = JsonParser.parseString(response.body?.string()).asJsonObject

            val results = mutableListOf<VideoItem>()
            parseContents(json, results)
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseContents(json: JsonObject, results: MutableList<VideoItem>) {
        try {
            val contents = json.getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return

            for (tab in contents) {
                val tabRenderer = tab.asJsonObject?.getAsJsonObject("tabRenderer") ?: continue
                val tabContent = tabRenderer.getAsJsonObject("content") ?: continue

                val richGrid = tabContent.getAsJsonObject("richGridRenderer")
                if (richGrid != null) {
                    val richItems = richGrid.getAsJsonArray("contents") ?: return
                    for (richItem in richItems) {
                        val richSection = richItem.asJsonObject?.getAsJsonObject("richSectionRenderer") ?: continue
                        val content = richSection.getAsJsonObject("content") ?: continue
                        val richItemRenderer = content.getAsJsonObject("richItemRenderer") ?: continue
                        val videoContent = richItemRenderer.getAsJsonObject("content") ?: continue
                        val videoRenderer = videoContent.getAsJsonObject("videoRenderer")
                        if (videoRenderer != null) {
                            results.add(parseVideoRenderer(videoRenderer))
                        }
                    }
                }

                val sectionList = tabContent.getAsJsonObject("sectionListRenderer")
                if (sectionList != null) {
                    val sections = sectionList.getAsJsonArray("contents") ?: return
                    for (section in sections) {
                        val sectionRenderer = section.asJsonObject?.getAsJsonObject("sectionListRenderer")
                            ?: section.asJsonObject?.getAsJsonObject("itemSectionRenderer")
                            ?: continue
                        val sectionContents = sectionRenderer.getAsJsonArray("contents") ?: continue
                        for (item in sectionContents) {
                            parseItemForVideos(item.asJsonObject, results)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseItemForVideos(item: JsonObject?, results: MutableList<VideoItem>) {
        item ?: return
        val videoRenderer = item.getAsJsonObject("videoRenderer")
        if (videoRenderer != null) {
            results.add(parseVideoRenderer(videoRenderer))
            return
        }
        val shelfRenderer = item.getAsJsonObject("shelfRenderer")
        if (shelfRenderer != null) {
            val content = shelfRenderer.getAsJsonObject("content")
            val items = content?.getAsJsonArray("expandedShelfContentsRenderer")
                ?.getAsJsonArray("items")
            items?.forEach { parseItemForVideos(it.asJsonObject, results) }
        }
        val horizontalListRenderer = item.getAsJsonObject("horizontalListRenderer")
        if (horizontalListRenderer != null) {
            val items = horizontalListRenderer.getAsJsonArray("items")
            items?.forEach { parseItemForVideos(it.asJsonObject, results) }
        }
    }

    private fun parseVideoRenderer(video: JsonObject): VideoItem {
        val videoId = video.get("videoId")?.asString ?: ""
        val title = video.getAsJsonObject("title")
            ?.getAsJsonArray("runs")
            ?.firstOrNull()?.asJsonObject?.get("text")?.asString ?: ""
        val channelName = video.getAsJsonObject("ownerText")
            ?.getAsJsonArray("runs")
            ?.firstOrNull()?.asJsonObject?.get("text")?.asString ?: ""
        val channelId = video.getAsJsonObject("ownerText")
            ?.getAsJsonArray("runs")
            ?.firstOrNull()?.asJsonObject?.getAsJsonObject("navigationEndpoint")
            ?.getAsJsonObject("browseEndpoint")
            ?.get("browseId")?.asString ?: ""
        val channelAvatar = video.getAsJsonObject("thumbnail")
            ?.getAsJsonArray("thumbnails")
            ?.lastOrNull()?.asJsonObject?.get("url")?.asString
        val thumbnail = video.getAsJsonObject("thumbnail")
            ?.getAsJsonArray("thumbnails")
            ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
        val duration = video.getAsJsonObject("lengthText")
            ?.get("simpleText")?.asString
        val viewCount = video.getAsJsonObject("viewCountText")
            ?.get("simpleText")?.asString ?: ""
        val publishedTime = video.getAsJsonObject("publishedTimeText")
            ?.get("simpleText")?.asString ?: ""

        return VideoItem(
            videoId = videoId,
            title = title,
            channelName = channelName,
            channelId = channelId,
            channelAvatarUrl = channelAvatar,
            thumbnailUrl = thumbnail,
            duration = duration,
            viewCount = viewCount,
            publishedTime = publishedTime
        )
    }

    suspend fun getSubscriptions(oauthToken: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                add("context", getContext())
                addProperty("browseId", "FEsubscriptions")
            }
            val request = buildRequest("browse", body, oauthToken).build()
            val response = client.newCall(request).execute()
            val json = JsonParser.parseString(response.body?.string()).asJsonObject

            val results = mutableListOf<VideoItem>()
            parseContents(json, results)
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
