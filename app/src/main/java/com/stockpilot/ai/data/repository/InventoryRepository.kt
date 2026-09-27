package com.stockpilot.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.stockpilot.ai.data.local.ProductDao
import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.local.StockStatus
import com.stockpilot.ai.data.seed.SampleInventory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

private val Context.inventoryPrefs: DataStore<Preferences> by preferencesDataStore("inventory_prefs")

data class InventorySummary(
    val totalUnits: Int,
    val productCount: Int,
    val lowStockProducts: Int,
)

enum class InventoryFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK,
}

class InventoryRepository(
    private val context: Context,
    private val productDao: ProductDao,
) : InventoryOperations {
    private val seededKey = booleanPreferencesKey("inventory_seeded")

    val summary: Flow<InventorySummary> =
        combine(
            productDao.observeTotalUnits(),
            productDao.observeProductCount(),
            productDao.observeLowStockCount(),
        ) { units, count, low ->
            InventorySummary(
                totalUnits = units,
                productCount = count,
                lowStockProducts = low,
            )
        }

    fun observeProducts(searchQuery: String): Flow<List<ProductEntity>> {
        val trimmed = searchQuery.trim()
        return if (trimmed.isEmpty()) {
            productDao.observeAll()
        } else {
            productDao.observeSearch(trimmed)
        }
    }

    suspend fun ensureSeeded() {
        val prefs = context.inventoryPrefs.data.first()
        if (prefs[seededKey] == true) return
        if (productDao.getAllOnce().isNotEmpty()) {
            markSeeded()
            return
        }
        productDao.insertAll(SampleInventory.products())
        markSeeded()
    }

    suspend fun resetSeedData() {
        productDao.replaceAll(SampleInventory.products())
        markSeeded()
    }

    private suspend fun markSeeded() {
        context.inventoryPrefs.edit { it[seededKey] = true }
    }

    suspend fun updateProduct(product: ProductEntity) {
        require(product.stockQuantity >= 0) { "stockQuantity must be non-negative" }
        require(product.reorderThreshold >= 0)
        require(product.targetStock >= 0)
        require(product.dailySalesEstimate >= 0)
        productDao.update(product.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun getAllProducts(): List<ProductEntity> = productDao.getAllOnce()

    override suspend fun findByNameOrSku(query: String): List<ProductEntity> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        productDao.getBySku(trimmed)?.let { return listOf(it) }
        val lower = trimmed.lowercase()
        val singular = lower.removeSuffix("s")
        val primary = productDao.searchByNameToken(lower)
        if (primary.isNotEmpty()) return primary
        if (singular != lower) {
            val alt = productDao.searchByNameToken(singular)
            if (alt.isNotEmpty()) return alt
        }
        return productDao.searchByNameToken(singular)
    }

    override suspend fun listLowStock(): List<ProductEntity> =
        productDao.getAllOnce().filter {
            it.stockStatus() == StockStatus.LOW_STOCK || it.stockStatus() == StockStatus.OUT_OF_STOCK
        }

    fun applyFilter(products: List<ProductEntity>, filter: InventoryFilter): List<ProductEntity> =
        when (filter) {
            InventoryFilter.ALL -> products
            InventoryFilter.LOW_STOCK ->
                products.filter { it.stockStatus() == StockStatus.LOW_STOCK }
            InventoryFilter.OUT_OF_STOCK ->
                products.filter { it.stockStatus() == StockStatus.OUT_OF_STOCK }
        }
}