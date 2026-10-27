package com.stockpilot.ai.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class TtsStatus(
    val initialized: Boolean = false,
    val available: Boolean = false,
    val offlineCapable: Boolean = false,
    val warning: String? = null,
    val muted: Boolean = false,
    val speaking: Boolean = false,
)

class AndroidTtsManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val _status = MutableStateFlow(TtsStatus())
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)

    fun initialize() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { result ->
            val engine = tts
            if (result != TextToSpeech.SUCCESS || engine == null) {
                _status.value = TtsStatus(
                    initialized = true,
                    available = false,
                    warning = "Text-to-speech engine unavailable on this device.",
                )
                return@TextToSpeech
            }
            engine.language = Locale.US
            val langResult = engine.isLanguageAvailable(Locale.US)
            val offline = langResult >= TextToSpeech.LANG_AVAILABLE
            val warning = if (offline) {
                null
            } else {
                "Offline US voice may not be installed; responses stay on screen."
            }
            ready.set(true)
            _status.value = TtsStatus(
                initialized = true,
                available = true,
                offlineCapable = offline,
                warning = warning,
                muted = _status.value.muted,
            )
            engine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _status.value = _status.value.copy(speaking = true)
                    }

                    override fun onDone(utteranceId: String?) {
                        _status.value = _status.value.copy(speaking = false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _status.value = _status.value.copy(speaking = false)
                    }
                },
            )
        }
    }

    fun setMuted(muted: Boolean) {
        _status.value = _status.value.copy(muted = muted)
        if (muted) stop()
    }

    fun speak(text: String) {
        if (_status.value.muted || !ready.get()) return
        val clean = text.lines().joinToString(" ") { it.trim() }.take(4000)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "stockpilot_${System.nanoTime()}")
    }

    fun stop() {
        tts?.stop()
        _status.value = _status.value.copy(speaking = false)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready.set(false)
    }
}