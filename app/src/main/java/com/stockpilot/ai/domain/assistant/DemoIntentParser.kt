package com.stockpilot.ai.domain.assistant

import com.stockpilot.ai.domain.inventory.InventoryCommand

/**
 * Phase 1 **demo-only** intent parser for deterministic tests and UX.
 * Not Gemma inference — replace or bypass in Phase 2 when a real [com.stockpilot.ai.inference.InferenceEngine] is ready.
 */
object DemoIntentParser {
    fun parse(request: String, context: ConversationContext): InventoryCommand {
        val text = request.trim().lowercase()
        if (text.isEmpty()) return InventoryCommand.Unsupported

        if (lowStockPatterns.any { text.contains(it) }) {
            return InventoryCommand.ListLowStock
        }

        val reorder = tryParseReorder(text, context)
        if (reorder != null) return reorder

        val stockQuery = tryParseStockLookup(text)
        if (stockQuery != null) return InventoryCommand.GetStock(stockQuery)

        return InventoryCommand.Unsupported
    }

    private val lowStockPatterns = listOf(
        "low stock",
        "needs restocking",
        "need restocking",
        "what needs restock",
        "restocking",
        "running low",
        "out of stock",
    )

    private fun tryParseStockLookup(text: String): String? {
        val patterns = listOf(
            Regex("how many (.+?) do we have"),
            Regex("how many (.+?) (?:are there|in stock|left)"),
            Regex("stock (?:of|for) (.+)"),
            Regex("quantity (?:of|for) (.+)"),
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            return cleanupProductPhrase(match.groupValues[1])
        }
        return null
    }

    private fun tryParseReorder(text: String, context: ConversationContext): InventoryCommand? {
        val sales = Regex("(\\d+)").findAll(text).map { it.groupValues[1].toIntOrNull() }.filterNotNull().toList()
        if (sales.size < 2 && !reorderHint(text)) return null

        val target = Regex("have\\s+(\\d+)\\s+left").find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(?:want|end up with|left with)\\s+(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: when {
                text.contains("30 left") || text.contains("30 on hand") -> 30
                text.contains("want 30") || text.contains("end up with 30") || text.contains("have 30 left") -> 30
                sales.size >= 2 -> sales.last()
                else -> null
            }
        val expected = Regex("sell\\s+(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: when {
                text.contains("sell ten") -> 10
                sales.isNotEmpty() -> sales.first()
                else -> null
            }
        if (target == null || expected == null) {
            if (reorderHint(text)) {
                return InventoryCommand.CalculateReorder(
                    productNameOrSku = context.lastProduct?.name,
                    expectedSales = expected ?: -1,
                    targetStock = target ?: -1,
                )
            }
            return null
        }

        val product = extractProductForReorder(text) ?: context.lastProduct?.name
        return InventoryCommand.CalculateReorder(
            productNameOrSku = product,
            expectedSales = expected,
            targetStock = target,
        )
    }

    private fun reorderHint(text: String): Boolean =
        listOf("how many should i order", "reorder", "should i order", "order to").any { text.contains(it) }

    private fun extractProductForReorder(text: String): String? {
        val patterns = listOf(
            Regex("for (?:the )?(.+?) if"),
            Regex("for (?:the )?(.+?) when"),
            Regex("(.+?) if we sell"),
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            return cleanupProductPhrase(match.groupValues[1])
        }
        return null
    }

    private fun cleanupProductPhrase(raw: String): String =
        raw.trim()
            .removeSuffix("?")
            .removeSuffix(".")
            .replace(Regex("\\s+"), " ")
}