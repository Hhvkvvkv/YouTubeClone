package com.ytclone.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ytclone.models.VideoItem
import com.ytclone.models.ChannelItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object YouTubeApi {

    private const val INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val BASE_URL = "https://www.youtube.com/youtubei/v1"
    private const val CLIENT_VERSION = "2.20240701.00.00"

    var authCookies: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    // حساب SAPISID hash لتوثيق طلبات الـ InnerTube
    private fun getSapisidHash(): String? {
        if (authCookies.isEmpty()) return null
        val sapisid = authCookies.split(";").firstOrNull { it.trim().startsWith("SAPISID") }
            ?.split("=")?.get(1)?.trim() ?: return null
        val timestamp = System.currentTimeMillis() / 1000
        val hashInput = "$timestamp $sapisid"
        val hash = MessageDigest.getInstance("SHA-1").digest(hashInput.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "$timestamp_$hash"
    }

    private fun buildContext(): String {
        return """{"client":{"clientName":"WEB","clientVersion":"$CLIENT_VERSION","hl":"ar","gl":"EG"}}"""
    }

    private fun executeRequest(endpoint: String, bodyJson: String): String? {
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
                val hash = getSapisidHash()
                if (hash != null) {
                    requestBuilder.header("Authorization", "SAPISIDHASH $hash")
                    requestBuilder.header("X-Origin", "https://www.youtube.com")
                }
            }

            val response = client.newCall(requestBuilder.build()).execute()
            response.body?.string()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ========== SEARCH (بحث عام) ==========
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
                    val vr = item.asJsonObject.getAsJsonObject("videoRenderer") ?: continue
                    val v = parseVideoRenderer(vr) ?: continue
                    results.add(v)
                }
            }
            results
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    private fun parseVideoRenderer(vr: JsonObject): VideoItem? {
        return try {
            val videoId = vr.get("videoId")?.asString ?: return null
            val title = extractText(vr, "title")
            val channelName = extractRunsText(vr, "ownerText")
            val channelId = vr.get("channelId")?.asString ?: ""
            val duration = extractSimpleText(vr, "lengthText") ?: ""
            val views = extractSimpleText(vr, "viewCountText") ?: ""
            val pub = vr.getAsJsonObject("publishedTimeText")?.get("simpleText")?.asString ?: ""
            val thumbs = vr.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")
            val thumbUrl = if (thumbs != null && thumbs.size() > 0)
                thumbs[thumbs.size()-1].asJsonObject.get("url")?.asString ?: "" else ""
            val chThumbs = vr.getAsJsonObject("channelThumbnail")?.getAsJsonArray("thumbnails")
            val avatarUrl = if (chThumbs != null && chThumbs.size() > 0)
                chThumbs[chThumbs.size()-1].asJsonObject.get("url")?.asString ?: "" else ""

            VideoItem(videoId, title, channelName, channelId, avatarUrl, thumbUrl, duration, views, pub)
        } catch (e: Exception) { null }
    }

    // ========== BROWSE (تصفح: رئيسية، سجل، اشتراكات) ==========
    private fun parseBrowseVideos(jsonStr: String?): List<VideoItem> {
        if (jsonStr == null) return emptyList()
        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject
            val tabs = json.getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()
            if (tabs.size() == 0) return emptyList()

            val content = tabs[0].asJsonObject.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content") ?: return emptyList()

            val results = mutableListOf<VideoItem>()

            // richGridRenderer (الرئيسية)
            val richGrid = content.getAsJsonObject("richGridRenderer")
            if (richGrid != null) {
                val items = richGrid.getAsJsonArray("contents") ?: return@let
                for (item in items) {
                    // richSectionRenderer
                    val rsr = item.asJsonObject.getAsJsonObject("richSectionRenderer")
                    val sectionContent = rsr?.getAsJsonObject("content") ?: continue
                    val vsr = sectionContent.getAsJsonObject("richShelfRenderer")
                    val shelfContent = vsr?.getAsJsonObject("content")
                    val horizontalList = shelfContent?.getAsJsonObject("horizontalListRenderer")
                    val shelfItems = horizontalList?.getAsJsonArray("items")
                        ?: shelfContent?.getAsJsonObject("expandedShelfContentsRenderer")?.getAsJsonArray("items")

                    if (shelfItems != null) {
                        for (si in shelfItems) {
                            val vr = si.asJsonObject.getAsJsonObject("videoRenderer") ?: continue
                            val v = parseVideoRenderer(vr) ?: continue
                            results.add(v)
                        }
                    }
                }
                return@let
            }

            // sectionListRenderer (السجل، الاشتراكات)
            val slr = content.getAsJsonObject("sectionListRenderer")
            val sections = slr?.getAsJsonArray("contents") ?: return emptyList()
            for (section in sections) {
                val items = section.asJsonObject
                    .getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents") ?: continue
                for (item in items) {
                    val vr = item.asJsonObject.getAsJsonObject("videoRenderer") ?: continue
                    val v = parseVideoRenderer(vr) ?: continue
                    results.add(v)
                }
            }

            results
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // ========== استخراج القنوات المشترك بها من الاشتراكات ==========
    private fun parseSubscribedChannels(jsonStr: String?): List<ChannelItem> {
        if (jsonStr == null) return emptyList()
        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject
            val tabs = json.getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()
            if (tabs.size() == 0) return emptyList()

            val content = tabs[0].asJsonObject.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content") ?: return emptyList()

            val channels = mutableListOf<ChannelItem>()

            // السكشن الأول يحتوي على قائمة القنوات
            val sections = content.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            for (section in sections) {
                val items = section.asJsonObject
                    .getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents") ?: continue

                for (item in items) {
                    val sr = item.asJsonObject.getAsJsonObject("shelfRenderer")
                    val horizontalList = sr?.getAsJsonObject("content")
                        ?.getAsJsonObject("horizontalListRenderer")
                    val hItems = horizontalList?.getAsJsonArray("items") ?: continue

                    for (hi in hItems) {
                        val cr = hi.asJsonObject.getAsJsonObject("gridChannelRenderer")
                        if (cr != null) {
                            val channelId = cr.get("channelId")?.asString ?: continue
                            val title = extractRunsText(cr, "title")
                            val thumbs = cr.getAsJsonObject("thumbnail")
                                ?.getAsJsonArray("thumbnails")
                            val thumbUrl = if (thumbs != null && thumbs.size() > 0)
                                thumbs[thumbs.size()-1].asJsonObject.get("url")?.asString ?: "" else ""
                            val subCount = extractSimpleText(cr, "subscriberCountText") ?: ""
                            channels.add(ChannelItem(channelId, title, thumbUrl, subCount))
                        }
                    }
                }
            }
            channels
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    private fun extractText(obj: JsonObject, key: String): String {
        val runs = obj.getAsJsonObject(key)?.getAsJsonArray("runs")
        if (runs != null && runs.size() > 0)
            return runs[0].asJsonObject.get("text")?.asString ?: ""
        return obj.getAsJsonObject(key)?.get("simpleText")?.asString ?: ""
    }

    private fun extractRunsText(obj: JsonObject, key: String): String {
        return obj.getAsJsonObject(key)?.getAsJsonArray("runs")?.get(0)
            ?.asJsonObject?.get("text")?.asString ?: ""
    }

    private fun extractSimpleText(obj: JsonObject, key: String): String? {
        return obj.getAsJsonObject(key)?.get("simpleText")?.asString
    }

    // ========== الدوال العامة ==========

    // بحث
    suspend fun search(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        val result = executeRequest("search", """{"context":${buildContext()},"query":"$query"}""")
        parseSearchResults(result)
    }

    // الرئيسية - إذا كان مسجل دخول -> browse شخصي، وإلا -> بحث عادي
    suspend fun getHomeFeed(category: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        if (authCookies.isNotEmpty() && category == null) {
            // محتوى مخصص حسب الحساب
            val result = executeRequest("browse", """{"context":${buildContext()},"browseId":"FEwhat_to_watch"}""")
            val videos = parseBrowseVideos(result)
            if (videos.isNotEmpty()) return@withContext videos
        }

        // fallback: بحث عادي
        val searchQuery = when (category) {
            "music" -> "music"
            "podcast" -> "podcast"
            "mixes" -> "music mix"
            "live" -> "live stream"
            "gaming" -> "gaming"
            "news" -> "news"
            "sports" -> "sports"
            "learning" -> "learning"
            "fashion" -> "fashion"
            else -> if (category != null) category else "trending"
        }
        val result = executeRequest("search", """{"context":${buildContext()},"query":"$searchQuery"}""")
        parseSearchResults(result)
    }

    // السجل
    suspend fun getHistory(): List<VideoItem> = withContext(Dispatchers.IO) {
        val result = executeRequest("browse", """{"context":${buildContext()},"browseId":"FEhistory"}""")
        parseBrowseVideos(result)
    }

    // شورتس
    suspend fun getShorts(): List<VideoItem> = withContext(Dispatchers.IO) {
        val result = executeRequest("search", """{"context":${buildContext()},"query":"#shorts"}""")
        parseSearchResults(result)
    }

    // الاشتراكات - فيديوهات
    suspend fun getSubscriptions(): List<VideoItem> = withContext(Dispatchers.IO) {
        val result = executeRequest("browse", """{"context":${buildContext()},"browseId":"FEsubscriptions"}""")
        val videos = parseBrowseVideos(result)
        if (videos.isNotEmpty()) return@withContext videos
        // fallback
        getHomeFeed()
    }

    // القنوات المشترك بها
    suspend fun getSubscribedChannels(): List<ChannelItem> = withContext(Dispatchers.IO) {
        val result = executeRequest("browse", """{"context":${buildContext()},"browseId":"FEsubscriptions"}""")
        parseSubscribedChannels(result)
    }
}
