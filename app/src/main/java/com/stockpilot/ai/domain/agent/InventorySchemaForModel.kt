package com.stockpilot.ai.domain.agent

/** Where inventory data lives and how to query it (planner prompt only). */
object InventorySchemaForModel {
    fun plannerCatalog(): String = buildString {
        appendLine("DATA_SOURCE: local Room table `products` (read-only). No SQL — use tool_calls only.")
        appendLine()
        appendLine("COLUMNS (maps to app UI):")
        appendLine("- sku, name, category")
        appendLine("- stock_quantity → on_hand_qty (quantity on hand RIGHT NOW)")
        appendLine("- target_stock → target_shelf_qty (planning goal, NOT current qty)")
        appendLine("- reorder_threshold, daily_sales_estimate")
        appendLine("- status: IN_STOCK | LOW_STOCK | OUT_OF_STOCK (derived from on_hand vs threshold)")
        appendLine()
        appendLine(InventoryFieldGlossary.asJson().toString(2))
        appendLine()
        appendLine("TOOLS (output JSON {\"tool_calls\":[...]} only):")
        appendLine("1. inventory_summary {} — totals, status_counts, highest_quantity_*, zero_on_hand_* (no full list)")
        appendLine(
            "2. query_products { \"filter\"?: {...}, \"sort\"?: {\"field\": string, \"order\": \"asc\"|\"desc\"}, " +
                "\"limit\"?: int max 30 }",
        )
        appendLine("   filter keys (combine as needed):")
        appendLine("   - on_hand_qty_eq, on_hand_qty_min, on_hand_qty_max (int)")
        appendLine("   - availability: \"OUT\" | \"LOW\" | \"OK\" (OUT = on_hand 0)")
        appendLine("   - name_or_sku_contains (string)")
        appendLine("   - category_eq (string)")
        appendLine("   sort fields: stock_quantity, name, sku, target_stock, daily_sales_estimate")
        appendLine("3. full_inventory_read {} — entire catalog (slow; only if user wants all SKUs)")
        appendLine("4. compute_reorder { sku_or_name, current_stock?, expected_sales?, target_stock? }")
        appendLine()
        appendLine("PLANNING RULES:")
        appendLine("- Pick the smallest query that answers the user (prefer query_products + inventory_summary).")
        appendLine("- Zero / out of stock → query_products filter on_hand_qty_eq=0 OR availability OUT.")
        appendLine("- Highest quantity → inventory_summary (has highest_quantity_*) or query_products sort stock_quantity desc limit 1.")
        appendLine("- Do NOT use target_stock when user means quantity on hand.")
    }.trim()
}