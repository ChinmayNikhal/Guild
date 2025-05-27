package com.example.guild.chatResources

import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val chatPreviews = mutableStateListOf<ChatPreview>()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messagesFlow: StateFlow<List<ChatMessage>> = _messagesFlow

    private val _senderUsernames = mutableStateMapOf<String, String>()
    val senderUsernames: Map<String, String> = _senderUsernames

    private val _chatPartnerName = mutableStateOf("Unknown")
    val chatPartnerName: State<String> get() = _chatPartnerName

    fun listenForMessages(chatId: String) {
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatViewModel", "Listen failed", e)
                    return@addSnapshotListener
                }

                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()

                // Fetch usernames for all unique senderIds
                msgs.map { it.senderId }.distinct().forEach { senderId ->
                    fetchUsernameFor(senderId)
                }

                _messages.value = msgs
            }
    }

    fun sendMessage(chatId: String, text: String) {
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessage(
            messageId = messageId,
            senderId = auth.currentUser?.uid ?: "unknown",
            text = text,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(messageId)
            .set(message)
    }

    fun loadUserChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatViewModel", "Failed to load chats", e)
                    return@addSnapshotListener
                }

                chatPreviews.clear()

                snapshot?.documents?.forEach { doc ->
                    val chatId = doc.id
                    val participants = doc.get("participants") as? List<String> ?: return@forEach
                    val lastMessage = doc.get("lastMessage") as? String ?: ""

                    val otherUserId = participants.firstOrNull { it != currentUserId } ?: return@forEach

                    // Fetch the username of the other participant
                    firestore.collection("users")
                        .document(otherUserId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: "Unknown"

                            chatPreviews.add(
                                ChatPreview(
                                    chatId = chatId,
                                    otherUserId = otherUserId,
                                    otherUsername = username,
                                    lastMessage = lastMessage
                                )
                            )
                        }
                }
            }
    }

    fun startOrNavigateToChatWith(
        otherUserId: String,
        onSuccess: (chatId: String, otherUsername: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onFailure("User not logged in.")
            return
        }

        val chatId = listOf(currentUserId, otherUserId).sorted().joinToString("_")
        val chatRef = firestore.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                // Chat doesn't exist (maybe due to error), create fallback
                val chatData = hashMapOf(
                    "participants" to listOf(currentUserId, otherUserId),
                    "lastTimestamp" to FieldValue.serverTimestamp()
                )
                chatRef.set(chatData).addOnSuccessListener {
                    fetchUsernameAndNavigate(otherUserId, chatId, onSuccess, onFailure)
                }
            } else {
                fetchUsernameAndNavigate(otherUserId, chatId, onSuccess, onFailure)
            }
        }.addOnFailureListener {
            onFailure("Failed to retrieve chat.")
        }
    }

    private fun fetchUsernameAndNavigate(
        otherUserId: String,
        chatId: String,
        onSuccess: (chatId: String, otherUsername: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection("users").document(otherUserId)
            .get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username") ?: "Unknown"
                onSuccess(chatId, username)
            }
            .addOnFailureListener {
                onFailure("Failed to get user info.")
            }
    }

    private fun fetchUsernameFor(senderId: String) {
        if (_senderUsernames.containsKey(senderId)) return

        firestore.collection("users")
            .document(senderId)
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: senderId
                _senderUsernames[senderId] = username
            }
    }

    fun fetchChatPartnerName(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("username")
                if (!username.isNullOrBlank()) {
                    _chatPartnerName.value = username
                }
            }
    }

}