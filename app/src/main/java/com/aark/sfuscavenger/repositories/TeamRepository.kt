package com.aark.sfuscavenger.repositories

import com.aark.sfuscavenger.data.model.TeamMember
import com.aark.sfuscavenger.data.models.Team
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class TeamRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun getUserTeamId(gameId: String): String? {
        val uid = auth.currentUser?.uid ?: return null

        val teamsRef = db.collection("games")
            .document(gameId)
            .collection("teams")

        val snapshot = teamsRef.get().await()

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

        val newTeamRef = teamsColl.document()

        val team = Team(
            id = newTeamRef.id,
            name = teamName.trim(),
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
            val memberRef = doc.reference.collection("members").document(uid)
            val memberSnap = memberRef.get().await()

            if (memberSnap.exists()) {
                // Remove member
                memberRef.delete().await()

                // Decrement memberCount
                doc.reference.update(
                    "memberCount",
                    FieldValue.increment(-1)
                ).await()

                return
            }
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

            val memberRef = doc.reference
                .collection("members")
                .document(uid)

            val memberSnap = memberRef.get().await()

            if (memberSnap.exists()) {
                // Remove membership
                memberRef.delete().await()

                // Decrement memberCount
                doc.reference.update(
                    "memberCount",
                    FieldValue.increment(-1)
                ).await()
            }
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
}
