package com.aark.sfuscavenger.repositories

import android.net.Uri
import com.aark.sfuscavenger.data.models.Friend
import com.aark.sfuscavenger.data.models.GameMember
import com.aark.sfuscavenger.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Central place for reading/writing user-related Firestore data.
 * Used by the profile screen (friends, avatar updates) and by the
 * history feature (memberships list).
 */
class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private val users = firestore.collection("users")

    private fun currentUid(): String =
        auth.currentUser?.uid ?: error("User not logged in")

    suspend fun fetchUser(): User? =
        users.document(currentUid()).get().await().toObject(User::class.java)

    suspend fun fetchUserById(userId: String): User? =
        users.document(userId).get().await().toObject(User::class.java)

    suspend fun fetchFriends(limit: Long = 8): List<Friend> {
        if (limit <= 0) return emptyList()

        val myId = currentUid()
        val snapshot = users
            .limit(limit + 1)
            .get()
            .await()

        return snapshot.documents
            .filter { it.id != myId }
            .take(limit.toInt())
            .map { doc ->
                Friend(
                    id = doc.id,
                    displayName = doc.getString("displayName")
                        .orEmpty()
                        .ifBlank { doc.getString("email").orEmpty() },
                    username = doc.getString("username"),
                    photoUrl = doc.getString("photoUrl"),
                    level = (doc.getLong("level") ?: 1L).toInt(),
                    xp = (doc.getLong("xp") ?: 0L).toInt()
                )
            }
    }

    suspend fun updateDisplayName(name: String) =
        updateUserFields(mapOf("displayName" to name.trim()))

    suspend fun updateUsername(username: String) =
        updateUserFields(mapOf("username" to username.trim()))

    suspend fun updatePhotoUrl(url: String?) =
        updateUserFields(mapOf("photoUrl" to url))

    suspend fun uploadProfileImage(imageUri: Uri): String {
        val path = "profilePhotos/${currentUid()}/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(path)
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun getMembershipsForUser(userId: String = currentUid()): List<GameMember> {
        val snapshot = users
            .document(userId)
            .collection("memberships")
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(GameMember::class.java) }
    }

    private suspend fun updateUserFields(fields: Map<String, Any?>) {
        users.document(currentUid())
            .set(fields, SetOptions.merge())
            .await()
    }
}

