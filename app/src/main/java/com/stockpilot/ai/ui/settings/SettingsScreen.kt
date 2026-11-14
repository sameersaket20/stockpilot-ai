package com.stockpilot.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stockpilot.ai.ui.components.GemmaRuntimeBanner
import com.stockpilot.ai.ui.theme.StockPilotNavy

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    appVersion: String,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val gemmaStatus by viewModel.gemmaStatus.collectAsStateWithLifecycle()
    var showReset by remember { mutableStateOf(false) }

    SettingsScreen(
        appVersion = appVersion,
        gemmaStatus = gemmaStatus,
        onBack = onBack,
        onOpenModels = onOpenModels,
        onRequestReset = { showReset = true },
    )

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset sample inventory?") },
            text = { Text("This restores the 30 demo products and overwrites local stock values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetSeedInventory()
                        showReset = false
                    },
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appVersion: String,
    gemmaStatus: com.stockpilot.ai.inference.ModelStatus,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onRequestReset: () -> Unit,
) {
    Scaffold(
        containerColor = StockPilotNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StockPilotNavy),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings & About") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GemmaRuntimeBanner(gemmaStatus)
            Text("StockPilot AI $appVersion", style = MaterialTheme.typography.titleMedium)
            Text(
                "Offline-first inventory with Gemma 4 E2B (LiteRT-LM) and optional Whisper Tiny.en speech input.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onOpenModels, modifier = Modifier.fillMaxWidth()) {
                Text("Open Model Manager")
            }
            OutlinedButton(onClick = onRequestReset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset sample inventory data")
            }
        }
    }
}