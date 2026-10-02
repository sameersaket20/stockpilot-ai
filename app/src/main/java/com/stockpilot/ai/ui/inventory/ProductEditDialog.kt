package com.stockpilot.ai.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stockpilot.ai.data.local.ProductEntity

@Composable
fun ProductEditDialog(
    product: ProductEntity,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
) {
    var stock by remember(product.id) { mutableStateOf(product.stockQuantity.toString()) }
    var reorder by remember(product.id) { mutableStateOf(product.reorderThreshold.toString()) }
    var target by remember(product.id) { mutableStateOf(product.targetStock.toString()) }
    var daily by remember(product.id) { mutableStateOf(product.dailySalesEstimate.toString()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SKU ${product.sku} • ${product.category}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantity on hand") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = reorder,
                    onValueChange = { reorder = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Reorder threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Target stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = daily,
                    onValueChange = { daily = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Est. daily sales") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val s = stock.toIntOrNull()
                    val r = reorder.toIntOrNull()
                    val t = target.toIntOrNull()
                    val d = daily.toIntOrNull()
                    if (s == null || r == null || t == null || d == null) {
                        validationError = "Enter valid non-negative numbers."
                        return@TextButton
                    }
                    if (s < 0 || r < 0 || t < 0 || d < 0) {
                        validationError = "Values must be zero or greater."
                        return@TextButton
                    }
                    validationError = null
                    onSave(
                        product.copy(
                            stockQuantity = s,
                            reorderThreshold = r,
                            targetStock = t,
                            dailySalesEstimate = d,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}