package com.aark.sfuscavenger.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aark.sfuscavenger.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel (private val repo: AuthRepository = AuthRepository()) : ViewModel() {
    private val _state = MutableStateFlow(
        AuthUiState(isLoggedIn = repo.currentUid != null)
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        // Reacts to FirebaseAuth state changes
        viewModelScope.launch {
            repo.authState().collect { loggedIn ->
                _state.update { it.copy(isLoggedIn = loggedIn, loading = false, error = null) }
            }
        }
    }
    // Functions for our screen to invoke
    fun signIn(email: String, password: String) = runAuth { repo.signIn(email, password) }
    fun signUp(email: String, password: String) = runAuth { repo.signUp(email, password) }
    // TODO: Use this in the social (maybe?) tab to log out
    fun signOut() = repo.signOut()

    private fun runAuth(block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            block()
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = userMessage(e)) }
        }
    }

    fun updateDisplayName(name: String) = runAuth {
        repo.updateDisplayName(name)
    }

    // To display messages with any exceptions thrown
    private fun userMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            "weak-password" in msg -> "Password is too weak (min 6)."
            "email-already-in-use" in msg -> "Email already in use."
            "invalid-credential" in msg || "invalid-email" in msg -> "Invalid email or password."
            else -> msg.ifBlank { "Authentication error." }
        }
    }
}