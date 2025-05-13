package com.example.guild.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.guild.R
import com.example.guild.chatResources.ChatMessage
import com.example.guild.chatResources.ChatScreen
import com.example.guild.chatResources.ChatViewModel
import com.example.guild.ui.GroupManageScreen
import com.example.guild.ui.MainScreenState.*
import com.example.guild.ui.theme.GuildTheme


sealed class MainScreenState {
    object DM : MainScreenState()
    object Profile : MainScreenState()
    object Friends : MainScreenState()
    object Settings : MainScreenState()
    object Groups : MainScreenState()
    object GroupManage : MainScreenState()
    data class GroupChat(val groupId: String, val groupName: String) : MainScreenState()
    data class Chat(val chatId: String, val otherUserId: String, val otherUsername: String) : MainScreenState()

}

@Composable
fun MainScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onOpenChat: (String, String, String) -> Unit
) {
    var selectedScreen by remember { mutableStateOf<MainScreenState>(MainScreenState.DM) }

    val chatViewModel: ChatViewModel = viewModel()
    val messages by chatViewModel.messages.collectAsState(initial = emptyList())

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            selectedScreen = when (selectedScreen) {
                is MainScreenState.DM -> "DM"
                is MainScreenState.Profile -> "Profile"
                is MainScreenState.Friends -> "Friends"
                is MainScreenState.Settings -> "Settings"
                is MainScreenState.Groups -> "Groups"
                is MainScreenState.Chat -> "Chat"
                is MainScreenState.GroupManage-> "GroupManage"
                is MainScreenState.GroupChat -> TODO()
            },
            onScreenSelected = { screen ->
                selectedScreen = when (screen) {
                    "DM" -> MainScreenState.DM
                    "Friends" -> MainScreenState.Friends
                    "Settings" -> MainScreenState.Settings
                    "Profile" -> MainScreenState.Profile
                    "Groups" -> MainScreenState.Groups
                    "GroupManage" -> MainScreenState.GroupManage
                    else -> MainScreenState.DM
                }
            }
        )

        when (val screen = selectedScreen) {
            is MainScreenState.DM -> DirectMessagesScreen(
                onChatSelected = { chatId, otherUserId, otherUsername ->
                    chatViewModel.listenForMessages(chatId)
                    selectedScreen = Chat(chatId, otherUserId, otherUsername)
                }
            )

            is MainScreenState.Chat -> ChatScreen(
                chatId = screen.chatId,
                otherUserId = screen.otherUserId,
                username = screen.otherUsername,
                chatViewModel = chatViewModel
            )

            MainScreenState.Profile -> ProfileScreen(navController = navController)
            MainScreenState.Friends -> FriendsScreen(authViewModel = viewModel())

            MainScreenState.GroupManage -> GroupManageScreen(
                onOpenGroupChat = { groupId, groupName ->
                    selectedScreen = MainScreenState.GroupChat(groupId, groupName)
                }
            )

            MainScreenState.Settings -> SettingsScreen(onLogout = onLogout)

            MainScreenState.Groups -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Groups Page", color = Color.White, fontSize = 24.sp)
                }
            }

            is MainScreenState.GroupChat -> TODO()
        }
    }
}

@Composable
fun DirectMessagesScreen(
    onChatSelected: (chatId: String, otherUserId: String, otherUsername: String) -> Unit
) {
    val viewModel: ChatViewModel = viewModel()
    val chatList = viewModel.chatPreviews

    LaunchedEffect(Unit) {
        viewModel.loadUserChats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text("Direct Messages", color = Color.White, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (chatList.isEmpty()) {
            Text(text = "Loading Chats...", color = Color.Gray)
        }

        chatList.forEach { chat ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        onChatSelected(chat.chatId, chat.otherUserId, chat.otherUsername)
                    },
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = chat.otherUsername,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = chat.lastMessage,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}



@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings Page", color = Color.White, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { onLogout() }) {
            Text("Logout")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    GuildTheme {
        val fakeNavController = rememberNavController()
        MainScreen(
            navController = fakeNavController,
            onLogout = {},
            onOpenChat = { _, _, _ -> } // dummy implementation for preview
        )

    }
}

@Composable
fun Sidebar(selectedScreen: String, onScreenSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(65.dp)
            .fillMaxHeight()
            .background(Color.DarkGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        // DM button (Guild logo)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Gray, shape = CircleShape)
                .clickable { onScreenSelected("DM") },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Guild App Logo",
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Groups View button
        IconButton(
            onClick = { onScreenSelected("Groups") },
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (selectedScreen == "Groups") Color.LightGray else Color.Gray,
                    shape = CircleShape
                )
        ) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = "Groups", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Calendar button
        IconButton(
            onClick = { /* Calendar logic to be implemented */ },
            modifier = Modifier
                .size(40.dp)
                .background(Color.Gray, shape = CircleShape)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = Color.White)
        }

        Spacer(modifier = Modifier.weight(1f)) // Push rest to bottom

        // === BOTTOM SECTION ===
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF800080), shape = CircleShape)
                .clickable { onScreenSelected("Profile") },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Friends", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Friends button
        IconButton(
            onClick = { onScreenSelected("Friends") },
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (selectedScreen == "Friends") Color.LightGray else Color.Gray,
                    shape = CircleShape
                )
        ) {
            Icon(Icons.Default.Person, contentDescription = "Friends", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Add Group button
        IconButton(
            onClick = { onScreenSelected("GroupManage") },
            modifier = Modifier
                .size(40.dp)
                .background(Color.Gray, shape = CircleShape)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Add Group", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Settings button
        IconButton(
            onClick = { onScreenSelected("Settings") },
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (selectedScreen == "Settings") Color.LightGray else Color.Gray,
                    shape = CircleShape
                )
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}


@Composable
fun GroupsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Groups Page", color = Color.White)
    }
}

@Composable
fun GroupChatsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Group Chats Page", color = Color.White)
    }
}

// DateRange  Menu  Person  AccountBox