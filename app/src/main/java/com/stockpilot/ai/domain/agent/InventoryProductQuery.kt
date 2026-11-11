package com.stockpilot.ai.domain.agent

import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.local.StockStatus
import org.json.JSONObject

data class ProductQueryFilter(
    val onHandQtyEq: Int? = null,
    val onHandQtyMin: Int? = null,
    val onHandQtyMax: Int? = null,
    val availability: String? = null,
    val nameOrSkuContains: String? = null,
    val categoryEq: String? = null,
) {
    companion object {
        fun fromJson(filter: JSONObject?): ProductQueryFilter {
            if (filter == null) return ProductQueryFilter()
            return ProductQueryFilter(
                onHandQtyEq = if (filter.has("on_hand_qty_eq")) filter.optInt("on_hand_qty_eq") else null,
                onHandQtyMin = if (filter.has("on_hand_qty_min")) filter.optInt("on_hand_qty_min") else null,
                onHandQtyMax = if (filter.has("on_hand_qty_max")) filter.optInt("on_hand_qty_max") else null,
                availability = filter.optString("availability").takeIf { it.isNotBlank() },
                nameOrSkuContains = filter.optString("name_or_sku_contains").takeIf { it.isNotBlank() },
                categoryEq = filter.optString("category_eq").takeIf { it.isNotBlank() },
            )
        }
    }
}

data class ProductQuerySort(
    val field: String,
    val ascending: Boolean,
) {
    companion object {
        fun fromJson(sort: JSONObject?): ProductQuerySort? {
            sort ?: return null
            val field = sort.optString("field", "name")
            val order = sort.optString("order", "asc")
            return ProductQuerySort(field = field, ascending = !order.equals("desc", ignoreCase = true))
        }
    }
}

object InventoryProductQuery {
    private const val MAX_LIMIT = 30

    fun apply(
        products: List<ProductEntity>,
        args: JSONObject,
    ): List<ProductEntity> {
        val filter = ProductQueryFilter.fromJson(args.optJSONObject("filter"))
        val sort = ProductQuerySort.fromJson(args.optJSONObject("sort"))
        val limit = args.optInt("limit", MAX_LIMIT).coerceIn(1, MAX_LIMIT)

        var rows = products
        filter.onHandQtyEq?.let { eq -> rows = rows.filter { it.stockQuantity == eq } }
        filter.onHandQtyMin?.let { min -> rows = rows.filter { it.stockQuantity >= min } }
        filter.onHandQtyMax?.let { max -> rows = rows.filter { it.stockQuantity <= max } }
        filter.nameOrSkuContains?.let { q ->
            rows = rows.filter {
                it.name.contains(q, ignoreCase = true) || it.sku.contains(q, ignoreCase = true)
            }
        }
        filter.categoryEq?.let { cat -> rows = rows.filter { it.category.equals(cat, ignoreCase = true) } }
        filter.availability?.let { av ->
            rows = rows.filter { product -> availabilityMatches(product, av) }
        }

        val field = sort?.field?.lowercase()?.replace('-', '_') ?: "name"
        val asc = sort?.ascending ?: true
        val ordered = when (field) {
            "stock_quantity", "on_hand_qty" ->
                if (asc) rows.sortedBy { it.stockQuantity } else rows.sortedByDescending { it.stockQuantity }
            "target_stock", "target_shelf_qty" ->
                if (asc) rows.sortedBy { it.targetStock } else rows.sortedByDescending { it.targetStock }
            "daily_sales_estimate", "daily_sales_est" ->
                if (asc) rows.sortedBy { it.dailySalesEstimate } else rows.sortedByDescending { it.dailySalesEstimate }
            "sku" ->
                if (asc) rows.sortedBy { it.sku.lowercase() } else rows.sortedByDescending { it.sku.lowercase() }
            else ->
                if (asc) rows.sortedBy { it.name.lowercase() } else rows.sortedByDescending { it.name.lowercase() }
        }
        return ordered.take(limit)
    }

    fun availabilityMatches(product: ProductEntity, token: String): Boolean {
        val short = InventoryFieldGlossary.availabilityShort(product.stockStatus().name)
        return short.equals(token, ignoreCase = true) ||
            (token.equals("OUT", true) && product.stockStatus() == StockStatus.OUT_OF_STOCK)
    }
}