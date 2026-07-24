package com.ytclone.models

data class VideoItem(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val channelAvatarUrl: String?,
    val thumbnailUrl: String,
    val duration: String?,
    val viewCount: String,
    val publishedTime: String,
    val description: String? = null
)
