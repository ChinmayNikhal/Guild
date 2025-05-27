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
fun GroupManageScreen(
    groupViewModel: GroupViewModel,
    onOpenGroupChat: (GroupData) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    val userGroups by remember { derivedStateOf { groupViewModel.userGroups } }
    val groupInvites by remember { derivedStateOf { groupViewModel.groupInvites } }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showInvitesDialog by remember { mutableStateOf(false) }

    var groupName by remember { mutableStateOf(TextFieldValue("")) }
    var groupDescription by remember { mutableStateOf(TextFieldValue("")) }
    var joinGroupId by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(userId) {
        userId?.let {
            groupViewModel.loadUserGroups(it)
            groupViewModel.loadGroupInvites(it) // Load invites too
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Groups", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Row 1: Join + Create
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Join Group")
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create Group")
                    }
                }

                // Row 2: View Invites + Spacer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            userId?.let { groupViewModel.loadGroupInvites(it) } // Refresh
                            showInvitesDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Invites")
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (userGroups.isEmpty()) {
                Text("No groups joined yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(userGroups) { group ->
                        GroupCard(
                            groupData = group,
                            onLeaveGroup = { groupId -> groupViewModel.leaveGroup(groupId) },
                            onMessageGroup = { onOpenGroupChat(it) }
                        )
                    }
                }
            }
        }

        // --- Create Group Dialog ---
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
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- Join Group Dialog ---
        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val groupId = joinGroupId.text.trim()
                        if (groupId.isNotEmpty()) {
                            groupViewModel.sendJoinRequest(groupId)
                            joinGroupId = TextFieldValue("")
                            showJoinDialog = false
                        }
                    }) {
                        Text("Send Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Join Group") },
                text = {
                    OutlinedTextField(
                        value = joinGroupId,
                        onValueChange = { joinGroupId = it },
                        label = { Text("Group ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- View Invites Dialog ---
        if (showInvitesDialog) {
            AlertDialog(
                onDismissRequest = { showInvitesDialog = false },
                confirmButton = {
                    TextButton(onClick = { showInvitesDialog = false }) {
                        Text("Close")
                    }
                },
                title = { Text("Group Invites") },
                text = {
                    if (groupInvites.isEmpty()) {
                        Text("No group invites.")
                    } else {
                        Column {
                            groupInvites.forEach { invite ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(invite.name, fontSize = 16.sp)
                                        if (invite.description.isNotBlank()) {
                                            Text(invite.description, fontSize = 14.sp, color = Color.Gray)
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            TextButton(onClick = {
                                                groupViewModel.acceptGroupInvite(invite.groupId)
                                                showInvitesDialog = false
                                            }) {
                                                Text("✔️ Accept")
                                            }
                                            TextButton(onClick = {
                                                groupViewModel.declineGroupInvite(invite.groupId)
                                                showInvitesDialog = false
                                            }) {
                                                Text("❌ Decline")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}


@Composable
fun GroupCard(
    groupData: GroupData,
    onLeaveGroup: (String) -> Unit,
    onMessageGroup: (GroupData) -> Unit
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
