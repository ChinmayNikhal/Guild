package com.example.guild.calendar.model

import java.util.Date

object TaskUtils {
    fun computeStatus(task: Task): TaskStatus {
        return when {
            task.status == TaskStatus.COMPLETED -> TaskStatus.COMPLETED
            task.dueDate != null && task.dueDate.before(Date()) -> TaskStatus.OVERDUE
            else -> task.status
        }
    }
}
