package com.example.guild.calendar.model

data class TaskEditorState(
    val title: String = "",
    val description: String = "",
    val dueDateMillis: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val assignees: List<String> = emptyList(),
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null
) {
    fun isValid(): Boolean = title.isNotBlank()
}
