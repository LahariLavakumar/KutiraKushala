package com.kutira.kushala.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutira.kushala.data.model.UserProfile
import com.kutira.kushala.data.repository.AuthRepository
import com.kutira.kushala.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val authRepo: AuthRepository = AuthRepository(),
    private val firestoreRepo: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser = authRepo.authStateFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, authRepo.currentUser
    )

    fun signUp(email: String, password: String, role: String, displayName: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "Starting signUp for $email")
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val user = authRepo.signUp(email, password)
                android.util.Log.d("AuthViewModel", "Auth signUp success: ${user.uid}")
                // Create Firestore profile after sign-up
                val profile = UserProfile(
                    uid = user.uid,
                    email = email,
                    displayName = displayName,
                    role = role
                )
                firestoreRepo.createUserProfile(profile)
                android.util.Log.d("AuthViewModel", "Firestore profile created")
                _uiState.update { it.copy(isLoading = false, successMessage = "Check your email to verify your account.") }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "signUp error", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "Starting signIn for $email")
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authRepo.signIn(email, password)
                android.util.Log.d("AuthViewModel", "Auth signIn success")
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "signIn error", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun signOut() = authRepo.signOut()

    fun clearError() = _uiState.update { it.copy(error = null) }
}
