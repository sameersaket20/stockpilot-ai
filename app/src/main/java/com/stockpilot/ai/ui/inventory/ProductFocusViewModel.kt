package com.stockpilot.ai.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductFocusUiState(
    val product: ProductEntity? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProductFocusViewModel @Inject constructor(
    private val repository: InventoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProductFocusUiState())
    val state: StateFlow<ProductFocusUiState> = _state.asStateFlow()

    fun load(productId: Long) {
        if (productId <= 0L) {
            _state.value = ProductFocusUiState(error = "Invalid product")
            return
        }
        viewModelScope.launch {
            val product = repository.getProductById(productId)
            _state.value = if (product == null) {
                ProductFocusUiState(error = "Product not found")
            } else {
                ProductFocusUiState(product = product)
            }
        }
    }

    fun save(product: ProductEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                repository.updateProduct(product)
                onSuccess()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Failed to save")
            }
        }
    }
}