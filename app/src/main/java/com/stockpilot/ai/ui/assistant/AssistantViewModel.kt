package com.stockpilot.ai.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockpilot.ai.data.repository.AssistantChatRepository
import com.stockpilot.ai.domain.assistant.AssistantOrchestrator
import com.stockpilot.ai.domain.assistant.ConversationContext
import com.stockpilot.ai.speech.AndroidTtsManager
import com.stockpilot.ai.speech.TtsStatus
import com.stockpilot.ai.speech.AudioRecorder
import com.stockpilot.ai.speech.RecordingState
import com.stockpilot.ai.speech.SpeechEngine
import com.stockpilot.ai.speech.TranscriptionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import javax.inject.Inject

data class AssistantUiState(
    val timeline: List<ChatListItem> = emptyList(),
    val input: String = "",
    val isProcessing: Boolean = false,
    val recording: Boolean = false,
    val error: String? = null,
    val ttsMuted: Boolean = false,
    val ttsWarning: String? = null,
    val micPermissionDenied: Boolean = false,
    val hasMoreHistory: Boolean = false,
    val isLoadingOlder: Boolean = false,
    /** Set when user message is in chat and we wait for the assistant reply. */
    val processingStartedAtMs: Long? = null,
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val orchestrator: AssistantOrchestrator,
    private val chatRepository: AssistantChatRepository,
    private val speechEngine: SpeechEngine,
    private val audioRecorder: AudioRecorder,
    private val ttsManager: AndroidTtsManager,
) : ViewModel() {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val historyLimit = MutableStateFlow(ChatPagination.INITIAL_LIMIT)
    private val hasMoreHistory = MutableStateFlow(false)
    private val isLoadingOlder = MutableStateFlow(false)
    private val loadingOlderGuard = AtomicBoolean(false)
    private val input = MutableStateFlow("")
    private val isProcessing = MutableStateFlow(false)
    private val processingStartedAtMs = MutableStateFlow<Long?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val micDenied = MutableStateFlow(false)
    private var conversationContext = ConversationContext()
    private var lastHandledRecordingPath: String? = null

    private val timeline = messages
        .map(::buildChatTimeline)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<AssistantUiState> = combine(
        combine(
            combine(timeline, input, isProcessing) { items, inp, processing ->
                Triple(items, inp, processing)
            },
            combine(processingStartedAtMs, audioRecorder.state, error) { processingStart, recState, err ->
                Triple(processingStart, recState, err)
            },
        ) { chat, timing ->
            AssistantCoreInputs(
                timeline = chat.first,
                input = chat.second,
                isProcessing = chat.third,
                processingStartedAtMs = timing.first,
                recordingState = timing.second,
                error = timing.third,
            )
        },
        combine(
            micDenied,
            ttsManager.status,
            hasMoreHistory,
            isLoadingOlder,
        ) { denied, tts, moreHistory, loadingOlder ->
            MicAndHistory(denied, tts, moreHistory, loadingOlder)
        },
    ) { core, micAndHistory ->
        AssistantUiState(
            timeline = core.timeline,
            input = core.input,
            isProcessing = core.isProcessing,
            recording = core.recordingState is RecordingState.Recording,
            error = core.error,
            ttsMuted = micAndHistory.tts.muted,
            ttsWarning = micAndHistory.tts.warning,
            micPermissionDenied = micAndHistory.micDenied,
            hasMoreHistory = micAndHistory.hasMoreHistory,
            isLoadingOlder = micAndHistory.isLoadingOlder,
            processingStartedAtMs = core.processingStartedAtMs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssistantUiState())

    private data class AssistantCoreInputs(
        val timeline: List<ChatListItem>,
        val input: String,
        val isProcessing: Boolean,
        val processingStartedAtMs: Long?,
        val recordingState: RecordingState,
        val error: String?,
    )

    private data class MicAndHistory(
        val micDenied: Boolean,
        val tts: TtsStatus,
        val hasMoreHistory: Boolean,
        val isLoadingOlder: Boolean,
    )

    init {
        ttsManager.initialize()
        ttsManager.setMuted(true)
        viewModelScope.launch {
            chatRepository.ensureWelcomeIfEmpty()
            kotlinx.coroutines.flow.combine(
                historyLimit.flatMapLatest { limit -> chatRepository.observeLastN(limit) },
                chatRepository.observeMessageCount(),
            ) { loaded, total -> loaded to total }
                .collect { (loaded, total) ->
                    messages.value = loaded
                    hasMoreHistory.value = loaded.size < total
                    isLoadingOlder.value = false
                    loadingOlderGuard.set(false)
                }
        }
        viewModelScope.launch {
            audioRecorder.state.collect { recState ->
                when (recState) {
                    is RecordingState.Finished -> handleRecordingFinished(recState.outputFile)
                    is RecordingState.Error -> {
                        error.value = recState.message
                        isProcessing.value = false
                        processingStartedAtMs.value = null
                        audioRecorder.resetIdle()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun hasMicPermission(): Boolean = audioRecorder.hasMicPermission()

    fun loadOlderMessages() {
        if (!hasMoreHistory.value || !loadingOlderGuard.compareAndSet(false, true)) return
        isLoadingOlder.value = true
        historyLimit.value += ChatPagination.PAGE_SIZE
    }

    fun onInputChange(value: String) {
        input.value = value
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
                audioRecorder.stop()
            }
            else -> Unit
        }
    }

    fun toggleTtsMute() {
        ttsManager.setMuted(!ttsManager.status.value.muted)
    }

    fun sendMessage() {
        val text = input.value.trim()
        if (text.isEmpty() || isProcessing.value) return
        input.value = ""
        viewModelScope.launch { submitUserText(text) }
    }

    private suspend fun submitUserText(text: String) {
        val turnStartedMs = System.currentTimeMillis()
        processingStartedAtMs.value = turnStartedMs
        isProcessing.value = true
        error.value = null
        appendMessage(ChatMessage(role = ChatRole.USER, text = text))
        try {
            val (reply, newContext) = orchestrator.respond(text, conversationContext)
            conversationContext = newContext
            val responseDurationMs = System.currentTimeMillis() - turnStartedMs
            appendMessage(
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    text = reply.text,
                    productLinks = reply.productLinks,
                    referencedProducts = reply.referencedProducts,
                    provenance = reply.provenance,
                    responseDurationMs = responseDurationMs,
                ),
            )
            if (!ttsManager.status.value.muted) {
                ttsManager.speak(reply.text.substringAfter("\n\n", reply.text))
            }
        } catch (e: Exception) {
            error.value = e.message
            appendMessage(ChatMessage(role = ChatRole.SYSTEM, text = "Something went wrong: ${e.message}"))
        } finally {
            isProcessing.value = false
            processingStartedAtMs.value = null
        }
    }

    private fun handleRecordingFinished(file: File) {
        val key = file.absolutePath
        if (key == lastHandledRecordingPath) return
        lastHandledRecordingPath = key
        viewModelScope.launch {
            isProcessing.value = true
            processingStartedAtMs.value = null
            try {
                when (val result = speechEngine.transcribe(file)) {
                    is TranscriptionResult.Unavailable -> {
                        appendMessage(ChatMessage(role = ChatRole.SYSTEM, text = result.message))
                        isProcessing.value = false
                        processingStartedAtMs.value = null
                    }
                    is TranscriptionResult.Failure -> {
                        appendMessage(ChatMessage(role = ChatRole.SYSTEM, text = result.message))
                        isProcessing.value = false
                        processingStartedAtMs.value = null
                    }
                    is TranscriptionResult.Success -> submitUserText(result.text)
                }
            } finally {
                file.delete()
                audioRecorder.resetIdle()
            }
        }
    }

    private suspend fun appendMessage(message: ChatMessage) {
        chatRepository.insert(message)
    }

    override fun onCleared() {
        audioRecorder.cancel()
        ttsManager.stop()
        ttsManager.shutdown()
        super.onCleared()
    }
}