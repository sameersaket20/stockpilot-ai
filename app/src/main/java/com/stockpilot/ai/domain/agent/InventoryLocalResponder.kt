package com.stockpilot.ai.domain.agent

import com.stockpilot.ai.domain.assistant.AssistantDeliveryMode
import com.stockpilot.ai.domain.assistant.AssistantProvenance
import com.stockpilot.ai.domain.assistant.AssistantReply
import com.stockpilot.ai.domain.assistant.AssistantReplyLinks
import com.stockpilot.ai.domain.assistant.ConversationContext
import com.stockpilot.ai.domain.inventory.ReorderCalculator

/** Deterministic replies when Gemma is slow or unavailable — uses live full read only. */
object InventoryLocalResponder {
    private val stockFocusKeywords = listOf(
        "low", "restock", "reorder", "attention", "out of stock", "order", "stock",
    )

    fun focusesOnStockHealth(userMessage: String): Boolean =
        stockFocusKeywords.any { userMessage.contains(it, ignoreCase = true) }

    fun lowStockFallback(
        userMessage: String,
        fullRead: ToolInvocationResult,
        context: ConversationContext,
        fallbackReason: String = "Gemma unavailable",
    ): Pair<AssistantReply, ConversationContext>? {
        if (!focusesOnStockHealth(userMessage)) return null
        val zeroCount = fullRead.facts.optInt("zero_on_hand_count")
        val low = fullRead.products.filter { it.stockQuantity <= 0 }
        if (low.isEmpty()) {
            val text = "No zero-quantity (out-of-stock) items right now ($zeroCount SKUs at 0, " +
                "${fullRead.facts.optInt("product_count")} SKUs in catalog)."
            return AssistantReply(
                text = text,
                referencedProducts = emptyList(),
                provenance = AssistantProvenance(
                    mode = AssistantDeliveryMode.TOOL_FACTS_ONLY,
                    detail = "Live inventory summary ($fallbackReason)",
                    toolsUsed = listOf("full_inventory_read"),
                ),
            ) to context
        }
        val lines = low.sortedBy { it.name.lowercase() }.map { product ->
            val suggested = ReorderCalculator.calculate(
                product.stockQuantity,
                product.dailySalesEstimate,
                product.targetStock,
            )
            "• ${product.name} (${product.sku}) — OUT, on_hand_qty ${product.stockQuantity}, suggested order $suggested"
        }
        val text = buildString {
            appendLine("${low.size} out-of-stock items (zero on hand):")
            lines.forEach { appendLine(it) }
        }.trim()
        val refs = AssistantReplyLinks.toRefs(low)
        val newContext = low.lastOrNull()?.let { context.withProduct(it) } ?: context
        return AssistantReply(
            text = text,
            referencedProducts = refs,
            provenance = AssistantProvenance(
                mode = AssistantDeliveryMode.TOOL_FACTS_ONLY,
                detail = "Live inventory summary ($fallbackReason)",
                toolsUsed = listOf("full_inventory_read"),
            ),
        ) to newContext
    }
}