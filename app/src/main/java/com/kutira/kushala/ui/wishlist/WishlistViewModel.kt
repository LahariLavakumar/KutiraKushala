package com.kutira.kushala.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kutira.kushala.data.model.Business
import com.kutira.kushala.data.model.Product
import com.kutira.kushala.data.model.WishlistItem
import com.kutira.kushala.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val repo: FirestoreRepository = FirestoreRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _items = MutableStateFlow<List<WishlistItem>>(emptyList())
    val items: StateFlow<List<WishlistItem>> = _items.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadWishlist() }

    private fun loadWishlist() {
        viewModelScope.launch {
            try {
                repo.getWishlistFlow().collect { _items.value = it }
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun addToWishlist(business: Business, product: Product) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                repo.addToWishlist(
                    WishlistItem(
                        userId = uid,
                        businessId = business.id,
                        productId = product.id,
                        productName = product.name,
                        productPrice = product.wholesalePrice,
                        productImageUrl = product.imageUrl
                    )
                )
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun removeFromWishlist(wishlistId: String) {
        viewModelScope.launch {
            try { repo.removeFromWishlist(wishlistId) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun isInWishlist(productId: String) = _items.value.any { it.productId == productId }
}
