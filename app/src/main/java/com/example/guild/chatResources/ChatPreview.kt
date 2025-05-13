package com.example.guild.chatResources

data class ChatPreview(
    val chatId: String,
    val otherUserId: String,
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,
    val otherUsername: String = ""
)
