package com.ytclone.models

data class ChannelItem(
    val channelId: String,
    val title: String,
    val thumbnailUrl: String,
    val subscriberCount: String = ""
)
