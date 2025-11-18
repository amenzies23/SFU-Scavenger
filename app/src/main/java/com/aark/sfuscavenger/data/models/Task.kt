package com.aark.sfuscavenger.data.models

import com.google.firebase.Timestamp

/**
 * Starter data class for a Task. We will probably want to change things / add more to it
 * This is meant to be the task that the admin creates, this does not handle any submission
 * ex: /games/{game_id}/tasks/{task_id}
 * */
data class Task(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val points: Int = 0,
    val type: String = "photo", // "photo" | "qr" | "text" | "geo"
    val validationMode: String = "manual",// "manual" | "auto" | "hybrid"
    val qrValue: String? = null,// for QR tasks
    val dependsOnTaskIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)