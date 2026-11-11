package com.stockpilot.ai.ui.assistant

/** Formats wall-clock wait from user message posted until assistant reply saved. */
object ChatResponseTiming {
    fun formatDurationMs(elapsedMs: Long): String {
        if (elapsedMs < 0) return "0s"
        val totalSec = elapsedMs / 1000
        if (totalSec < 60) return "${totalSec}s"
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return if (seconds == 0L) "${minutes}m" else "${minutes}m ${seconds}s"
    }

    fun labelForCompleted(durationMs: Long?): String? =
        durationMs?.takeIf { it > 0 }?.let { "Responded in ${formatDurationMs(it)}" }
}