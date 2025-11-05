package com.aark.sfuscavenger.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Profile Picture
 * Display Name
 * Username
 * Level #
 * XP that level
 * Friends List
 */

// Friends Info
data class Friend (
    val name: String,
    val profilePicture: String? = null,
    val totalXP: Int = 0
)

// Profile State
data class ProfileState (
    val profilePicture: String? = null,
    val displayName: String = "",
    val username: String = "",
    val userLevel: Int = 0,
    val totalXP: Int = 0, // total XP for each specific level
    val friends: List<Friend> = emptyList()
)

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    // Set display name text
    fun setDisplayName(name: String) {
        _state.update { it.copy(displayName = name )}
    }

    // Set username text
    fun setUsername(username: String) {
        _state.update { it.copy(username = username)}
    }

    // Show profile picture
    fun setAvatar(url: String?) {
        _state.update { it.copy(profilePicture = url)}
    }

    // Show level
    fun setLevel(level: Int) {
        _state.update { it.copy(userLevel = level)}
    }

    // Add XP
    fun addXP(levelXP: Int) {
        _state.update { it.copy(totalXP = levelXP)}
    }

    // Add friend
}
