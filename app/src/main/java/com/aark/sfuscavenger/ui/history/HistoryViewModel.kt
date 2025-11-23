package com.aark.sfuscavenger.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.repositories.GameRepository
import com.aark.sfuscavenger.repositories.TeamRepository
import com.aark.sfuscavenger.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val cards: List<HistoryCard> = emptyList()
)

data class HistoryCard(
    val gameId: String,
    val teamId: String?,
    val title: String,
    val placement: String,
    val joinedAt: Long
)

class HistoryViewModel(
    private val gameRepository: GameRepository = GameRepository(),
    private val teamRepository: TeamRepository = TeamRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

// For testing purposes, use this to look like what it'll look like in history!
//    private val placeholderCards = listOf(
//        HistoryCard(
//            gameId = "demoGame",
//            teamId = null,
//            title = "Campus Quest",
//            placement = "1st place",
//            joinedAt = System.currentTimeMillis()
//        )
//    )

    init {
        refreshHistory()
    }

    fun refreshHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                val memberships = userRepository.getMembershipsForUser()
                memberships.mapNotNull { membership ->
                    val game = gameRepository.getGame(membership.gameId) ?: return@mapNotNull null
                    val placementLabel = membership.teamId?.let { teamId ->
                        teamRepository.getTeamSummary(membership.gameId, teamId)
                            ?.placement
                            ?.toOrdinalString()
                    } ?: "In progress"

                    HistoryCard(
                        gameId = game.id,
                        teamId = membership.teamId,
                        title = game.name.ifBlank { "Untitled Game" },
                        placement = placementLabel,
                        joinedAt = membership.joinedAt
                    )
                }.sortedByDescending { it.joinedAt }
            }.onSuccess { cards ->
//                val displayCards = cards.ifEmpty { placeholderCards }
                _uiState.value = HistoryUiState(
                    loading = false,
                    error = null,
                    cards = cards
                )
            }.onFailure { error ->
                _uiState.value = HistoryUiState(
                    loading = false,
                    error = error.message ?: "Unable to load game history",
                    cards = emptyList()
                )
            }
        }
    }
}

// English is hard 
private fun Int.toOrdinalString(): String = when (this) {
    1 -> "1st place"
    2 -> "2nd place"
    3 -> "3rd place"
    else -> "${this}th place"
}