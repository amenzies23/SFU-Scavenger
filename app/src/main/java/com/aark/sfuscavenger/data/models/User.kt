package com.aark.sfuscavenger.data.models
import com.google.firebase.Timestamp

/**
 * User information, not the same as the auth table. This is a top-level collection
 * ex: /users/{user_id}
 * */
data class User(
    val email: String = "",
    val displayName: String? = null,
    val username: String? = null,
    val level: Int = 1,
    val xp: Int = 0,
    val createdAt: Timestamp? = null,
    val photoUrl: String? = null
)