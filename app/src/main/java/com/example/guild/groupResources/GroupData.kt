package com.example.guild.groupResources

data class GroupData(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val mostRecentMessage: String = "",
    val mostRecentTimestamp: Long = 0L
)
