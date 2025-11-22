package com.aark.sfuscavenger.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.repositories.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class EventsViewModel(
    private val gameRepo: GameRepository = GameRepository()
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

}