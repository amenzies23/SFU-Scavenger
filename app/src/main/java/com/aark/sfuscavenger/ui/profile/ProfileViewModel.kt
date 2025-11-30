package com.aark.sfuscavenger.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.data.models.Friend
import com.aark.sfuscavenger.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Profile Picture
 * Display Name
 * Username
 * Level #
 * XP that level
 * Friends List
 */

// Profile State
data class ProfileState(
    val profilePicture: String? = null,
    val displayName: String = "",
    val username: String = "",
    val userLevel: Int = 0,
    val totalXP: Int = 0, // total XP for each specific level
    val xpForNextLevel: Int = 50,
    val friends: List<Friend> = emptyList(),
    val addFriendError: String? = null 
)

class ProfileViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        refreshProfile()
    }

    /**
     * shows users that are in (users/{userId}/friends/{friendId}).
     */
    fun refreshProfile() = launchScoped {
        val user = repository.fetchUser()
        val friends = repository.fetchFriends()
        val level = user?.level ?: 1
        val nextLevelXp = repository.calculateLevelRequirement(level)

        _state.update {
            it.copy(
                profilePicture = user?.photoUrl,
                displayName = user?.displayName.orEmpty(),
                username = user?.username.orEmpty(),
                userLevel = level,
                totalXP = user?.xp ?: 0,
                xpForNextLevel = nextLevelXp,
                friends = friends
            )
        }
    }

    fun saveProfile(displayName: String, username: String, newImage: Uri?, removePhoto: Boolean) =
        launchScoped {
            try {
                repository.updateDisplayName(displayName)
                repository.updateUsername(username)
                val photoUrl = resolvePhotoUrl(newImage, removePhoto)
                
                _state.update {
                    it.copy(
                        displayName = displayName,
                        username = username,
                        profilePicture = photoUrl
                    )
                }
                
                refreshProfile()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile", e)
            }
        }

    private suspend fun resolvePhotoUrl(newImage: Uri?, removePhoto: Boolean): String? {
        val current = _state.value.profilePicture
        return when {
            removePhoto -> {
                repository.updatePhotoUrl(null)
                null
            }
            newImage != null -> {
                val uploadedUrl = repository.uploadProfileImage(newImage)
                repository.updatePhotoUrl(uploadedUrl)
                uploadedUrl
            }
            else -> current
        }
    }

    /**
     * Adds a friend by their username.
     * After adding, refreshes the profile to show the new friend.
     * 
     * @param username The username of the person to add as a friend
     */
    fun addFriendByUsername(username: String) = launchScoped {
        // Clear any previous error messages
        _state.update { it.copy(addFriendError = null) }
        
        try {
            // Try to add the friend by username
            repository.addFriendByUsername(username)
            
            // If successful, refresh the profile to show the new friend
            refreshProfile()
            Log.d(TAG, "Successfully added friend by username: $username")
            
        } catch (e: IllegalArgumentException) {
            // Handle specific error cases
            Log.e(TAG, "Error adding friend by username: ${e.message}", e)
            
            val errorMessage = when {
                // Username doesn't exist in the database
                e.message?.contains("not found") == true -> {
                    "User with username '$username' not found"
                }
                // User tried to add themselves as a friend
                e.message?.contains("yourself") == true -> {
                    "You cannot add yourself as a friend :["
                }
                // Any other IllegalArgumentException - use the original message
                else -> {
                    e.message ?: "Failed to add friend"
                }
            }
            
            // Update state with the error message so UI can display it
            _state.update { it.copy(addFriendError = errorMessage) }
            
        } catch (e: Exception) {
            // Handle any other unexpected errors
            Log.e(TAG, "Unexpected error adding friend by username: ${e.message}", e)
            _state.update { 
                it.copy(addFriendError = "Failed to add friend. Please try again.")
            }
        }
    }

    /**
     * Clears the add friend error message.
     */
    fun clearAddFriendError() {
        _state.update { it.copy(addFriendError = null) }
    }

    private fun launchScoped(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { Log.e(TAG, "Profile error", it) }
        }
    }

}
