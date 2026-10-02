package com.stockpilot.ai.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class RecordingState {
    data object Idle : RecordingState()
    data class Recording(val outputFile: File) : RecordingState()
    data class Finished(val outputFile: File) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

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
            val output = File(context.cacheDir, "recording_${System.currentTimeMillis()}.3gp")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setAudioSamplingRate(16_000)
            recorder.setAudioChannels(1)
            recorder.setOutputFile(output.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
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
            stopInternal(discard = false)
            _state.value = RecordingState.Finished(current.outputFile)
            current.outputFile
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

    private fun stopInternal(discard: Boolean) {
        val recorder = mediaRecorder ?: return
        try {
            recorder.stop()
        } catch (_: Exception) {
        }
        try {
            recorder.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        if (discard) {
            val current = _state.value
            if (current is RecordingState.Recording) {
                current.outputFile.delete()
            }
        }
    }
}