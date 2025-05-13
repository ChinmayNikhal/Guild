package com.example.guild.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guild.chatResources.ChatViewModel
import com.example.guild.chatResources.GroupData

@Composable
fun GroupManageScreen(
    chatViewModel: ChatViewModel = viewModel(),
    onOpenGroupChat: (String, String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }

    // Load user groups on first composition
    LaunchedEffect(Unit) {
        chatViewModel.loadUserGroups()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Create a New Group", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = groupDescription,
            onValueChange = { groupDescription = it },
            label = { Text("Group Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (groupName.isNotBlank()) {
                    chatViewModel.createGroup(groupName.trim(), groupDescription.trim())
                    groupName = ""
                    groupDescription = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add +")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Your Groups", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chatViewModel.userGroups) { group ->
                GroupListItem(
                    group = group,
                    onLeave = { chatViewModel.leaveGroup(group.groupId) },
                    onMessage = {
                        onOpenGroupChat(group.groupId, group.name)
                    }
                )
                Divider()
            }
        }
    }
}

@Composable
fun GroupListItem(
    group: GroupData,
    onLeave: () -> Unit,
    onMessage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(onClick = onMessage) {
            Text("Message")
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedButton(onClick = onLeave) {
            Text("Leave")
        }
    }
}
