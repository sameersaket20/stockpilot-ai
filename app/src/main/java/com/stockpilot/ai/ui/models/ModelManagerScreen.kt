package com.stockpilot.ai.ui.models

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockpilot.ai.models.ModelType
import com.stockpilot.ai.models.ModelUiState
import com.stockpilot.ai.ui.components.DemoModeBanner
import com.stockpilot.ai.ui.components.ModelStatusBadge
import com.stockpilot.ai.ui.theme.StockPilotNavy
import com.stockpilot.ai.ui.theme.StockPilotSurface
import java.text.DateFormat
import java.util.Date

@Composable
fun ModelManagerRoute(onBack: () -> Unit) {
    val viewModel: ModelManagerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingType by remember { mutableStateOf<ModelType?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val type = pendingType
        if (uri != null && type != null) {
            viewModel.importModel(type, uri)
        }
        pendingType = null
    }
  ModelManagerScreen(
        state = state,
        onBack = onBack,
        onImport = { type ->
            pendingType = type
            picker.launch(arrayOf("*/*"))
        },
        onRemove = viewModel::removeModel,
        statusLabel = viewModel::statusLabel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    state: ModelManagerScreenState,
    onBack: () -> Unit,
    onImport: (ModelType) -> Unit,
    onRemove: (ModelType) -> Unit,
    statusLabel: (com.stockpilot.ai.inference.ModelStatus) -> String,
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
                title = { Text("Model Manager") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { DemoModeBanner() }
            item {
                Text(
                    "Import model files you transferred to this device (Downloads / Files). " +
                        "Import does not enable inference until Phase 2 integrates LiteRT-LM and whisper.cpp.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                Text("App storage: ${state.freeSpaceLabel}", style = MaterialTheme.typography.labelLarge)
            }
            state.statusMessage?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.tertiary) }
            }
            state.errorMessage?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.error) }
            }
            items(state.models, key = { it.type.id }) { model ->
                ModelCard(model, onImport, onRemove, statusLabel)
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelUiState,
    onImport: (ModelType) -> Unit,
    onRemove: (ModelType) -> Unit,
    statusLabel: (com.stockpilot.ai.inference.ModelStatus) -> String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StockPilotSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${model.type.displayName} — ${model.type.description}", style = MaterialTheme.typography.titleMedium)
            ModelStatusBadge(model.status)
            Text(statusLabel(model.status), style = MaterialTheme.typography.bodyMedium)
            model.manifest?.let { manifest ->
                val file = manifest.files.firstOrNull()
                Text("File: ${manifest.originalDisplayName}", style = MaterialTheme.typography.bodyMedium)
                file?.let { Text("Size: ${file.sizeBytes} bytes", style = MaterialTheme.typography.bodyMedium) }
                Text(
                    "Imported: ${DateFormat.getDateTimeInstance().format(Date(manifest.importedAtEpochMs))}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                manifest.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            model.importProgress?.let {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Button(onClick = { onImport(model.type) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (model.manifest == null) "Import" else "Replace")
            }
            if (model.manifest != null) {
                OutlinedButton(onClick = { onRemove(model.type) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove")
                }
            }
        }
    }
}