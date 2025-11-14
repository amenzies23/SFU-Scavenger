package com.aark.sfuscavenger.data.models

import com.google.firebase.Timestamp

/**
 * To store the status of a task for this team
 *  /games/{game_id}/teams/{team_id}/taskStatus
 * */
data class TaskStatus(
    val taskId: String = "",
    val completed: Boolean = false,
    val bestSubmissionId: String? = null,
    val completedAt: Timestamp? = null,
    val points: Int = 0
)
