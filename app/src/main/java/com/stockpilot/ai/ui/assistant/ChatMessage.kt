package com.stockpilot.ai.ui.assistant

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val role: ChatRole,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
)