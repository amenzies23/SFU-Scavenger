package com.aark.sfuscavenger.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.repositories.GameRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val gameRepo: GameRepository = GameRepository()
) : ViewModel() {
    // Live games this user is currently a member of
    var liveGamesForUser by mutableStateOf<List<Game>>(emptyList())
        private set

    init {
        // On Home load, check if this user has any live games
        viewModelScope.launch {
            liveGamesForUser = gameRepo.getLiveGamesForCurrentUser()
        }
    }
}
