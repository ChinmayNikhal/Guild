package com.example.guild.calendar.model

import java.util.Date

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: Date? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val assignees: List<String> = emptyList(),  // UID list
    val attachments: List<String> = emptyList(), // URLs or paths
    val createdBy: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null // e.g. "WEEKLY", "MONTHLY"
)

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, OVERDUE
}

enum class Priority {
    LOW, MEDIUM, HIGH
}
