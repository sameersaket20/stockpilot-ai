package com.stockpilot.ai.speech

import com.stockpilot.ai.inference.ModelStatus
import kotlinx.coroutines.flow.StateFlow
import java.io.File

sealed class TranscriptionResult {
    data class Success(val text: String) : TranscriptionResult()
    data class Unavailable(val message: String) : TranscriptionResult()
    data class Failure(val message: String) : TranscriptionResult()
}

interface SpeechEngine {
    val status: StateFlow<ModelStatus>
    suspend fun transcribe(audioFile: File): TranscriptionResult
    suspend fun close()
}