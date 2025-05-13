package com.example.guild.chatResources

import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
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

    private val realtimeDb = FirebaseDatabase.getInstance()
    val userGroups = mutableStateListOf<GroupData>()

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

    fun createGroup(name: String, description: String) {
        val groupId = UUID.randomUUID().toString()
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // 1. Push to Realtime Database
        val groupRef = realtimeDb.getReference("Groups").child(groupId)
        val groupData = mapOf(
            "groupName" to name,
            "groupDescription" to description,
            "groupCreatedTimestamp" to System.currentTimeMillis(),
            "owner" to userId,
            "members" to mapOf(userId to true),
            "mostRecentMessageContent" to "",
            "mostRecentMessageTimestamp" to 0L
        )

        groupRef.setValue(groupData).addOnSuccessListener {
            Log.d("ChatViewModel", "Group pushed to Realtime DB: $groupId")

            // 2. Append groupId to Firestore `groups` field (create array if needed)
            val userDocRef = firestore.collection("users").document(userId)
            userDocRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Use FieldValue.arrayUnion to safely add groupId
                    userDocRef.update("groups", FieldValue.arrayUnion(groupId))
                        .addOnSuccessListener {
                            Log.d("ChatViewModel", "Group added to user Firestore profile")
                            // Optional: Reload the group list immediately
                            loadUserGroups()
                        }
                        .addOnFailureListener {
                            Log.e("ChatViewModel", "Failed to add group to Firestore: ${it.message}")
                        }
                } else {
                    Log.w("ChatViewModel", "User document not found in Firestore")
                }
            }.addOnFailureListener {
                Log.e("ChatViewModel", "Error accessing user Firestore doc: ${it.message}")
            }
        }.addOnFailureListener {
            Log.e("ChatViewModel", "Failed to create group in Realtime DB: ${it.message}")
        }
    }

    fun createDummyGroupForTesting() {
        val dummyGroupId = UUID.randomUUID().toString()
        val dummyRef = realtimeDb.getReference("Groups").child(dummyGroupId)

        val dummyGroup = mapOf(
            "groupName" to "Test Group",
            "groupDescription" to "This is a test group",
            "createdAt" to System.currentTimeMillis(),
            "owner" to auth.currentUser?.uid,
            "members" to mapOf(auth.currentUser?.uid )
        )

        dummyRef.setValue(dummyGroup).addOnSuccessListener {
            Log.d("ChatViewModel", "Dummy group created: $dummyGroupId")
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Failed to create dummy group: ${e.message}", e)
        }
    }


    fun loadUserGroups() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val groupIds = doc.get("groups") as? List<String> ?: return@addOnSuccessListener
                userGroups.clear()

                groupIds.forEach { groupId ->
                    realtimeDb.getReference("Groups").child(groupId)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val name = snapshot.child("groupName").getValue(String::class.java) ?: "Unnamed"
                            val description = snapshot.child("groupDescription").getValue(String::class.java) ?: ""
                            val recentMsg = snapshot.child("mostRecentMessageContent").getValue(String::class.java) ?: ""
                            val recentTs = snapshot.child("mostRecentMessageTimestamp").getValue(Long::class.java) ?: 0L

                            userGroups.add(
                                GroupData(
                                    groupId = groupId,
                                    name = name,
                                    description = description,
                                    mostRecentMessage = recentMsg,
                                    mostRecentTimestamp = recentTs
                                )
                            )
                        }
                }
            }
    }

    fun leaveGroup(groupId: String) {
        val userId = auth.currentUser?.uid ?: return

        // Remove from group members
        realtimeDb.getReference("Groups").child(groupId).child("members").child(userId).removeValue()

        // Remove from Firestore
        firestore.collection("users").document(userId)
            .update("groups", FieldValue.arrayRemove(groupId))

        // Remove locally
        userGroups.removeAll { it.groupId == groupId }
    }

    fun listenForGroupMessages(groupId: String) {
        realtimeDb.getReference("Groups")
            .child(groupId)
            .child("messages")
            .orderByChild("timestamp")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val msgs = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                    _messages.value = msgs

                    // Cache usernames
                    msgs.map { it.senderId }.distinct().forEach { fetchUsernameFor(it) }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.w("ChatViewModel", "Failed to load group messages", error.toException())
                }
            })
    }

    fun sendGroupMessage(groupId: String, text: String) {
        val senderId = auth.currentUser?.uid ?: return
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = ChatMessage(
            messageId = messageId,
            senderId = senderId,
            text = text,
            timestamp = timestamp
        )

        val groupMessagesRef = realtimeDb.getReference("Groups")
            .child(groupId)
            .child("messages")
            .child(messageId)

        groupMessagesRef.setValue(message)

        // Update metadata for most recent message
        val groupRef = realtimeDb.getReference("Groups").child(groupId)
        groupRef.child("mostRecentMessageContent").setValue(text)
        groupRef.child("mostRecentMessageTimestamp").setValue(timestamp)
    }


}