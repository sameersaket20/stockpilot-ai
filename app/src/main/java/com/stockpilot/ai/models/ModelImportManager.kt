package com.stockpilot.ai.models

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.stockpilot.ai.inference.ModelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ModelImportManager(
    private val context: Context,
    private val storage: ModelStorage,
) {
    private val _importProgress = MutableStateFlow<Pair<ModelType, Float>?>(null)
    val importProgress: StateFlow<Pair<ModelType, Float>?> = _importProgress.asStateFlow()

    suspend fun importDocument(type: ModelType, uri: Uri): Result<ModelManifest> = withContext(Dispatchers.IO) {
        if (storage.availableBytes() < MIN_FREE_BYTES) {
            return@withContext Result.failure(IllegalStateException("Not enough free storage for import."))
        }
        val displayName = queryDisplayName(uri) ?: "unknown"
        val validation = validateBasic(type, displayName, uri)
        if (validation != null) {
            return@withContext Result.failure(IllegalArgumentException(validation))
        }

        val temp = storage.tempImportFile(type)
        var bytesCopied = 0L
        try {
            _importProgress.value = type to 0f
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        _importProgress.value = type to (bytesCopied % 1000) / 1000f
                    }
                    output.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open selected file."))

            if (bytesCopied <= 0) {
                temp.delete()
                return@withContext Result.failure(IllegalArgumentException("Selected file is empty."))
            }

            val finalName = safeFileName(displayName)
            val destDir = storage.directoryFor(type)
            destDir.listFiles()?.forEach { it.deleteRecursively() }
            val destFile = File(destDir, finalName)
            if (!temp.renameTo(destFile)) {
                temp.inputStream().use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                temp.delete()
            }

            val manifest = ModelManifest(
                type = type.id,
                importedAtEpochMs = System.currentTimeMillis(),
                originalDisplayName = displayName,
                files = listOf(
                    ModelFileEntry(
                        relativePath = "${type.id}/$finalName",
                        sizeBytes = destFile.length(),
                    ),
                ),
                notes = "Imported for future Phase 2 integration — not verified for inference.",
            )
            storage.commitManifest(type, manifest)
            Result.success(manifest)
        } catch (e: Exception) {
            temp.delete()
            Result.failure(e)
        } finally {
            _importProgress.value = null
        }
    }

    suspend fun cancelActiveImport(type: ModelType) {
        File(context.cacheDir, "model_imports").listFiles()
            ?.filter { it.name.startsWith(type.id) }
            ?.forEach { it.delete() }
        _importProgress.value = null
    }

    fun runtimeStatus(type: ModelType): ModelStatus = storage.status(type).value

    private fun validateBasic(type: ModelType, name: String, uri: Uri): String? {
        val lower = name.lowercase()
        return when (type) {
            ModelType.WHISPER -> {
                if (!lower.endsWith(".bin") && !lower.endsWith(".ggml") && !lower.contains("whisper")) {
                    "Whisper import expects a .ggml/.bin model file (e.g. ggml-tiny.en.bin)."
                } else {
                    null
                }
            }
            ModelType.GEMMA -> {
                if (lower.endsWith(".json") || lower.endsWith(".zip") || lower.contains("gemma") || lower.endsWith(".litertlm")) {
                    null
                } else {
                    "Gemma/LiteRT packages vary; select the main model archive or weights file you transferred."
                }
            }
        }.let { msg ->
            if (msg != null) msg else {
                val size = querySize(uri)
                if (size != null && size < 1024) "File looks too small to be a model." else null
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) {
                return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }

    private fun querySize(uri: Uri): Long? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && index >= 0) {
                return cursor.getLong(index)
            }
        }
        return null
    }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)

    companion object {
        private const val MIN_FREE_BYTES = 50L * 1024 * 1024
    }
}