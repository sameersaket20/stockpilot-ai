package com.stockpilot.ai.inference

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.stockpilot.ai.models.ModelStorage
import com.stockpilot.ai.models.ModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LiteRT-LM runtime for on-device Gemma. Used by [com.stockpilot.ai.domain.agent.InventoryAgentOrchestrator]
 * via [complete] — no fixed command enum path.
 */
@Singleton
class GemmaInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelStorage: ModelStorage,
) {
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "litert-lm-gemma").apply { isDaemon = true }
    }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()

    private var engine: Engine? = null
    private var loadedPath: String? = null

    private val _status = MutableStateFlow(ModelStatus.NotInstalled)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _runtimeBackend = MutableStateFlow<String?>(null)
    val runtimeBackend: StateFlow<String?> = _runtimeBackend.asStateFlow()

    fun scheduleInitialize() {
        scope.launch { initializeIfNeeded(force = false) }
    }

    fun releaseModel() {
        scope.launch {
            mutex.withLock { releaseLocked() }
            syncStorageStatus(ModelStatus.ImportedUnverified, null)
        }
    }

    fun isReady(): Boolean = _status.value == ModelStatus.Ready

    private val completionTimeoutMs = 90_000L

    /** One-shot completion with its own short-lived conversation. */
    suspend fun complete(systemInstruction: String, userMessage: String): Result<String> {
        if (_status.value != ModelStatus.Ready) {
            return Result.failure(IllegalStateException("Gemma runtime not ready"))
        }
        return try {
            withTimeout(completionTimeoutMs) {
                runCompletion(systemInstruction, userMessage)
            }
        } catch (_: TimeoutCancellationException) {
            _lastError.value = "Gemma completion timed out"
            Result.failure(IllegalStateException("Gemma timed out after ${completionTimeoutMs / 1000}s"))
        }
    }

    private suspend fun runCompletion(systemInstruction: String, userMessage: String): Result<String> =
        withContext(dispatcher) {
            mutex.withLock {
                val eng = engine
                    ?: return@withContext Result.failure(IllegalStateException("Gemma engine not loaded"))
                var conv: Conversation? = null
                try {
                    conv = eng.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(systemInstruction),
                            samplerConfig = SamplerConfig(topK = 24, topP = 0.85, temperature = 0.15),
                        ),
                    )
                    val reply = StringBuilder()
                    conv.sendMessageAsync(userMessage).collect { chunk ->
                        reply.append(chunk.toString())
                        if (reply.length > 4_000) {
                            conv?.cancelProcess()
                        }
                    }
                    Result.success(reply.toString())
                } catch (e: Exception) {
                    _lastError.value = e.message
                    Result.failure(e)
                } finally {
                    try {
                        conv?.cancelProcess()
                    } catch (_: Exception) {
                    }
                    try {
                        conv?.close()
                    } catch (_: Exception) {
                    }
                }
            }
        }

    suspend fun close() {
        withContext(dispatcher) {
            mutex.withLock { releaseLocked() }
        }
        scope.cancel()
        executor.shutdown()
    }

    suspend fun initializeIfNeeded(force: Boolean) = mutex.withLock {
        val path = modelStorage.gemmaModelAbsolutePath()
        if (path == null) {
            releaseLocked()
            _status.value = ModelStatus.NotInstalled
            syncStorageStatus(ModelStatus.NotInstalled, null)
            return
        }
        if (!force && _status.value == ModelStatus.Ready && loadedPath == path) return
        if (!force && loadedPath == path && _status.value == ModelStatus.Loading) return

        releaseLocked()
        _status.value = ModelStatus.Loading
        syncStorageStatus(ModelStatus.Loading, null)
        _lastError.value = null

        if (!File(path).exists()) {
            _status.value = ModelStatus.Error
            syncStorageStatus(ModelStatus.Error, "Model file missing on disk")
            return
        }

        val backendCandidates = listOf(
            Backend.GPU() to "GPU (Adreno)",
            Backend.CPU() to "CPU (XNNPACK)",
        )
        var lastFailure: Exception? = null
        for ((backend, label) in backendCandidates) {
            try {
                val config = EngineConfig(
                    modelPath = path,
                    backend = backend,
                    cacheDir = context.cacheDir.absolutePath,
                )
                val newEngine = Engine(config)
                newEngine.initialize()
                engine = newEngine
                loadedPath = path
                _runtimeBackend.value = label
                _status.value = ModelStatus.Ready
                syncStorageStatus(ModelStatus.Ready, null)
                return
            } catch (e: Exception) {
                releaseLocked()
                lastFailure = e
            }
        }
        _runtimeBackend.value = null
        _lastError.value = lastFailure?.message
        _status.value = ModelStatus.Error
        syncStorageStatus(
            ModelStatus.Error,
            lastFailure?.message ?: "LiteRT-LM initialize failed (GPU and CPU)",
        )
    }

    private fun releaseLocked() {
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = null
        loadedPath = null
        _runtimeBackend.value = null
    }

    private fun syncStorageStatus(status: ModelStatus, error: String?) {
        modelStorage.updateRuntimeStatus(ModelType.GEMMA, status, error)
    }
}