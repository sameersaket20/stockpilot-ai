package com.stockpilot.ai.speech

import com.stockpilot.ai.inference.ModelStatus
import com.stockpilot.ai.models.ModelType
import com.stockpilot.ai.models.ModelStorage
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UninstalledSpeechEngine @Inject constructor(
    private val modelStorage: ModelStorage,
) : SpeechEngine {
    override val status: StateFlow<ModelStatus> = modelStorage.status(ModelType.WHISPER)

    override suspend fun transcribe(audioFile: File): TranscriptionResult {
        val st = modelStorage.status(ModelType.WHISPER).value
        val base = when (st) {
            ModelStatus.ImportedUnverified ->
                "Speech model file imported but Whisper runtime is not integrated yet (Phase 2)."
            ModelStatus.NotInstalled ->
                "Speech model not installed"
            ModelStatus.Ready ->
                "Whisper runtime not available in Phase 1."
            else -> "Speech recognition unavailable"
        }
        return TranscriptionResult.Unavailable(
            "$base — recording saved at ${audioFile.name}. Import Whisper Tiny.en and complete Phase 2 for transcription. Use typed input for now.",
        )
    }

    override suspend fun close() = Unit
}