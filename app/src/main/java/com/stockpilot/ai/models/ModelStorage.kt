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
import java.io.File

private val Context.modelPrefs: DataStore<Preferences> by preferencesDataStore("model_prefs")

class ModelStorage(private val context: Context) {
    private val mutex = Mutex()

    private val gemmaStatus = MutableStateFlow(ModelStatus.NotInstalled)
    private val whisperStatus = MutableStateFlow(ModelStatus.NotInstalled)

    private val gemmaManifest = MutableStateFlow<ModelManifest?>(null)
    private val whisperManifest = MutableStateFlow<ModelManifest?>(null)

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
                    setStatus(type, ModelStatus.ImportedUnverified)
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