package com.stockpilot.ai.ui.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatResponseTimingTest {
    @Test
    fun formatDurationMs_secondsAndMinutes() {
        assertEquals("12s", ChatResponseTiming.formatDurationMs(12_500))
        assertEquals("1m 5s", ChatResponseTiming.formatDurationMs(65_000))
        assertEquals("2m", ChatResponseTiming.formatDurationMs(120_000))
    }

    @Test
    fun labelForCompleted_formatsAssistantLabel() {
        assertEquals("Responded in 42s", ChatResponseTiming.labelForCompleted(42_000))
    }
}