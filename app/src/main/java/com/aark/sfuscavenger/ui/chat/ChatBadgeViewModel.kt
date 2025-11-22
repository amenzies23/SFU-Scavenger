package com.aark.sfuscavenger.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.ChatMessage
import com.aark.sfuscavenger.repositories.ChatRepository
import com.aark.sfuscavenger.repositories.TeamRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Handles the logic of determining if we need to display the "badge" on the chat icon
 * If the users lastChatReadAt is older than the latest chat message, then we display this icon
 * */
class ChatBadgeViewModel(
    private val teamRepo: TeamRepository = TeamRepository(),
    private val chatRepo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _hasUnreadChat = MutableStateFlow(false)
    val hasUnreadChat: StateFlow<Boolean> = _hasUnreadChat.asStateFlow()

    private var job: Job? = null

    fun start(gameId: String) {
        if (job != null) return

        job = viewModelScope.launch {
            val teamId = teamRepo.getUserTeamId(gameId) ?: return@launch

            // Latest message createdAt
            val latestMessageFlow = chatRepo.listenToTeamChat(gameId, teamId)
                .map { messages: List<ChatMessage> ->
                    messages.maxByOrNull { it.createdAt?.toDate()?.time ?: Long.MIN_VALUE }
                        ?.createdAt
                }

            // This users lastReadChatAt
            val lastReadFlow = teamRepo.listenToMyLastReadChatAt(gameId, teamId)

            combine(latestMessageFlow, lastReadFlow) { latest: Timestamp?, lastRead: Timestamp? ->
                latest != null && (lastRead == null || latest > lastRead)
            }.collect { hasUnread ->
                _hasUnreadChat.value = hasUnread
            }
        }
    }
}
