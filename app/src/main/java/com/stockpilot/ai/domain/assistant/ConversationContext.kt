package com.stockpilot.ai.domain.assistant

import com.stockpilot.ai.data.local.ProductEntity

data class ConversationContext(
    val lastProduct: ProductEntity? = null,
) {
    fun withProduct(product: ProductEntity?): ConversationContext =
        copy(lastProduct = product ?: lastProduct)
}