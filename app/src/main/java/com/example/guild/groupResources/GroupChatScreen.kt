package com.example.guild.groupResources

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guild.chatResources.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    groupName: String,
    groupViewModel: GroupViewModel,
    onLeaveGroup: () -> Unit
) {
    val messages by groupViewModel.messages.collectAsState()
    val usernames = groupViewModel.senderUsernames
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAdminPanelDialog by remember { mutableStateOf(false) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var isAdmin by remember { mutableStateOf(false) }
    var groupMembers by remember { mutableStateOf<List<GroupMember>>(emptyList()) }

    val context = LocalContext.current

    // Load group data
    LaunchedEffect(groupId) {
        groupViewModel.listenForGroupMessages(groupId)

        FirebaseFirestore.getInstance().collection("Groups").document(groupId).get()
            .addOnSuccessListener { doc ->
                val owner = doc.getString("owner")
                val admins = doc.get("administrators") as? List<String> ?: emptyList()
                isAdmin = currentUserId == owner || admins.contains(currentUserId)
            }

        groupViewModel.fetchGroupMembers(groupId) { members ->
            groupMembers = members
        }
    }

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
                    modifier = Modifier.clickable { showInfoDialog = true },
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text("Admin Panel") },
                                onClick = {
                                    showAdminPanelDialog = true
                                    showMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Group Info") },
                            onClick = {
                                showInfoDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Leave Group") },
                            onClick = {
                                groupViewModel.leaveGroup(groupId)
                                showMenu = false
                                onLeaveGroup()
                            }
                        )
                    }
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* attachments */ }) {
                Text("+", fontSize = 24.sp)
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
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

    if (showInfoDialog) {
        GroupInfoDialog(
            groupId = groupId,
            onDismiss = { showInfoDialog = false },
            groupViewModel = groupViewModel
        )
    }

    if (isAdmin && showAdminPanelDialog) {
        AdminPanelDialog(
            groupId = groupId,
            groupMembers = groupMembers,
            onDismiss = { showAdminPanelDialog = false },
            groupViewModel = groupViewModel
        )
    }
}

@Composable
fun AdminPanelDialog(
    groupId: String,
    groupMembers: List<GroupMember>,
    onDismiss: () -> Unit,
    groupViewModel: GroupViewModel
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Admin Panel") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Members:", fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.fillMaxHeight(0.4f)) {
                    items(groupMembers) { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(member.username, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                groupViewModel.toggleAdminStatus(groupId, member.uid)
                            }) {
                                Icon(Icons.Default.Star, contentDescription = "Toggle Admin")
                            }
                            IconButton(onClick = {
                                groupViewModel.removeMember(groupId, member.uid)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { showInviteDialog = true }) {
                    Text("Send Group Invite")
                }

                Button(onClick = { showRequestsDialog = true }) {
                    Text("Review Join Requests")
                }

                Button(onClick = { showBlockedDialog = true }) {
                    Text("View Blocked Users")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        groupViewModel.deleteGroup(groupId)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Group", color = Color.White)
                }
            }
        }
    )

    if (showInviteDialog) {
        InviteUserDialog(groupId, onDismiss = { showInviteDialog = false }, groupViewModel)
    }

    if (showRequestsDialog) {
        JoinRequestsDialog(groupId, onDismiss = { showRequestsDialog = false }, groupViewModel)
    }

    if (showBlockedDialog) {
        BlockedUsersDialog(groupId, onDismiss = { showBlockedDialog = false }, groupViewModel)
    }
}


@Composable
fun GroupInfoDialog(
    groupId: String,
    onDismiss: () -> Unit,
    groupViewModel: GroupViewModel
) {
    val clipboardManager = LocalClipboardManager.current
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Group ID: $groupId", fontSize = 12.sp, color = Color.Gray)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(groupId))
                        },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Copy", modifier = Modifier.size(24.dp))
                    }
                }

                Divider()
                Text("Members:", fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
                    items(members) { uid ->
                        val color = when (uid) {
                            ownerId -> Color(0xFFBB86FC)
                            in admins -> Color.Red
                            else -> Color.Blue
                        }
                        val name = groupViewModel.senderUsernames[uid] ?: uid
                        Text(name, color = color)
                    }
                }
            }
        }
    )
}

@Composable
fun InviteUserDialog(groupId: String, onDismiss: () -> Unit, groupViewModel: GroupViewModel) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                groupViewModel.sendGroupInvite(groupId, username)
                onDismiss()
            }) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Send Group Invite") },
        text = {
            Column {
                Text("Enter username to invite:")
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("e.g. johndoe") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun JoinRequestsDialog(groupId: String, onDismiss: () -> Unit, groupViewModel: GroupViewModel) {
    var requests by remember { mutableStateOf<List<String>>(emptyList()) }
    val usernames = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(true) {
        groupViewModel.getPendingRequests(groupId) { reqs ->
            requests = reqs
            reqs.forEach { uid ->
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener {
                        val name = it.getString("username") ?: "Unknown"
                        usernames[uid] = name
                    }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Join Requests") },
        text = {
            Column {
                if (requests.isEmpty()) {
                    Text("No pending requests.")
                } else {
                    requests.forEach { uid ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(usernames[uid] ?: uid)
                            Row {
                                IconButton(onClick = {
                                    groupViewModel.approveRequest(groupId, uid)
                                    onDismiss()
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Approve")
                                }
                                IconButton(onClick = {
                                    FirebaseFirestore.getInstance().collection("Groups")
                                        .document(groupId)
                                        .update("requests", FieldValue.arrayRemove(uid))
                                    onDismiss()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Decline")
                                }
                            }
                        }
                    }
//
                }
            }
        }
    )
}

@Composable
fun BlockedUsersDialog(groupId: String, onDismiss: () -> Unit, groupViewModel: GroupViewModel) {
    var blocked by remember { mutableStateOf<List<GroupMember>>(emptyList()) }

    LaunchedEffect(true) {
        groupViewModel.getBlockedUsers(groupId) {
            blocked = it
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Blocked Users") },
        text = {
            Column {
                if (blocked.isEmpty()) {
                    Text("No blocked users.")
                } else {
                    blocked.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(member.username)
                            IconButton(onClick = {
                                groupViewModel.unblockUser(groupId, member.uid)
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Unblock")
                            }
                        }
                    }
                }
            }
        }
    )
}