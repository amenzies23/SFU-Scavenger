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
    val friends: List<Friend> = emptyList()
)

class ProfileViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        refreshProfile()
    }

    fun refreshProfile() = launchScoped {
        val user = repository.fetchUser()
        val friends = repository.fetchFriends()
        _state.update {
            it.copy(
                profilePicture = user?.photoUrl,
                displayName = user?.displayName.orEmpty(),
                username = user?.username.orEmpty(),
                userLevel = user?.level ?: 1,
                totalXP = user?.xp ?: 0,
                friends = friends
            )
        }
    }

    // TODO: Implement AppCheck for uploads
    // right now we cannot save profile pictures because firebase must validate the photos that are being saved
    fun saveProfile(displayName: String, username: String, newImage: Uri?, removePhoto: Boolean) =
        launchScoped {
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

    private fun launchScoped(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { Log.e(TAG, "Profile error", it) }
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
