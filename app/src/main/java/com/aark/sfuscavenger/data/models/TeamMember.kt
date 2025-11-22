package com.aark.sfuscavenger.data.model

import com.google.firebase.Timestamp

/**
 * To store the members of a team. This is just the object to define the fields, but it will
 * be used in the members sub-collection
 * ex: /games/{game_id}/teams/{team_id}/members/{member_id}
 * */
data class TeamMember(
    val userId: String = "",
    val role: String = "player", // Maybe we want roles, not sure yet
    val joinedAt: Timestamp? = null,
    val lastReadChatAt: Timestamp? = null
)