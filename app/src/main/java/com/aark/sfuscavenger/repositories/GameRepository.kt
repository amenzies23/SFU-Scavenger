package com.aark.sfuscavenger.repositories

import androidx.compose.runtime.snapshotFlow
import com.aark.sfuscavenger.data.models.Game
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import kotlinx.coroutines.channels.awaitClose

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

    fun observeGames() = callbackFlow<List<Game>> {
        val listener = gamesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Game::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose {
            listener.remove()
        }
    }


    /**
     * Returns all games with status == "started" that the current user is a member of
     */
    suspend fun getLiveGamesForCurrentUser(): List<Game> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        val membershipsSnap = db.collection("users")
            .document(uid)
            .collection("memberships")
            .get()
            .await()

        if (membershipsSnap.isEmpty) return emptyList()

        val gameIds = membershipsSnap.documents.map { it.id }

        val snapshot = gamesCollection
            .whereIn(FieldPath.documentId(), gameIds)
            .whereEqualTo("status", "started")
            .get()
            .await()

        if (snapshot.isEmpty) return emptyList()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Game::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun deleteGame(gameId: String) {
        gamesCollection.document(gameId).delete().await()
    }

    suspend fun updateGame(game: Game) {
        val updates = hashMapOf<String, Any>(
            "name" to game.name,
            "description" to game.description,
            "joinMode" to game.joinMode,
            "updatedAt" to Timestamp.now()
        )

        gamesCollection.document(game.id).update(updates).await()
    }

    /**
     * Returns all games with status == "ended" and the user has the membership of the game as well
     */
    suspend fun getEndedGamesForCurrentUser(): List<Game> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        val membershipsSnap = db.collection("users")
            .document(uid)
            .collection("memberships")
            .get()
            .await()

        if (membershipsSnap.isEmpty) return emptyList()

        val gameIds = membershipsSnap.documents.map { it.id }

        // Get all games the user is a member of, then filter to only ended games
        val allGames = getGamesByIds(gameIds)
        return allGames.filter { it.status == "ended" }
    }

    /**
     * Returns all games from the provided list of game IDs.
     * Returns all games that exist, regardless of status.
     */
    suspend fun getGamesByIds(gameIds: List<String>): List<Game> {
        if (gameIds.isEmpty()) {
            return emptyList()
        }

        val snapshot = gamesCollection
            .whereIn(FieldPath.documentId(), gameIds)
            .get()
            .await()

        if (snapshot.isEmpty) {
            return emptyList()
        }

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Game::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun publishGame(gameId: String, status: String) {
        gamesCollection.document(gameId)
            .update(
                mapOf(
                    "status" to status,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
    }

}