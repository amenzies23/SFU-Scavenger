package com.aark.sfuscavenger.data.models

/**
 * Minimal info about a team within a game, stored at /games/{gameId}/teams/{teamId}.
 */
data class TeamSummary(
    val id: String = "",
    val name: String = "",
    val placement: Int = 0,
    val score: Int = 0
)

