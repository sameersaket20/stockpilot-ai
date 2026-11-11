package com.stockpilot.ai.domain.agent

import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.repository.InventoryOperations
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class InventoryToolRunnerTest {
    @Test
    fun readEntireInventory_includesAllRows() = runBlocking {
        val runner = InventoryToolRunner(FakeInventory())
        val result = runner.readEntireInventory()
        assertTrue(result.ok)
        assertEquals(1, result.facts.getInt("product_count"))
        assertEquals(5, result.facts.getInt("total_units"))
        assertEquals("unrestricted_read_products_table", result.facts.getString("read_scope"))
        assertTrue(result.facts.has("field_glossary"))
        assertTrue(result.facts.getJSONObject("field_glossary").has("on_hand_qty"))
        assertEquals(InventoryFieldGlossary.PRODUCT_LINE_HEADER, result.facts.getString("product_line_header"))
        val bundle = InventoryFactsCompressor.factsBundleForModel(
            JSONArray().put(
                JSONObject()
                    .put("tool", "full_inventory_read")
                    .put("ok", true)
                    .put("facts", result.facts),
            ),
            null,
        )
        assertTrue(bundle.contains("field_glossary"))
        assertTrue(bundle.contains("on_hand_qty"))
        assertTrue(bundle.contains(InventoryFieldGlossary.PRODUCT_LINE_HEADER))
        assertTrue(result.facts.has("zero_on_hand_count"))
        assertTrue(result.facts.has("zero_on_hand_product_lines"))
    }

    @Test
    fun computeReorder_defaultsFromSkuRow() = runBlocking {
        val runner = InventoryToolRunner(FakeInventory())
        val result = runner.invoke("compute_reorder", JSONObject().put("sku_or_name", "TEE-BLU-M"))
        assertTrue(result.ok)
        assertEquals(35, result.facts.getInt("reorder_quantity"))
    }

    private class FakeInventory : InventoryOperations {
        private val products = listOf(
            ProductEntity(
                id = 1,
                sku = "TEE-BLU-M",
                name = "Blue T-shirt",
                category = "Tops",
                stockQuantity = 5,
                reorderThreshold = 10,
                targetStock = 30,
                dailySalesEstimate = 10,
            ),
        )

        override suspend fun findByNameOrSku(query: String) = products

        override suspend fun listLowStock() = products

        override suspend fun getAllProducts() = products
    }
}