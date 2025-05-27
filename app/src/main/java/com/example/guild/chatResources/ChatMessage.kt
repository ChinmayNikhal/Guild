package com.example.guild.chatResources

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L, // Sent time
    val ttl: Long = 0L, // Optional: Can be used to specify TTL duration if needed

  //  val isDisappearing: Boolean = false, // Toggle to enable disappearing behavior
  //  val seenAt: Long? = null ,// When the message was viewed by the receiver

    val seenAt: Long = 0L,    //updated to this
    val disappearing: Boolean = false  //updated to disappearing from isDisappearing
)
