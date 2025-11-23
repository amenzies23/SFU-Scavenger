package com.aark.sfuscavenger.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.repositories.GameRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreateGameViewModel(
    private val gameRepo: GameRepository = GameRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _game = MutableStateFlow(Game())
    val game: MutableStateFlow<Game> = _game

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
                        description = currentGame.description
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
}