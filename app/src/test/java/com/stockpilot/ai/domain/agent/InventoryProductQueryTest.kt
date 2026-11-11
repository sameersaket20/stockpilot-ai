package com.stockpilot.ai.domain.agent

import com.stockpilot.ai.data.local.ProductEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryProductQueryTest {
    @Test
    fun filterZeroOnHand() {
        val products = listOf(
            entity("A", qty = 0),
            entity("B", qty = 9),
            entity("C", qty = 0),
        )
        val args = JSONObject().put(
            "filter",
            JSONObject().put("on_hand_qty_eq", 0),
        )
        val out = InventoryProductQuery.apply(products, args)
        assertEquals(2, out.size)
        assertEquals(setOf("A", "C"), out.map { it.sku }.toSet())
    }

    @Test
    fun sortStockDescLimit() {
        val products = listOf(
            entity("A", qty = 1),
            entity("B", qty = 50),
            entity("C", qty = 20),
        )
        val args = JSONObject()
            .put("sort", JSONObject().put("field", "on_hand_qty").put("order", "desc"))
            .put("limit", 1)
        val out = InventoryProductQuery.apply(products, args)
        assertEquals(1, out.size)
        assertEquals("B", out.single().sku)
    }

    private fun entity(sku: String, qty: Int) = ProductEntity(
        sku = sku,
        name = sku,
        category = "Test",
        stockQuantity = qty,
        reorderThreshold = 5,
        targetStock = 30,
        dailySalesEstimate = 2,
    )
}