package com.aark.sfuscavenger.data.models

import com.google.firebase.Timestamp

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromDisplayName: String = "",
    val fromUsername: String = "",
    val fromPhotoUrl: String? = null,
    val status: String = "pending",
    val createdAt: Timestamp? = null
)

