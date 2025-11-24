package com.aark.sfuscavenger.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.data.models.GameMember
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
    val cards: List<HistoryCard> = emptyList(),
    val searchQuery: String = ""
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
    
    /**
     * get the filtered name of the game when searched
     */
    val filteredCards: List<HistoryCard>
        get() {
            val query = _uiState.value.searchQuery.trim()
            if (query.isEmpty()) {
                return _uiState.value.cards
            }
            return _uiState.value.cards.filter { card ->
                card.title.contains(query, ignoreCase = true)
            }
        }
    
    /**
     * Updates the search query to filter history cards.
     */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

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
                // STEP 1: Get all memberships - ensures user is a member
                val allMemberships = userRepository.getMembershipsForUser()
                
                if (allMemberships.isEmpty()) {
                    _uiState.value = HistoryUiState(
                        loading = false,
                        error = null,
                        cards = emptyList()
                    )
                    return@launch
                }
                
                // STEP 2: Get game IDs from memberships (only games user is a member of)
                val gameIds = allMemberships.map { it.gameId }
                
                // STEP 3: Get all games that the user is a member of
                val allGames = gameRepository.getGamesByIds(gameIds)
                
                // STEP 4: Filter to ONLY ended games AND verify membership exists
                val membershipByGameId = allMemberships.associateBy { it.gameId }
                
                // Create history cards only for games that meet BOTH conditions:
                // 1. User is a member (has membership document)
                // 2. Game status is "ended"
                val historyCards = allGames.mapNotNull { game ->
                    // Check condition 1: User must be a member
                    val membership = membershipByGameId[game.id]
                    if (membership == null) {
                        return@mapNotNull null // Skip games without membership
                    }
                    
                    // Check condition 2: Game must be ended
                    if (game.status != "ended") {
                        return@mapNotNull null // Skip games that are not ended
                    }
                    
                    // Both conditions met - create history card
                    createHistoryCard(game, membership)
                }
                
                historyCards.sortedByDescending { it.joinedAt }
            }.onSuccess { sortedCards ->
                _uiState.value = HistoryUiState(
                    loading = false,
                    error = null,
                    cards = sortedCards
                )
            }.onFailure { exception ->
                _uiState.value = HistoryUiState(
                    loading = false,
                    error = exception.message ?: "Unable to load game history",
                    cards = emptyList()
                )
            }
        }
    }
    
    /**
     * Creates a HistoryCard from a game and its membership info.
     * Returns null if membership is missing.
     * If teamId is not in membership, looks it up from the game's teams.
     */
    private suspend fun createHistoryCard(
        game: Game,
        membership: GameMember?
    ): HistoryCard? {
        if (membership == null) return null
        
        val teamId = membership.teamId ?: teamRepository.getUserTeamId(game.id)
        val placementLabel = getPlacement(game.id, teamId)
        
        return HistoryCard(
            gameId = game.id,
            teamId = teamId,
            title = game.name.ifBlank { "Untitled Game" },
            placement = placementLabel,
            joinedAt = membership.joinedAt
        )
    }
    
    /**
     * Gets the placement string for a team in a game.
     * Returns "N/A" if placement data is not available.
     */
    private suspend fun getPlacement(gameId: String, teamId: String?): String {
        if (teamId == null) return "N/A"
        
        val teamSummary = teamRepository.getTeamSummary(gameId, teamId)
        val placement = teamSummary?.placement ?: 0
        
        return if (placement > 0) {
            placement.toOrdinalString()
        } else {
            "N/A"
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