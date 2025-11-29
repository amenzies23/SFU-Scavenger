package com.aark.sfuscavenger.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.data.models.Submission
import com.aark.sfuscavenger.data.models.Task
import com.aark.sfuscavenger.data.models.Team
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// UI models used by the Task screen
data class TaskUi(
    val id: String,
    val name: String,
    val description: String,
    val points: Int,
    val type: String,
    val isCompleted: Boolean = false,
    val isPending: Boolean = false,
    val isRejected: Boolean = false
)

data class SubmissionUi(
    val id: String,
    val taskId: String,
    val taskName: String,
    val teamId: String,
    val teamName: String,
    val submitterId: String,
    val submitterName: String,
    val type: String,
    val textAnswer: String? = null,
    val photoUrl: String? = null,
    val status: String
)

data class TaskUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val isHost: Boolean = false,
    val gameId: String? = null,
    val teamId: String? = null,

    // Player view
    val tasks: List<TaskUi> = emptyList(),
    val completedCount: Int = 0,
    val teamScore: Int = 0,

    // Host view
    val pendingSubmissions: List<SubmissionUi> = emptyList()
)

class TaskViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(TaskUiState())
    val state: StateFlow<TaskUiState> = _state.asStateFlow()

    private var gameListener: ListenerRegistration? = null
    private var tasksListener: ListenerRegistration? = null
    private var submissionsListener: ListenerRegistration? = null
    private var hostSubmissionsListeners: MutableList<ListenerRegistration> = mutableListOf()

    fun start(gameId: String) {
        if (_state.value.gameId == gameId) return

        _state.update { TaskUiState(loading = true, gameId = gameId) }

        viewModelScope.launch {
            try {
                // Load the game document to check if the user is the host
                val gameDoc = db.collection("games").document(gameId).get().await()
                val game = gameDoc.toObject(Game::class.java)
                val uid = auth.currentUser?.uid
                val isHost = uid != null && uid == game?.ownerId

                _state.update { it.copy(isHost = isHost) }

                if (isHost) {
                    // Hosts watch all submissions from all teams
                    observeAllSubmissions(gameId)
                } else {
                    // Players only watch their own team's tasks + submissions
                    val teamId = findUserTeam(gameId, uid)
                    _state.update { it.copy(teamId = teamId) }
                    if (teamId != null) {

                        if (!isHost && teamId != null) {
                            // Get current team score
                            val teamSnap = db.collection("games")
                                .document(gameId)
                                .collection("teams")
                                .document(teamId)
                                .get()
                                .await()

                            val score = teamSnap.getLong("score")?.toInt() ?: 0
                            _state.update { it.copy(teamScore = score) }

                            // Start watching the team doc for live score updates
                            observeTeamScore(gameId, teamId)
                        }

                        observeTasksAndSubmissions(gameId, teamId)

                    } else {
                        _state.update {
                            // If not on a team, player can't play the game
                            it.copy(loading = false, error = "You are not in a team")
                        }
                    }
                }

            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    /**
     * Finds which team the user belongs to for this game
     * We check every team and see if the user ID is in the team's members subcollection
     */
    private suspend fun findUserTeam(gameId: String, uid: String?): String? {
        if (uid == null) return null

        val teamsSnap = db.collection("games")
            .document(gameId)
            .collection("teams")
            .get()
            .await()
        // Loop through all teams and look for a matching member document
        for (teamDoc in teamsSnap.documents) {
            val memberSnap = teamDoc.reference
                .collection("members")
                .document(uid)
                .get()
                .await()
            if (memberSnap.exists()) {
                return teamDoc.id
            }
        }
        return null
    }

    /**
     * Player Observe tasks and own submissions
     */
    private fun observeTasksAndSubmissions(gameId: String, teamId: String) {
        tasksListener?.remove()
        submissionsListener?.remove()

        // Listen to tasks
        tasksListener = db.collection("games")
            .document(gameId)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.update { it.copy(loading = false, error = error.message) }
                    return@addSnapshotListener
                }

                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                updateTasksWithStatus(gameId, teamId, tasks)
            }

        submissionsListener = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("submissions")
            .addSnapshotListener { _, _ ->
                // Refetch task statuses when submissions change
                viewModelScope.launch {
                    val tasksSnap = db.collection("games")
                        .document(gameId)
                        .collection("tasks")
                        .get()
                        .await()

                    val tasks = tasksSnap.documents.mapNotNull { doc ->
                        doc.toObject(Task::class.java)?.copy(id = doc.id)
                    }

                    updateTasksWithStatus(gameId, teamId, tasks)
                }
            }
    }

    private fun updateTasksWithStatus(gameId: String, teamId: String, tasks: List<Task>) {
        viewModelScope.launch {
            try {
                val submissionsSnap = db.collection("games")
                    .document(gameId)
                    .collection("teams")
                    .document(teamId)
                    .collection("submissions")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val submissions = submissionsSnap.documents.mapNotNull { doc ->
                    doc.toObject(Submission::class.java)
                }

                // Map taskId to submission status
                // get the most recent submission per taskId
                val taskStatusMap = submissions
                    .groupBy { it.taskId }
                    .mapValues { (_, list) ->
                        list.maxByOrNull { it.createdAt?.seconds ?: 0 }!!.status
                    }

                val taskUiList = tasks.map { task ->
                    val status = taskStatusMap[task.id]
                    TaskUi(
                        id = task.id,
                        name = task.name,
                        description = task.description,
                        points = task.points,
                        type = task.type,
                        isCompleted = status == "approved",
                        isPending = status == "pending",
                        isRejected = status == "rejected"
                    )
                }

                _state.update {
                    it.copy(
                        loading = false,
                        tasks = taskUiList,
                        completedCount = taskUiList.count { t -> t.isCompleted }
                    )
                }

            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    /**
     * Player
     * Submits text answer
     */
    fun submitTextAnswer(taskId: String, answer: String) {
        val gameId = _state.value.gameId ?: return
        val teamId = _state.value.teamId ?: return
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val submission = hashMapOf(
                    "taskId" to taskId,
                    "userId" to uid,
                    "type" to "text",
                    "status" to "pending",
                    "text" to answer.trim(),
                    "createdAt" to Timestamp.now(),
                    "scoreAwarded" to 0
                )

                db.collection("games")
                    .document(gameId)
                    .collection("teams")
                    .document(teamId)
                    .collection("submissions")
                    .add(submission)
                    .await()

            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to submit: ${e.message}") }
            }
        }
    }

    /**
     * Player
     */
    fun submitPhotoAnswer(taskId: String, imageData: ByteArray) {
        val gameId = _state.value.gameId ?: return
        val teamId = _state.value.teamId ?: return
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val teamSubmissionsRef = db.collection("games")
                    .document(gameId)
                    .collection("teams")
                    .document(teamId)
                    .collection("submissions")

                val submissionDocRef = teamSubmissionsRef.document()
                val submissionId = submissionDocRef.id

                val storagePath = "submissions/$submissionId/image.jpg"
                val storageRef = storage.reference.child(storagePath)

                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()
                storageRef.putBytes(imageData, metadata).await()

                val submission = hashMapOf(
                    "taskId" to taskId,
                    "userId" to uid,
                    "type" to "photo",
                    "status" to "pending",
                    "mediaStoragePath" to storagePath,
                    "createdAt" to Timestamp.now(),
                    "scoreAwarded" to 0
                )

                submissionDocRef.set(submission).await()

            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to submit photo: ${e.message}") }
            }
        }
    }


    /**
     * Host; Observe all submissions
     */
    private fun observeAllSubmissions(gameId: String) {

        viewModelScope.launch {
            try {

                val teamsSnap = db.collection("games")
                    .document(gameId)
                    .collection("teams")
                    .get()
                    .await()

                val teams = teamsSnap.documents.mapNotNull { doc ->
                    doc.toObject(Team::class.java)?.copy(id = doc.id)
                }

                val tasksSnap = db.collection("games")
                    .document(gameId)
                    .collection("tasks")
                    .get()
                    .await()

                val taskMap = tasksSnap.documents.associate { doc ->
                    doc.id to (doc.toObject(Task::class.java)?.name ?: "Unknown Task")
                }

                // Listen to each teams submissions
                for (team in teams) {
                    val listener = db.collection("games")
                        .document(gameId)
                        .collection("teams")
                        .document(team.id)
                        .collection("submissions")
                        .whereEqualTo("status", "pending")
                        .addSnapshotListener { _, error ->
                            if (error != null) return@addSnapshotListener

                            viewModelScope.launch {
                                refreshAllPendingSubmissions(gameId, teams, taskMap)
                            }
                        }

                    hostSubmissionsListeners.add(listener)
                }

                refreshAllPendingSubmissions(gameId, teams, taskMap)

            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    /**
     * Loads ALL pending submissions across ALL teams
     * Builds the UI list the host sees in the review screen
     */
    private suspend fun refreshAllPendingSubmissions(
        gameId: String,
        teams: List<Team>,
        taskMap: Map<String, String>
    ) {
        val allSubmissions = mutableListOf<SubmissionUi>()

        for (team in teams) {
            val subsSnap = db.collection("games")
                .document(gameId)
                .collection("teams")
                .document(team.id)
                .collection("submissions")
                .whereEqualTo("status", "pending")
                .get()
                .await()

            for (doc in subsSnap.documents) {
                val sub = doc.toObject(Submission::class.java)?.copy(id = doc.id) ?: continue

                val userDoc = db.collection("users").document(sub.userId).get().await()
                val submitterName = userDoc.getString("displayName")
                    ?: userDoc.getString("email")
                    ?: "Unknown"

                allSubmissions.add(
                    SubmissionUi(
                        id = sub.id,
                        taskId = sub.taskId,
                        taskName = taskMap[sub.taskId] ?: "Unknown Task",
                        teamId = team.id,
                        teamName = team.name,
                        submitterId = sub.userId,
                        submitterName = submitterName,
                        type = sub.type,
                        textAnswer = sub.text,
                        photoUrl = sub.mediaStoragePath,
                        status = sub.status
                    )
                )
            }
        }

        _state.update {
            it.copy(
                loading = false,
                pendingSubmissions = allSubmissions
            )
        }
    }

    /**
     * Host: Approve/Reject
     */
    fun approveSubmission(submission: SubmissionUi) {
        val gameId = _state.value.gameId ?: return
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val gameRef = db.collection("games").document(gameId)
                val taskRef = gameRef
                    .collection("tasks")
                    .document(submission.taskId)
                val teamRef = gameRef
                    .collection("teams")
                    .document(submission.teamId)
                val submissionRef = teamRef
                    .collection("submissions")
                    .document(submission.id)

                db.runTransaction { tx ->
                    //  Read the submission
                    val subSnap = tx.get(submissionRef)
                    if (!subSnap.exists()) {
                        throw IllegalStateException("Submission does not exist")
                    }

                    val currentStatus = subSnap.getString("status") ?: "pending"
                    val alreadyAwarded = subSnap.getLong("scoreAwarded") ?: 0L

                    // If its already approved or has points, dont award again
                    if (currentStatus == "approved" || alreadyAwarded > 0L) {
                        return@runTransaction null
                    }

                    // Read the task to get the point value
                    val taskSnap = tx.get(taskRef)
                    val points = (taskSnap.getLong("points") ?: 0L).toInt()

                    // update the submission: mark approved + record score
                    tx.update(
                        submissionRef,
                        mapOf(
                            "status" to "approved",
                            "verifiedBy" to uid,
                            "verifiedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "scoreAwarded" to points
                        )
                    )

                    // Update the team: increment score + latestSubmissionAt
                    tx.update(
                        teamRef,
                        mapOf(
                            "score" to com.google.firebase.firestore.FieldValue.increment(points.toLong()),
                            "latestSubmissionAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    )

                    null
                }.await()

            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun rejectSubmission(submission: SubmissionUi) {
        val gameId = _state.value.gameId ?: return
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                db.collection("games")
                    .document(gameId)
                    .collection("teams")
                    .document(submission.teamId)
                    .collection("submissions")
                    .document(submission.id)
                    .update(
                        mapOf(
                            "status" to "rejected",
                            "verifiedBy" to uid,
                            "verifiedAt" to Timestamp.now()
                        )
                    )
                    .await()

            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Host: End the game
     * Updates game status to "ended" and calculates team placements
     */
    fun endGame() {
        val gameId = _state.value.gameId ?: return

        viewModelScope.launch {
            try {
                // Get all teams and sort by score
                val teamsSnap = db.collection("games")
                    .document(gameId)
                    .collection("teams")
                    .get()
                    .await()

                val teams = teamsSnap.documents.mapNotNull { doc ->
                    doc.toObject(Team::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.score }

                // Update each team with their placement
                teams.forEachIndexed { index, team ->
                    db.collection("games")
                        .document(gameId)
                        .collection("teams")
                        .document(team.id)
                        .update("placement", index + 1)
                        .await()
                }

                // Update game status to "ended"
                db.collection("games")
                    .document(gameId)
                    .update(
                        mapOf(
                            "status" to "ended",
                            "endedAt" to Timestamp.now()
                        )
                    )
                    .await()

            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to end game: ${e.message}") }
            }
        }
    }

    private fun observeTeamScore(gameId: String, teamId: String) {
        db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    val score = snap.getLong("score")?.toInt() ?: 0
                    _state.update { it.copy(teamScore = score) }
                }
            }
    }

    // Cleanup
    override fun onCleared() {
        super.onCleared()
        gameListener?.remove()
        tasksListener?.remove()
        submissionsListener?.remove()
        hostSubmissionsListeners.forEach { it.remove() }
    }
}