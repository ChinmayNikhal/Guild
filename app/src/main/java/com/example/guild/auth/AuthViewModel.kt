package com.example.guild.auth

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.mutableStateOf



class AuthViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid
    val currentUser = mutableStateOf(firebaseAuth.currentUser)
    val username = mutableStateOf("")
    val authError = mutableStateOf("")

    val friendUids = mutableStateListOf<String>()
    val friendRequests = mutableStateListOf<String>()
    val sentRequests = mutableStateListOf<String>()

    fun signUp(
        email: String,
        password: String,
        username: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = firebaseAuth.currentUser?.uid
                    if (uid != null) {
                        // Main user document (basic user + friend data)
                        val userMap = hashMapOf(
                            "uid" to uid,
                            "email" to email,
                            "username" to username,
                            "friends" to listOf<String>(),
                            "friendRequests" to listOf<String>(),
                            "sentRequests" to listOf<String>()
                        )

                        // Profile subdocument (detailed profile info)
                        val profileMap = hashMapOf(
                            "about" to "",
                            "status" to "Available",
                            "mobile" to "",
                            "gender" to "Other",
                            "dob" to "",
                            "color" to "#BB86FC",
                            "username" to username
                        )

                        // Save user doc first
                        firestore.collection("users").document(uid).set(userMap)
                            .addOnSuccessListener {
                                // Then save profile subdoc
                                firestore.collection("users").document(uid)
                                    .collection("profile").document("basic")
                                    .set(profileMap)
                                    .addOnSuccessListener { onSuccess() }
                                    .addOnFailureListener { e ->
                                        onFailure("Failed to save profile: ${e.message}")
                                    }
                            }
                            .addOnFailureListener { e ->
                                onFailure("Failed to save user data: ${e.message}")
                            }
                    } else {
                        onFailure("User ID is null")
                    }
                } else {
                    onFailure(task.exception?.message ?: "Signup failed")
                }
            }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loadFriendsAndRequests() // <- one-time load
                    // Optionally also listen for real-time updates:
                    listenToFriends()
                    listenToFriendRequests()
                    listenToSentRequests()
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
        currentUser.value = null
        username.value = ""
        friendUids.clear()
        friendRequests.clear()
        sentRequests.clear()
    }

    fun loadProfile(
        onLoaded: (Map<String, Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }

        firestore.collection("users").document(uid)
            .collection("profile").document("basic")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onLoaded(document.data ?: emptyMap())
                } else {
                    // Return default values for new users
                    onLoaded(
                        mapOf(
                            "about" to "",
                            "status" to "Available",
                            "mobile" to "",
                            "gender" to "Other",
                            "dob" to "",
                            "username" to username
                        )
                    )
                }
            }
            .addOnFailureListener {
                onError("Failed to load profile: ${it.message}")
            }
    }


    fun saveProfile(
        about: String,
        status: String,
        mobile: String,
        gender: String,
        dob: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            onFailure("User not logged in")
            return
        }

        val userDoc = firestore.collection("users").document(uid)

        // Combine profile with username, if it's stored in the parent doc
        userDoc.get().addOnSuccessListener { snapshot ->
            val username = snapshot.getString("username") ?: ""

            val data = mapOf(
                "about" to about,
                "status" to status,
                "mobile" to mobile,
                "gender" to gender,
                "dob" to dob,
                "username" to username // optional: keeps username accessible in profile
            )

            userDoc.collection("profile").document("basic")
                .set(data)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure("Failed to save profile: ${e.localizedMessage}")
                }

        }.addOnFailureListener { e ->
            onFailure("Could not fetch user doc: ${e.localizedMessage}")
        }
    }


    fun sendFriendRequest(targetUsername: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val senderId = currentUser.value?.uid ?: return

        firestore.collection("users").whereEqualTo("username", targetUsername).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val recipientId = querySnapshot.documents[0].id

                    if (recipientId == senderId) {
                        onFailure("You can't add yourself.")
                        return@addOnSuccessListener
                    }

                    val batch = firestore.batch()
                    val recipientRef = firestore.collection("users").document(recipientId)
                    val senderRef = firestore.collection("users").document(senderId)

                    batch.update(recipientRef, "friendRequests", FieldValue.arrayUnion(senderId))
                    batch.update(senderRef, "sentRequests", FieldValue.arrayUnion(recipientId))

                    batch.commit().addOnSuccessListener { onSuccess() }
                } else {
                    onFailure("User not found")
                }
            }.addOnFailureListener {
                onFailure("Failed to send request")
            }
    }

    fun removeFriend(uidToRemove: String) {
        val currentUid = currentUser.value?.uid ?: return

        val batch = firestore.batch()
        val userRef = firestore.collection("users").document(currentUid)
        val otherUserRef = firestore.collection("users").document(uidToRemove)

        batch.update(userRef, "friends", FieldValue.arrayRemove(uidToRemove))
        batch.update(otherUserRef, "friends", FieldValue.arrayRemove(currentUid))

        batch.commit().addOnSuccessListener {
            friendUids.remove(uidToRemove)
        }
    }

    fun acceptFriendRequest(requestingUserId: String, onComplete: (Boolean) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val batch = firestore.batch()

        val currentUserRef = firestore.collection("users").document(currentUserId)
        val requestingUserRef = firestore.collection("users").document(requestingUserId)

        // Update friend lists
        batch.update(currentUserRef, "friends", FieldValue.arrayUnion(requestingUserId))
        batch.update(requestingUserRef, "friends", FieldValue.arrayUnion(currentUserId))

        // Remove from pending requests
        batch.update(currentUserRef, "friendRequests", FieldValue.arrayRemove(requestingUserId))

        // Commit batch
        batch.commit().addOnSuccessListener {
            // Create chat after successful friend acceptance
            val chatId = listOf(currentUserId, requestingUserId).sorted().joinToString("_")
            val chatRef = firestore.collection("chats").document(chatId)

            chatRef.get().addOnSuccessListener { document ->
                if (!document.exists()) {
                    val chatData = hashMapOf(
                        "participants" to listOf(currentUserId, requestingUserId),
                        "lastTimestamp" to FieldValue.serverTimestamp()
                    )
                    chatRef.set(chatData)
                }
            }

            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }



    fun rejectFriendRequest(requesterUid: String) {
        val currentUid = currentUser.value?.uid ?: return

        val batch = firestore.batch()
        val currentUserRef = firestore.collection("users").document(currentUid)
        val requesterRef = firestore.collection("users").document(requesterUid)

        batch.update(currentUserRef, "friendRequests", FieldValue.arrayRemove(requesterUid))
        batch.update(requesterRef, "sentRequests", FieldValue.arrayRemove(currentUid))

        batch.commit().addOnSuccessListener {
            friendRequests.remove(requesterUid)
        }
    }

    fun listenToFriends() {
        val uid = currentUser.value?.uid ?: return
        firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val friendIds = snapshot.get("friends") as? List<String> ?: listOf()
                friendUids.clear()
                friendUids.addAll(friendIds)
            }
        }
    }

    fun listenToFriendRequests() {
        val uid = currentUser.value?.uid ?: return
        firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val requestIds = snapshot.get("friendRequests") as? List<String> ?: listOf()
                friendRequests.clear()
                friendRequests.addAll(requestIds)
            }
        }
    }

    fun listenToSentRequests() {
        val uid = currentUser.value?.uid ?: return
        firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val requestIds = snapshot.get("sentRequests") as? List<String> ?: listOf()
                sentRequests.clear()
                sentRequests.addAll(requestIds)
            }
        }
    }

    fun loadFriendsAndRequests() {
        val uid = currentUser.value?.uid ?: return
        firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val friends = doc.get("friends") as? List<String> ?: listOf()
            val incoming = doc.get("friendRequests") as? List<String> ?: listOf()
            val sent = doc.get("sentRequests") as? List<String> ?: listOf()

            friendUids.clear()
            friendRequests.clear()
            sentRequests.clear()

            friendUids.addAll(friends)
            friendRequests.addAll(incoming)
            sentRequests.addAll(sent)
        }
    }

    fun fetchUsername(uid: String, onResult: (String) -> Unit) {
        firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val uname = doc.getString("username") ?: ""
            onResult(uname)
        }.addOnFailureListener {
            onResult("Unknown")
        }
    }

    fun startChatWithFriend(
        otherUserId: String,
        otherUsername: String,
        onChatReady: (chatId: String, otherUserId: String, otherUsername: String) -> Unit
    ) {
        val myUid = currentUser.value?.uid ?: return

        // Deterministic chatId
        val chatId = if (myUid < otherUserId) "${myUid}_$otherUserId" else "${otherUserId}_$myUid"

        val chatRef = firestore.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                // Create new chat document
                val chatData = hashMapOf(
                    "participants" to listOf(myUid, otherUserId),
                    "lastTimestamp" to FieldValue.serverTimestamp()
                )

                chatRef.set(chatData).addOnSuccessListener {
                    // Optionally, add chatId to each user's "chats" subcollection or map
                    val userRef1 = firestore.collection("users").document(myUid)
                    val userRef2 = firestore.collection("users").document(otherUserId)

                    val batch = firestore.batch()
                    batch.set(userRef1.collection("chats").document(chatId), mapOf("participant" to otherUserId))
                    batch.set(userRef2.collection("chats").document(chatId), mapOf("participant" to myUid))

                    batch.commit().addOnSuccessListener {
                        onChatReady(chatId, otherUserId, otherUsername)
                    }
                }
            } else {
                onChatReady(chatId, otherUserId, otherUsername)
            }
        }
    }

//    fun startOrNavigateToChatWith(
//        otherUserId: String,
//        onSuccess: (chatId: String, otherUsername: String) -> Unit,
//        onFailure: (String) -> Unit
//    ) {
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        val currentUserId = currentUser?.uid
//
//        if (currentUserId == null) {
//            onFailure("Not logged in.")
//            return
//        }
//
//        val chatId = listOf(currentUserId, otherUserId).sorted().joinToString("_")
//
//        val chatRef = firestore.collection("chats").document(chatId)
//
//        chatRef.get().addOnSuccessListener { document ->
//            if (!document.exists()) {
//                // Chat does not exist, so we create it
//                val chatData = hashMapOf(
//                    "participants" to listOf(currentUserId, otherUserId),
//                    "lastTimestamp" to FieldValue.serverTimestamp()
//                )
//
//                chatRef.set(chatData).addOnSuccessListener {
//                    // Now fetch the friend's username to return for the chat UI
//                    firestore.collection("users").document(otherUserId)
//                        .get()
//                        .addOnSuccessListener { userDoc ->
//                            val username = userDoc.getString("username") ?: "Unknown"
//                            onSuccess(chatId, username)
//                        }
//                }.addOnFailureListener {
//                    onFailure("Failed to create chat")
//                }
//            } else {
//                // Chat exists, fetch friend's username and return
//                firestore.collection("users").document(otherUserId)
//                    .get()
//                    .addOnSuccessListener { userDoc ->
//                        val username = userDoc.getString("username") ?: "Unknown"
//                        onSuccess(chatId, username)
//                    }
//            }
//        }.addOnFailureListener {
//            onFailure("Failed to check chat")
//        }
//    }
}


//fun getFriendUsernames(onLoaded: (List<Pair<String, String>>) -> Unit) {
//    if (friendUids.isEmpty()) {
//        onLoaded(emptyList())
//        return
//    }
//    val results = mutableListOf<Pair<String, String>>()
//    var count = 0
//    friendUids.forEach { uid ->
//        fetchUsername(uid) { uname ->
//            results.add(uid to uname)
//            count++
//            if (count == friendUids.size) {
//                onLoaded(results)
//            }
//        }
//    }
//}
//
//fun getFriendRequestsUsernames(onLoaded: (List<Pair<String, String>>) -> Unit) {
//    if (friendRequests.isEmpty()) {
//        onLoaded(emptyList())
//        return
//    }
//    val results = mutableListOf<Pair<String, String>>()
//    var count = 0
//    friendRequests.forEach { uid ->
//        fetchUsername(uid) { uname ->
//            results.add(uid to uname)
//            count++
//            if (count == friendRequests.size) {
//                onLoaded(results)
//            }
//        }
//    }
//}
//
//fun getSentRequestsUsernames(onLoaded: (List<Pair<String, String>>) -> Unit) {
//    if (sentRequests.isEmpty()) {
//        onLoaded(emptyList())
//        return
//    }
//    val results = mutableListOf<Pair<String, String>>()
//    var count = 0
//    sentRequests.forEach { uid ->
//        fetchUsername(uid) { uname ->
//            results.add(uid to uname)
//            count++
//            if (count == sentRequests.size) {
//                onLoaded(results)
//            }
//        }
//    }
//}