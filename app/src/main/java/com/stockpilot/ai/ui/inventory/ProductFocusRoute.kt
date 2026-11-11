package com.stockpilot.ai.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockpilot.ai.ui.theme.StockPilotNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFocusRoute(
    productId: Long,
    onBack: () -> Unit,
    viewModel: ProductFocusViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(productId) {
        viewModel.load(productId)
    }

    Scaffold(
        containerColor = StockPilotNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StockPilotNavy),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chat")
                    }
                },
                title = { Text(state.product?.name ?: "Product") },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.product != null -> {
                    ProductEditDialog(
                        product = state.product!!,
                        isSaving = state.isSaving,
                        saveError = state.error,
                        onDismiss = onBack,
                        onSave = { updated -> viewModel.save(updated, onSuccess = onBack) },
                    )
                }
                state.error != null -> {
                    Text(state.error!!, modifier = Modifier.padding(24.dp))
                }
                else -> CircularProgressIndicator()
            }
        }
    }
}