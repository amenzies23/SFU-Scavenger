package com.aark.sfuscavenger.repositories

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.aark.sfuscavenger.data.models.User

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUid: String? get() = auth.currentUser?.uid
    private val usersCollection = db.collection("users")

    fun authState(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim()

        // Create auth user
        val result = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
        val uid = result.user?.uid
            ?: throw IllegalStateException("User created but uid is null")

        // Create Firestore user document
        val user = User(
            displayName = null, // will be filled in later
            email = trimmedEmail,
            level = 1,
            xp = 0,
            createdAt = Timestamp.now()
        )

        usersCollection.document(uid).set(user).await()

        return result
    }

    suspend fun signIn(email: String, password: String): AuthResult =
        auth.signInWithEmailAndPassword(email.trim(), password).await()

    fun signOut() = auth.signOut()

    // To be used after sign up, to add a display name
    suspend fun updateDisplayName(displayName: String) {
        val uid = currentUid ?: throw IllegalStateException("Not logged in")
        usersCollection.document(uid)
            .update("displayName", displayName.trim())
            .await()
    }
}