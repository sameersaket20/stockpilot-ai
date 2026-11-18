package com.stockpilot.ai.speech

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/** Documents silence-detection tuning (rms on 16-bit PCM). */
class AudioRecorderSilenceTest {
    @Test
    fun rms_distinguishesQuietFromSpeech() {
        val quiet = ShortArray(160) { 30 }
        val loud = ShortArray(160) { 2500 }
        val quietRms = rms(quiet)
        val loudRms = rms(loud)
        assertTrue(quietRms < 550.0)
        assertTrue(loudRms > 550.0)
    }

    private fun rms(samples: ShortArray): Double {
        var sum = 0.0
        for (s in samples) {
            val v = s.toDouble()
            sum += v * v
        }
        return sqrt(sum / samples.size)
    }
}