package com.stockpilot.ai.speech

import android.content.Context
import android.os.Build
import com.stockpilot.ai.inference.ModelStatus
import com.stockpilot.ai.models.ModelStorage
import com.stockpilot.ai.models.ModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperSpeechEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelStorage: ModelStorage,
) : SpeechEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var whisperModel: WhisperModel? = null
    private var loadedModelPath: String? = null

    private val _status = MutableStateFlow(ModelStatus.NotInstalled)
    override val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun scheduleInitialize() {
        scope.launch { initializeIfNeeded(force = false) }
    }

    fun releaseModel() {
        scope.launch {
            mutex.withLock { releaseLocked() }
            syncStorage(ModelStatus.ImportedUnverified, null)
        }
    }

    override suspend fun transcribe(audioFile: File): TranscriptionResult = withContext(Dispatchers.IO) {
        if (_status.value != ModelStatus.Ready) {
            return@withContext TranscriptionResult.Unavailable("Whisper is not ready.")
        }
        val model = mutex.withLock { whisperModel }
            ?: return@withContext TranscriptionResult.Unavailable("Whisper model not loaded.")
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext TranscriptionResult.Failure("Recording file is empty.")
        }
        try {
            val wavPath = audioFile.absolutePath
            val config = WhisperConfig(language = "en", translate = false, threads = 4, maxSegmentLength = 0, printTimestamps = false)
            val result = Whisper.transcribe(model, wavPath, config)
            val text = TranscriptionSanitizer.acceptOrNull(result.text)
            if (text == null) {
                TranscriptionResult.Failure("No speech detected. Try speaking closer to the mic.")
            } else {
                TranscriptionResult.Success(text)
            }
        } catch (e: Exception) {
            TranscriptionResult.Failure(e.message ?: "Transcription failed")
        }
    }

    override suspend fun close() {
        releaseModel()
    }

    private suspend fun initializeIfNeeded(force: Boolean) {
        if (!isArm64Device()) {
            syncStorage(ModelStatus.Error, "Whisper binary requires arm64 (not available on this device/emulator).")
            return
        }
        val path = modelStorage.whisperModelAbsolutePath()
        if (path == null) {
            releaseLocked()
            syncStorage(ModelStatus.NotInstalled, null)
            return
        }
        if (!force && _status.value == ModelStatus.Ready && loadedModelPath == path) return

        mutex.withLock {
            syncStorage(ModelStatus.Loading, null)
            try {
                releaseLocked()
                val loaded = Whisper.loadModel(context, path)
                whisperModel = loaded
                loadedModelPath = path
                syncStorage(ModelStatus.Ready, null)
            } catch (e: UnsatisfiedLinkError) {
                syncStorage(ModelStatus.Error, "Whisper native library missing for this CPU ABI.")
            } catch (e: Exception) {
                releaseLocked()
                syncStorage(ModelStatus.Error, e.message ?: "Whisper init failed")
            }
        }
    }

    private fun releaseLocked() {
        whisperModel?.let { Whisper.releaseModel(it) }
        whisperModel = null
        loadedModelPath = null
    }

    private fun syncStorage(status: ModelStatus, error: String?) {
        _status.value = status
        _lastError.value = if (status == ModelStatus.Error) error else null
        modelStorage.updateRuntimeStatus(ModelType.WHISPER, status, error)
    }

    private fun isArm64Device(): Boolean {
        val abis = Build.SUPPORTED_ABIS ?: emptyArray()
        return abis.any { it.contains("arm64", ignoreCase = true) }
    }
}