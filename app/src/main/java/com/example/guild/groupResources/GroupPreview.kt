package com.example.guild.groupResources

data class GroupPreview(
    val groupId: String = "",
    val groupName: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L
)
