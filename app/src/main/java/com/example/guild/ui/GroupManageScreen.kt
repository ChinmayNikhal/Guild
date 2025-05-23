package com.example.guild.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guild.groupResources.GroupChatScreen
import com.example.guild.groupResources.GroupData
import com.example.guild.groupResources.GroupViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManageScreen(groupViewModel: GroupViewModel) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    val userGroups by remember { derivedStateOf { groupViewModel.userGroups } }

    var showCreateDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf(TextFieldValue("")) }
    var groupDescription by remember { mutableStateOf(TextFieldValue("")) }

    // 👇 State to track selected group for messaging
    var selectedGroup by remember { mutableStateOf<GroupData?>(null) }

    // Load groups when screen is entered
    LaunchedEffect(userId) {
        if (userId != null) {
            groupViewModel.loadUserGroups(userId)
        }
    }

    // 👇 Show GroupChatScreen if a group is selected
    selectedGroup?.let { group ->
        GroupChatScreen(
            groupId = group.groupId,
            groupName = group.name,
            groupViewModel = groupViewModel
        )
        return  // Skip rendering the rest of the UI
    }

    // 👇 Original UI for group management
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Groups", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* TODO: Join logic */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Join Group")
                }

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Group")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Group")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (userGroups.isEmpty()) {
                Text("Loading Groups...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(userGroups) { group ->
                        GroupCard(
                            groupData = group,
                            onLeaveGroup = { groupId -> groupViewModel.leaveGroup(groupId) },
                            onMessageGroup = { selectedGroup = it }  // 👈 callback to set the selected group
                        )
                    }
                }
            }
        }

        // Dialog for creating a new group
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val name = groupName.text.trim()
                        val desc = groupDescription.text.trim()
                        if (name.isNotEmpty()) {
                            groupViewModel.createGroup(name, desc)
                            groupName = TextFieldValue("")
                            groupDescription = TextFieldValue("")
                            showCreateDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("New Group") },
                text = {
                    Column {
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
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun GroupCard(
    groupData: GroupData,
    onLeaveGroup: (String) -> Unit,
    onMessageGroup: (GroupData) -> Unit  // 👈 New parameter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(groupData.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
                    if (groupData.description.isNotBlank()) {
                        Text(
                            groupData.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onLeaveGroup(groupData.groupId) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("❌", fontSize = 14.sp)
                    }

                    IconButton(
                        onClick = { onMessageGroup(groupData) },  // 👈 Trigger navigation
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("💬", fontSize = 14.sp)
                    }
                }
            }

            if (groupData.mostRecentMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Latest: ${groupData.mostRecentMessage}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
