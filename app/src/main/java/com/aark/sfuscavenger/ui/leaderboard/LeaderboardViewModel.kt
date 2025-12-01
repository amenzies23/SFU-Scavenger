package com.aark.sfuscavenger.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Team
import com.aark.sfuscavenger.repositories.TeamRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardRowUi(
    val rank: Int,
    val teamName: String,
    val score: Int,
    val members: List<String> = emptyList(),
    val isMyTeam: Boolean
)

data class LeaderboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<LeaderboardRowUi> = emptyList()
)

class LeaderboardViewModel(
    private val teamRepo: TeamRepository = TeamRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var currentGameId: String? = null
    private val teamMembersCache = mutableMapOf<String, CachedMembers>()

    private data class CachedMembers(
        val memberCount: Int,
        val names: List<String>
    )

    /**
     * Start listening for leaderboard updates for this game.
     */
    fun start(gameId: String) {
        if (listenJob != null && currentGameId == gameId) return

        currentGameId = gameId
        listenJob?.cancel()

        listenJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }

            // Find which team this user belongs to
            val myTeamId = try {
                teamRepo.getUserTeamId(gameId)
            } catch (e: Exception) {
                null
            }

            teamRepo.listenToTeamsFlow(gameId).collect { teams ->
                val sorted = sortTeamsForLeaderboard(teams)
                teamMembersCache.keys.retainAll(sorted.map { it.id }.toSet())
                val rows = buildLeaderboardRows(gameId, sorted, myTeamId)

                _uiState.update {
                    it.copy(
                        loading = false,
                        error = null,
                        rows = rows
                    )
                }
            }
        }
    }

    /**
     * Sort teams by descending score. Tiebreaker goes to the latest submitted task
     */
    private fun sortTeamsForLeaderboard(teams: List<Team>): List<Team> {
        return teams.sortedWith(
            compareByDescending<Team> { it.score }
                .thenBy { safeTimestampForTieBreak(it.latestSubmissionAt) }
        )
    }

    private fun safeTimestampForTieBreak(ts: Timestamp?): Long {
        return ts?.toDate()?.time ?: Long.MAX_VALUE
    }

    private suspend fun buildLeaderboardRows(
        gameId: String,
        sortedTeams: List<Team>,
        myTeamId: String?
    ): List<LeaderboardRowUi> {
        if (sortedTeams.isEmpty()) return emptyList()

        val memberNames = coroutineScope {
            sortedTeams.map { team ->
                async { getMemberNamesForTeam(gameId, team) }
            }.awaitAll()
        }

        return sortedTeams.mapIndexed { index, team ->
            LeaderboardRowUi(
                rank = index + 1,
                teamName = team.name.ifBlank { "Unnamed team" },
                score = team.score,
                members = memberNames[index],
                isMyTeam = (team.id == myTeamId)
            )
        }
    }

    private suspend fun getMemberNamesForTeam(
        gameId: String,
        team: Team
    ): List<String> {
        if (team.id.isBlank()) return emptyList()

        val cached = teamMembersCache[team.id]
        if (cached != null && cached.memberCount == team.memberCount) {
            return cached.names
        }

        val names = runCatching {
            teamRepo.getTeamMembersWithUserObject(gameId, team.id).values.map { user ->
                user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.username?.takeIf { it.isNotBlank() }
                    ?: "Player"
            }.sorted()
        }.getOrElse { emptyList() }

        teamMembersCache[team.id] = CachedMembers(team.memberCount, names)
        return names
    }
}
