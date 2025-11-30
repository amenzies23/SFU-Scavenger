package com.aark.sfuscavenger.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.model.TeamMember
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.data.models.Team
import com.aark.sfuscavenger.repositories.TeamRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LobbyTeamUi(
    val id: String,
    val name: String,
    val members: List<String> = emptyList(),
    val memberPhotos: List<String?> = emptyList()
)

data class LobbyUiState(
    val loading: Boolean = true,
    val error: String? = null,

    val gameId: String? = null,
    val gameName: String = "",
    val gameStatus: String = "draft",

    val isHost: Boolean = false,
    val currentUserTeamId: String? = null,

    val joinCode: String = "",
    val teams: List<LobbyTeamUi> = emptyList()
)


class LobbyViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val teamRepo: TeamRepository = TeamRepository()
) : ViewModel() {

    private val gamesCollection = db.collection("games")
    private val usersCollection = db.collection("users")

    private val _state = MutableStateFlow(LobbyUiState())
    val state: StateFlow<LobbyUiState> = _state.asStateFlow()

    private var gameListener: ListenerRegistration? = null
    private var teamsListener: ListenerRegistration? = null

    private var memberListeners = mutableMapOf<String, ListenerRegistration>()

    /**
     * Call once from LobbyScreen for a given gameId
     */
    fun startObserving(gameId: String) {
        if (_state.value.gameId == gameId) return

        _state.update { LobbyUiState(loading = true, gameId = gameId) }

        observeGame(gameId)
        observeTeamsAndMembers(gameId)
    }

    /**
     * Observes the game document to detect name, status, host, etc
     */
    private fun observeGame(gameId: String) {
        gameListener?.remove()
        gameListener = gamesCollection.document(gameId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _state.update { it.copy(loading = false, error = e.message) }
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    _state.update { it.copy(loading = false, error = "Game not found") }
                    return@addSnapshotListener
                }

                val game = snapshot.toObject(Game::class.java)?.copy(id = snapshot.id)
                val uid = auth.currentUser?.uid

                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        gameName = game?.name.orEmpty(),
                        gameStatus = game?.status ?: "draft",
                        isHost = uid != null && uid == game?.ownerId,
                        joinCode = game?.joinCode ?: ""

                    )
                }
            }
    }

    /**
     * Observes teams and loads member names for each team.
     */
    private fun observeTeamsAndMembers(gameId: String) {
        teamsListener?.remove()
        memberListeners.values.forEach { it.remove() }
        memberListeners.clear()

        teamsListener = teamRepo.listenToTeams(gameId) { teamsList ->

            val initialTeams = teamsList.map { team ->
                LobbyTeamUi(
                    id = team.id,
                    name = team.name,
                    members = emptyList()
                )
            }

            _state.update {
                it.copy(
                    teams = initialTeams,
                    loading = false,
                    error = null
                )
            }

            teamsList.forEach { team ->

                memberListeners[team.id]?.remove()

                val reg = teamRepo.listenToTeamMembers(gameId, team.id) { members ->

                    viewModelScope.launch {
                        val memberNames = mutableListOf<String>()
                        val memberPhotos = mutableListOf<String?>()

                        for (member in members) {
                            val userDoc = usersCollection.document(member.userId).get().await()
                            val name = userDoc.getString("displayName")
                                ?: userDoc.getString("email")
                                ?: "Player"

                            val photo = userDoc.getString("photoUrl")

                            memberNames.add(name)
                            memberPhotos.add(photo)
                        }


                        _state.update { old ->
                            val updatedTeams = old.teams.map {
                                if (it.id == team.id)
                                    it.copy(
                                        members = memberNames,
                                        memberPhotos = memberPhotos
                                    )
                                else it
                            }
                            old.copy(teams = updatedTeams)
                        }
                    }
                }

                memberListeners[team.id] = reg
            }

            viewModelScope.launch {
                val uidTeam = auth.currentUser?.uid?.let {
                    teamRepo.getUserTeamId(gameId)
                }
                _state.update { it.copy(currentUserTeamId = uidTeam) }
            }
        }
    }

    /**
     * User joins an existing team.
     */
    fun joinTeam(teamId: String) {
        val gameId = _state.value.gameId ?: return

        viewModelScope.launch {
            try {
                teamRepo.joinTeam(gameId, teamId)
                _state.update { it.copy(currentUserTeamId = teamId, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun leaveTeam() {
        val gameId = _state.value.gameId ?: return

        viewModelScope.launch {
            try {
                teamRepo.leaveTeam(gameId)
                _state.update { it.copy(currentUserTeamId = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTeam() {
        val gameId = _state.value.gameId ?: return
        val teamId = _state.value.currentUserTeamId ?: return

        viewModelScope.launch {
            try {
                teamRepo.deleteTeam(gameId, teamId)
                _state.update { it.copy(currentUserTeamId = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }



    /**
     * User creates a new team and joins it.
     */
    fun createTeam(teamName: String) {
        val gameId = _state.value.gameId ?: return
        if (teamName.isBlank()) return

        viewModelScope.launch {
            try {
                val newTeamId = teamRepo.createTeam(gameId, teamName)
                _state.update { it.copy(currentUserTeamId = newTeamId, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Host starts the game
     */
    fun startGame() {
        val gameId = _state.value.gameId ?: return
        if (!_state.value.isHost) return

        viewModelScope.launch {
            try {
                gamesCollection.document(gameId).update(
                    mapOf(
                        "status" to "started",
                        "startTime" to Timestamp.now()
                    )
                ).await()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameListener?.remove()
        teamsListener?.remove()
    }
}
