package com.aark.sfuscavenger.repositories

import com.aark.sfuscavenger.data.models.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.aark.sfuscavenger.crypto.ChatCrypto

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun listenToTeamChat(
        gameId: String,
        teamId: String
    ): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("chat")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val listener: ListenerRegistration =
            ref.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.let { msg ->
                        val decryptedText = ChatCrypto.decrypt(msg.text)
                        msg.copy(text = decryptedText)
                    }
                }

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        gameId: String,
        teamId: String,
        text: String
    ) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be logged in to send messages")

        val ref = db.collection("games")
            .document(gameId)
            .collection("teams")
            .document(teamId)
            .collection("chat")

        // Encrypt before storing
        val encryptedText = ChatCrypto.encrypt(text)

        val message = ChatMessage(
            text = encryptedText,
            senderId = uid,
            createdAt = Timestamp.now()
        )

        ref.add(message).await()
    }
}
