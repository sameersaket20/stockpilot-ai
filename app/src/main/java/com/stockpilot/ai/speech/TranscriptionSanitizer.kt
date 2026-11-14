package com.stockpilot.ai.speech

/**
 * Whisper often hallucinates on silence or near-silent clips (e.g. [BLANK_AUDIO]).
 */
object TranscriptionSanitizer {
    private val blankAudioRegex =
        Regex("""^\[?\s*blank[_\s-]*audio\s*\]?\.?$""", RegexOption.IGNORE_CASE)

    private val noiseHallucinations = listOf(
        Regex("""^thanks for watching\.?$""", RegexOption.IGNORE_CASE),
        Regex("""^subscribe\.?$""", RegexOption.IGNORE_CASE),
        Regex("""^you\.?$""", RegexOption.IGNORE_CASE),
    )

    fun acceptOrNull(raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val compact = text.replace(Regex("\\s+"), " ")
        if (blankAudioRegex.matches(compact)) return null
        if (compact.equals("[BLANK_AUDIO]", ignoreCase = true)) return null
        if (noiseHallucinations.any { it.matches(compact) }) return null
        return compact
    }
}