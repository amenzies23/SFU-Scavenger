package com.aark.sfuscavenger.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Submission
import com.aark.sfuscavenger.repositories.SubmissionRepository
import com.aark.sfuscavenger.repositories.TeamRepository
import com.aark.sfuscavenger.repositories.GameRepository
import com.aark.sfuscavenger.repositories.TaskRepository
import com.aark.sfuscavenger.data.models.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class MapUiState(
    val submissions: List<Submission> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val selectedSubmission: Submission? = null,
    val selectedTask: Task? = null,
    val dialogLoading: Boolean = false,
    val gameName: String = ""
)

class MapViewModel(
    private val submissionRepo: SubmissionRepository = SubmissionRepository(),
    private val teamRepo: TeamRepository = TeamRepository(),
    private val gameRepo: GameRepository = GameRepository(),
    private val taskRepo: TaskRepository = TaskRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var currentGameId: String? = null

    /**
     * To be called from the MapScreen with the gameId passed in
     */
    fun start(gameId: String) {
        if (listenJob != null) return

        listenJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }

            val name = withContext(Dispatchers.IO) {
                gameRepo.getGameName(gameId) ?: "Unknown game"
            }
            _uiState.update { it.copy(gameName = name) }

            currentGameId = gameId

            val teamId = withContext(Dispatchers.IO) {
                teamRepo.getUserTeamId(gameId)
            }
            if (teamId == null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = "You are not assigned to a team."
                    )
                }
                return@launch
            }

            // Collect the Firestore flow on IO too
            withContext(Dispatchers.IO) {
                submissionRepo.listenToTeamSubmissions(gameId, teamId)
                    .collect { submissions ->
                        // Switch back to main only for state update
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    submissions = submissions,
                                    loading = false,
                                    error = null
                                )
                            }
                        }
                    }
            }
        }
    }

    fun onMarkerSelected(submission: Submission) {
        _uiState.update {
            it.copy(
                selectedSubmission = submission,
                selectedTask = null,
                dialogLoading = true
            )
        }

        val gameId = currentGameId ?: return

        viewModelScope.launch {
            val task = try {
                withContext(Dispatchers.IO) {
                    taskRepo.getTask(gameId, submission.taskId)
                }
            } catch (e: Exception) {
                null
            }

            _uiState.update {
                it.copy(
                    selectedTask = task,
                    dialogLoading = false
                )
            }
        }
    }

    fun onDialogDismiss() {
        _uiState.update {
            it.copy(
                selectedSubmission = null,
                selectedTask = null,
                dialogLoading = false
            )
        }
    }
}
