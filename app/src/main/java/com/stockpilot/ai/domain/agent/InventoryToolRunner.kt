package com.stockpilot.ai.domain.agent

import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.local.StockStatus
import com.stockpilot.ai.data.repository.InventoryOperations
import com.stockpilot.ai.domain.inventory.ReorderCalculator
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only inventory surface. Every turn auto-loads the full products table.
 * The only optional tool is trusted reorder arithmetic — not intent-specific shortcuts.
 */
@Singleton
class InventoryToolRunner @Inject constructor(
    private val inventory: InventoryOperations,
) {
    val accessPolicyDescription: String = """
        READ-ONLY access to the entire local inventory database (Room products table).
        Each turn automatically includes FULL_INVENTORY_READ (every product row + aggregates).
        You must NOT delete, update, insert, or alter data.
        Reorder math (when needed): max(0, target_stock - (current_stock - expected_sales)).
        Each row includes suggested_reorder_quantity (from daily_sales_estimate + target_stock). Use those unless the user gives different sales/target — then call compute_reorder for that SKU only.
    """.trimIndent()

    fun plannerSchema(): String = InventorySchemaForModel.plannerCatalog()

    /** Unconditional read of all rows (tool or fallback). */
    suspend fun readEntireInventory(): ToolInvocationResult = buildFullInventoryRead()

    suspend fun invoke(tool: String, args: JSONObject): ToolInvocationResult {
        val normalized = tool.trim().lowercase().replace('-', '_')
        return try {
            when (normalized) {
                "read_entire_inventory", "read_everything", "full_inventory_read" -> buildFullInventoryRead()
                "inventory_summary" -> buildInventorySummary()
                "query_products" -> buildQueryProducts(args)
                "compute_reorder" -> computeReorder(args)
                else -> ToolInvocationResult(
                    ok = false,
                    error = "Unknown tool '$tool'. See schema: inventory_summary, query_products, " +
                        "full_inventory_read, compute_reorder.",
                )
            }
        } catch (e: Exception) {
            ToolInvocationResult(ok = false, error = e.message ?: "Tool execution failed")
        }
    }

    private suspend fun buildFullInventoryRead(): ToolInvocationResult {
        val all = inventory.getAllProducts().sortedBy { it.name.lowercase() }
        val facts = aggregateFacts(all)
            .put("read_scope", "unrestricted_read_products_table")
            .put("products", productsArray(all))
        return ToolInvocationResult(ok = true, facts = facts, products = all)
    }

    private suspend fun buildInventorySummary(): ToolInvocationResult {
        val all = inventory.getAllProducts()
        val facts = aggregateFacts(all)
            .put("read_scope", "inventory_summary")
        return ToolInvocationResult(ok = true, facts = facts, products = emptyList())
    }

    private suspend fun buildQueryProducts(args: JSONObject): ToolInvocationResult {
        val all = inventory.getAllProducts()
        val matched = InventoryProductQuery.apply(all, args)
        val facts = JSONObject()
            .put("read_scope", "query_products")
            .put("query_arguments", args)
            .put("matched_count", matched.size)
            .put("catalog_product_count", all.size)
            .put("product_line_header", InventoryFieldGlossary.PRODUCT_LINE_HEADER)
            .put("field_glossary", InventoryFieldGlossary.asJson())
            .put("product_lines", compactProductLines(matched))
            .put("products", productsArray(matched))
        return ToolInvocationResult(ok = true, facts = facts, products = matched)
    }

    private fun aggregateFacts(all: List<ProductEntity>): JSONObject {
        val byStatus = StockStatus.entries.associateWith { status ->
            all.count { it.stockStatus() == status }
        }
        val highestQty = all.maxOfOrNull { it.stockQuantity } ?: 0
        val highestProducts = all.filter { it.stockQuantity == highestQty }
        val highest = highestProducts.minByOrNull { it.name.lowercase() }
        val zeroOnHand = all.filter { it.stockQuantity <= 0 }
        return JSONObject()
            .put("product_count", all.size)
            .put("total_units", all.sumOf { it.stockQuantity })
            .put("highest_quantity_on_hand", highestQty)
            .apply {
                highest?.let { top ->
                    put(
                        "highest_quantity_product",
                        JSONObject()
                            .put("sku", top.sku)
                            .put("name", top.name)
                            .put("on_hand_qty", top.stockQuantity)
                            .put("tied_sku_count", highestProducts.size),
                    )
                }
            }
            .put("zero_on_hand_count", zeroOnHand.size)
            .put("zero_on_hand_product_lines", compactProductLines(zeroOnHand))
            .put("categories", JSONArray(all.map { it.category }.distinct().sorted()))
            .put(
                "status_counts",
                JSONObject()
                    .put("IN_STOCK", byStatus[StockStatus.IN_STOCK] ?: 0)
                    .put("LOW_STOCK", byStatus[StockStatus.LOW_STOCK] ?: 0)
                    .put("OUT_OF_STOCK", byStatus[StockStatus.OUT_OF_STOCK] ?: 0),
            )
            .put("product_line_header", InventoryFieldGlossary.PRODUCT_LINE_HEADER)
            .put("field_glossary", InventoryFieldGlossary.asJson())
    }

    private suspend fun computeReorder(args: JSONObject): ToolInvocationResult {
        val key = args.optString("sku_or_name", args.optString("product", "")).trim()
        val expectedArg = if (args.has("expected_sales")) args.optInt("expected_sales") else null
        val targetArg = if (args.has("target_stock")) args.optInt("target_stock") else null
        val explicitStock = if (args.has("current_stock")) args.optInt("current_stock") else null

        val product: ProductEntity? = if (key.isNotEmpty()) {
            val matches = inventory.findByNameOrSku(key)
            when {
                matches.isEmpty() -> null
                matches.size == 1 -> matches.single()
                else -> return ToolInvocationResult(
                    ok = true,
                    facts = JSONObject().put("product_resolved", false).put("match_count", matches.size),
                    products = matches,
                )
            }
        } else {
            null
        }

        val current = explicitStock ?: product?.stockQuantity
        val expected = expectedArg ?: product?.dailySalesEstimate
        val target = targetArg ?: product?.targetStock

        if (current == null || current < 0 || expected == null || expected < 0 || target == null || target < 0) {
            return ToolInvocationResult(
                ok = false,
                error = "compute_reorder needs current_stock, expected_sales, target_stock " +
                    "(via sku_or_name row or explicit integers)",
            )
        }

        val qty = ReorderCalculator.calculate(current, expected, target)
        val facts = JSONObject()
            .put("reorder_quantity", qty)
            .put("expected_sales", expected)
            .put("target_stock", target)
            .put("current_stock", current)
        product?.let { facts.put("product", productJson(it)) }
        if (key.isNotEmpty()) facts.put("sku_or_name", key)
        return ToolInvocationResult(
            ok = true,
            facts = facts,
            products = product?.let { listOf(it) } ?: emptyList(),
        )
    }

    private fun compactProductLines(products: List<ProductEntity>): String {
        if (products.isEmpty()) return ""
        return buildString {
            appendLine(InventoryFieldGlossary.PRODUCT_LINE_HEADER)
            products.forEach { p ->
                append(p.sku)
                append('|')
                append(p.name)
                append('|')
                append(p.stockQuantity)
                append('|')
                append(p.dailySalesEstimate)
                append('|')
                append(p.targetStock)
                append('|')
                append(
                    ReorderCalculator.calculate(
                        p.stockQuantity,
                        p.dailySalesEstimate,
                        p.targetStock,
                    ),
                )
                append('|')
                append(InventoryFieldGlossary.availabilityShort(p.stockStatus().name))
                append('\n')
            }
        }.trim()
    }

    private fun productsArray(products: List<ProductEntity>): JSONArray {
        val arr = JSONArray()
        products.forEach { arr.put(productJson(it)) }
        return arr
    }

    private fun productJson(p: ProductEntity): JSONObject {
        val suggested = ReorderCalculator.calculate(
            p.stockQuantity,
            p.dailySalesEstimate,
            p.targetStock,
        )
        return JSONObject()
            .put("id", p.id)
            .put("sku", p.sku)
            .put("name", p.name)
            .put("category", p.category)
            .put("stock_quantity", p.stockQuantity)
            .put("reorder_threshold", p.reorderThreshold)
            .put("target_stock", p.targetStock)
            .put("daily_sales_estimate", p.dailySalesEstimate)
            .put("suggested_reorder_quantity", suggested)
            .put("status", p.stockStatus().name)
            .put("updated_at", p.updatedAt)
    }
}

data class ToolInvocationResult(
    val ok: Boolean,
    val facts: JSONObject = JSONObject(),
    val error: String? = null,
    val products: List<ProductEntity> = emptyList(),
)