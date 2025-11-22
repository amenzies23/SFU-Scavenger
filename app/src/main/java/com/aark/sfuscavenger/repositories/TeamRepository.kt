package com.aark.sfuscavenger.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.aark.sfuscavenger.data.models.User
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.aark.sfuscavenger.data.model.TeamMember
import com.google.firebase.Timestamp

class TeamRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val TAG = "TeamRepository"
    }

    suspend fun getUserTeamId(gameId: String): String? {
        val uid = auth.currentUser?.uid ?: return null

        val teamsRef = db.collection("games")
            .document(gameId)
            .collection("teams")
            .whereEqualTo("active", true)

        val snapshot = teamsRef.get().await()

        // Search through all teams to find which one the user is in
        for (doc in snapshot.documents) {
            val membersRef = doc.reference.collection("members")
            val memberDoc = membersRef.document(uid).get().await()

            if (memberDoc.exists()) {
                return doc.id
            }
        }

        return null
    }

    /**
     * Returns a map of userId -> User for all members of a team.
     *
     * Team membership is read from:
     *   /games/{gameId}/teams/{teamId}/members/{userId}
     *
     * User data is read from:
     *   /users/{userId}
     */
    suspend fun getTeamMembersWithUserObject(
        gameId: String,
        teamId: String
    ): Map<String, User> {
        val membersRef = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("members")

        val membersSnapshot = membersRef.get().await()

        Log.d(TAG,
            "getTeamMembers: found ${membersSnapshot.size()} member docs for teamId=$teamId")


        if (membersSnapshot.isEmpty) return emptyMap()

        val usersCollection = db.collection("users")
        val result = mutableMapOf<String, User>()

        for (memberDoc in membersSnapshot.documents) {
            val userId = memberDoc.id
            val userSnap = usersCollection.document(userId).get().await()
            Log.d(TAG, "getTeamMembers: /users/$userId raw data = ${userSnap.data}")
            val user = userSnap.toObject(User::class.java)
            if (user != null) {
                result[userId] = user
            }
        }

        return result
    }

    /**
     * Update the current users lastReadChatAt for this team to "now"
     * Stored at:
     * /games/{gameId}/teams/{teamId}/members/{uid}.lastReadChatAt
     */
    suspend fun setMyLastReadChatNow(
        gameId: String,
        teamId: String
    ) {
        val uid = auth.currentUser?.uid ?: return

        val memberRef = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("members")
            .document(uid)

        memberRef.set(
            mapOf("lastReadChatAt" to Timestamp.now()),
            SetOptions.merge()
        ).await()
    }

    /**
     * Listen to the current user's lastReadChatAt field for this team.
     */
    fun listenToMyLastReadChatAt(
        gameId: String,
        teamId: String
    ): Flow<Timestamp?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val memberRef = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("members")
            .document(uid)

        val registration = memberRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "listenToMyLastReadChatAt: error", error)
                return@addSnapshotListener
            }

            val member = snapshot?.toObject(TeamMember::class.java)
            trySend(member?.lastReadChatAt)
        }

        awaitClose { registration.remove() }
    }
}
