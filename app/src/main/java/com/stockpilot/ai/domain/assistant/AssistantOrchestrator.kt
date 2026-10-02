package com.stockpilot.ai.domain.assistant

import com.stockpilot.ai.domain.inventory.InventoryCommandExecutor
import com.stockpilot.ai.domain.inventory.InventoryResult
import com.stockpilot.ai.inference.DemoInferenceEngine
import com.stockpilot.ai.inference.IntentResult

class AssistantOrchestrator(
    private val inferenceEngine: DemoInferenceEngine,
    private val commandExecutor: InventoryCommandExecutor,
) {
    suspend fun respond(userMessage: String, context: ConversationContext): Pair<String, ConversationContext> {
        val intent: IntentResult = inferenceEngine.interpret(userMessage, context)
        val command = intent.command
        val result = commandExecutor.execute(
            if (command is com.stockpilot.ai.domain.inventory.InventoryCommand.CalculateReorder &&
                (command.expectedSales < 0 || command.targetStock < 0)
            ) {
                com.stockpilot.ai.domain.inventory.InventoryCommand.Unsupported
            } else {
                command
            },
        )

        val body = formatResult(result)
        val prefix = "Demo interpreter • Live Room data\n\n"
        val newContext = when (result) {
            is InventoryResult.StockAnswer -> context.withProduct(result.product)
            is InventoryResult.ReorderAnswer -> context.withProduct(result.product)
            is InventoryResult.ProductDetails -> context.withProduct(result.product)
            else -> context
        }
        return prefix + body to newContext
    }

    private fun formatResult(result: InventoryResult): String = when (result) {
        is InventoryResult.StockAnswer -> result.message
        is InventoryResult.ProductDetails -> result.message
        is InventoryResult.LowStockList -> result.message
        is InventoryResult.ReorderAnswer -> result.message
        is InventoryResult.Clarification -> result.message
        is InventoryResult.Error -> result.message
    }
}