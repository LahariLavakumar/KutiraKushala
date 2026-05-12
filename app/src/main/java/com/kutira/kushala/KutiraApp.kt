package com.kutira.kushala

import android.app.Application
import com.google.firebase.FirebaseApp
import com.kutira.kushala.data.repository.*

class KutiraApp : Application() {

    // Simple manual DI — swap for Hilt/Koin if the project grows.
    val authRepository by lazy { AuthRepository() }
    val firestoreRepository by lazy { FirestoreRepository() }
    val storageRepository by lazy { StorageRepository() }
    val geminiRepository by lazy { GeminiRepository() }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}

/** Extension to access repositories from any Composable via LocalContext. */
fun android.content.Context.app() = applicationContext as KutiraApp
