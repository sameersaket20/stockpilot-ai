package com.stockpilot.ai.models

import org.json.JSONArray
import org.json.JSONObject

enum class ModelType(val id: String, val displayName: String, val description: String) {
    GEMMA("gemma", "Gemma 4 E2B", "Language model"),
    WHISPER("whisper", "Whisper Tiny.en", "Speech recognition"),
}

data class ModelFileEntry(
    val relativePath: String,
    val sizeBytes: Long,
    val mimeType: String? = null,
)

data class ModelManifest(
    val type: String,
    val importedAtEpochMs: Long,
    val originalDisplayName: String,
    val files: List<ModelFileEntry>,
    val notes: String? = null,
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("type", type)
        root.put("importedAtEpochMs", importedAtEpochMs)
        root.put("originalDisplayName", originalDisplayName)
        root.put("notes", notes)
        val filesArr = JSONArray()
        files.forEach { entry ->
            val obj = JSONObject()
            obj.put("relativePath", entry.relativePath)
            obj.put("sizeBytes", entry.sizeBytes)
            entry.mimeType?.let { obj.put("mimeType", it) }
            filesArr.put(obj)
        }
        root.put("files", filesArr)
        return root.toString()
    }

    companion object {
        fun fromJson(raw: String): ModelManifest? = try {
            val root = JSONObject(raw)
            val files = mutableListOf<ModelFileEntry>()
            val arr = root.optJSONArray("files") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                files += ModelFileEntry(
                    relativePath = obj.getString("relativePath"),
                    sizeBytes = obj.getLong("sizeBytes"),
                    mimeType = obj.optString("mimeType").ifBlank { null },
                )
            }
            ModelManifest(
                type = root.getString("type"),
                importedAtEpochMs = root.getLong("importedAtEpochMs"),
                originalDisplayName = root.getString("originalDisplayName"),
                files = files,
                notes = root.optString("notes").ifBlank { null },
            )
        } catch (_: Exception) {
            null
        }
    }
}

data class ModelUiState(
    val type: ModelType,
    val status: com.stockpilot.ai.inference.ModelStatus,
    val manifest: ModelManifest? = null,
    val errorMessage: String? = null,
    val importProgress: Float? = null,
)