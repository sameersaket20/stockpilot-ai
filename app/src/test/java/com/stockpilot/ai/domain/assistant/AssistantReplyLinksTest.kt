package com.stockpilot.ai.domain.assistant

import com.stockpilot.ai.data.local.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantReplyLinksTest {
    private val blueShirt = ProductEntity(
        id = 1,
        sku = "TEE-BLU-M",
        name = "Blue T-shirt",
        category = "Tops",
        stockQuantity = 24,
        reorderThreshold = 10,
        targetStock = 30,
        dailySalesEstimate = 5,
    )

    @Test
    fun productsMentionedIn_matchesSkuAndName() {
        val found = AssistantReplyLinks.productsMentionedIn(
            "Check TEE-BLU-M and Red Hoodie",
            listOf(blueShirt),
        )
        assertEquals(1, found.size)
        assertEquals("TEE-BLU-M", found.single().sku)
    }

    @Test
    fun spansForProducts_linksSkuTokens() {
        val links = AssistantReplyLinks.spansForProducts(
            "Low: SKU-PF-1502",
            listOf(
                blueShirt.copy(sku = "SKU-PF-1502", name = "Protein Bar"),
            ),
        )
        assertTrue(links.isNotEmpty())
        assertEquals("SKU-PF-1502", "Low: SKU-PF-1502".substring(links.single().startIndex, links.single().endIndex))
    }
}