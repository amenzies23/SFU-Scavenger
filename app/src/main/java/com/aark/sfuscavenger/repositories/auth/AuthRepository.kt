package com.aark.sfuscavenger.repositories.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUid: String? get() = auth.currentUser?.uid

    fun authState(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String): AuthResult =
        auth.createUserWithEmailAndPassword(email.trim(), password).await()

    suspend fun signIn(email: String, password: String): AuthResult =
        auth.signInWithEmailAndPassword(email.trim(), password).await()

    fun signOut() = auth.signOut()
}