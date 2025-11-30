package com.aark.sfuscavenger.data.models

/**
 * Represents a team's placement in a game
 * Stored at /games/{gameId}/teamPlacements/{teamId}
 */
data class TeamPlacement(
    val teamId: String = "",
    val placement: Int = 0,
    val score: Int = 0,
    val teamName: String = "",
    val teamPhotoUrl: String? = null,
    val isTeam: Boolean = true
)
