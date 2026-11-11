package com.stockpilot.ai.domain.assistant

import com.stockpilot.ai.data.local.ProductEntity

data class ChatProductLink(
    val productId: Long,
    val startIndex: Int,
    val endIndex: Int,
)

data class AssistantReply(
    val text: String,
    val productLinks: List<ChatProductLink> = emptyList(),
    val referencedProducts: List<ChatProductRef> = emptyList(),
    val provenance: AssistantProvenance,
)

object AssistantReplyLinks {
    fun productsMentionedIn(text: String, catalog: List<ProductEntity>): List<ProductEntity> =
        catalog.filter { product ->
            text.contains(product.sku, ignoreCase = true) ||
                text.contains(product.name, ignoreCase = true)
        }.distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

    fun toRefs(products: List<ProductEntity>): List<ChatProductRef> = products.map {
        ChatProductRef(productId = it.id, sku = it.sku, name = it.name)
    }

    fun spansForProducts(text: String, products: List<ProductEntity>): List<ChatProductLink> {
        if (products.isEmpty()) return emptyList()
        val links = mutableListOf<ChatProductLink>()
        for (product in products.distinctBy { it.id }) {
            addAllOccurrences(text, product.id, product.name, links)
            addAllOccurrences(text, product.id, product.sku, links)
        }
        return links
            .sortedBy { it.startIndex }
            .distinctBy { "${it.productId}:${it.startIndex}" }
    }

    private fun addAllOccurrences(
        text: String,
        productId: Long,
        token: String,
        links: MutableList<ChatProductLink>,
    ) {
        if (token.isBlank()) return
        var fromIndex = 0
        while (fromIndex < text.length) {
            val start = text.indexOf(token, fromIndex, ignoreCase = true)
            if (start < 0) break
            val end = start + token.length
            links += ChatProductLink(productId = productId, startIndex = start, endIndex = end)
            fromIndex = end
        }
    }
}