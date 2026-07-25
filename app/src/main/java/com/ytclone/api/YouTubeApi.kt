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

    private fun getSapisidHash(): String? {
        if (authCookies.isEmpty()) return null
        // إيجاد كوكيز SAPISID
        val sapisid = authCookies.split(";").firstOrNull { it.trim().startsWith("SAPISID=") }
            ?.substringAfter("SAPISID=")?.trim() ?: return null
        if (sapisid.isEmpty()) return null
        val timestamp = System.currentTimeMillis() / 1000
        val hashInput = "$timestamp $sapisid"
        val hash = hashInput.hashCode().toString(16)  // أبسط hash مؤقت
        return "$timestamp $hash"
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
                requestBuilder.header("X-Origin", "https://www.youtube.com")
                // المحاولة مع SAPISID hash إذا كان موجود
                val hash = getSapisidHash()
                if (hash != null) {
                    requestBuilder.header("Authorization", "SAPISIDHASH $hash")
                }
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
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
                thumbs[thumbs.size() - 1].asJsonObject.get("url")?.asString ?: "" else ""
            val chThumbs = vr.getAsJsonObject("channelThumbnail")?.getAsJsonArray("thumbnails")
            val avatarUrl = if (chThumbs != null && chThumbs.size() > 0)
                chThumbs[chThumbs.size() - 1].asJsonObject.get("url")?.asString ?: "" else ""

            VideoItem(videoId, title, channelName, channelId, avatarUrl, thumbUrl, duration, views, pub)
        } catch (e: Exception) { null }
    }

    // ========== BROWSE (تصفح) - يعمل بدون auth للأقسام العامة ==========
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

            // ========== richGridRenderer (الرئيسية FEwhat_to_watch) ==========
            val richGrid = content.getAsJsonObject("richGridRenderer")
            if (richGrid != null) {
                val items = richGrid.getAsJsonArray("contents") ?: return results
                for (item in items) {
                    val rsr = item.asJsonObject.getAsJsonObject("richSectionRenderer")
                        ?: continue
                    val sectionContent = rsr.getAsJsonObject("content") ?: continue

                    // محاولة richShelfRenderer (أغلفة أفقية تحتوي فيديوهات)
                    val vsr = sectionContent.getAsJsonObject("richShelfRenderer")
                    if (vsr != null) {
                        val shelfContent = vsr.getAsJsonObject("content") ?: continue
                        // horizontalListRenderer
                        val hlr = shelfContent.getAsJsonObject("horizontalListRenderer")
                        val shelfItems = hlr?.getAsJsonArray("items")
                        // أو expandedShelfContentsRenderer
                            ?: shelfContent.getAsJsonObject("expandedShelfContentsRenderer")?.getAsJsonArray("items")

                        if (shelfItems != null) {
                            for (si in shelfItems) {
                                val vid = si.asJsonObject.getAsJsonObject("videoRenderer")
                                if (vid != null) {
                                    val v = parseVideoRenderer(vid)
                                    if (v != null) results.add(v)
                                }
                            }
                        }
                    } else {
                        // gridRenderer
                        val gr = sectionContent.getAsJsonObject("gridRenderer")
                        if (gr != null) {
                            val gridItems = gr.getAsJsonArray("items") ?: continue
                            for (gi in gridItems) {
                                val gridRenderer = gi.asJsonObject.getAsJsonObject("gridVideoRenderer")
                                    ?: gi.asJsonObject.getAsJsonObject("gridFeedItemRenderer")
                                if (gridRenderer != null) {
                                    // gridFeedItemRenderer يحتوي على videoId داخل content
                                    val contentObj = gridRenderer.getAsJsonObject("content")
                                    if (contentObj != null) {
                                        val vid = contentObj.getAsJsonObject("videoRenderer")
                                        if (vid != null) {
                                            val v = parseVideoRenderer(vid)
                                            if (v != null) results.add(v)
                                        }
                                    }
                                }
                            }
                        }

                        // feedNudgeRenderer (رسالة تشجيعية) - تجاهل
                    }
                }
                return results
            }

            // ========== sectionListRenderer (السجل، الاشتراكات) ==========
            val slr = content.getAsJsonObject("sectionListRenderer")
            val sections = slr?.getAsJsonArray("contents") ?: return results
            for (section in sections) {
                val items = section.asJsonObject
                    .getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents") ?: continue
                for (item in items) {
                    // فيديو مباشر
                    val vid = item.asJsonObject.getAsJsonObject("videoRenderer")
                    if (vid != null) {
                        val v = parseVideoRenderer(vid)
                        if (v != null) results.add(v)
                        continue
                    }

                    // أو عبر gridRenderer داخل itemSectionRenderer
                    val grid = item.asJsonObject.getAsJsonObject("shelfRenderer")
                        ?.getAsJsonObject("content")
                        ?.getAsJsonObject("horizontalListRenderer")
                        ?.getAsJsonArray("items")
                    if (grid != null) {
                        for (gi in grid) {
                            val gvid = gi.asJsonObject.getAsJsonObject("videoRenderer")
                            if (gvid != null) {
                                val v = parseVideoRenderer(gvid)
                                if (v != null) results.add(v)
                            }
                        }
                    }
                }
            }

            results
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // ========== parseSubscribedChannels ==========
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

            // البحث عن gridChannelRenderer عبر الأقسام
            val slr = content.getAsJsonObject("sectionListRenderer")
            val sections = slr?.getAsJsonArray("contents") ?: return emptyList()

            for (section in sections) {
                val items = section.asJsonObject
                    .getAsJsonObject("itemSectionRenderer")
                    ?.getAsJsonArray("contents") ?: continue

                for (item in items) {
                    // gridChannelRenderer مباشرة
                    val gc = item.asJsonObject.getAsJsonObject("gridChannelRenderer")
                    if (gc != null) {
                        val channelId = gc.get("channelId")?.asString ?: continue
                        val title = extractRunsText(gc, "title")
                        val thumbs = gc.getAsJsonObject("thumbnail")
                            ?.getAsJsonArray("thumbnails")
                        val thumbUrl = if (thumbs != null && thumbs.size() > 0)
                            thumbs[thumbs.size() - 1].asJsonObject.get("url")?.asString ?: "" else ""
                        val subCount = extractSimpleText(gc, "subscriberCountText") ?: ""
                        channels.add(ChannelItem(channelId, title, thumbUrl, subCount))
                        continue
                    }

                    // gridChannelRenderer داخل shelfRenderer
                    val shelf = item.asJsonObject.getAsJsonObject("shelfRenderer")
                        ?.getAsJsonObject("content")
                        ?.getAsJsonObject("horizontalListRenderer")
                        ?.getAsJsonArray("items") ?: continue
                    for (hi in shelf) {
                        val gc = hi.asJsonObject.getAsJsonObject("gridChannelRenderer")
                        if (gc != null) {
                            val channelId = gc.get("channelId")?.asString ?: continue
                            val title = extractRunsText(gc, "title")
                            val thumbs = gc.getAsJsonObject("thumbnail")
                                ?.getAsJsonArray("thumbnails")
                            val thumbUrl = if (thumbs != null && thumbs.size() > 0)
                                thumbs[thumbs.size() - 1].asJsonObject.get("url")?.asString ?: "" else ""
                            val subCount = extractSimpleText(gc, "subscriberCountText") ?: ""
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

    suspend fun search(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        parseSearchResults(executeRequest("search", """{"context":${buildContext()},"query":"$query"}"""))
    }

    suspend fun getHomeFeed(category: String? = null): List<VideoItem> = withContext(Dispatchers.IO) {
        if (category == null) {
            val result = executeRequest("browse", """{"context":${buildContext()},"browseId":"FEwhat_to_watch"}""")
            val videos = parseBrowseVideos(result)
            if (videos.isNotEmpty()) return@withContext videos
        }

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
        parseSearchResults(executeRequest("search", """{"context":${buildContext()},"query":"$searchQuery"}"""))
    }

    suspend fun getHistory(): List<VideoItem> = withContext(Dispatchers.IO) {
        parseBrowseVideos(executeRequest("browse", """{"context":${buildContext()},"browseId":"FEhistory"}"""))
    }

    suspend fun getShorts(): List<VideoItem> = withContext(Dispatchers.IO) {
        parseSearchResults(executeRequest("search", """{"context":${buildContext()},"query":"#shorts"}"""))
    }

    suspend fun getSubscriptions(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videos = parseBrowseVideos(executeRequest("browse", """{"context":${buildContext()},"browseId":"FEsubscriptions"}"""))
        if (videos.isNotEmpty()) videos else getHomeFeed()
    }

    suspend fun getSubscribedChannels(): List<ChannelItem> = withContext(Dispatchers.IO) {
        parseSubscribedChannels(executeRequest("browse", """{"context":${buildContext()},"browseId":"FEsubscriptions"}"""))
    }
}
