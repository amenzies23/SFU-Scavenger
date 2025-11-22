package com.aark.sfuscavenger.repositories

import com.aark.sfuscavenger.data.models.Game
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Sample GameRepository to get an idea of how to interact with Firebase Firestore
 * This isn't set in stone, we likely want to change a lot of things, its just to give an idea of
 * how to interact with Firebase / Firestore
 * */
class GameRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val gamesCollection = db.collection("games")

    // This can be changed later. Just a simple function to generate a random 6 char string
    // Theres probably a library to do this?
    private fun generateJoinCode(length: Int = 6): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    // Creates a new game document, returns the document id on success
    suspend fun createGame(
        name: String,
        lat: Double,
        lng: Double,
        joinMode: String = "code",
        joinCode: String? = null,
        description: String = ""
    ): String {
        val ownerId = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be logged in to create a game")
        val geo = GeoPoint(lat, lng)
        val geohash = "" // TODO

        // Let Firestore generate the document id
        val docRef = gamesCollection.document()

        val finalJoinCode = if (joinCode.isNullOrBlank()) {
            generateJoinCode()
        } else {
            joinCode.trim().uppercase()
        }
        val game = Game(
            id = docRef.id,
            name = name,
            ownerId = ownerId,
            status = "draft",
            joinMode = joinMode,
            joinCode = finalJoinCode,
            description = description,
            createdAt = Timestamp.now(),
            location = geo,
            geohash = geohash
        )

        // Write to Firestore
        docRef.set(game).await()

        return docRef.id
    }

    // Get a single game by its document id.
    suspend fun getGame(gameId: String): Game? {
        val snapshot = gamesCollection.document(gameId).get().await()
        if (!snapshot.exists()) return null

        val game = snapshot.toObject(Game::class.java)

        // Ensure id matches the document id
        return game?.copy(id = snapshot.id)
    }

    // Get all games
    suspend fun getAllGames(): List<Game> {
        val snapshot = gamesCollection.get().await()
        if (snapshot.isEmpty) return emptyList()

        val games = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Game::class.java)?.copy(id = doc.id)
        }
        return games
    }
    
    suspend fun getGameName(gameId: String): String? {
        val snap = db.collection("games").document(gameId).get().await()
        return snap.getString("name")
    }
}