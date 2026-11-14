package com.stockpilot.ai.ui.models

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.stockpilot.ai.inference.GemmaInferenceEngine
import com.stockpilot.ai.speech.WhisperSpeechEngine
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
    private val gemmaInferenceEngine: GemmaInferenceEngine,
    private val whisperSpeechEngine: WhisperSpeechEngine,
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
                    statusMessage.value = when (type) {
                        ModelType.GEMMA -> "${type.displayName} imported — loading LiteRT-LM on CPU…"
                        ModelType.WHISPER -> "${type.displayName} imported — loading Whisper on CPU…"
                    }
                },
                onFailure = { errorMessage.value = it.message ?: "Import failed" },
            )
        }
    }

    fun removeModel(type: ModelType) {
        viewModelScope.launch {
            when (type) {
                ModelType.GEMMA -> gemmaInferenceEngine.releaseModel()
                ModelType.WHISPER -> whisperSpeechEngine.releaseModel()
            }
            storage.removeModel(type)
            statusMessage.value = "${type.displayName} removed."
        }
    }

    fun retryGemmaLoad() {
        gemmaInferenceEngine.scheduleInitialize()
        statusMessage.value = "Retrying Gemma LiteRT-LM initialization…"
    }

    fun retryWhisperLoad() {
        whisperSpeechEngine.scheduleInitialize()
        statusMessage.value = "Retrying Whisper initialization…"
    }

    fun statusLabel(status: ModelStatus): String = when (status) {
        ModelStatus.NotInstalled -> "Missing"
        ModelStatus.ImportedUnverified -> "Imported — starting on-device runtime"
        ModelStatus.Loading -> "Loading Gemma on CPU (LiteRT-LM)…"
        ModelStatus.Ready -> {
            val backend = gemmaInferenceEngine.runtimeBackend.value
            if (backend != null) {
                "Ready — on-device model active ($backend)"
            } else {
                "Ready — on-device model active"
            }
        }
        ModelStatus.Error -> "Error"
    }

    companion object {
        private fun formatBytes(bytes: Long): String {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1) String.format("%.2f GB free", gb) else String.format("%.0f MB free", bytes / (1024.0 * 1024.0))
        }
    }
}