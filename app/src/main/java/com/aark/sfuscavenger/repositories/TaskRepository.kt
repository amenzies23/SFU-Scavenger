package com.aark.sfuscavenger.repositories

import androidx.compose.runtime.snapshotFlow
import com.aark.sfuscavenger.data.models.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
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

//    Get all tasks for a game
    suspend fun getAllTasksForGame(gameId: String): List<Task> {
        val snap = db.collection("games")
            .document(gameId)
            .collection("tasks")
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            doc.toObject(Task::class.java)?.copy(id = doc.id)
        }
    }

    fun observeTasks(gameId: String) = callbackFlow<List<Task>> {
        val listener = db.collection("games")
            .document(gameId)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createTask(gameId: String, task: Task): String {
        val docRef = db.collection("games")
            .document(gameId)
            .collection("tasks")
            .document()

        val newTask = task.copy(
            id = docRef.id,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        docRef.set(newTask).await()
        return docRef.id
    }

    suspend fun updateTask(gameId: String, task: Task) {
        db.collection("games")
            .document(gameId)
            .collection("tasks")
            .document(task.id)
            .update(
                mapOf(
                    "name" to task.name,
                    "description" to task.description,
                    "points" to task.points,
                    "type" to task.type,
                    "validationMode" to task.validationMode,
                    "value" to task.value,
                    "dependsOnTaskIds" to task.dependsOnTaskIds,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
    }

    suspend fun deleteTask(gameId: String, taskId: String) {
        db.collection("games")
            .document(gameId)
            .collection("tasks")
            .document(taskId)
            .delete()
            .await()
    }
}
