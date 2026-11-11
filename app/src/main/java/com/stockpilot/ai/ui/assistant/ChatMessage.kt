package com.stockpilot.ai.ui.assistant

import com.stockpilot.ai.domain.assistant.AssistantProvenance
import com.stockpilot.ai.domain.assistant.ChatProductLink
import com.stockpilot.ai.domain.assistant.ChatProductRef

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val role: ChatRole,
    val text: String,
    val productLinks: List<ChatProductLink> = emptyList(),
    val referencedProducts: List<ChatProductRef> = emptyList(),
    val provenance: AssistantProvenance? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    /** Wall time from user bubble posted until this assistant reply was saved. */
    val responseDurationMs: Long? = null,
)