package com.kutira.kushala.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val currentUid get() = auth.currentUser?.uid
        ?: throw IllegalStateException("Not authenticated")

    /** Upload a business workspace image and return the download URL. */
    suspend fun uploadBusinessImage(businessId: String, imageUri: Uri): String {
        val ref = storage.reference.child("businesses/$businessId/profile.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    /** Upload a product image and return the download URL. */
    suspend fun uploadProductImage(businessId: String, productId: String, imageUri: Uri): String {
        val ref = storage.reference.child("businesses/$businessId/products/$productId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteBusinessImage(businessId: String) {
        storage.reference.child("businesses/$businessId/profile.jpg").delete().await()
    }
}
