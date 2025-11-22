package com.aark.sfuscavenger.data.models

data class Friend(
    val id: String,
    val displayName: String,
    val username: String? = null,
    val photoUrl: String? = null,
    val level: Int = 1,
    val xp: Int = 0
)

