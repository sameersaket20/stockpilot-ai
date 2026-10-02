package com.stockpilot.ai.inference

import com.stockpilot.ai.domain.assistant.ConversationContext
import com.stockpilot.ai.domain.inventory.InventoryCommand
import kotlinx.coroutines.flow.StateFlow

data class IntentResult(
    val command: InventoryCommand,
    val confidenceNote: String = "demo-pattern",
)

interface InferenceEngine {
    val status: StateFlow<ModelStatus>
    suspend fun interpret(request: String, context: ConversationContext): IntentResult
    suspend fun close()
}