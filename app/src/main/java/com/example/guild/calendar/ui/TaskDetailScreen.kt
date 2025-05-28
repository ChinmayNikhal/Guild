package com.example.guild.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.guild.calendar.model.Task
import com.example.guild.calendar.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task,
    currentUserId: String,
    isCreator: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task.title) },
                actions = {
                    if (isCreator) {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Status: ${task.status.name.replace("_", " ")}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            task.dueDate?.let {
                val dateText = SimpleDateFormat("EEE, MMM d, yyyy 'at' hh:mm a", Locale.getDefault()).format(it)
                Text("Due: $dateText", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text("Priority: ${task.priority.name}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (task.description.isNotBlank()) {
                Text("Description", style = MaterialTheme.typography.titleSmall)
                Text(task.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("Assigned by: ${task.createdBy}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (task.assignees.isNotEmpty()) {
                Text("Assigned to:", style = MaterialTheme.typography.titleSmall)
                LazyColumn {
                    items(task.assignees) { userId ->
                        Text(userId, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (task.attachments.isNotEmpty()) {
                Text("Attachments", style = MaterialTheme.typography.titleSmall)
                task.attachments.forEach {
                    Text("- $it", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (currentUserId in task.assignees) {
                when (task.status) {
                    TaskStatus.PENDING, TaskStatus.IN_PROGRESS -> Button(onClick = {
                        onStatusChange(TaskStatus.COMPLETED)
                    }) {
                        Text("Mark as Complete")
                    }
                    TaskStatus.COMPLETED -> OutlinedButton(onClick = {
                        onStatusChange(TaskStatus.IN_PROGRESS)
                    }) {
                        Text("Reopen Task")
                    }
                    else -> {}
                }
            }
        }
    }
}
