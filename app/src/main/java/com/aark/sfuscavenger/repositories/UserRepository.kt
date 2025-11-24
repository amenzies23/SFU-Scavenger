package com.aark.sfuscavenger.repositories

import android.net.Uri
import com.aark.sfuscavenger.data.models.Friend
import com.aark.sfuscavenger.data.models.GameMember
import com.aark.sfuscavenger.data.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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

    suspend fun fetchUser(): User? =
        users.document(currentUid()).get().await().toObject(User::class.java)

    suspend fun fetchUserById(userId: String): User? =
        users.document(userId).get().await().toObject(User::class.java)

    /**
     * Finds a user by their username.
     * 
     * @param username The username to search for 
     * @return The user ID if found, null otherwise
     */
    suspend fun findUserByUsername(username: String): String? {
        // space don't matter
        val trimmedUsername = username.trim()
        if (trimmedUsername.isEmpty()) {
            return null
        }

        // Search for user with matching username 
        val snapshot = users
            .whereEqualTo("username", trimmedUsername)
            .limit(1)
            .get()
            .await()
        
        return snapshot.documents.firstOrNull()?.id
    }

    /**
     * Adds a friend by their username.
     * 
     * @param username The username of the person to add as a friend
     * @throws IllegalArgumentException if username not found or trying to add yourself
     */
    suspend fun addFriendByUsername(username: String) {
        val friendId = findUserByUsername(username)
            ?: throw IllegalArgumentException("User with username '$username' not found")
        
        addFriend(friendId)
    }

    /**
     * Gets all friends for the current user.
     * Friends are stored at: users/{userId}/friends/{friendId}
     * 
     * @param limit Maximum number of friends to return (0 = get all friends)
     * @return List of friends with their profile information
     */
    suspend fun fetchFriends(limit: Long = 0): List<Friend> {
        val currentUserId = currentUid()
        val friendIds = getFriendIds(currentUserId, limit)
        
        // Convert friend IDs to Friend objects with full profile info
        return friendIds.mapNotNull { friendId ->
            convertUserToFriend(friendId)
        }
    }

    /**
     * Adds a friend to the current user's friends list.
     * This creates a bidirectional friendship (both users become friends with each other).
     * 
     * @param friendId The user ID of the person to add as a friend
     * @throws IllegalArgumentException if trying to add yourself or if user doesn't exist
     */
    suspend fun addFriend(friendId: String) {
        val currentUserId = currentUid()
        
        // Prevent adding yourself
        if (currentUserId == friendId) {
            throw IllegalArgumentException("Cannot add yourself as a friend")
        }
        
        // Make sure the friend exists
        if (!userExists(friendId)) {
            throw IllegalArgumentException("User with ID $friendId does not exist")
        }
        
        // Create bidirectional friendship
        addFriendship(currentUserId, friendId)
        addFriendship(friendId, currentUserId)
    }

    /**
     * Removes a friend from the current user's friends list.
     * This removes the friendship from both users (bidirectional).
     * 
     * @param friendId The user ID of the person to remove from friends
     */
    suspend fun removeFriend(friendId: String) {
        val currentUserId = currentUid()
        
        // Remove friendship from both sides
        removeFriendship(currentUserId, friendId)
        removeFriendship(friendId, currentUserId)
    }

    /**
     * Gets all friends for a specific user.
     * 
     * @param userId the member we want to check the friends of 
     * @return List of Friend objects with profile information
     */
    suspend fun getFriendsList(userId: String = currentUid()): List<Friend> {
        val friendIds = getFriendIds(userId, limit = 0)
        return friendIds.mapNotNull { friendId ->
            convertUserToFriend(friendId)
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
        return snapshot.documents.mapNotNull { doc ->
            val membership = doc.toObject(GameMember::class.java)
            // Use document ID as gameId if the field is empty (for empty membership documents)
            membership?.copy(gameId = membership.gameId.ifBlank { doc.id })
        }
    }

    private suspend fun updateUserFields(fields: Map<String, Any?>) {
        users.document(currentUid())
            .set(fields, SetOptions.merge())
            .await()
    }

    // ==================== Helper Functions for Friends ====================
    
    /**
     * Gets the list of friend IDs for a user from Firestore.
     * Path: users/{userId}/friends/{friendId}
     */
    private suspend fun getFriendIds(userId: String, limit: Long): List<String> {
        val friendsCollection = users.document(userId).collection("friends")
        
        val snapshot = if (limit > 0) {
            friendsCollection.limit(limit).get().await()
        } else {
            friendsCollection.get().await()
        }
        
        return snapshot.documents.map { it.id }
    }
    
    /**
     * Converts a user document to a Friend object.
     * Returns null if the user doesn't exist.
     */
    private suspend fun convertUserToFriend(friendId: String): Friend? {
        val userDoc = users.document(friendId).get().await()
        
        if (!userDoc.exists()) {
            return null
        }
        
        return Friend(
            id = friendId,
            displayName = getDisplayName(userDoc),
            username = userDoc.getString("username"),
            photoUrl = userDoc.getString("photoUrl"),
            level = getIntField(userDoc, "level", defaultValue = 1),
            xp = getIntField(userDoc, "xp", defaultValue = 0)
        )
    }
    
    /**
     * Gets the display name from a user document, falling back to email if needed.
     */
    private fun getDisplayName(userDoc: com.google.firebase.firestore.DocumentSnapshot): String {
        val displayName = userDoc.getString("displayName").orEmpty()
        if (displayName.isNotBlank()) {
            return displayName
        }
        return userDoc.getString("email").orEmpty()
    }
    
    /**
     * Gets an integer field from a document, with a default value if missing.
     */
    private fun getIntField(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        fieldName: String,
        defaultValue: Int
    ): Int {
        return (doc.getLong(fieldName) ?: defaultValue.toLong()).toInt()
    }
    
    /**
     * Checks if a user exists in Firestore.
     */
    private suspend fun userExists(userId: String): Boolean {
        val userDoc = users.document(userId).get().await()
        return userDoc.exists()
    }
    
    /**
     * Adds a friendship relationship.
     * Creates: users/{userId1}/friends/{userId2}
     */
    private suspend fun addFriendship(userId1: String, userId2: String) {
        val friendshipDoc = getFriendshipDocument(userId1, userId2)
        friendshipDoc.set(mapOf("addedAt" to Timestamp.now())).await()
    }
    
    /**
     * Removes a friendship relationship.
     * Deletes: users/{userId1}/friends/{userId2}
     */
    private suspend fun removeFriendship(userId1: String, userId2: String) {
        val friendshipDoc = getFriendshipDocument(userId1, userId2)
        friendshipDoc.delete().await()
    }
    
    /**
     * Gets the friendship document reference.
     * Path: users/{userId1}/friends/{userId2}
     */
    private fun getFriendshipDocument(userId1: String, userId2: String) =
        users.document(userId1).collection("friends").document(userId2)
}

