package com.aark.sfuscavenger.repositories

import com.aark.sfuscavenger.data.models.Submission
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


class SubmissionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun listenToTeamSubmissions(
        gameId: String,
        teamId: String
    ): Flow<List<Submission>> = callbackFlow {
        val ref = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("submissions")

        val listener: ListenerRegistration =
            ref.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                android.util.Log.d("SubmissionRepo",
                    "Got snapshot with ${snapshot?.size()} submissions")

                val subs = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Submission::class.java)?.copy(id = doc.id)
                    }
                    // Only show markers that actually have a geo location
                    ?.filter { it.geo != null }
                    .orEmpty()

                trySend(subs)
            }

        awaitClose { listener.remove() }
    }
}
