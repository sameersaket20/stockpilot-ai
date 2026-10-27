package com.stockpilot.ai.domain.inventory

import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.local.StockStatus
import com.stockpilot.ai.data.repository.InventoryOperations
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryCommandExecutor @Inject constructor(
    private val repository: InventoryOperations,
) {
    suspend fun execute(command: InventoryCommand): InventoryResult = when (command) {
        is InventoryCommand.GetStock -> handleGetStock(command.productNameOrSku)
        is InventoryCommand.GetProductDetails -> handleDetails(command.productNameOrSku)
        InventoryCommand.ListLowStock -> handleLowStock()
        is InventoryCommand.CalculateReorder -> handleReorder(
            command.productNameOrSku,
            command.expectedSales,
            command.targetStock,
        )
        InventoryCommand.Unsupported -> InventoryResult.Clarification(
            "Demo mode supports stock lookups, low-stock lists, and reorder math. " +
                "Try: \"How many blue T-shirts do we have?\" or \"What needs restocking?\"",
        )
    }

    private suspend fun resolveProduct(query: String?): Result<ProductEntity> {
        if (query.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("missing product"))
        }
        val matches = repository.findByNameOrSku(query)
        return when {
            matches.isEmpty() -> Result.failure(NoSuchElementException(query))
            matches.size > 1 -> Result.failure(AmbiguousProductException(matches))
            else -> Result.success(matches.single())
        }
    }

    private suspend fun handleGetStock(query: String): InventoryResult {
        return resolveProduct(query).fold(
            onSuccess = { product ->
                InventoryResult.StockAnswer(
                    product = product,
                    message = "${product.name} (${product.sku}): ${product.stockQuantity} units on hand.",
                )
            },
            onFailure = { error ->
                when (error) {
                    is AmbiguousProductException -> InventoryResult.Clarification(
                        "Multiple products match \"$query\": ${error.matches.joinToString { it.name }}. " +
                            "Please specify SKU or a more precise name.",
                    )
                    is NoSuchElementException -> InventoryResult.Clarification(
                        "I couldn't find a product matching \"$query\". Check spelling or use the SKU.",
                    )
                    else -> InventoryResult.Error("Unable to look up stock.")
                }
            },
        )
    }

    private suspend fun handleDetails(query: String): InventoryResult =
        resolveProduct(query).fold(
            onSuccess = { product ->
                InventoryResult.ProductDetails(
                    product = product,
                    message = buildString {
                        append(product.name)
                        append(" • SKU ")
                        append(product.sku)
                        append(" • ")
                        append(product.category)
                        append(" • Stock ")
                        append(product.stockQuantity)
                        append(" • Reorder at ")
                        append(product.reorderThreshold)
                        append(" • Target ")
                        append(product.targetStock)
                    },
                )
            },
            onFailure = { err ->
                if (err is AmbiguousProductException) {
                    InventoryResult.Clarification("Several matches found; please be more specific.")
                } else {
                    InventoryResult.Clarification("Product not found for \"$query\".")
                }
            },
        )

    private suspend fun handleLowStock(): InventoryResult {
        val low = repository.listLowStock()
        val message = if (low.isEmpty()) {
            "All products are above their reorder thresholds."
        } else {
            buildString {
                append("Low or out-of-stock items (${low.size}):\n")
                low.forEach { p ->
                    val label = when (p.stockStatus()) {
                        StockStatus.OUT_OF_STOCK -> "OUT"
                        else -> "LOW"
                    }
                    append("• ${p.name} — ${p.stockQuantity} ($label)\n")
                }
            }.trim()
        }
        return InventoryResult.LowStockList(low, message)
    }

    private suspend fun handleReorder(
        query: String?,
        expectedSales: Int,
        targetStock: Int,
    ): InventoryResult {
        if (expectedSales < 0 || targetStock < 0) {
            return InventoryResult.Error("Sales and target quantities must be zero or greater.")
        }
        val productResult = resolveProduct(query)
        val product = productResult.getOrElse { error ->
            return when (error) {
                is AmbiguousProductException ->
                    InventoryResult.Clarification("Which product? ${error.matches.joinToString { it.name }}")
                is NoSuchElementException ->
                    InventoryResult.Clarification(
                        if (query.isNullOrBlank()) {
                            "Which product should I calculate reorder for?"
                        } else {
                            "Product \"$query\" not found."
                        },
                    )
                else -> InventoryResult.Error("Unable to calculate reorder.")
            }
        }
        val reorder = ReorderCalculator.calculate(
            product.stockQuantity,
            expectedSales,
            targetStock,
        )
        val msg =
            "Reorder suggestion for ${product.name}: order $reorder units " +
                "(current ${product.stockQuantity}, expected sales $expectedSales, target on-hand $targetStock)."
        return InventoryResult.ReorderAnswer(product, reorder, msg)
    }

    class AmbiguousProductException(val matches: List<ProductEntity>) : Exception()
}