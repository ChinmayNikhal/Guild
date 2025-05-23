package com.example.guild.groupResources

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.example.guild.chatResources.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val admin: String = ""
)

private val firestore = FirebaseFirestore.getInstance()
private val auth = FirebaseAuth.getInstance()

private val _userGroups = mutableStateListOf<GroupData>()

private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

private val _senderUsernames = mutableStateMapOf<String, String>()

class GroupViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    val userGroups: List<GroupData> = _userGroups
    val messages: StateFlow<List<ChatMessage>> = _messages
    val senderUsernames: Map<String, String> = _senderUsernames


    fun createGroup(name: String, description: String) {
        val groupId = UUID.randomUUID().toString()
        val currentUser = com.example.guild.groupResources.auth.currentUser ?: return
        val userId = currentUser.uid

        val groupData = hashMapOf(
            "groupId" to groupId,
            "groupName" to name,
            "groupDescription" to description,
            "groupCreatedTimestamp" to System.currentTimeMillis(),
            "owner" to userId,
            "administrators" to listOf(userId),
            "members" to listOf(userId),
            "mostRecentMessageContent" to "",
            "mostRecentMessageTimestamp" to 0L
        )

        firestore.collection("Groups").document(groupId).set(groupData)
            .addOnSuccessListener {
                // Add groupId to user's profile
                firestore.collection("users").document(userId)
                    .update("groups", FieldValue.arrayUnion(groupId))
                    .addOnSuccessListener {
                        loadUserGroups(userId)
                    }
            }
    }

    fun loadUserGroups(userId: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val groupIds = userDoc.get("groups") as? List<String> ?: emptyList()
                _userGroups.clear()
                val groupDataList = mutableListOf<GroupData>()
                for (groupId in groupIds) {
                    FirebaseFirestore.getInstance()
                        .collection("Groups")
                        .document(groupId)
                        .get()
                        .addOnSuccessListener { groupDoc ->
                            val name = groupDoc.getString("groupName") ?: "Unnamed"
                            val description = groupDoc.getString("groupDescription") ?: ""
                            val lastMsg = groupDoc.getString("mostRecentMessageContent") ?: ""
                            val timestamp = groupDoc.getLong("mostRecentMessageTimestamp") ?: 0L
                            groupDataList.add(
                                GroupData(
                                    groupId = groupId,
                                    name = name,
                                    description = description,
                                    mostRecentMessage = lastMsg,
                                    mostRecentTimestamp = timestamp
                                )
                            )
                            // After fetching all group data, sort and update the list
                            if (groupDataList.size == groupIds.size) {
                                _userGroups.addAll(groupDataList.sortedByDescending { it.mostRecentTimestamp })
                            }
                        }
                }
            }
    }

    fun leaveGroup(groupId: String) {
        val userId = com.example.guild.groupResources.auth.currentUser?.uid ?: return

        // Remove user from group document
        firestore.collection("Groups").document(groupId)
            .update("members", FieldValue.arrayRemove(userId))

        // Remove groupId from user's profile
        firestore.collection("users").document(userId)
            .update("groups", FieldValue.arrayRemove(groupId))

        _userGroups.removeAll { it.groupId == groupId }
    }

    fun sendGroupMessage(groupId: String, text: String, ttl: Long = 0L) {
        val senderId = com.example.guild.groupResources.auth.currentUser?.uid ?: return
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = hashMapOf(
            "messageId" to messageId,
            "senderId" to senderId,
            "text" to text,
            "timestamp" to timestamp,
            "ttl" to ttl
        )

        val messageRef = firestore.collection("Groups")
            .document(groupId)
            .collection("Messages")
            .document(messageId)

        messageRef.set(message)

        firestore.collection("Groups").document(groupId)
            .update(
                mapOf(
                    "mostRecentMessageContent" to text,
                    "mostRecentMessageTimestamp" to timestamp
                )
            )
    }

    fun listenForGroupMessages(groupId: String) {
        firestore.collection("Groups")
            .document(groupId)
            .collection("Messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ChatViewModel", "Failed to listen for group messages", error)
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val ttl = doc.getLong("ttl") ?: 0L
                    val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null

                    // TTL logic: only keep if not expired or ttl == 0
                    if (ttl == 0L || (timestamp + ttl > now)) {
                        doc.toObject(ChatMessage::class.java)
                    } else {
                        null
                    }
                } ?: emptyList()

                _messages.value = messages
                messages.map { it.senderId }.distinct().forEach { fetchUsernameFor(it) }
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
}
