package com.aark.sfuscavenger.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
}
