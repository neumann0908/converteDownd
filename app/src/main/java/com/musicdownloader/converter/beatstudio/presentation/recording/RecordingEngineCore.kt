package com.musicdownloader.converter.beatstudio.presentation.recording

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object RecordingEngineCore {
    private const val TAG = "RecordingEngineCore"
    private const val SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File, onAmplitudeUpdate: (Float) -> Unit) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord no se pudo inicializar")
            return
        }

        isRecording = true
        audioRecord?.startRecording()

        recordingThread = Thread({
            val buffer = ByteArray(bufferSize)
            FileOutputStream(outputFile).use { fos ->
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        fos.write(buffer, 0, readSize)
                        
                        // Calculamos la fuerza del sonido para mover el vúmetro
                        val amplitude = calculateRms(buffer, readSize)
                        onAmplitudeUpdate(amplitude)
                    }
                }
            }
        }, "AudioRecordingThread")

        recordingThread?.start()
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread?.join()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener la grabación", e)
        }
    }

    private fun calculateRms(buffer: ByteArray, size: Int): Float {
        var sum = 0.0
        val shorts = ByteBuffer.wrap(buffer, 0, size).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val numShorts = shorts.remaining()
        for (i in 0 until numShorts) {
            val sample = shorts.get(i) / 32768.0f
            sum += (sample * sample)
        }
        return kotlin.math.sqrt(sum / numShorts).toFloat().coerceIn(0.0f, 1.0f)
    }
}
