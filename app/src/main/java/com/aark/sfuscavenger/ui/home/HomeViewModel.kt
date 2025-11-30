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

    // Games to display as markers on the home map
    var mapGames by mutableStateOf<List<Game>>(emptyList())
        private set

    init {
        // Re-join: only "started" games current user is in(we need to enforce only one active game)
        viewModelScope.launch {
            liveGamesForUser = gameRepo.getLiveGamesForCurrentUser()
        }

        // Map: listen to all games and filter by status + location
        viewModelScope.launch {
            gameRepo.observeGames().collect { allGames ->
                mapGames = allGames.filter { game ->
                    val loc = game.location
                    if (loc == null) {
                        false
                    } else {
                        when {
                            game.status == "live" -> true // live
                            game.status == "draft" && game.startTime != null -> true // scheduled
                            else -> false
                        }
                    }
                }
            }
        }
    }
}
