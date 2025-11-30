package com.aark.sfuscavenger.data.models

/**
 * Stored under /users/{uid}/memberships/{gameId}
 * Each document links the user to a specific game and team.
 */
data class GameMember(
    val gameId: String = "",
    val teamId: String? = null,
    val joinedAt: Long = 0L,
    val xpEarned: Int = 0 // XP earned in each specific game to count later on 
)
