package com.stockpilot.ai.domain.inventory

import com.stockpilot.ai.data.local.ProductEntity

sealed class InventoryResult {
    data class StockAnswer(
        val product: ProductEntity,
        val message: String,
    ) : InventoryResult()

    data class ProductDetails(
        val product: ProductEntity,
        val message: String,
    ) : InventoryResult()

    data class LowStockList(
        val products: List<ProductEntity>,
        val message: String,
    ) : InventoryResult()

    data class ReorderAnswer(
        val product: ProductEntity,
        val reorderQuantity: Int,
        val message: String,
    ) : InventoryResult()

    data class Clarification(val message: String) : InventoryResult()

    data class Error(val message: String) : InventoryResult()
}

object ReorderCalculator {
    /**
     * Target stock is the desired on-hand quantity after expected sales are accounted for.
     * reorder = max(0, targetStock - (currentStock - expectedSales))
     */
    fun calculate(currentStock: Int, expectedSales: Int, targetStock: Int): Int {
        require(currentStock >= 0 && expectedSales >= 0 && targetStock >= 0)
        val projected = currentStock - expectedSales
        val raw = targetStock - projected
        return maxOf(0, raw)
    }
}