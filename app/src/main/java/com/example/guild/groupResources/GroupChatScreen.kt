package com.example.guild.groupResources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guild.chatResources.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    groupName: String,
    groupViewModel: GroupViewModel
) {
    val messages by groupViewModel.messages.collectAsState()
    val usernames = groupViewModel.senderUsernames
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        groupViewModel.listenForGroupMessages(groupId)
    }

    // Scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = groupName,
                    modifier = Modifier.clickable { showDialog = true },
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            actions = {
                IconButton(onClick = { /* TODO: kebab menu */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            items(messages) { msg ->
                val sender = usernames[msg.senderId] ?: msg.senderId
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(sender, fontSize = 12.sp, color = Color.Gray)
                    Text(msg.text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                        fontSize = 10.sp, color = Color.Gray
                    )
                }
            }
        }

        // Message Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* TODO: attachments */ },
                modifier = Modifier.size(48.dp)
            ) {
                Text("+", fontSize = 24.sp)
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                placeholder = { Text("Message...") }
            )

            IconButton(onClick = {
                if (messageText.isNotBlank()) {
                    groupViewModel.sendGroupMessage(groupId, messageText.trim())
                    messageText = ""
                }
            }) {
                Text("➤", fontSize = 18.sp)
            }
        }
    }

    // Group Info Dialog
    if (showDialog) {
        GroupInfoDialog(groupId = groupId, onDismiss = { showDialog = false })
    }
}

@Composable
fun GroupInfoDialog(
    groupId: String,
    onDismiss: () -> Unit,
    groupViewModel: GroupViewModel = GroupViewModel()
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<String>>(emptyList()) }
    var admins by remember { mutableStateOf<List<String>>(emptyList()) }
    var ownerId by remember { mutableStateOf("") }

    LaunchedEffect(groupId) {
        val doc = FirebaseFirestore.getInstance().collection("Groups").document(groupId).get().await()
        groupName = doc.getString("groupName") ?: ""
        description = doc.getString("groupDescription") ?: ""
        members = doc.get("members") as? List<String> ?: emptyList()
        admins = doc.get("administrators") as? List<String> ?: emptyList()
        ownerId = doc.getString("owner") ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Group Info") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(groupName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(description, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                Divider()
                Text("Members:", fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
                    items(members) { uid ->
                        val color = when (uid) {
                            ownerId -> Color(0xFFBB86FC) // purple
                            in admins -> Color.Red
                            else -> Color.Blue
                        }
                        val name = groupViewModel.senderUsernames[uid] ?: uid
                        Text(name, color = color)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxHeight()
    )
}
