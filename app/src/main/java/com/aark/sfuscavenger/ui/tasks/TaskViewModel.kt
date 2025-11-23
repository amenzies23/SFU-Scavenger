package com.aark.sfuscavenger.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TaskUi(
    val id: String,
    val name: String,
    val description: String,
    val points: Int,
    val type: String
)

data class TaskUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val tasks: List<TaskUi> = emptyList()
)

class TaskViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(TaskUiState())
    val state: StateFlow<TaskUiState> = _state

    fun loadTasks(gameId: String) {
        _state.update { it.copy(loading = true, error = null) }

        // Launch a coroutine for Firestore work
        viewModelScope.launch {
            try {
                // Grab all tasks from Firestore
                val snap = db.collection("games")
                    .document(gameId)
                    .collection("tasks")
                    .get()
                    .await()

                // Convert Firestore docs to UI models
                val tasks = snap.documents.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.let { task ->
                        TaskUi(
                            id = doc.id,
                            name = task.name,
                            description = task.description,
                            points = task.points,
                            type = task.type
                        )
                    }
                }

                // Update UI with the data
                _state.update {
                    it.copy(
                        loading = false,
                        tasks = tasks
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message
                    )
                }
            }
        }
    }
}
