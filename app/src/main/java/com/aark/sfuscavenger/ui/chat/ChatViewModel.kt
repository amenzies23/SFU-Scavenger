package com.aark.sfuscavenger.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.ChatMessage
import com.aark.sfuscavenger.data.models.User
import com.aark.sfuscavenger.repositories.ChatRepository
import com.aark.sfuscavenger.repositories.TeamRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class ChatMessageUi(
    val text: String,
    val senderId: String,
    val senderName: String,
    val senderLevel: Int,
    val isMine: Boolean,
    val time: String,
    val senderPhotoUrl: String? = null,
    val hasProfilePicture: Boolean,
)

data class ChatUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val messages: List<ChatMessageUi> = emptyList(),
    val inputText: String = "",
    val teamId: String? = null
)

class ChatViewModel(
    private val chatRepo: ChatRepository = ChatRepository(),
    private val teamRepo: TeamRepository = TeamRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var currentGameId: String? = null

    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun start(gameId: String) {
        if (listenJob != null) return

        currentGameId = gameId

        listenJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }

            val teamId = teamRepo.getUserTeamId(gameId)
            if (teamId == null) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = "You are not assigned to a team."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(teamId = teamId) }

            val membersMap: Map<String, User> = try {
                teamRepo.getTeamMembersWithUserObject(gameId, teamId)
            } catch (e: Exception) {
                emptyMap()
            }

            val currentUid = auth.currentUser?.uid

            // To handle receiving new messages through the chat repo, and formatting them for chat
            chatRepo.listenToTeamChat(gameId, teamId).collect { messages: List<ChatMessage> ->
                val uiMessages = messages.map { msg ->
                    val user = membersMap[msg.senderId]

                    val timeText = msg.createdAt
                        ?.toDate()
                        ?.let { date -> timeFormatter.format(date) }
                        ?: ""

                    ChatMessageUi(
                        text = msg.text,
                        senderId = msg.senderId,
                        senderName = user?.displayName ?: user?.email ?: "Unknown",
                        senderLevel = user?.level ?: 1,
                        isMine = (msg.senderId == currentUid),
                        time = timeText,
                        senderPhotoUrl = user?.photoUrl,
                        hasProfilePicture = user?.photoUrl?.isNotBlank() == true
                    )
                }

                _uiState.update {
                    it.copy(
                        messages = uiMessages, // UI messages is now a list of objects for the UI
                        loading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onInputChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendCurrentMessage() {
        val text = _uiState.value.inputText.trim()
        val gameId = currentGameId
        val teamId = _uiState.value.teamId

        if (text.isBlank() || gameId == null || teamId == null) return

        viewModelScope.launch {
            try {
                chatRepo.sendMessage(gameId, teamId, text)
                _uiState.update { it.copy(inputText = "") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to send message") }
            }
        }
    }

    fun markAllMessagesRead() {
        val gameId = currentGameId
        val teamId = _uiState.value.teamId
        if (gameId == null || teamId == null) return

        viewModelScope.launch {
            try {
                // To keep track of the last time the chat was opened
                teamRepo.setMyLastReadChatNow(gameId, teamId)
            } catch (_: Exception) {
            }
        }
    }
}
