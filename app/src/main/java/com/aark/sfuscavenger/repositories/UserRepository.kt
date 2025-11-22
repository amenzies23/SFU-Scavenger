package com.aark.sfuscavenger.repositories

import android.net.Uri
import com.aark.sfuscavenger.data.models.Friend
import com.aark.sfuscavenger.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID


class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private val users = firestore.collection("users")

    private fun currentUid(): String =
        auth.currentUser?.uid ?: error("User not logged in")

    suspend fun fetchUser(): User? {
        val snapshot = users.document(currentUid()).get().await()
        return snapshot.toObject(User::class.java)
    }

    suspend fun fetchFriends(limit: Long = 8): List<Friend> {
        if (limit <= 0) return emptyList()

        val documents = users
            .limit(limit + 1) // fetch one extra so we can drop the current user
            .get()
            .await()
            .documents

        val myId = currentUid()
        val friends = mutableListOf<Friend>()

        for (doc in documents) {
            if (doc.id == myId) continue
            friends += doc.toFriend()
            if (friends.size >= limit) break
        }

        return friends
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

    private suspend fun updateUserFields(fields: Map<String, Any?>) {
        users.document(currentUid())
            .set(fields, SetOptions.merge())
            .await()
    }

    private fun DocumentSnapshot.toFriend(): Friend {
        val fallbackName = getString("email") ?: "Explorer"
        val displayName = getString("displayName").orEmpty().ifBlank { fallbackName }
        return Friend(
            id = id,
            displayName = displayName,
            username = getString("username"),
            photoUrl = getString("photoUrl"),
            level = (getLong("level") ?: 1L).toInt(),
            xp = (getLong("xp") ?: 0L).toInt()
        )
    }
}

