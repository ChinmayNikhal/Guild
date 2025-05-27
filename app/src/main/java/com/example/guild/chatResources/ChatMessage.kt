package com.example.guild.chatResources

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val ttl: Long = 0L,


    val seenAt: Long = 0L,
    val disappearing: Boolean = false,
    val seenBy: Map<String, Long>? = null
)
