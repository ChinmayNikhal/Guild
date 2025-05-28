package com.example.guild.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.guild.calendar.model.Priority
import com.example.guild.calendar.model.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditTaskScreen(
    initialTask: Task? = null,
    currentUserId: String,
    onSave: (Task) -> Unit,
    onCancel: () -> Unit
) {
    val isEditing = initialTask != null

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var dueDateString by remember {
        mutableStateOf(
            initialTask?.dueDate?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
            } ?: ""
        )
    }
    var priority by remember { mutableStateOf(initialTask?.priority ?: Priority.MEDIUM) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Task" else "Create Task") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dueDateString,
                onValueChange = { dueDateString = it },
                label = { Text("Due Date (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val dueDate = try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dueDateString)
                        } catch (e: Exception) {
                            null
                        }

                        val newTask = Task(
                            id = initialTask?.id ?: UUID.randomUUID().toString(),
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            priority = priority,
                            createdBy = initialTask?.createdBy ?: currentUserId,
                            createdAt = initialTask?.createdAt ?: Date(),
                            updatedAt = Date(),
                            assignees = initialTask?.assignees ?: listOf(currentUserId)
                        )
                        onSave(newTask)
                    }
                ) {
                    Text(if (isEditing) "Update" else "Create")
                }

                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}
