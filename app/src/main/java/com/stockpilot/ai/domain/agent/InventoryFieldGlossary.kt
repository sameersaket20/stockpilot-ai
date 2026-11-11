package com.stockpilot.ai.domain.agent

import org.json.JSONObject

/** Plain labels so Gemma does not confuse on-hand qty vs target shelf vs availability status. */
object InventoryFieldGlossary {
    const val PRODUCT_LINE_HEADER =
        "sku|product_name|on_hand_qty|daily_sales_est|target_shelf_qty|suggested_reorder_qty|availability"

    fun asJson(): JSONObject = JSONObject()
        .put(
            "on_hand_qty",
            "Quantity on hand = units physically in the store RIGHT NOW (same as quantity on hand in the app UI). " +
                "Use for: how many, highest, most, total units per SKU, anything about current inventory.",
        )
        .put(
            "quantity_on_hand",
            "Staff wording for on_hand_qty — always the 3rd column in product_lines.",
        )
        .put(
            "target_shelf_qty",
            "Target stock in the app = desired level after replenishment (planning goal). " +
                "NOT how many units you have today; never use for highest/lowest quantity questions.",
        )
        .put("daily_sales_est", "Est. daily sales — units sold per day estimate for reorder math.")
        .put("suggested_reorder_qty", "Suggested units to order to move toward target_shelf_qty.")
        .put(
            "availability",
            "Per-SKU health label in product_lines: OK / LOW / OUT only. " +
                "OUT means on_hand_qty is 0. This is NOT a quantity and NOT 'items in stock' for counting.",
        )
        .put(
            "total_units",
            "Sum of on_hand_qty across all SKUs in the catalog (total physical units on hand).",
        )
        .put(
            "status_counts",
            "How many SKUs are labeled IN_STOCK / LOW_STOCK / OUT_OF_STOCK (counts of products, not unit quantities).",
        )
        .put(
            "in_stock_everyday_meaning",
            "When the user says 'in stock' or 'how much stock', they mean on_hand_qty unless they ask about low/out items.",
        )
        .put(
            "highest_quantity_on_hand",
            "Precomputed max(on_hand_qty) in the catalog — use for highest / most quantity questions.",
        )
        .put(
            "highest_quantity_product",
            "SKU with that max on_hand_qty (includes name, sku, on_hand_qty) — authoritative for 'top item' answers.",
        )
        .put(
            "zero_on_hand_count",
            "Number of SKUs with on_hand_qty exactly 0 (out of stock / zero quantity).",
        )
        .put(
            "zero_on_hand_product_lines",
            "Subset of product_lines: only SKUs with on_hand_qty 0. Use for out-of-stock and zero-quantity questions.",
        )
        .put(
            "out_of_stock_meaning",
            "Same as on_hand_qty = 0 / availability OUT — never describe LOW (qty > 0) as out of stock.",
        )

    fun availabilityShort(status: String): String = when (status) {
        "OUT_OF_STOCK" -> "OUT"
        "LOW_STOCK" -> "LOW"
        else -> "OK"
    }
}