package com.aark.sfuscavenger.data.models
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * To store all the information about a game
 * This will be top-level collection in the database
 * ex: /game/{game_id}
 * */
data class Game(
    val id: String = "", // From doc.id
    val name: String = "",
    val ownerId: String = "",
    val description: String ="",
    val status: String = "draft", // I'm thinking "draft" | "live" | "started" | "ended"
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val joinMode: String = "code", // "code" | "open"
    val joinCode: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val location: GeoPoint? = null, // location for games, helpful for markers
    val geohash: String? = null // geohash, apparently helpful for radius based queries
)