package com.stockpilot.ai.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stockpilot.ai.domain.assistant.AssistantOrchestrator
import com.stockpilot.ai.speech.AndroidTtsManager
import com.stockpilot.ai.speech.AudioRecorder
import com.stockpilot.ai.speech.SpeechEngine

class AssistantViewModelFactory(
    private val orchestrator: AssistantOrchestrator,
    private val speechEngine: SpeechEngine,
    private val audioRecorder: AudioRecorder,
    private val ttsManager: AndroidTtsManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssistantViewModel::class.java)) {
            return AssistantViewModel(orchestrator, speechEngine, audioRecorder, ttsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    }
}