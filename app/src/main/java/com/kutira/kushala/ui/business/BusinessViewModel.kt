package com.kutira.kushala.ui.business

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutira.kushala.data.model.Business
import com.kutira.kushala.data.model.Product
import com.kutira.kushala.data.model.ProductWithBusiness
import com.kutira.kushala.data.repository.FirestoreRepository
import com.kutira.kushala.data.repository.GeminiRepository
import com.kutira.kushala.data.repository.StorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BusinessUiState(
    val businesses: List<Business> = emptyList(),
    val selectedBusiness: Business? = null,
    val products: List<Product> = emptyList(),
    val allProducts: List<ProductWithBusiness> = emptyList(),
    val isListLoading: Boolean = false,
    val isDetailLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val aiDescription: String = ""
)

class BusinessViewModel(
    private val firestoreRepo: FirestoreRepository = FirestoreRepository(),
    private val storageRepo: StorageRepository = StorageRepository(),
    private val geminiRepo: GeminiRepository = GeminiRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessUiState())
    val uiState: StateFlow<BusinessUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private var businessesJob: Job? = null
    private var detailJob: Job? = null
    private var productsJob: Job? = null
    private var allProductsJob: Job? = null
    private var currentProductsBusinessId: String? = null

    fun loadBusinesses(category: String? = null) {
        allProductsJob?.cancel()
        businessesJob?.cancel()

        businessesJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isListLoading = true, error = null, saveSuccess = false)
            }

            firestoreRepo.getBusinessesFlow(category)
                .catch { e ->
                    if (e is CancellationException) throw e
                    android.util.Log.e("BusinessViewModel", "Error loading businesses", e)
                    _uiState.update {
                        it.copy(error = e.message ?: "Unable to load businesses.", isListLoading = false)
                    }
                }
                .collect { list ->
                    val sorted = list.sortedWith(
                        compareBy<Business> { it.name.isBlank() }
                            .thenBy { it.name.lowercase() }
                    )
                    android.util.Log.d(
                        "BusinessViewModel",
                        "Loaded ${sorted.size} businesses for category: $category"
                    )
                    _uiState.update {
                        it.copy(businesses = sorted, isListLoading = false, error = null)
                    }
                }
        }
    }

    fun loadBusiness(businessId: String) {
        if (detailJob?.isActive == true && _uiState.value.selectedBusiness?.id == businessId) return

        detailJob?.cancel()
        if (_uiState.value.selectedBusiness?.id != businessId) {
            _uiState.update {
                it.copy(
                    selectedBusiness = null,
                    products = emptyList(),
                    isDetailLoading = true,
                    error = null,
                    saveSuccess = false
                )
            }
        }

        detailJob = viewModelScope.launch {
            loadProducts(businessId)

            firestoreRepo.getBusinessByIdFlow(businessId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    android.util.Log.e("BusinessViewModel", "Error loading business detail", e)
                    _uiState.update {
                        it.copy(error = e.message ?: "Unable to load business.", isDetailLoading = false)
                    }
                }
                .collect { business ->
                    android.util.Log.d(
                        "BusinessViewModel",
                        "Business updated: ${business?.name}, accepting=${business?.isAcceptingOrders}"
                    )
                    _uiState.update {
                        it.copy(selectedBusiness = business, isDetailLoading = false, error = null)
                    }
                }
        }
    }

    private fun loadProducts(businessId: String) {
        if (currentProductsBusinessId == businessId && productsJob?.isActive == true) return

        currentProductsBusinessId = businessId
        productsJob?.cancel()

        productsJob = viewModelScope.launch {
            firestoreRepo.getProductsFlow(businessId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    android.util.Log.e("BusinessViewModel", "Error loading products", e)
                    _uiState.update { it.copy(error = e.message ?: "Unable to load products.") }
                }
                .collect { list ->
                    _uiState.update { it.copy(products = list, error = null) }
                }
        }
    }

    fun loadAllProducts(category: String? = null) {
        businessesJob?.cancel()
        allProductsJob?.cancel()

        allProductsJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isListLoading = true, error = null, saveSuccess = false)
            }

            firestoreRepo.getAllProductsFlow()
                .combine(firestoreRepo.getBusinessesFlow(null)) { products, businesses ->
                    val businessesById = businesses.associateBy { it.id }

                    products.mapNotNull { productWithBusiness ->
                        val business = businessesById[productWithBusiness.businessId] ?: return@mapNotNull null

                        productWithBusiness.copy(
                            businessName = business.name,
                            businessLocation = business.location,
                            businessCategory = business.category,
                            isAcceptingOrders = business.isAcceptingOrders
                        )
                    }.filter { productWithBusiness ->
                        category.isNullOrBlank() ||
                                productWithBusiness.businessCategory.equals(category, ignoreCase = true)
                    }
                }
                .catch { e ->
                    if (e is CancellationException) throw e
                    android.util.Log.e("BusinessViewModel", "Error loading catalogue products", e)
                    _uiState.update {
                        it.copy(error = e.message ?: "Unable to load products.", isListLoading = false)
                    }
                }
                .collect { enriched ->
                    _uiState.update {
                        it.copy(allProducts = enriched, isListLoading = false, error = null)
                    }
                }
        }
    }

    fun createBusiness(business: Business, imageUri: Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, saveSuccess = false) }

            try {
                val businessId = firestoreRepo.createBusiness(business)

                if (imageUri != null) {
                    val imageUrl = storageRepo.uploadBusinessImage(businessId, imageUri)
                    firestoreRepo.updateBusiness(businessId, business.copy(imageUrl = imageUrl))
                }

                _uiState.update { it.copy(isActionLoading = false, saveSuccess = true, error = null) }
                _navigationEvent.emit(Unit)
            } catch (e: Exception) {
                android.util.Log.e("BusinessViewModel", "Error creating business", e)
                _uiState.update {
                    it.copy(error = e.message ?: "Unable to save business.", isActionLoading = false)
                }
            }
        }
    }

    fun updateBusiness(
        businessId: String,
        business: Business,
        imageUri: Uri? = null,
        isSilent: Boolean = false
    ) {
        viewModelScope.launch {
            if (!isSilent) {
                _uiState.update { it.copy(isActionLoading = true, error = null, saveSuccess = false) }
            }

            try {
                val finalBusiness = if (imageUri != null) {
                    val imageUrl = storageRepo.uploadBusinessImage(businessId, imageUri)
                    business.copy(imageUrl = imageUrl)
                } else {
                    business
                }

                firestoreRepo.updateBusiness(businessId, finalBusiness)

                if (!isSilent) {
                    _uiState.update { it.copy(isActionLoading = false, saveSuccess = true, error = null) }
                    _navigationEvent.emit(Unit)
                }
            } catch (e: Exception) {
                android.util.Log.e("BusinessViewModel", "Error updating business", e)
                _uiState.update {
                    it.copy(error = e.message ?: "Unable to update business.", isActionLoading = false)
                }
            }
        }
    }

    fun deleteBusiness(businessId: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.deleteBusiness(businessId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unable to delete business.") }
            }
        }
    }

    fun createProduct(businessId: String, product: Product, imageUri: Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, saveSuccess = false) }

            try {
                val productId = firestoreRepo.createProduct(businessId, product)
                val imageUrl = imageUri?.let {
                    storageRepo.uploadProductImage(businessId, productId, it)
                }.orEmpty()

                if (imageUrl.isNotBlank()) {
                    firestoreRepo.updateProduct(businessId, productId, product.copy(imageUrl = imageUrl))
                }

                _uiState.update { it.copy(isActionLoading = false, saveSuccess = true, error = null) }
                _navigationEvent.emit(Unit)
            } catch (e: Exception) {
                android.util.Log.e("BusinessViewModel", "Error creating product", e)
                _uiState.update {
                    it.copy(error = e.message ?: "Unable to save product.", isActionLoading = false)
                }
            }
        }
    }

    fun deleteProduct(businessId: String, productId: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.deleteProduct(businessId, productId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unable to delete product.") }
            }
        }
    }

    fun generateProductDescription(productName: String, category: String, keywords: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(aiDescription = "") }

            geminiRepo.generateProductDescription(productName, category, keywords)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _uiState.update { it.copy(error = e.message ?: "Unable to generate description.") }
                }
                .collect { chunk ->
                    _uiState.update { it.copy(aiDescription = it.aiDescription + chunk) }
                }
        }
    }

    fun resetActionState() {
        _uiState.update {
            it.copy(
                isActionLoading = false,
                saveSuccess = false,
                error = null,
                aiDescription = ""
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
