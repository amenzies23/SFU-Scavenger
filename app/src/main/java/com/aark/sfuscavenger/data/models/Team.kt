package com.aark.sfuscavenger.data.models

import com.google.firebase.Timestamp

/**
 * To store the team info for a specific game (we get the gameId through the path)
 * ex: /games/{game_id}/teams/{team_id}
 * */
data class Team(
    val id: String = "",
    val name: String = "",
    val score: Int = 0, // Might want some score tracker? Idk
    val memberCount: Int = 0,
    val latestSubmissionAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val active: Boolean = true
)