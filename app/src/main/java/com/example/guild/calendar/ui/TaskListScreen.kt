// TaskListScreen.kt
package com.example.guild.calendar.ui

import android.widget.CalendarView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.core.widget.TextViewCompat
import com.example.guild.calendar.model.Task
import com.example.guild.calendar.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.widget.TextView

enum class TaskScreenView {
    LIST, CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    currentUserId: String,
    onTaskClick: (Task) -> Unit,
    onCreateTaskClick: () -> Unit
) {
    val taskList by viewModel.taskList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessageFlow = viewModel.errorMessage.collectAsState()
    val errorMessage by remember { errorMessageFlow }

    var currentView by remember { mutableStateOf(TaskScreenView.CALENDAR) } // Start with calendar view
    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(calendar.time) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LaunchedEffect(currentUserId) {
        viewModel.loadTasksForUser(currentUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") }
            )
        },
        floatingActionButton = {
            if (currentView == TaskScreenView.LIST || currentView == TaskScreenView.CALENDAR) {
                FloatingActionButton(onClick = onCreateTaskClick) { // Using Material 3 FAB
                    Icon(Icons.Default.Add, contentDescription = "Create Task")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { currentView = TaskScreenView.LIST },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentView == TaskScreenView.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (currentView == TaskScreenView.LIST) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("List") // Using Material 3 Text
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { currentView = TaskScreenView.CALENDAR },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentView == TaskScreenView.CALENDAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (currentView == TaskScreenView.CALENDAR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Calendar") // Using Material 3 Text
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (currentView) {
                TaskScreenView.LIST -> {
                    when {
                        isLoading -> CircularProgressIndicator()
                        errorMessageFlow.value != null -> Text(text = errorMessageFlow.value!!, color = Color.Red) // Using Material 3 Text
                        else -> {
                            if (taskList.isEmpty()) {
                                Text("No tasks yet.") // Using Material 3 Text
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(taskList) { task ->
                                        TaskCard(task = task, onClick = { onTaskClick(task) })
                                    }
                                }
                            }
                        }
                    }
                }
                TaskScreenView.CALENDAR -> {
                    val context = LocalContext.current
                    val isDarkTheme = isSystemInDarkTheme()
                    AndroidView(
                        factory = {
                            CalendarView(it).apply {
                                setOnDateChangeListener { _, year, month, dayOfMonth ->
                                    val calendarInstance = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    selectedDate = calendarInstance.time
                                }
                                // Attempt to set text color for dark mode (month title)
                                val textColor = if (isDarkTheme) White.hashCode() else Black.hashCode()
                                try {
                                    val firstTextView = getChildAt(0) as? TextView
                                    firstTextView?.setTextColor(textColor)
                                } catch (e: Exception) {
                                    // Handle potential exceptions if the view structure changes
                                    e.printStackTrace()
                                }
                            }
                        },
                        update = { view ->
                            val textColor = if (isDarkTheme) White.hashCode() else Black.hashCode()
                            try {
                                val firstTextView = view.getChildAt(0) as? TextView
                                firstTextView?.setTextColor(textColor)
                            } catch (e: Exception) {
                                // Handle potential exceptions if the view structure changes
                                e.printStackTrace()
                            }
                            // Further customization of other elements is difficult with CalendarView
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tasks for ${displayDateFormatter.format(selectedDate)}", // Using Material 3 Text
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val selectedDateString = dateFormatter.format(selectedDate)
                    val tasksForSelectedDate = taskList.filter { task ->
                        task.dueDate?.let { dateFormatter.format(it) } == selectedDateString
                    }
                    if (tasksForSelectedDate.isNotEmpty()) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tasksForSelectedDate) { task ->
                                TaskCard(task = task, onClick = { onTaskClick(task) })
                            }
                        }
                    } else {
                        Text("No tasks for the selected day.") // Using Material 3 Text
                    }
                }
            }
        }
    }
}

// Assuming TaskCard and StatusChip are using Material 3 components
@Composable
fun TaskCard(task: Task, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            task.dueDate?.let {
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                Text(text = "Due: ${dateFormatter.format(it)}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            StatusChip(status = task.status)
        }
    }
}

@Composable
fun StatusChip(status: com.example.guild.calendar.model.TaskStatus) {
    val color = when (status) {
        com.example.guild.calendar.model.TaskStatus.PENDING -> Color.Blue
        com.example.guild.calendar.model.TaskStatus.IN_PROGRESS -> Color(0xFFFFA500)
        com.example.guild.calendar.model.TaskStatus.COMPLETED -> Color.Green
        com.example.guild.calendar.model.TaskStatus.OVERDUE -> Color.Red
    }

    androidx.compose.material3.Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = status.name.replace("_", " "),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}