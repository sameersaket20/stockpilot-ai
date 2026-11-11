package com.stockpilot.ai.domain.assistant

import com.stockpilot.ai.domain.agent.InventoryAgentOrchestrator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantOrchestrator @Inject constructor(
    private val agent: InventoryAgentOrchestrator,
) {
    suspend fun respond(userMessage: String, context: ConversationContext): Pair<AssistantReply, ConversationContext> =
        agent.respond(userMessage, context)
}