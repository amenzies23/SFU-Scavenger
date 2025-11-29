package com.aark.sfuscavenger.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.data.models.Task
import com.aark.sfuscavenger.repositories.GameRepository
import com.aark.sfuscavenger.repositories.TaskRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CreateGameViewModel(
    private val gameRepo: GameRepository = GameRepository(),
    private val taskRepo: TaskRepository = TaskRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _game = MutableStateFlow(Game())
    val game: MutableStateFlow<Game> = _game

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _gameCreated = MutableStateFlow(false)
    val gameCreated: StateFlow<Boolean> = _gameCreated

    private var isEditMode = false

    fun loadGame(gameId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val existingGame = gameRepo.getGame(gameId)
                if (existingGame != null) {
                    _game.value = existingGame
                    isEditMode = true

                    observeTasks(gameId)
                } else {
                    _error.value = "Game not found"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load game"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateName(name: String) {
        _game.value = _game.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _game.value = _game.value.copy(description = description)
    }

    fun updateJoinMode(joinMode: String) {
        _game.value = _game.value.copy(joinMode = joinMode)
    }

    fun updateJoinCode(joinCode: String) {
        _game.value = _game.value.copy(joinCode = joinCode)
    }

    fun updateLocation(geoPoint: GeoPoint) {
        _game.value = _game.value.copy(location = geoPoint)
    }

    fun updateStartTime(timestamp: Timestamp?) {
        _game.value = _game.value.copy(startTime = timestamp)
    }

    fun saveGame() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val currentGame = _game.value

                if (currentGame.name.isBlank()) {
                    _error.value = "Game name cannot be empty"
                    _loading.value = false
                    return@launch
                }

                if (isEditMode) {
                    // Update existing game
                    gameRepo.updateGame(currentGame)
                } else {
                    // Create new game
                    gameRepo.createGame(
                        name = currentGame.name,
                        lat = currentGame.location?.latitude ?: 49.2827,
                        lng = currentGame.location?.longitude ?: -123.1207,
                        joinMode = currentGame.joinMode,
                        joinCode = currentGame.joinCode?.ifBlank { null },
                        description = currentGame.description,
                        startTime = currentGame.startTime
                    )
                }

                _gameCreated.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create game"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun observeTasks(gameId: String) {
        viewModelScope.launch {
            taskRepo.observeTasks(gameId)
                .catch { e -> _error.value = e.message }
                .collect { taskList -> _tasks.value = taskList }
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            _error.value = null
            try {
                val gameId = _game.value.id
                if (gameId.isBlank()) {
                    _error.value = "Save the game first before adding tasks"
                    return@launch
                }
                taskRepo.createTask(gameId, task)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add task"
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            _error.value = null
            try {
                taskRepo.updateTask(_game.value.id, task)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update task"
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                taskRepo.deleteTask(_game.value.id, taskId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete task"
            }
        }
    }
}