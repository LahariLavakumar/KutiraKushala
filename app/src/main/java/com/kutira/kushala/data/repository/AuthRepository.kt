package com.kutira.kushala.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String): FirebaseUser {
        require(email.isNotBlank()) { "Email cannot be empty." }
        require(password.length >= 8) { "Password must be at least 8 characters." }
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.sendEmailVerification()?.await()
        return result.user ?: throw IllegalStateException("User creation failed.")
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw IllegalStateException("Sign-in failed.")
    }

    suspend fun reloadUser() {
        auth.currentUser?.reload()?.await()
    }

    fun isEmailVerified() = auth.currentUser?.isEmailVerified == true

    fun signOut() = auth.signOut()

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }
}
