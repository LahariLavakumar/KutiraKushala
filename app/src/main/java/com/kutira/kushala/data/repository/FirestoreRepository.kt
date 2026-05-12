package com.kutira.kushala.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kutira.kushala.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val currentUid get() = auth.currentUser?.uid
        ?: throw IllegalStateException("User not authenticated")

    suspend fun createUserProfile(profile: UserProfile) {
        require(profile.role in listOf("Maker", "Collector")) { "Role must be Maker or Collector." }
        require(profile.uid == currentUid) { "Profile UID must match authenticated user." }
        db.collection("users").document(currentUid).set(profile, SetOptions.merge()).await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? =
        db.collection("users").document(uid).get().await()
            .toObject(UserProfile::class.java)

    fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                trySend(snap?.toObject(UserProfile::class.java))
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateUserProfile(displayName: String) {
        db.collection("users").document(currentUid)
            .update(
                mapOf(
                    "displayName" to displayName,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    fun getBusinessesFlow(category: String? = null): Flow<List<Business>> = callbackFlow {
        android.util.Log.d("FirestoreRepo", "DEBUG: getBusinessesFlow started by user: $currentUid")
        val listener = db.collection("businesses").addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.e("FirestoreRepo", "DEBUG: Firestore Error: ${err.message}")
                close(err)
                return@addSnapshotListener
            }

            if (snap == null || snap.isEmpty) {
                android.util.Log.d("FirestoreRepo", "DEBUG: No businesses found in Firestore (Snapshot empty)")
            } else {
                android.util.Log.d("FirestoreRepo", "DEBUG: Received ${snap.size()} raw documents from Firestore")
            }

            val list = snap?.documents
                ?.mapNotNull { doc ->
                    val biz = doc.toBusinessOrNull()
                    if (biz == null) {
                        android.util.Log.w("FirestoreRepo", "DEBUG: Skipping malformed doc: ${doc.id} | Data: ${doc.data}")
                    }
                    biz
                }
                ?.filter { business ->
                    category.isNullOrBlank() ||
                            business.category.equals(category, ignoreCase = true)
                }
                ?: emptyList()

            android.util.Log.d("FirestoreRepo", "DEBUG: Emitting ${list.size} businesses to UI")
            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    fun getBusinessByIdFlow(businessId: String): Flow<Business?> = callbackFlow {
        val listener = db.collection("businesses").document(businessId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                trySend(snap?.toBusinessOrNull())
            }

        awaitClose { listener.remove() }
    }

    suspend fun getBusinessById(businessId: String): Business? =
        db.collection("businesses").document(businessId).get().await()
            .toBusinessOrNull()

    suspend fun createBusiness(business: Business): String {
        validateBusiness(business)

        val doc = db.collection("businesses").document()
        // Ensure the ID is saved inside the document field as well
        val finalBusiness = business.copy(id = doc.id, ownerId = currentUid)

        android.util.Log.d(
            "FirestoreRepo",
            "Creating business: ${finalBusiness.name} at path: ${doc.path}"
        )

        doc.set(finalBusiness).await()
        return doc.id
    }

    suspend fun updateBusiness(businessId: String, business: Business) {
        val existing = getBusinessById(businessId)
            ?: throw IllegalArgumentException("Business not found.")

        require(existing.ownerId == currentUid) { "Only the owner can update this business." }
        validateBusiness(business)

        android.util.Log.d(
            "FirestoreRepo",
            "Updating business: $businessId, isAccepting=${business.isAcceptingOrders}"
        )

        val updatedBusiness = business.copy(
            ownerId = existing.ownerId,
            createdAt = existing.createdAt,
            imageUrl = if (business.imageUrl.isBlank()) existing.imageUrl else business.imageUrl
        )

        db.collection("businesses").document(businessId)
            .set(updatedBusiness, SetOptions.merge())
            .await()
    }

    suspend fun deleteBusiness(businessId: String) {
        val existing = getBusinessById(businessId)
            ?: throw IllegalArgumentException("Business not found.")

        require(existing.ownerId == currentUid) { "Only the owner can delete this business." }

        db.collection("businesses").document(businessId).delete().await()
    }

    fun getProductsFlow(businessId: String): Flow<List<Product>> = callbackFlow {
        val listener = db.collection("businesses").document(businessId)
            .collection("products")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Product::class.java)?.copy(id = it.id) }
                    ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun getAllProductsFlow(): Flow<List<ProductWithBusiness>> = callbackFlow {
        android.util.Log.d("FirestoreRepo", "Starting collectionGroup('products') listener...")
        val listener = db.collectionGroup("products")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("FirestoreRepo", "Error in getAllProductsFlow: ${err.message}", err)
                    if (err.message?.contains("FAILED_PRECONDITION") == true) {
                        android.util.Log.e("FirestoreRepo", "INDEX MISSING! Please check the link in the error message above to create the required index.")
                    }
                    close(err)
                    return@addSnapshotListener
                }
                
                if (snap == null || snap.isEmpty) {
                    android.util.Log.d("FirestoreRepo", "getAllProductsFlow: Snapshot is empty or null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snap.documents.mapNotNull { doc ->
                    try {
                        val product = doc.toObject(Product::class.java)?.copy(id = doc.id)
                        // In a subcollection 'products', the parent is the business document
                        val businessId = doc.reference.parent.parent?.id
                        
                        if (product != null && businessId != null) {
                            ProductWithBusiness(product = product, businessId = businessId)
                        } else {
                            android.util.Log.w("FirestoreRepo", "Malformed product doc ${doc.id}: product=$product, bizId=$businessId")
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepo", "Error parsing product ${doc.id}", e)
                        null
                    }
                }
                android.util.Log.d("FirestoreRepo", "getAllProductsFlow emitted ${list.size} products")
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getProductById(businessId: String, productId: String): Product? =
        db.collection("businesses").document(businessId)
            .collection("products").document(productId)
            .get().await()
            .toObject(Product::class.java)

    suspend fun createProduct(businessId: String, product: Product): String {
        val business = getBusinessById(businessId)
            ?: throw IllegalArgumentException("Parent business not found.")

        require(business.ownerId == currentUid) { "Only the business owner can add products." }
        validateProduct(product)

        val doc = db.collection("businesses")
            .document(businessId)
            .collection("products")
            .document()

        // Ensure the ID is saved inside the document field
        val finalProduct = product.copy(id = doc.id)
        
        android.util.Log.d("FirestoreRepo", "Creating product: ${product.name} at path: ${doc.path}")
        doc.set(finalProduct).await()
        return doc.id
    }

    suspend fun updateProduct(businessId: String, productId: String, product: Product) {
        val business = getBusinessById(businessId)
            ?: throw IllegalArgumentException("Parent business not found.")

        require(business.ownerId == currentUid) { "Only the business owner can update products." }

        val existing = getProductById(businessId, productId)
            ?: throw IllegalArgumentException("Product not found.")

        validateProduct(product)

        val updatedProduct = product.copy(
            createdAt = existing.createdAt,
            imageUrl = if (product.imageUrl.isBlank()) existing.imageUrl else product.imageUrl
        )

        db.collection("businesses").document(businessId)
            .collection("products").document(productId)
            .set(updatedProduct, SetOptions.merge())
            .await()
    }

    suspend fun deleteProduct(businessId: String, productId: String) {
        val business = getBusinessById(businessId)
            ?: throw IllegalArgumentException("Parent business not found.")

        require(business.ownerId == currentUid) { "Only the business owner can delete products." }

        db.collection("businesses").document(businessId)
            .collection("products").document(productId)
            .delete()
            .await()
    }

    fun getWishlistFlow(): Flow<List<WishlistItem>> = callbackFlow {
        val listener = db.collection("wishlists")
            .whereEqualTo("userId", currentUid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                trySend(snap?.toObjects(WishlistItem::class.java) ?: emptyList())
            }

        awaitClose { listener.remove() }
    }

    suspend fun addToWishlist(item: WishlistItem) {
        require(item.userId == currentUid) { "userId must match authenticated user." }
        db.collection("wishlists").document().set(item).await()
    }

    suspend fun removeFromWishlist(wishlistId: String) {
        val doc = db.collection("wishlists").document(wishlistId).get().await()
        require(doc.getString("userId") == currentUid) { "You can only remove your own wishlist items." }
        db.collection("wishlists").document(wishlistId).delete().await()
    }

    suspend fun createQuote(quote: Quote): String {
        require(quote.buyerId == currentUid) { "buyerId must match authenticated user." }
        require(quote.quantity > 0) { "Quantity must be greater than 0." }
        require(quote.status == QuoteStatus.Pending.name) { "New quotes must have Pending status." }

        val doc = db.collection("quotes").document()
        doc.set(quote).await()
        return doc.id
    }

    fun getBuyerQuotesFlow(): Flow<List<Quote>> = callbackFlow {
        val listener = db.collection("quotes")
            .whereEqualTo("buyerId", currentUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Quote::class.java)?.copy(id = it.id) }
                    ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun getSellerQuotesFlow(): Flow<List<Quote>> = callbackFlow {
        val listener = db.collection("quotes")
            .whereEqualTo("sellerId", currentUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Quote::class.java)?.copy(id = it.id) }
                    ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateQuote(quoteId: String, updates: Map<String, Any>) {
        val existing = db.collection("quotes").document(quoteId).get().await()
        val sellerId = existing.getString("sellerId")
        val buyerId = existing.getString("buyerId")
        val isSeller = sellerId == currentUid
        val isBuyer = buyerId == currentUid

        require(isSeller || isBuyer) { "Only the buyer or seller can update this quote." }

        val allowedSellerKeys = setOf("status", "updatedAt", "trackingCarrier", "trackingNumber")
        val allowedBuyerKeys = setOf("status", "updatedAt")

        if (isBuyer) {
            require(updates["status"] == QuoteStatus.Cancelled.name) { "Buyers can only cancel orders." }
            require(updates.keys.all { it in allowedBuyerKeys }) {
                "Buyers may only update status and updatedAt."
            }
        }

        if (isSeller) {
            require(updates.keys.all { it in allowedSellerKeys }) {
                "Sellers may only update status, tracking info, and updatedAt."
            }
        }

        db.collection("quotes").document(quoteId).update(updates).await()
    }

    fun getRatingsForProduct(productId: String): Flow<List<Rating>> = callbackFlow {
        val listener = db.collection("ratings")
            .whereEqualTo("productId", productId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                trySend(snap?.toObjects(Rating::class.java) ?: emptyList())
            }

        awaitClose { listener.remove() }
    }

    suspend fun submitRating(rating: Rating) {
        require(rating.userId == currentUid) { "userId must match authenticated user." }
        require(rating.rating in 1..5) { "Rating must be between 1 and 5." }
        require(rating.review.length <= 500) { "Review must not exceed 500 characters." }

        db.collection("ratings").document().set(rating).await()
    }

    suspend fun deleteRating(ratingId: String) {
        val doc = db.collection("ratings").document(ratingId).get().await()
        require(doc.getString("userId") == currentUid) { "You can only delete your own ratings." }
        db.collection("ratings").document(ratingId).delete().await()
    }

    private fun validateBusiness(b: Business) {
        require(b.name.isNotBlank() && b.name.length <= 100) { "Business name: 1-100 chars." }
        require(b.location.isNotBlank() && b.location.length <= 200) { "Location: 1-200 chars." }
        require(b.category.isNotBlank() && b.category.length <= 50) { "Category: 1-50 chars." }
        require(b.contactNumber.matches(Regex("^\\+?[0-9]{10,15}$"))) { "Invalid contact number format." }
        require(b.capacity >= 0) { "Capacity must be non-negative." }
        require(b.capacityUnit.length <= 20) { "Capacity unit: max 20 chars." }
        require(b.teamDetails.length <= 500) { "Team details: max 500 chars." }
        require(b.imageUrl.length <= 1000) { "Image URL: max 1000 chars." }
    }

    private fun validateProduct(p: Product) {
        require(p.name.isNotBlank() && p.name.length <= 100) { "Product name: 1-100 chars." }
        require(p.wholesalePrice >= 0) { "Wholesale price must be non-negative." }
        require(p.minOrderQuantity > 0) { "Min order quantity must be greater than 0." }
        require(p.description.length <= 1000) { "Description: max 1000 chars." }
        require(p.imageUrl.length <= 1000) { "Image URL: max 1000 chars." }
        require(p.categoryTags.size <= 10) { "Max 10 category tags allowed." }
    }

    private fun DocumentSnapshot.toBusinessOrNull(): Business? {
        val business = Business(
            id = id,
            name = firstString("name", "businessName", "shopName", "title"),
            location = firstString("location", "city", "address"),
            category = firstString("category", "businessCategory", "type"),
            teamDetails = firstString("teamDetails", "description", "about"),
            ownerId = firstString("ownerId", "userId", "uid"),
            contactNumber = firstString("contactNumber", "phoneNumber", "phone", "contact"),
            capacity = firstDouble("capacity", "productionCapacity"),
            capacityUnit = firstString("capacityUnit", "unit"),
            isAcceptingOrders = firstBoolean("isAcceptingOrders", "acceptingOrders", "isOpen", "open"),
            imageUrl = firstString("imageUrl", "imageURL", "photoUrl", "image", "photo"),
            createdAt = firstTimestamp("createdAt"),
            updatedAt = firstTimestamp("updatedAt")
        )

        return business.takeIf { it.name.isNotBlank() }
    }

    private fun DocumentSnapshot.firstString(vararg keys: String): String =
        keys.firstNotNullOfOrNull { key ->
            when (val value = get(key)) {
                is String -> value.trim().takeIf { it.isNotBlank() }
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> null
            }
        } ?: ""

    private fun DocumentSnapshot.firstDouble(vararg keys: String): Double =
        keys.firstNotNullOfOrNull { key ->
            when (val value = get(key)) {
                is Number -> value.toDouble()
                is String -> value.trim().toDoubleOrNull()
                else -> null
            }
        } ?: 0.0

    private fun DocumentSnapshot.firstBoolean(vararg keys: String): Boolean =
        keys.firstNotNullOfOrNull { key ->
            when (val value = get(key)) {
                is Boolean -> value
                is Number -> value.toInt() != 0
                is String -> when (value.trim().lowercase()) {
                    "true", "yes", "open", "active", "1" -> true
                    "false", "no", "closed", "inactive", "0" -> false
                    else -> null
                }
                else -> null
            }
        } ?: false

    private fun DocumentSnapshot.firstTimestamp(vararg keys: String): Timestamp? =
        keys.firstNotNullOfOrNull { key ->
            get(key) as? Timestamp
        }
}
