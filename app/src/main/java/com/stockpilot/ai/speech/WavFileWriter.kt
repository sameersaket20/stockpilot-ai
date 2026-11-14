package com.stockpilot.ai.speech

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes 16-bit PCM mono WAV files (Whisper expects ~16 kHz mono).
 */
class WavFileWriter(
    file: File,
    private val sampleRate: Int = 16_000,
    private val channels: Int = 1,
) {
    private val raf = RandomAccessFile(file, "rw")
    private var pcmBytesWritten = 0

    val bytesWritten: Int get() = pcmBytesWritten

    init {
        raf.setLength(0)
        raf.write(ByteArray(HEADER_SIZE))
    }

    fun writePcm16(samples: ShortArray, sampleCount: Int) {
        val buffer = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            buffer.putShort(samples[i])
        }
        raf.write(buffer.array())
        pcmBytesWritten += sampleCount * 2
    }

    fun finish() {
        raf.seek(0)
        raf.write(buildWavHeader(pcmBytesWritten, sampleRate, channels))
        raf.close()
    }

    companion object {
        private const val HEADER_SIZE = 44
        private const val BITS_PER_SAMPLE = 16

        private fun buildWavHeader(dataSize: Int, sampleRate: Int, channels: Int): ByteArray {
            val byteRate = sampleRate * channels * BITS_PER_SAMPLE / 8
            val blockAlign = channels * BITS_PER_SAMPLE / 8
            val chunkSize = 36 + dataSize
            val buffer = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray())
            buffer.putInt(chunkSize)
            buffer.put("WAVE".toByteArray())
            buffer.put("fmt ".toByteArray())
            buffer.putInt(16)
            buffer.putShort(1)
            buffer.putShort(channels.toShort())
            buffer.putInt(sampleRate)
            buffer.putInt(byteRate)
            buffer.putShort(blockAlign.toShort())
            buffer.putShort(BITS_PER_SAMPLE.toShort())
            buffer.put("data".toByteArray())
            buffer.putInt(dataSize)
            return buffer.array()
        }
    }
}