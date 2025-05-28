package com.example.guild.calendar.repository

import com.example.guild.calendar.model.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class TaskRepository {

    private val db = FirebaseFirestore.getInstance()
    private val tasksRef = db.collection("tasks")

    suspend fun createTask(task: Task) {
        val docRef = tasksRef.document()
        val taskWithId = task.copy(id = docRef.id)
        docRef.set(taskWithId).await()
    }

    suspend fun updateTask(task: Task) {
        if (task.id.isNotEmpty()) {
            tasksRef.document(task.id).set(task).await()
        }
    }

    suspend fun deleteTask(taskId: String) {
        if (taskId.isNotEmpty()) {
            tasksRef.document(taskId).delete().await()
        }
    }

    suspend fun getTask(taskId: String): Task? {
        val doc = tasksRef.document(taskId).get().await()
        return if (doc.exists()) doc.toObject<Task>() else null
    }

    suspend fun getAllTasksForUser(userId: String): List<Task> {
        val snapshot = tasksRef.whereArrayContains("assignees", userId).get().await()
        return snapshot.documents.mapNotNull { it.toObject<Task>() }
    }
}
