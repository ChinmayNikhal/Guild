package com.example.guild.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guild.auth.AuthViewModel
import kotlin.math.absoluteValue

@Composable
fun FriendsScreen(authViewModel: AuthViewModel = viewModel()) {
    val friendUsername = remember { mutableStateOf("") }
    val friendUids by remember { derivedStateOf { authViewModel.friendUids } }
    val receivedRequests: List<String> by remember { derivedStateOf { authViewModel.friendRequests } }
    val sentRequests: List<String> by remember { derivedStateOf { authViewModel.sentRequests } }
    var requestMessage by remember { mutableStateOf("") }

    fun refreshData() {
        authViewModel.loadFriendsAndRequests()
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Text("Friends", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = friendUsername.value,
                onValueChange = { friendUsername.value = it },
                label = { Text("Enter friend's username") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                authViewModel.sendFriendRequest(friendUsername.value.trim(),
                    onSuccess = {
                        requestMessage = "Request sent!"
                        refreshData()
                    },
                    onFailure = {
                        requestMessage = it
                    }
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }

        if (requestMessage.isNotBlank()) {
            Text(requestMessage, color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Friend Requests", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { refreshData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray)
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (receivedRequests.isEmpty() && sentRequests.isEmpty()) {
                Text("No pending requests", color = Color.Gray)
            } else {
                // Received Requests
                if (receivedRequests.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.medium)
                            .padding(12.dp)
                    ) {
                        receivedRequests.forEach { uid ->
                            val username by produceState<String?>(initialValue = null) {
                                authViewModel.fetchUsername(uid) { fetched ->
                                    value = fetched
                                }
                            }

                            if (username != null) {
                                FriendRequestItem(
                                    uid = uid,
                                    username = username!!,
                                    onAccept = {
                                        authViewModel.acceptFriendRequest(uid) { success ->
                                            requestMessage = if (success) {
                                                "Friend request accepted!"
                                            } else {
                                                "Something went wrong. Try again."
                                            }
                                        }
                                        requestMessage = "Friend request accepted!"
                                        refreshData()
                                    },
                                    onReject = {
                                        authViewModel.rejectFriendRequest(uid)
                                        requestMessage = "Friend request rejected."
                                        refreshData()
                                    }
                                )
                            }
                        }
                    }
                }

                // Sent Requests
                sentRequests.forEach { uid ->
                    val username by produceState<String?>(initialValue = null) {
                        authViewModel.fetchUsername(uid) { fetched ->
                            value = fetched
                        }

                    }

                    if (username != null) {
                        Text("Sent to $username", color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Your Friends", style = MaterialTheme.typography.titleMedium, color = Color.White)

        Column(modifier = Modifier.fillMaxWidth()) {
            friendUids.forEach { uid ->
                val username by produceState<String?>(initialValue = null) {
                    authViewModel.fetchUsername(uid) { fetched ->
                        value = fetched
                    }

                }

                if (username != null) {
                    FriendListItem(
                        username = username!!,
                        onChat = {
                            authViewModel.startChatWithFriend(
                                otherUserId = uid,
                                otherUsername = username!!,
                                onChatReady = { chatId, otherId, otherName ->
                                    requestMessage = "Chat ready with $otherName!"
                                    // TODO: Navigate to ChatScreen(chatId, otherUserId, otherUsername)
                                }
                            )
                        },
                        onRemove = {
                            authViewModel.removeFriend(uid)
                            requestMessage = "$username removed from friends"
                            refreshData()
                        }
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun FriendListItem(username: String, onChat: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarColor = remember { generateColorFromUsername(username) }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(avatarColor, CircleShape)
                .border(1.dp, Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.first().uppercase(),
                color = Color.White,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(username, modifier = Modifier.weight(1f), fontSize = 16.sp, color = Color.White)

        IconButton(onClick = onChat) {
            Icon(Icons.Default.MailOutline, contentDescription = "Chat", tint = Color.LightGray)
        }

        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.LightGray)
        }
    }
}

@Composable
fun FriendRequestItem(uid: String, username: String, onAccept: () -> Unit, onReject: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "From: $username",
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 14.sp
        )

        IconButton(onClick = onAccept) {
            Icon(Icons.Default.Done, contentDescription = "Accept", tint = Color.LightGray)
        }
        IconButton(onClick = onReject) {
            Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.LightGray)
        }
    }
}

fun generateColorFromUsername(username: String): Color {
    val colors = listOf(
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0),
        Color(0xFF29B6F6), Color(0xFF66BB6A), Color(0xFFFFCA28)
    )
    val index = username.hashCode().absoluteValue % colors.size
    return colors[index]
}
