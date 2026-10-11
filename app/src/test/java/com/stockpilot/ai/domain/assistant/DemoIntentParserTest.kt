package com.stockpilot.ai.domain.assistant

import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.domain.inventory.InventoryCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoIntentParserTest {
  private val blueShirt = ProductEntity(
        sku = "SKU-BT-001",
        name = "Blue T-shirt",
        category = "Apparel",
        stockQuantity = 24,
        reorderThreshold = 10,
        targetStock = 40,
        dailySalesEstimate = 4,
    )

    @Test
    fun parsesStockLookup() {
        val cmd = DemoIntentParser.parse("How many blue T-shirts do we have?", ConversationContext())
        assertTrue(cmd is InventoryCommand.GetStock)
        assertTrue((cmd as InventoryCommand.GetStock).productNameOrSku.contains("blue t-shirt"))
    }

    @Test
    fun parsesLowStock() {
        val cmd = DemoIntentParser.parse("What needs restocking?", ConversationContext())
        assertEquals(InventoryCommand.ListLowStock, cmd)
    }

    @Test
    fun parsesReorderWithContext() {
        val cmd = DemoIntentParser.parse(
            "If we sell 10 tomorrow, how many should I order to have 30 left?",
            ConversationContext(lastProduct = blueShirt),
        )
        assertTrue(cmd is InventoryCommand.CalculateReorder)
        val reorder = cmd as InventoryCommand.CalculateReorder
        assertEquals(10, reorder.expectedSales)
        assertEquals(30, reorder.targetStock)
        assertEquals("Blue T-shirt", reorder.productNameOrSku)
    }
}