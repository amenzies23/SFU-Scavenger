package com.aark.sfuscavenger.repositories

import com.aark.sfuscavenger.data.models.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TaskRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getTask(gameId: String, taskId: String): Task? {
        if (taskId.isBlank()) return null

        val snap = db.collection("games")
            .document(gameId)
            .collection("tasks")
            .document(taskId)
            .get()
            .await()

        return if (snap.exists()) {
            snap.toObject(Task::class.java)?.copy(id = snap.id)
        } else {
            null
        }
    }
}
