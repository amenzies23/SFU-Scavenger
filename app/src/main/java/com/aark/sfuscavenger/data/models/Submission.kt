package com.aark.sfuscavenger.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Data class for submission of a task. This will be a pretty big piece of our app, and will
 * be used in a lot of places
 * ex: /game/{game_id}/teams/{team_id}/submissions/{submission_id}
 * */
data class Submission(
    val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val type: String = "", // "photo" | "qr" | "text" | "geo"
    val status: String = "pending", // "pending" | "approved" | "rejected" | "auto_approved"
    // TODO: Figure out how were handling storing images. I (Alex) can look into this.
    // val mediaStoragePath: String? = null, // path in Cloud Storage
    // val thumbnailPath: String? = null,
    val text: String? = null, // For text-based tasks
    val geo: GeoPoint? = null, // Location tracking
    val scoreAwarded: Int = 0,
    val verifiedBy: String? = null, // uid
    val createdAt: Timestamp? = null,
    val verifiedAt: Timestamp? = null
)