package com.stockpilot.ai.domain.inventory

import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.repository.InventoryOperations
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryCommandExecutorTest {
    private val blueShirt = ProductEntity(
        id = 1,
        sku = "SKU-BT-001",
        name = "Blue T-shirt",
        category = "Apparel",
        stockQuantity = 24,
        reorderThreshold = 10,
        targetStock = 40,
        dailySalesEstimate = 4,
    )

    @Test
    fun stockLookupUsesLiveQuantity() = runTest {
        val executor = InventoryCommandExecutor(FakeInventoryOperations(listOf(blueShirt)))
        val result = executor.execute(InventoryCommand.GetStock("blue t-shirt"))
        assertTrue(result is InventoryResult.StockAnswer)
        assertEquals(24, (result as InventoryResult.StockAnswer).product.stockQuantity)
    }

    @Test
    fun reorderMathUsesLiveStock() = runTest {
        val executor = InventoryCommandExecutor(FakeInventoryOperations(listOf(blueShirt)))
        val result = executor.execute(
            InventoryCommand.CalculateReorder("Blue T-shirt", expectedSales = 10, targetStock = 30),
        )
        assertTrue(result is InventoryResult.ReorderAnswer)
        assertEquals(16, (result as InventoryResult.ReorderAnswer).reorderQuantity)
    }

    @Test
    fun unknownProductClarifies() = runTest {
        val executor = InventoryCommandExecutor(FakeInventoryOperations(listOf(blueShirt)))
        val result = executor.execute(InventoryCommand.GetStock("purple socks"))
        assertTrue(result is InventoryResult.Clarification)
    }
}

private class FakeInventoryOperations(
    private val products: List<ProductEntity>,
) : InventoryOperations {
    override suspend fun findByNameOrSku(query: String): List<ProductEntity> =
        products.filter {
            it.name.contains(query, ignoreCase = true) || it.sku.equals(query, ignoreCase = true)
        }

    override suspend fun listLowStock(): List<ProductEntity> =
        products.filter { it.stockQuantity <= it.reorderThreshold }
}