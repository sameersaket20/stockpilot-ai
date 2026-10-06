package com.stockpilot.ai.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.ai.domain.assistant.AssistantOrchestrator
import com.stockpilot.ai.domain.assistant.ConversationContext
import com.stockpilot.ai.speech.AndroidTtsManager
import com.stockpilot.ai.speech.AudioRecorder
import com.stockpilot.ai.speech.RecordingState
import com.stockpilot.ai.speech.SpeechEngine
import com.stockpilot.ai.speech.TranscriptionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isProcessing: Boolean = false,
    val recording: Boolean = false,
    val error: String? = null,
    val ttsMuted: Boolean = false,
    val ttsWarning: String? = null,
    val micPermissionDenied: Boolean = false,
)

class AssistantViewModel(
    private val orchestrator: AssistantOrchestrator,
    private val speechEngine: SpeechEngine,
    private val audioRecorder: AudioRecorder,
    private val ttsManager: AndroidTtsManager,
) : ViewModel() {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val input = MutableStateFlow("")
    private val isProcessing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val micDenied = MutableStateFlow(false)
    private var conversationContext = ConversationContext()

    val uiState: StateFlow<AssistantUiState> = combine(
        combine(messages, input, isProcessing, audioRecorder.state, error) { msg, inp, processing, recState, err ->
            Triple(msg, inp, Triple(processing, recState, err))
        },
        combine(micDenied, ttsManager.status) { denied, tts -> denied to tts },
    ) { left, right ->
        val (msg, inp, triple) = left
        val (processing, recState, err) = triple
        val (denied, tts) = right
        AssistantUiState(
            messages = msg,
            input = inp,
            isProcessing = processing,
            recording = recState is RecordingState.Recording,
            error = err,
            ttsMuted = tts.muted,
            ttsWarning = tts.warning,
            micPermissionDenied = denied,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssistantUiState())

    init {
        messages.value = listOf(
            ChatMessage(
                role = ChatRole.SYSTEM,
                text = "DEMO MODE — pattern-based interpreter with live Room inventory. No Gemma model loaded.",
            ),
        )
    }

    fun onInputChange(value: String) {
        input.value = value
    }

    fun sendMessage() {
        val text = input.value.trim()
        if (text.isEmpty() || isProcessing.value) return
        input.value = ""
        submitUserText(text)
    }

    fun sendSuggested(text: String) {
        if (isProcessing.value) return
        submitUserText(text)
    }

    fun onMicPermissionResult(granted: Boolean) {
        micDenied.value = !granted
        if (granted) toggleRecording(startOnly = true)
    }

    fun onMicPressed(hasPermission: Boolean, requestPermission: () -> Unit) {
        when (audioRecorder.state.value) {
            is RecordingState.Recording -> toggleRecording()
            else -> {
                if (hasPermission) toggleRecording() else requestPermission()
            }
        }
    }

    fun toggleRecording(startOnly: Boolean = false) {
        when (val state = audioRecorder.state.value) {
            RecordingState.Idle -> {
                if (!audioRecorder.hasMicPermission()) {
                    micDenied.value = true
                    return
                }
                audioRecorder.start()
            }
            is RecordingState.Recording -> {
                if (startOnly) return
                val file = audioRecorder.stop() ?: return
                handleRecordingFinished(file)
            }
            else -> Unit
        }
    }

    fun toggleTtsMute() {
        ttsManager.setMuted(!ttsManager.status.value.muted)
    }

    private fun submitUserText(text: String) {
        viewModelScope.launch {
            appendMessage(ChatMessage(role = ChatRole.USER, text = text))
            isProcessing.value = true
            error.value = null
            try {
                val (reply, newContext) = orchestrator.respond(text, conversationContext)
                conversationContext = newContext
                appendMessage(ChatMessage(role = ChatRole.ASSISTANT, text = reply))
                if (!ttsManager.status.value.muted) {
                    ttsManager.speak(reply.substringAfter("\n\n", reply))
                }
            } catch (e: Exception) {
                error.value = e.message
                appendMessage(ChatMessage(role = ChatRole.SYSTEM, text = "Something went wrong: ${e.message}"))
            } finally {
                isProcessing.value = false
            }
        }
    }

    private fun handleRecordingFinished(file: File) {
        viewModelScope.launch {
            isProcessing.value = true
            appendMessage(ChatMessage(role = ChatRole.USER, text = "[Voice message — audio recorded]"))
            when (val result = speechEngine.transcribe(file)) {
                is TranscriptionResult.Unavailable ->
                    appendMessage(ChatMessage(role = ChatRole.SYSTEM, text = result.message))
                is TranscriptionResult.Failure ->
                    appendMessage(ChatMessage(role = ChatRole.SYSTEM, text = result.message))
                is TranscriptionResult.Success -> submitUserText(result.text)
            }
            audioRecorder.resetIdle()
            isProcessing.value = false
        }
    }

    private fun appendMessage(message: ChatMessage) {
        messages.value = messages.value + message
    }

    override fun onCleared() {
        audioRecorder.cancel()
        ttsManager.stop()
        super.onCleared()
    }
}