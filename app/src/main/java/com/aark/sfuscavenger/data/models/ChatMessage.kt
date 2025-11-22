package com.aark.sfuscavenger.data.models

import com.google.firebase.Timestamp

/**
 * Single chat message in a team chat
 * For reference, this is the path to access in Firestore:
 * /games/{gameId}/teams/{teamId}/chat/{messageId}
 */
data class ChatMessage(
    val text: String = "",
    val senderId: String = "",
    val createdAt: Timestamp? = null
)