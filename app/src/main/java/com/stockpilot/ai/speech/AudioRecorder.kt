package com.stockpilot.ai.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import kotlin.math.sqrt

sealed class RecordingState {
    data object Idle : RecordingState()
    data class Recording(val outputFile: File) : RecordingState()
    data class Finished(val outputFile: File) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var audioRecord: AudioRecord? = null
    private var wavWriter: WavFileWriter? = null
    @Volatile
    private var capturing = false
    @Volatile
    private var endAfterSilence = false
    private var captureThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    /** RMS alone can stay high from room hiss — peak confirms voice. */
    private val speechRmsThreshold = 550.0
    private val speechPeakThreshold = 900

    /** Stop after this much quiet time once speech was detected. */
    private val silenceStopMs = 2_000L

    /** Failsafe if the user never speaks. */
    private val noSpeechTimeoutMs = 15_000L

    /** Do not auto-stop until at least this much audio is captured. */
    private val minRecordMs = 900L

    /** Hard cap so recording cannot run indefinitely. */
    private val maxRecordMs = 45_000L

    /** ~0.35 s of 16 kHz mono 16-bit PCM — below this we skip Whisper. */
    private val minPcmBytes = (16_000 * 0.35 * 2).toInt()

    @Volatile
    private var speechDetectedInSession = false

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun start(): File? {
        if (!hasMicPermission()) {
            _state.value = RecordingState.Error("Microphone permission not granted")
            return null
        }
        return try {
            stopInternal(discard = true)
            endAfterSilence = false
            speechDetectedInSession = false
            val output = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
            val sampleRate = 16_000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
            if (minBuffer <= 0) {
                _state.value = RecordingState.Error("Microphone not supported at 16 kHz")
                return null
            }
            val bufferSize = minBuffer * 2
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                encoding,
                bufferSize,
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                _state.value = RecordingState.Error("Failed to initialize microphone")
                return null
            }
            val writer = WavFileWriter(output, sampleRate, channels = 1)
            audioRecord = recorder
            wavWriter = writer
            capturing = true
            val recordStartMs = System.currentTimeMillis()
            var heardSpeech = false
            var lastSpeechMs = recordStartMs
            recorder.startRecording()
            captureThread = Thread {
                val buffer = ShortArray(bufferSize / 2)
                try {
                    while (capturing) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        val now = System.currentTimeMillis()
                        val elapsed = now - recordStartMs
                        if (elapsed >= maxRecordMs) {
                            endAfterSilence = true
                            capturing = false
                            break
                        }
                        if (read > 0) {
                            try {
                                writer.writePcm16(buffer, read)
                            } catch (_: Exception) {
                                break
                            }
                            val rms = rmsPcm16(buffer, read)
                            val peak = peakPcm16(buffer, read)
                            val isSpeech =
                                rms >= speechRmsThreshold || peak >= speechPeakThreshold
                            if (isSpeech) {
                                heardSpeech = true
                                speechDetectedInSession = true
                                lastSpeechMs = now
                            }
                            if (
                                heardSpeech &&
                                elapsed >= minRecordMs &&
                                now - lastSpeechMs >= silenceStopMs
                            ) {
                                endAfterSilence = true
                                capturing = false
                                break
                            }
                            if (!heardSpeech && elapsed >= noSpeechTimeoutMs) {
                                endAfterSilence = true
                                capturing = false
                                break
                            }
                        } else if (read == 0) {
                            Thread.sleep(10)
                        }
                    }
                } finally {
                    if (endAfterSilence) {
                        finalizeRecordingFromCaptureThread(recorder, writer, output)
                    }
                }
            }.apply { name = "stockpilot-audio-capture"; start() }
            _state.value = RecordingState.Recording(output)
            output
        } catch (e: Exception) {
            _state.value = RecordingState.Error(e.message ?: "Failed to start recording")
            null
        }
    }

    fun stop(): File? {
        val current = _state.value
        if (current !is RecordingState.Recording) return null
        return try {
            val output = current.outputFile
            val pcmBytes = stopInternal(discard = false)
            publishFinishedOrNoSpeech(output, pcmBytes)
        } catch (e: Exception) {
            _state.value = RecordingState.Error(e.message ?: "Failed to stop recording")
            null
        }
    }

    fun cancel() {
        stopInternal(discard = true)
        _state.value = RecordingState.Idle
    }

    fun resetIdle() {
        _state.value = RecordingState.Idle
    }

    private fun finalizeRecordingFromCaptureThread(
        recorder: AudioRecord,
        writer: WavFileWriter,
        output: File,
    ) {
        try {
            recorder.stop()
        } catch (_: Exception) {
        }
        try {
            recorder.release()
        } catch (_: Exception) {
        }
        val pcmBytes = writer.bytesWritten
        try {
            writer.finish()
        } catch (_: Exception) {
        }
        audioRecord = null
        wavWriter = null
        captureThread = null
        mainHandler.post {
            if (_state.value is RecordingState.Recording) {
                publishFinishedOrNoSpeech(output, pcmBytes)
            }
        }
    }

    private fun publishFinishedOrNoSpeech(output: File, pcmBytes: Int): File? {
        if (!speechDetectedInSession || pcmBytes < minPcmBytes) {
            output.delete()
            _state.value = RecordingState.Error(
                "No speech detected. Tap the mic, wait for the indicator, then speak clearly.",
            )
            return null
        }
        _state.value = RecordingState.Finished(output)
        return output
    }

    private fun stopInternal(discard: Boolean): Int {
        capturing = false
        captureThread?.join(2_000)
        captureThread = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
        val writer = wavWriter
        wavWriter = null
        val pcmBytes = writer?.bytesWritten ?: 0
        val current = _state.value
        if (!discard && writer != null) {
            try {
                writer.finish()
            } catch (_: Exception) {
            }
        } else if (discard) {
            if (current is RecordingState.Recording) {
                current.outputFile.delete()
            }
        }
        return pcmBytes
    }

    private fun rmsPcm16(samples: ShortArray, count: Int): Double {
        if (count <= 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) {
            val s = samples[i].toDouble()
            sum += s * s
        }
        return sqrt(sum / count)
    }

    private fun peakPcm16(samples: ShortArray, count: Int): Int {
        if (count <= 0) return 0
        var peak = 0
        for (i in 0 until count) {
            val amp = kotlin.math.abs(samples[i].toInt())
            if (amp > peak) peak = amp
        }
        return peak
    }
}