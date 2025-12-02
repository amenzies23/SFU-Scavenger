package com.aark.sfuscavenger.repositories

import android.util.Log
import com.aark.sfuscavenger.data.model.TeamMember
import com.aark.sfuscavenger.data.models.Team
import com.aark.sfuscavenger.data.models.TeamSummary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import com.aark.sfuscavenger.data.models.User
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.Source
import com.aark.sfuscavenger.data.models.TeamPlacement

class TeamRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "TeamRepository"
    }

    /**
     * Fetch all teams for a game directly from /games/{gameId}/teams
     */
    suspend fun getTeams(gameId: String): List<Team> {
        Log.d(TAG, "getTeams: Attempting to fetch from SERVER")
        
        val snapshot = db.collection("games")
            .document(gameId)
            .collection("teams")
            .get(Source.SERVER)
            .await()
            
        Log.d(TAG, "getTeams: Fetched ${snapshot.size()} documents")
        
        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(Team::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error converting team document: ${doc.id}", e)
                null
            }
        }
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
            val memberDoc = doc.reference
                .collection("members")
                .document(uid)
                .get()
                .await()
            if (memberDoc.exists()) {
                return doc.id
            }
        }

        return null
    }

    /**
     * Creates a new team ynder a given name and automatically add the current user as the first member.
     *
     */
    suspend fun createTeam(gameId: String, teamName: String): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

        val teamsColl = db.collection("games")
            .document(gameId)
            .collection("teams")

        val normalizedName = teamName.trim()
        if (normalizedName.isEmpty()) {
            throw IllegalArgumentException("Team name cannot be blank")
        }
        ensureUniqueTeamName(gameId, normalizedName)

        val newTeamRef = teamsColl.document()

        val team = Team(
            id = newTeamRef.id,
            name = normalizedName,
            memberCount = 1,
            createdAt = Timestamp.now()
        )

        // Create new team document
        newTeamRef.set(team).await()

        // Add creator as member
        val member = TeamMember(
            userId = uid,
            joinedAt = Timestamp.now()
        )
        newTeamRef.collection("members").document(uid).set(member).await()

        // Remove user from old team if present
        removeUserFromOtherTeams(gameId, uid, exceptTeamId = newTeamRef.id)

        setUserMembership(gameId, newTeamRef.id)
        return newTeamRef.id
    }

    /**
     * Adds the current user to an existing team and removes them from any previous team in the same game
     *
     */
    suspend fun joinTeam(gameId: String, teamId: String) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

        val teamsColl = db.collection("games")
            .document(gameId)
            .collection("teams")
        // First remove user from any existing team
        removeUserFromOtherTeams(gameId, uid, exceptTeamId = teamId)

        // Then join new team
        val teamRef = teamsColl.document(teamId)

        val member = TeamMember(
            userId = uid,
            joinedAt = Timestamp.now()
        )

        teamRef.collection("members")
            .document(uid)
            .set(member)
            .await()

        teamRef.update("memberCount", FieldValue.increment(1)).await()
        setUserMembership(gameId, teamId)
    }

    /**
     * Removes the current user from whatever team they are currenly in
     *
     * - Loop all teams in the game
     * - If the user exists in team.members, delete that member doc
     * - Decrement team.memberCount
     */
    suspend fun leaveTeam(gameId: String) {
        val uid = auth.currentUser?.uid ?: return

        val teamsRef = db.collection("games")
            .document(gameId)
            .collection("teams")

        val snapshot = teamsRef.get().await()

        for (doc in snapshot.documents) {
            val removed = removeMemberFromTeam(doc.reference, uid)
            if (removed) return
        }
    }


    /**
     * Fully deletes a team and all its members.
     * - Note: Firestore doesnt cascade delete so members need to be removed manually
     */
    suspend fun deleteTeam(gameId: String, teamId: String) {
        val teamRef = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)

        // Delete all members
        val members = teamRef.collection("members").get().await()
        members.documents.forEach { it.reference.delete().await() }

        // Delete team doc
        teamRef.delete().await()
    }



    /**
     * Remove a user from all teams in a game except the team they are joining or creating
     * This will ensure that each player belongs to at most one team per game
     */
    private suspend fun removeUserFromOtherTeams(gameId: String, uid: String, exceptTeamId: String?) {
        val teamsRef = db.collection("games")
            .document(gameId)
            .collection("teams")

        val snapshot = teamsRef.get().await()

        for (doc in snapshot.documents) {
            if (doc.id == exceptTeamId) continue
            removeMemberFromTeam(doc.reference, uid)
        }
    }

    private suspend fun ensureUniqueTeamName(gameId: String, desiredName: String) {
        val teamsColl = db.collection("games")
            .document(gameId)
            .collection("teams")

        val snapshot = teamsColl.get().await()
        val duplicateExists = snapshot.documents.any { doc ->
            val existingName = doc.getString("name").orEmpty().trim()
            existingName.equals(desiredName, ignoreCase = true)
        }

        if (duplicateExists) {
            throw IllegalArgumentException("That team name is already taken.")
        }
    }

    private suspend fun removeMemberFromTeam(
        teamRef: DocumentReference,
        uid: String
    ): Boolean {
        var deletedInTransaction = false

        val removed = try {
            db.runTransaction { tx ->
                val memberRef = teamRef.collection("members").document(uid)
                val memberSnap = tx.get(memberRef)
                if (!memberSnap.exists()) {
                    return@runTransaction false
                }

                val teamSnap = tx.get(teamRef)
                if (!teamSnap.exists()) {
                    return@runTransaction false
                }

                tx.delete(memberRef)

                val currentCount = teamSnap.getLong("memberCount") ?: 0L
                val newCount = if (currentCount > 0L) currentCount - 1L else 0L

                if (newCount <= 0L) {
                    tx.delete(teamRef)
                    deletedInTransaction = true
                } else {
                    tx.update(teamRef, "memberCount", newCount)
                }

                true
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "removeMemberFromTeam: failed for team=${teamRef.id}", e)
            false
        }

        if (removed && !deletedInTransaction) {
            deleteTeamIfNoMembers(teamRef)
        }

        return removed
    }

    private suspend fun deleteTeamIfNoMembers(teamRef: DocumentReference) {
        try {
            val remainingMembers = teamRef
                .collection("members")
                .limit(1)
                .get()
                .await()

            if (remainingMembers.isEmpty) {
                teamRef.delete().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteTeamIfNoMembers: failed for team=${teamRef.id}", e)
        }
    }

    /**
     * Real-time listener for teams under a game
     */
    fun listenToTeams(
        gameId: String,
        onTeamsChanged: (List<Team>) -> Unit
    ): ListenerRegistration {
        return db.collection("games")
            .document(gameId)
            .collection("teams")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onTeamsChanged(emptyList())
                    return@addSnapshotListener
                }

                val teams = snapshot.documents
                    .mapNotNull { it.toObject(Team::class.java)?.copy(id = it.id) }

                onTeamsChanged(teams)
            }
    }

    /**
     * Load members of a specific team
     */
    suspend fun getTeamMembers(gameId: String, teamId: String): List<TeamMember> {
        val snap = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("members")
            .get()
            .await()

        return snap.documents.mapNotNull { it.toObject(TeamMember::class.java) }
    }

    /**
     * Real-time listener for members of a specific team
     */
    fun listenToTeamMembers(
        gameId: String,
        teamId: String,
        onMembersChanged: (List<TeamMember>) -> Unit
    ): ListenerRegistration {
        return db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onMembersChanged(emptyList())
                    return@addSnapshotListener
                }

                val members = snapshot.documents
                    .mapNotNull { it.toObject(TeamMember::class.java) }

                onMembersChanged(members)
            }
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

    private suspend fun setUserMembership(gameId: String, teamId: String) {
        val uid = auth.currentUser?.uid ?: return

        val membershipRef = db.collection("users")
            .document(uid)
            .collection("memberships")
            .document(gameId)

        val data = mapOf(
            "gameId" to gameId,
            "teamId" to teamId,
            "updatedAt" to Timestamp.now()
        )

        membershipRef.set(data, SetOptions.merge()).await()
    }

    /**
     * Removes membership doc for this game (not used yet)
     */
    private suspend fun clearUserMembership(gameId: String) {
        val uid = auth.currentUser?.uid ?: return

        val membershipRef = db.collection("users")
            .document(uid)
            .collection("memberships")
            .document(gameId)

        membershipRef.delete().await()
    }

    /**
     * Get team summary for a specific team in a game
     */
    suspend fun getTeamSummary(gameId: String, teamId: String): TeamSummary? {
        val snapshot = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .get()
            .await()
        return snapshot.toObject(TeamSummary::class.java)?.copy(id = snapshot.id)
    }

    /**
     * Listener for all teams in a game
     * Used by the Leaderboard to get live updates when scores change.
     */
    fun listenToTeamsFlow(
        gameId: String
    ): Flow<List<Team>> = callbackFlow {
        val ref = db.collection("games")
            .document(gameId)
            .collection("teams")

        val registration: ListenerRegistration =
            ref.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.w(TAG, "listenToTeamsFlow: error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val teams = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Team::class.java)?.copy(id = doc.id)
                }

                trySend(teams)
            }

        awaitClose { registration.remove() }
    }
}
