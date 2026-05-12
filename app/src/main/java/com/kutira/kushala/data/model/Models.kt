package com.kutira.kushala.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// ─────────────────────────────────────────────
// UserProfile  →  /users/{userId}
// ─────────────────────────────────────────────
data class UserProfile(
    @DocumentId val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    /**
     * "Maker"     – cottage-industry owner who lists a business & products.
     * "Collector" – bulk buyer who browses, wishlists, and places quotes.
     * Role is IMMUTABLE once set (enforced by Firestore rules).
     */
    val role: String = "",          // "Maker" | "Collector"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// ─────────────────────────────────────────────
// Business  →  /businesses/{businessId}
// ─────────────────────────────────────────────
data class Business(
    @DocumentId val id: String = "",
    val name: String = "",
    val location: String = "",
    val category: String = "",          // e.g. "Textiles", "Food", "Handicrafts"
    val teamDetails: String = "",
    val ownerId: String = "",           // Firebase Auth UID — immutable
    val contactNumber: String = "",
    val capacity: Double = 0.0,
    val capacityUnit: String = "",      // e.g. "pieces/day"
    @get:PropertyName("isAcceptingOrders") @set:PropertyName("isAcceptingOrders") var isAcceptingOrders: Boolean = false,
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// ─────────────────────────────────────────────
// Product  →  /businesses/{businessId}/products/{productId}
// ─────────────────────────────────────────────
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val wholesalePrice: Double = 0.0,   // must be >= 0
    val minOrderQuantity: Double = 1.0, // must be > 0
    val categoryTags: List<String> = emptyList(),
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// ─────────────────────────────────────────────
// ProductWithBusiness  →  helper for collection group queries
// ─────────────────────────────────────────────
data class ProductWithBusiness(
    val product: Product = Product(),
    val businessId: String = "",
    val businessName: String = "",
    val businessLocation: String = "",
    val businessCategory: String = "",
    @get:PropertyName("isAcceptingOrders") @set:PropertyName("isAcceptingOrders") var isAcceptingOrders: Boolean = false
)

// ─────────────────────────────────────────────
// WishlistItem  →  /wishlists/{wishlistId}
// ─────────────────────────────────────────────
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val productImageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// ─────────────────────────────────────────────
// Quote  →  /quotes/{quoteId}
// ─────────────────────────────────────────────
enum class QuoteStatus { Pending, Processing, Shipped, Delivered, Cancelled }

data class Quote(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val businessId: String = "",
    val productId: String = "",
    val productName: String = "",
    val buyerEmail: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val status: String = QuoteStatus.Pending.name,
    val trackingCarrier: String = "",
    val trackingNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// ─────────────────────────────────────────────
// Rating  →  /ratings/{ratingId}
// ─────────────────────────────────────────────
data class Rating(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val productId: String = "",
    val rating: Int = 0,        // 1–5
    val review: String = "",    // max 500 chars
    @ServerTimestamp val createdAt: Timestamp? = null
)
