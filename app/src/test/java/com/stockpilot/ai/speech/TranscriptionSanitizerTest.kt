package com.stockpilot.ai.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptionSanitizerTest {
    @Test
    fun rejects_blank_audio_variants() {
        assertNull(TranscriptionSanitizer.acceptOrNull("[BLANK_AUDIO]"))
        assertNull(TranscriptionSanitizer.acceptOrNull("  [ blank audio ]  "))
        assertNull(TranscriptionSanitizer.acceptOrNull("BLANK_AUDIO"))
    }

    @Test
    fun keeps_real_phrases() {
        assertEquals(
            "What is out of stock?",
            TranscriptionSanitizer.acceptOrNull("  What is out of stock?  "),
        )
    }
}