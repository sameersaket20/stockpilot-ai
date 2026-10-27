package com.stockpilot.ai.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelManifestTest {
    @Test
    fun jsonRoundTrip() {
        val manifest = ModelManifest(
            type = "whisper",
            importedAtEpochMs = 1_700_000_000_000,
            originalDisplayName = "ggml-tiny.en.bin",
            files = listOf(ModelFileEntry("whisper/ggml-tiny.en.bin", 77_000_000)),
            notes = "Phase 1 import",
        )
        val parsed = ModelManifest.fromJson(manifest.toJson())
        assertNotNull(parsed)
        assertEquals(manifest.originalDisplayName, parsed?.originalDisplayName)
        assertEquals(manifest.files.single().sizeBytes, parsed?.files?.single()?.sizeBytes)
    }
}