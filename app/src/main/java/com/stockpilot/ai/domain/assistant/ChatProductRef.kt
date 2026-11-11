package com.stockpilot.ai.domain.assistant

data class ChatProductRef(
    val productId: Long,
    val sku: String,
    val name: String,
)