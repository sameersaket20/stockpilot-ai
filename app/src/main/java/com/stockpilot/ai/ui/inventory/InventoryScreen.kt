package com.stockpilot.ai.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpilot.ai.data.local.ProductEntity
import com.stockpilot.ai.data.local.StockStatus
import com.stockpilot.ai.data.repository.InventoryFilter
import com.stockpilot.ai.ui.components.OfflinePill
import com.stockpilot.ai.ui.components.StatCard
import com.stockpilot.ai.ui.theme.StockPilotInStock
import com.stockpilot.ai.ui.theme.StockPilotLowStock
import com.stockpilot.ai.ui.theme.StockPilotNavy
import com.stockpilot.ai.ui.theme.StockPilotOutOfStock
import com.stockpilot.ai.ui.theme.StockPilotSurface
import com.stockpilot.ai.ui.theme.StockPilotTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryRoute(
    onOpenAssistant: () -> Unit,
    onOpenSettings: () -> Unit,
    highlightProductId: Long = -1L,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(highlightProductId) {
        if (highlightProductId > 0L) {
            viewModel.openProductById(highlightProductId)
        }
    }
    InventoryScreen(
        state = state,
        onSearchChange = viewModel::onSearchChange,
        onFilterChange = viewModel::onFilterChange,
        onProductClick = viewModel::openEdit,
        onDismissEdit = viewModel::dismissEdit,
        onSaveProduct = viewModel::saveProduct,
        onOpenAssistant = onOpenAssistant,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    state: InventoryUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (InventoryFilter) -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onDismissEdit: () -> Unit,
    onSaveProduct: (ProductEntity) -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        containerColor = StockPilotNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StockPilotNavy),
                title = {
                    Column {
                        Text("StockPilot AI", style = MaterialTheme.typography.titleMedium)
                        Text("Store Inventory", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings and models",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OfflinePill()
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        label = "Total Units",
                        value = state.summary.totalUnits.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Products",
                        value = state.summary.productCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Low Stock",
                        value = state.summary.lowStockProducts.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Button(
                    onClick = onOpenAssistant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("Ask Inventory AI")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search SKU, name, category") },
                    singleLine = true,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { onFilterChange(filter) },
                            label = {
                                Text(
                                    when (filter) {
                                        InventoryFilter.ALL -> "All"
                                        InventoryFilter.LOW_STOCK -> "Low stock"
                                        InventoryFilter.OUT_OF_STOCK -> "Out of stock"
                                    },
                                )
                            },
                        )
                    }
                }
            }
            if (state.products.isEmpty()) {
                item {
                    Text(
                        "No products match your filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(state.products, key = { it.id }) { product ->
                    ProductRow(product = product, onClick = { onProductClick(product) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    state.editingProduct?.let { product ->
        ProductEditDialog(
            product = product,
            isSaving = state.isSaving,
            onDismiss = onDismissEdit,
            onSave = onSaveProduct,
        )
    }
}

@Composable
private fun ProductRow(product: ProductEntity, onClick: () -> Unit) {
    val statusColor = when (product.stockStatus()) {
        StockStatus.IN_STOCK -> StockPilotInStock
        StockStatus.LOW_STOCK -> StockPilotLowStock
        StockStatus.OUT_OF_STOCK -> StockPilotOutOfStock
    }
    val statusLabel = when (product.stockStatus()) {
        StockStatus.IN_STOCK -> "In stock"
        StockStatus.LOW_STOCK -> "Low stock"
        StockStatus.OUT_OF_STOCK -> "Out of stock"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${product.name}, quantity ${product.stockQuantity}" }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StockPilotSurface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = StockPilotTeal,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${product.sku} • ${product.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${product.stockQuantity}", style = MaterialTheme.typography.headlineMedium)
                Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}