package com.stockpilot.ai.domain.agent

import org.json.JSONArray
import org.json.JSONObject

/** Shrinks inventory JSON for on-device Gemma prompts (CPU latency). */
object InventoryFactsCompressor {
    fun compactProductLines(facts: JSONObject): String = buildString {
        val products = facts.optJSONArray("products") ?: return@buildString
        for (i in 0 until products.length()) {
            val p = products.optJSONObject(i) ?: continue
            append(p.optString("sku"))
            append('|')
            append(p.optString("name"))
            append('|')
            append(p.optInt("stock_quantity"))
            append('|')
            append(p.optInt("daily_sales_estimate"))
            append('|')
            append(p.optInt("target_stock"))
            append('|')
            append(p.optInt("suggested_reorder_quantity"))
            append('|')
            append(InventoryFieldGlossary.availabilityShort(p.optString("status")))
            append('\n')
        }
    }.trim()

    fun plannerSnapshot(facts: JSONObject): String = buildString {
        appendLine("product_count=${facts.optInt("product_count")}")
        appendLine("total_units=${facts.optInt("total_units")}")
        appendLine("status_counts=${facts.optJSONObject("status_counts")}")
        appendLine("See field_glossary in facts. Row header:")
        appendLine(InventoryFieldGlossary.PRODUCT_LINE_HEADER)
        append(compactProductLines(facts))
    }.trim()

    /** Full rows for narrator/safety, products as compact lines instead of nested JSON objects. */
    fun factsBundleForModel(
        toolResults: JSONArray,
        narrationProductFilter: ((org.json.JSONObject) -> Boolean)? = null,
    ): String {
        val out = JSONArray()
        for (i in 0 until toolResults.length()) {
            val row = toolResults.optJSONObject(i) ?: continue
            val compressed = JSONObject()
                .put("tool", row.optString("tool"))
                .put("ok", row.optBoolean("ok"))
            if (row.has("error")) compressed.put("error", row.optString("error"))
            val facts = row.optJSONObject("facts")
            if (facts != null) {
                val factsOut = JSONObject(facts.toString())
                if (factsOut.has("products")) {
                    val products = facts.optJSONArray("products") ?: JSONArray()
                    val filteredLines = if (narrationProductFilter != null) {
                        buildString {
                            for (i in 0 until products.length()) {
                                val p = products.optJSONObject(i) ?: continue
                                if (!narrationProductFilter(p)) continue
                                appendLine(
                                    "${p.optString("sku")}|${p.optString("name")}|${p.optInt("stock_quantity")}|" +
                                        "${p.optInt("daily_sales_estimate")}|${p.optInt("target_stock")}|" +
                                        "${p.optInt("suggested_reorder_quantity")}|" +
                                        InventoryFieldGlossary.availabilityShort(p.optString("status")),
                                )
                            }
                        }.trim()
                    } else {
                        buildString {
                            appendLine(InventoryFieldGlossary.PRODUCT_LINE_HEADER)
                            append(compactProductLines(facts))
                        }.trim()
                    }
                    factsOut.remove("products")
                    val linesWithHeader = buildString {
                        appendLine(InventoryFieldGlossary.PRODUCT_LINE_HEADER)
                        append(filteredLines)
                    }.trim()
                    factsOut.put("product_lines", linesWithHeader)
                    factsOut.put("product_lines_count", filteredLines.lines().count { it.isNotBlank() })
                }
                compressed.put("facts", factsOut)
            }
            out.put(compressed)
        }
        return JSONObject().put("tool_results", out).toString()
    }
}