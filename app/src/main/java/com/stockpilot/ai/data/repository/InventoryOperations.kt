package com.stockpilot.ai.data.repository

import com.stockpilot.ai.data.local.ProductEntity

interface InventoryOperations {
    suspend fun findByNameOrSku(query: String): List<ProductEntity>
    suspend fun listLowStock(): List<ProductEntity>
}