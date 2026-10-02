package com.stockpilot.ai.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.repository.InventoryFilter
import com.stockpilot.ai.data.repository.InventoryRepository
import com.stockpilot.ai.data.repository.InventorySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryUiState(
    val summary: InventorySummary = InventorySummary(0, 0, 0),
    val products: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val filter: InventoryFilter = InventoryFilter.ALL,
    val editingProduct: ProductEntity? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val repository: InventoryRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val filter = MutableStateFlow(InventoryFilter.ALL)
    private val editingProduct = MutableStateFlow<ProductEntity?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)

    val screenState: StateFlow<InventoryUiState> = combine(
        combine(
            repository.summary,
            searchQuery,
            filter,
            editingProduct,
            errorMessage,
        ) { summary, query, filterValue, editing, err ->
            InventoryUiState(
                summary = summary,
                products = emptyList(),
                searchQuery = query,
                filter = filterValue,
                editingProduct = editing,
                errorMessage = err,
                isSaving = false,
            )
        },
        isSaving,
        searchQuery.flatMapLatest { repository.observeProducts(it) },
    ) { partial, saving, products ->
        partial.copy(
            products = repository.applyFilter(products, partial.filter),
            isSaving = saving,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun onFilterChange(value: InventoryFilter) {
        filter.value = value
    }

    fun openEdit(product: ProductEntity) {
        editingProduct.value = product
        errorMessage.value = null
    }

    fun dismissEdit() {
        editingProduct.value = null
        errorMessage.value = null
    }

    fun saveProduct(updated: ProductEntity) {
        viewModelScope.launch {
            isSaving.value = true
            errorMessage.value = null
            try {
                repository.updateProduct(updated)
                editingProduct.value = null
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Failed to save product"
            } finally {
                isSaving.value = false
            }
        }
    }
}