package com.stockpilot.ai.speech

import com.stockpilot.ai.inference.ModelStatus
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingSpeechEngine @Inject constructor(
    private val whisperSpeechEngine: WhisperSpeechEngine,
) : SpeechEngine {
    override val status: StateFlow<ModelStatus> = whisperSpeechEngine.status

    override suspend fun transcribe(audioFile: File): TranscriptionResult = when (whisperSpeechEngine.status.value) {
        ModelStatus.Ready -> whisperSpeechEngine.transcribe(audioFile)
        ModelStatus.Loading -> TranscriptionResult.Unavailable(
            "Whisper is still loading the speech model. Wait a moment and try again.",
        )
        ModelStatus.Error -> TranscriptionResult.Unavailable(
            whisperSpeechEngine.lastError.value ?: "Whisper failed to load. Open Model Manager for details.",
        )
        ModelStatus.ImportedUnverified -> TranscriptionResult.Unavailable(
            "Whisper is imported but not loaded yet. Restart the app or wait for initialization.",
        )
        ModelStatus.NotInstalled -> TranscriptionResult.Unavailable(
            "Import ggml-tiny.en.bin in Settings → Model Manager → Whisper Tiny.en.",
        )
    }

    override suspend fun close() {
        whisperSpeechEngine.close()
    }
}