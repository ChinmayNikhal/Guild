package com.example.guild.groupResources

import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.example.guild.chatResources.ChatMessage
import com.example.guild.utils.AESCipher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestore.getInstance
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val admin: String = ""
)

data class GroupMember(
    val uid: String = "",
    val username: String = ""
)

private val firestore = getInstance()
private val auth = FirebaseAuth.getInstance()
private val _userGroups = mutableStateListOf<GroupData>()
private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
private val _senderUsernames = mutableStateMapOf<String, String>()
private val _groupInvites = mutableStateListOf<GroupData>()

class GroupViewModel : ViewModel() {
    private val db = getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    val userGroups: List<GroupData> = _userGroups
    val messages: StateFlow<List<ChatMessage>> = _messages
    val senderUsernames: Map<String, String> = _senderUsernames
    val groupInvites: List<GroupData> get() = _groupInvites

    private val _groupPreviews = MutableStateFlow<List<GroupData>>(emptyList())
    val groupPreviews: StateFlow<List<GroupData>> get() = _groupPreviews

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
        getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val groupIds = userDoc.get("groups") as? List<String> ?: emptyList()
                _userGroups.clear()
                val groupDataList = mutableListOf<GroupData>()
                for (groupId in groupIds) {
                    getInstance()
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
                                val sortedGroups = groupDataList.sortedByDescending { it.mostRecentTimestamp }
                                _userGroups.addAll(sortedGroups)
                                _groupPreviews.value = sortedGroups

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
        val senderId = auth.currentUser?.uid ?: return
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val groupRef = firestore.collection("Groups").document(groupId)

        groupRef.get().addOnSuccessListener { snapshot ->
            val encodedKey = snapshot.getString("aesKey")

            if (encodedKey.isNullOrEmpty()) {
                Log.w("GroupViewModel", "❌ No AES key found for group $groupId — generating fallback key")

                // Generate and store key
                val newKey = AESCipher.generateKey()
                val newEncodedKey = Base64.encodeToString(newKey.encoded, Base64.DEFAULT)

                // Update Firestore with the new key, then retry sending the message
                groupRef.update("aesKey", newEncodedKey).addOnSuccessListener {
                    // Retry sending message after key is saved
                    sendGroupMessage(groupId, text, ttl)
                }
                return@addOnSuccessListener
            }

            try {
                val keyBytes = Base64.decode(encodedKey, Base64.DEFAULT)
                val secretKey = SecretKeySpec(keyBytes, "AES")
                val encryptedText = AESCipher.encrypt(text, secretKey)

                Log.d("GroupViewModel", "🔐 Encrypted message: $encryptedText")

                val message = mapOf(
                    "messageId" to messageId,
                    "senderId" to senderId,
                    "text" to encryptedText,
                    "timestamp" to timestamp,
                    "ttl" to ttl
                )

                Log.d("GroupViewModel", "📤 Uploading message: $message")

                groupRef.collection("Messages").document(messageId)
                    .set(message)
                    .addOnSuccessListener {
                        Log.d("GroupViewModel", "✅ Message sent successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupViewModel", "❌ Failed to send message: ${e.localizedMessage}", e)
                    }

                // Update preview info
                groupRef.update(
                    "mostRecentMessageContent", encryptedText,
                    "mostRecentMessageTimestamp", timestamp
                )

            } catch (e: Exception) {
                Log.e("GroupViewModel", "❌ Encryption or upload failed: ${e.localizedMessage}", e)
            }
        }.addOnFailureListener { e ->
            Log.e("GroupViewModel", "❌ Failed to fetch group AES key: ${e.localizedMessage}", e)
        }
    }



    fun listenForGroupMessages(groupId: String, text: String? = null, ttl: Long = 0L) {
        val groupRef = firestore.collection("Groups").document(groupId)

        groupRef.get().addOnSuccessListener { groupSnapshot ->
            val encodedKey = groupSnapshot.getString("aesKey")

            if (encodedKey.isNullOrEmpty()) {
                Log.w("GroupViewModel", "❌ No AES key found for group $groupId — generating fallback key")

                val newKey = AESCipher.generateKey()
                val newEncodedKey = Base64.encodeToString(newKey.encoded, Base64.DEFAULT)

                groupRef.update("aesKey", newEncodedKey).addOnSuccessListener {
                    Log.d("GroupViewModel", "✅ Fallback AES key stored.")
                    if (!text.isNullOrEmpty()) {
                        Log.d("GroupViewModel", "📤 Retrying message sending.")
                        sendGroupMessage(groupId, text, ttl)
                    }
                }
                return@addOnSuccessListener
            }


            val keyBytes = Base64.decode(encodedKey, Base64.DEFAULT)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            groupRef.collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("GroupViewModel", "❌ Firestore snapshot error: ${error.localizedMessage}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w("GroupViewModel", "⚠️ Snapshot is null for group $groupId")
                        return@addSnapshotListener
                    }

                    Log.d("GroupViewModel", "📥 Fetched ${snapshot.documents.size} messages")

                    val now = System.currentTimeMillis()
                    val decryptedMessages = snapshot.documents.mapNotNull { doc ->
                        val ttl = doc.getLong("ttl") ?: 0L
                        val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null

                        if (ttl == 0L || timestamp + ttl > now) {
                            val encryptedText = doc.getString("text") ?: return@mapNotNull null
                            return@mapNotNull try {
                                val decryptedText = AESCipher.decrypt(encryptedText, secretKey)
                                Log.d("GroupViewModel", "🟢 Decrypted: $decryptedText")
                                ChatMessage(
                                    messageId = doc.getString("messageId") ?: "",
                                    senderId = doc.getString("senderId") ?: "",
                                    text = decryptedText,
                                    timestamp = timestamp,
                                    ttl = ttl
                                )
                            } catch (e: Exception) {
                                Log.e("GroupViewModel", "❌ Decryption failed: ${e.localizedMessage}", e)
                                null
                            }
                        } else {
                            Log.d("GroupViewModel", "🕓 Skipping expired message ${doc.id}")
                            null
                        }
                    }

                    Log.d("GroupViewModel", "✅ Decrypted ${decryptedMessages.size} messages")
                    _messages.value = decryptedMessages
                    decryptedMessages.map { it.senderId }.distinct().forEach { fetchUsernameFor(it) }
                }
        }.addOnFailureListener { e ->
            Log.e("GroupViewModel", "❌ Failed to load group info: ${e.localizedMessage}", e)
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

    fun sendJoinRequest(groupId: String) {
        val userId = auth.currentUser?.uid ?: return

        // Optional: check if already in blocked list
        val groupRef = firestore.collection("Groups").document(groupId)
        groupRef.get().addOnSuccessListener { doc ->
            val blocked = doc.get("blocked") as? List<String> ?: emptyList()
            if (blocked.contains(userId)) {
                Log.d("GroupViewModel", "User is blocked from this group")
                return@addOnSuccessListener
            }

            groupRef.update("requests", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    Log.d("GroupViewModel", "Join request sent")
                }
        }
    }

    fun approveRequest(groupId: String, userId: String) {
        val groupRef = firestore.collection("Groups").document(groupId)

        groupRef.update(
            mapOf(
                "requests" to FieldValue.arrayRemove(userId),
                "members" to FieldValue.arrayUnion(userId)
            )
        ).addOnSuccessListener {
            firestore.collection("users").document(userId)
                .update("groups", FieldValue.arrayUnion(groupId))
        }
    }

    fun removeUserFromGroup(groupId: String, userId: String) {
        val groupRef = firestore.collection("Groups").document(groupId)

        groupRef.update("members", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                firestore.collection("users").document(userId)
                    .update("groups", FieldValue.arrayRemove(groupId))
            }
    }

    fun blockUser(groupId: String, userId: String) {
        removeUserFromGroup(groupId, userId)
        firestore.collection("Groups").document(groupId)
            .update("blocked", FieldValue.arrayUnion(userId))
    }

    fun unblockUser(groupId: String, userId: String) {
        firestore.collection("Groups").document(groupId)
            .update("blocked", FieldValue.arrayRemove(userId))
    }

    fun getPendingRequests(groupId: String, callback: (List<String>) -> Unit) {
        firestore.collection("Groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                val requests = document.get("requests") as? List<String> ?: emptyList()
                callback(requests)
            }
    }

    fun loadGroupInvites(userId: String) {
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { userDoc ->
            val inviteGroupIds = userDoc.get("groupInvites") as? List<String> ?: emptyList()
            val userGroups = userDoc.get("groups") as? List<String> ?: emptyList()

            _groupInvites.clear()
            val filteredInvites = inviteGroupIds.filter { it !in userGroups }
            if (filteredInvites.isEmpty()) return@addOnSuccessListener

            val invites = mutableListOf<GroupData>()
            for (groupId in filteredInvites) {
                firestore.collection("Groups").document(groupId).get()
                    .addOnSuccessListener { groupDoc ->
                        val name = groupDoc.getString("groupName") ?: "Unnamed"
                        val description = groupDoc.getString("groupDescription") ?: ""
                        val lastMsg = groupDoc.getString("mostRecentMessageContent") ?: ""
                        val timestamp = groupDoc.getLong("mostRecentMessageTimestamp") ?: 0L

                        invites.add(
                            GroupData(
                                groupId = groupId,
                                name = name,
                                description = description,
                                mostRecentMessage = lastMsg,
                                mostRecentTimestamp = timestamp
                            )
                        )

                        if (invites.size == filteredInvites.size) {
                            _groupInvites.addAll(invites.sortedByDescending { it.mostRecentTimestamp })
                        }
                    }
            }

            // Clean up stale invites from DB (user already joined the group)
            val staleInvites = inviteGroupIds.filter { it in userGroups }
            if (staleInvites.isNotEmpty()) {
                firestore.collection("users").document(userId)
                    .update("groupInvites", FieldValue.arrayRemove(*staleInvites.toTypedArray()))
            }
        }
    }

    fun acceptGroupInvite(groupId: String) {
        val userId = auth.currentUser?.uid ?: return

        // Add groupId to user's group list
        firestore.collection("users").document(userId)
            .update("groups", FieldValue.arrayUnion(groupId,))

        // Remove invite from user's document
        firestore.collection("users").document(userId)
            .update("groupInvites", FieldValue.arrayRemove(groupId))

        // Add user to group members
        firestore.collection("Groups").document(groupId)
            .update("members", FieldValue.arrayUnion(userId))

        // Refresh invites list and user groups
        loadGroupInvites(userId)
        loadUserGroups(userId)
    }

    fun declineGroupInvite(groupId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .update("groupInvites", FieldValue.arrayRemove(groupId))
            .addOnSuccessListener {
                loadGroupInvites(userId)
            }
    }

    fun fetchGroupMembers(groupId: String, onResult: (List<GroupMember>) -> Unit) {
        firestore.collection("Groups")
            .document(groupId)
            .get()
            .addOnSuccessListener { groupDoc ->
                val memberIds = groupDoc.get("members") as? List<String> ?: emptyList()
                val members = mutableListOf<GroupMember>()
                if (memberIds.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                memberIds.forEach { uid ->
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: "Unknown"
                            members.add(GroupMember(uid = uid, username = username))

                            if (members.size == memberIds.size) {
                                onResult(members)
                            }
                        }
                }
            }
    }

    fun toggleAdminStatus(groupId: String, memberId: String) {
        val groupRef = getInstance().collection("Groups").document(groupId)
        getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(groupRef)
            val admins = snapshot.get("administrators") as? MutableList<String> ?: mutableListOf()
            if (admins.contains(memberId)) {
                admins.remove(memberId)
            } else {
                admins.add(memberId)
            }
            transaction.update(groupRef, "administrators", admins)
        }
    }

    fun removeMember(groupId: String, memberId: String) {
        val groupRef = getInstance().collection("Groups").document(groupId)
        groupRef.collection("members").document(memberId).delete()
        // Optional: Remove from administrators list
        groupRef.update("administrators", FieldValue.arrayRemove(memberId))
    }

    fun deleteGroup(groupId: String) {
        val groupRef = getInstance().collection("Groups").document(groupId)
        groupRef.delete()
    }

    fun isUserAdmin(groupId: String, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false)
        firestore.collection("Groups").document(groupId).get()
            .addOnSuccessListener { doc ->
                val admins = doc.get("administrators") as? List<String> ?: emptyList()
                callback(admins.contains(userId))
            }
    }

    fun sendGroupInvite(groupId: String, username: String) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { snapshot ->
                val userDoc = snapshot.documents.firstOrNull()
                val userId = userDoc?.id

                if (userId != null) {
                    firestore.collection("users").document(userId)
                        .update("groupInvites", FieldValue.arrayUnion(groupId))
                        .addOnSuccessListener {
                            Log.d("GroupViewModel", "Invite sent to user: $username ($userId)")
                        }
                        .addOnFailureListener {
                            Log.e("GroupViewModel", "Failed to update user invites", it)
                        }
                } else {
                    Log.w("GroupViewModel", "No user found with username: $username")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("GroupViewModel", "Failed to look up username: $username", exception)
            }
    }

    fun fetchGroupInfo(groupId: String, callback: (name: String, description: String, members: List<GroupMember>) -> Unit) {
        firestore.collection("Groups").document(groupId).get()
            .addOnSuccessListener { groupDoc ->
                val name = groupDoc.getString("groupName") ?: "Unnamed"
                val description = groupDoc.getString("groupDescription") ?: ""
                val memberIds = groupDoc.get("members") as? List<String> ?: emptyList()
                val members = mutableListOf<GroupMember>()

                if (memberIds.isEmpty()) {
                    callback(name, description, emptyList())
                    return@addOnSuccessListener
                }

                memberIds.forEach { uid ->
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: "Unknown"
                            members.add(GroupMember(uid = uid, username = username))
                            if (members.size == memberIds.size) {
                                callback(name, description, members)
                            }
                        }
                }
            }
    }

    fun getBlockedUsers(groupId: String, callback: (List<GroupMember>) -> Unit) {
        firestore.collection("Groups").document(groupId).get()
            .addOnSuccessListener { groupDoc ->
                val blockedIds = groupDoc.get("blocked") as? List<String> ?: emptyList()
                if (blockedIds.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                val blockedMembers = mutableListOf<GroupMember>()
                blockedIds.forEach { uid ->
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: "Unknown"
                            blockedMembers.add(GroupMember(uid = uid, username = username))
                            if (blockedMembers.size == blockedIds.size) {
                                callback(blockedMembers)
                            }
                        }
                }
            }
    }

    fun sendGroupInviteByUsername(groupId: String, username: String) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { snapshot ->
                val userDoc = snapshot.documents.firstOrNull()
                if (userDoc != null) {
                    val userId = userDoc.id
                    val userGroups = userDoc.get("groups") as? List<String> ?: emptyList()

                    if (userGroups.contains(groupId)) {
                        Log.d("GroupViewModel", "User is already a member of the group.")
                        return@addOnSuccessListener
                    }

                    firestore.collection("users").document(userId)
                        .update("groupInvites", FieldValue.arrayUnion(groupId))
                        .addOnSuccessListener {
                            Log.d("GroupViewModel", "Invite sent to $username ($userId)")
                        }
                } else {
                    Log.d("GroupViewModel", "User not found: $username")
                }
            }
    }

}