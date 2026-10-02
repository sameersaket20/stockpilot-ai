package com.stockpilot.ai.domain.inventory

sealed class InventoryCommand {
    data class GetStock(val productNameOrSku: String) : InventoryCommand()

    data class GetProductDetails(val productNameOrSku: String) : InventoryCommand()

    data object ListLowStock : InventoryCommand()

    data class CalculateReorder(
        val productNameOrSku: String?,
        val expectedSales: Int,
        val targetStock: Int,
    ) : InventoryCommand()

    data object Unsupported : InventoryCommand()
}