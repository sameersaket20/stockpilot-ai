package com.stockpilot.ai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"], unique = true)],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val name: String,
    val category: String,
    val stockQuantity: Int,
    val reorderThreshold: Int,
    val targetStock: Int,
    val dailySalesEstimate: Int,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun stockStatus(): StockStatus = when {
        stockQuantity <= 0 -> StockStatus.OUT_OF_STOCK
        stockQuantity <= reorderThreshold -> StockStatus.LOW_STOCK
        else -> StockStatus.IN_STOCK
    }
}

enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
}