package com.example.guild.chatResources

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

fun createDummyChat(firestore: FirebaseFirestore, currentUserId: String, friendId: String) {
    val chatId = listOf(currentUserId, friendId).sorted().joinToString("_")
    val chatDocRef = firestore.collection("chats").document(chatId)

    val dummyChat = mapOf(
        "participants" to listOf(currentUserId, friendId),
        "lastTimestamp" to FieldValue.serverTimestamp()
    )

    val dummyMessage = mapOf(
        "senderId" to currentUserId,
        "text" to "Hey there! This is a test message.",
        "timestamp" to Timestamp.now()
    )

    chatDocRef.set(dummyChat)
    chatDocRef.collection("messages").add(dummyMessage)

    firestore.collection("users").document(currentUserId)
        .update("chats", FieldValue.arrayUnion(chatId))

    firestore.collection("users").document(friendId)
        .update("chats", FieldValue.arrayUnion(chatId))
}
