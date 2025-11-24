package com.aark.sfuscavenger.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.repositories.GameRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventsViewModel(
    private val gameRepo: GameRepository = GameRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        observeGames()
    }

    private fun observeGames() {
        viewModelScope.launch {
            gameRepo.observeGames()
                .catch { e -> _error.value = e.message }
                .collect { list -> _games.value = list }
        }
    }

    val publicGames: StateFlow<List<Game>> =
        games.map { list ->
            list.filter { it.status == "live" && it.joinMode == "open" }
                .sortedWith(
                    compareBy<Game> { it.name }
                        .thenBy { it.id }
                )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    val privateGames: StateFlow<List<Game>> =
        games.map { list ->
            list.filter { it.status == "live" && it.joinMode == "code" }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    val myGames: StateFlow<List<Game>> =
        games.map { list ->
            list
                .filter { it.status == "draft" && it.ownerId == auth.currentUser?.uid }
                .sortedWith(
                    compareByDescending<Game> { it.updatedAt?.seconds ?: 0L }
                        .thenByDescending { it.createdAt?.seconds ?: 0L }
                        .thenBy { it.id }
                )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    fun loadGames() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try{
                val list = gameRepo.getAllGames()
                _games.value = list
            } catch (t: Throwable) {
                _error.value = t.message?: "Failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteGame(gameId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                gameRepo.deleteGame(gameId)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Failed to delete game"
            }
        }
    }

    fun publishGame(gameId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                gameRepo.publishGame(gameId, "live")
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to launch game"
            }
        }
    }

}