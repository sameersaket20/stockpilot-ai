package com.stockpilot.ai.ui.models

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.stockpilot.ai.inference.ModelStatus
import com.stockpilot.ai.models.ModelImportManager
import com.stockpilot.ai.models.ModelType
import com.stockpilot.ai.models.ModelUiState
import com.stockpilot.ai.models.ModelStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModelManagerScreenState(
    val models: List<ModelUiState> = emptyList(),
    val freeSpaceLabel: String = "",
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val storage: ModelStorage,
    private val importManager: ModelImportManager,
) : ViewModel() {
    private val statusMessage = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModelManagerScreenState> = combine(
        combine(
            storage.status(ModelType.GEMMA),
            storage.manifest(ModelType.GEMMA),
            storage.status(ModelType.WHISPER),
            storage.manifest(ModelType.WHISPER),
            importManager.importProgress,
        ) { gemmaStatus, gemmaManifest, whisperStatus, whisperManifest, progress ->
            ModelType.entries.map { type ->
                val status = when (type) {
                    ModelType.GEMMA -> gemmaStatus
                    ModelType.WHISPER -> whisperStatus
                }
                val manifest = when (type) {
                    ModelType.GEMMA -> gemmaManifest
                    ModelType.WHISPER -> whisperManifest
                }
                val prog = progress?.takeIf { it.first == type }?.second
                ModelUiState(type, status, manifest, importProgress = prog)
            }
        },
        combine(statusMessage, errorMessage) { status, error -> status to error },
    ) { models, messages ->
        ModelManagerScreenState(
            models = models,
            freeSpaceLabel = formatBytes(storage.availableBytes()),
            statusMessage = messages.first,
            errorMessage = messages.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelManagerScreenState())

    init {
        viewModelScope.launch { storage.refreshFromDisk() }
    }

    fun importModel(type: ModelType, uri: Uri) {
        viewModelScope.launch {
            errorMessage.value = null
            val result = importManager.importDocument(type, uri)
            result.fold(
                onSuccess = {
                    statusMessage.value =
                        "${type.displayName} imported (unverified). Phase 2 required for runtime validation."
                },
                onFailure = { errorMessage.value = it.message ?: "Import failed" },
            )
        }
    }

    fun removeModel(type: ModelType) {
        viewModelScope.launch {
            storage.removeModel(type)
            statusMessage.value = "${type.displayName} removed."
        }
    }

    fun statusLabel(status: ModelStatus): String = when (status) {
        ModelStatus.NotInstalled -> "Missing"
        ModelStatus.ImportedUnverified -> "Imported (unverified)"
        ModelStatus.Loading -> "Loading"
        ModelStatus.Ready -> "Ready (runtime verified)"
        ModelStatus.Error -> "Error"
    }

    companion object {
        private fun formatBytes(bytes: Long): String {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1) String.format("%.2f GB free", gb) else String.format("%.0f MB free", bytes / (1024.0 * 1024.0))
        }
    }
}