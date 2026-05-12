package com.kutira.kushala.ui.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.kutira.kushala.data.model.Quote
import com.kutira.kushala.data.model.QuoteStatus
import com.kutira.kushala.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QuoteViewModel(
    private val repo: FirestoreRepository = FirestoreRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _buyerQuotes = MutableStateFlow<List<Quote>>(emptyList())
    val buyerQuotes: StateFlow<List<Quote>> = _buyerQuotes.asStateFlow()

    private val _sellerQuotes = MutableStateFlow<List<Quote>>(emptyList())
    val sellerQuotes: StateFlow<List<Quote>> = _sellerQuotes.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadBuyerQuotes() {
        viewModelScope.launch {
            try { repo.getBuyerQuotesFlow().collect { _buyerQuotes.value = it } }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun loadSellerQuotes() {
        viewModelScope.launch {
            try { repo.getSellerQuotesFlow().collect { _sellerQuotes.value = it } }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun placeQuote(quote: Quote) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.createQuote(quote.copy(buyerEmail = auth.currentUser?.email ?: ""))
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    /** Seller: update status and optionally tracking info. */
    fun updateOrderStatus(quoteId: String, status: QuoteStatus, carrier: String = "", tracking: String = "") {
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (carrier.isNotBlank()) updates["trackingCarrier"] = carrier
                if (tracking.isNotBlank()) updates["trackingNumber"] = tracking
                repo.updateQuote(quoteId, updates)
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    /** Buyer: cancel an order. */
    fun cancelOrder(quoteId: String) {
        viewModelScope.launch {
            try {
                repo.updateQuote(quoteId, mapOf(
                    "status" to QuoteStatus.Cancelled.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            } catch (e: Exception) { _error.value = e.message }
        }
    }
}
