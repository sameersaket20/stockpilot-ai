package com.stockpilot.ai.models

import android.content.Context
import android.os.StatFs
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stockpilot.ai.inference.ModelStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.modelPrefs: DataStore<Preferences> by preferencesDataStore("model_prefs")

@Singleton
class ModelStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()

    private val gemmaStatus = MutableStateFlow(ModelStatus.NotInstalled)
    private val whisperStatus = MutableStateFlow(ModelStatus.NotInstalled)

    private val gemmaManifest = MutableStateFlow<ModelManifest?>(null)
    private val whisperManifest = MutableStateFlow<ModelManifest?>(null)
    private val gemmaRuntimeError = MutableStateFlow<String?>(null)
    private val whisperRuntimeError = MutableStateFlow<String?>(null)

    fun status(type: ModelType): StateFlow<ModelStatus> = when (type) {
        ModelType.GEMMA -> gemmaStatus.asStateFlow()
        ModelType.WHISPER -> whisperStatus.asStateFlow()
    }

    fun manifest(type: ModelType): StateFlow<ModelManifest?> = when (type) {
        ModelType.GEMMA -> gemmaManifest.asStateFlow()
        ModelType.WHISPER -> whisperManifest.asStateFlow()
    }

    suspend fun refreshFromDisk() = mutex.withLock {
        ModelType.entries.forEach { type ->
            val manifest = readManifest(type)
            val filesOk = manifest?.files?.all { File(modelsRoot(), it.relativePath).exists() } == true
            when {
                manifest == null || manifest.files.isEmpty() || !filesOk -> {
                    setStatus(type, ModelStatus.NotInstalled)
                    setManifest(type, null)
                }
                else -> {
                    setManifest(type, manifest)
                    val keepRuntime = when (type) {
                        ModelType.GEMMA -> gemmaStatus.value in setOf(
                            ModelStatus.Ready,
                            ModelStatus.Loading,
                            ModelStatus.Error,
                        )
                        ModelType.WHISPER -> whisperStatus.value in setOf(
                            ModelStatus.Ready,
                            ModelStatus.Loading,
                            ModelStatus.Error,
                        )
                    }
                    if (!keepRuntime) {
                        setStatus(type, ModelStatus.ImportedUnverified)
                    }
                }
            }
        }
    }

    fun directoryFor(type: ModelType): File {
        val dir = File(modelsRoot(), type.id)
        dir.mkdirs()
        return dir
    }

    fun modelsRoot(): File = File(context.filesDir, "models").also { it.mkdirs() }

    fun tempImportFile(type: ModelType): File {
        val tmpDir = File(context.cacheDir, "model_imports").also { it.mkdirs() }
        return File(tmpDir, "${type.id}_${System.currentTimeMillis()}.part")
    }

    suspend fun commitManifest(type: ModelType, manifest: ModelManifest) = mutex.withLock {
        context.modelPrefs.edit { prefs ->
            prefs[manifestKey(type)] = manifest.toJson()
        }
        setManifest(type, manifest)
        setStatus(type, ModelStatus.ImportedUnverified)
    }

    suspend fun removeModel(type: ModelType) = mutex.withLock {
        directoryFor(type).listFiles()?.forEach { it.deleteRecursively() }
        context.modelPrefs.edit { it.remove(manifestKey(type)) }
        setManifest(type, null)
        setStatus(type, ModelStatus.NotInstalled)
    }

    fun availableBytes(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBytes
    }

    fun gemmaModelAbsolutePath(): String? = modelAbsolutePath(ModelType.GEMMA)

    fun whisperModelAbsolutePath(): String? = modelAbsolutePath(ModelType.WHISPER)

    private fun modelAbsolutePath(type: ModelType): String? {
        val manifest = when (type) {
            ModelType.GEMMA -> gemmaManifest.value
            ModelType.WHISPER -> whisperManifest.value
        } ?: return null
        val entry = manifest.files.firstOrNull() ?: return null
        val file = File(modelsRoot(), entry.relativePath)
        return file.takeIf { it.exists() && it.length() > 0 }?.absolutePath
    }

    fun updateRuntimeStatus(type: ModelType, status: ModelStatus, errorMessage: String?) {
        setStatus(type, status)
        when (type) {
            ModelType.GEMMA ->
                gemmaRuntimeError.value = if (status == ModelStatus.Error) errorMessage else null
            ModelType.WHISPER ->
                whisperRuntimeError.value = if (status == ModelStatus.Error) errorMessage else null
        }
    }

    fun gemmaRuntimeError(): StateFlow<String?> = gemmaRuntimeError.asStateFlow()

    fun whisperRuntimeError(): StateFlow<String?> = whisperRuntimeError.asStateFlow()

    private suspend fun readManifest(type: ModelType): ModelManifest? {
        val raw = context.modelPrefs.data.first()[manifestKey(type)] ?: return null
        return ModelManifest.fromJson(raw)
    }

    private fun manifestKey(type: ModelType) = stringPreferencesKey("manifest_${type.id}")

    private fun setStatus(type: ModelType, status: ModelStatus) {
        when (type) {
            ModelType.GEMMA -> gemmaStatus.value = status
            ModelType.WHISPER -> whisperStatus.value = status
        }
    }

    private fun setManifest(type: ModelType, manifest: ModelManifest?) {
        when (type) {
            ModelType.GEMMA -> gemmaManifest.value = manifest
            ModelType.WHISPER -> whisperManifest.value = manifest
        }
    }
}