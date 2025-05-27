package com.example.guild.chatResources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ChatScreen(
    chatId: String,
    otherUserId: String,
    username: String, // will be replaced
    chatViewModel: ChatViewModel
) {
    val messages by chatViewModel.messages.collectAsState()
    val senderUsernames by remember { derivedStateOf { chatViewModel.senderUsernames } }
    val partnerName by chatViewModel.chatPartnerName

    var messageText by remember { mutableStateOf("") }
    var isDisappearing by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Fetch messages + partner name
    LaunchedEffect(chatId) {
        chatViewModel.listenForMessages(chatId)
        chatViewModel.fetchChatPartnerName(otherUserId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFBB86FC), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = partnerName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = partnerName, color = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { /* menu */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1F1F1F))
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            items(messages) { msg ->
                val senderName = senderUsernames[msg.senderId] ?: msg.senderId

                // Mark message as seen if it's from the other user and not already seen
                if (msg.senderId != currentUserId && msg.seenAt == 0L) {
                    chatViewModel.markMessageAsSeen(chatId, msg.messageId)
                }

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(text = senderName, fontSize = 12.sp, color = Color.Gray)
                    Text(text = msg.text, fontSize = 16.sp, color = Color.White)
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F1F1F))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* + */ }) {
                Text("+", color = Color.White, fontSize = 20.sp)
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                placeholder = { Text("Type a message...", color = Color.Gray) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.Gray,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = isDisappearing,
                    onCheckedChange = { isDisappearing = it }
                )
                Text("Disappear", fontSize = 10.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(onClick = {
                if (messageText.isNotBlank()) {
                    chatViewModel.sendMessage(chatId, messageText.trim(), isDisappearing)
                    messageText = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}
