package com.musicdownloader.converter.beatstudio.presentation.recording

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object RecordingEngineCore {
    private const val TAG = "RecordingEngineCore"
    private const val SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    // --- Control de Metrónomo y Tempo ---
    var currentBpm: Int = 120
        set(value) {
            field = value.coerceIn(40, 240)
        }
    
    private var metronomeThread: Thread? = null
    private var isMetronomeActive = false

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
        stopMetronome()
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread?.join()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener la grabación", e)
        }
    }

    // --- Sistema de Metrónomo Sincronizado ---
    fun startMetronome(onBeat: (Int) -> Unit) {
        if (isMetronomeActive) return
        isMetronomeActive = true

        metronomeThread = Thread({
            var beatCount = 0
            while (isMetronomeActive) {
                val intervalMs = (60000 / currentBpm).toLong()
                
                // El primer tiempo del compás (4/4) tiene un tono más agudo (accent)
                val isAccent = (beatCount % 4 == 0)
                playClickSound(if (isAccent) 1200.0 else 800.0, 50)
                
                onBeat((beatCount % 4) + 1)
                beatCount++

                try {
                    Thread.sleep(intervalMs)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }, "MetronomeThread")

        metronomeThread?.start()
    }

    fun stopMetronome() {
        isMetronomeActive = false
        metronomeThread?.interrupt()
        metronomeThread = null
    }

    // Generador de tono sintético en tiempo real para el clic de metrónomo
    private fun playClickSound(frequency: Double, durationMs: Int) {
        try {
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val sample = ByteArray(numSamples * 2)
            var idx = 0
            for (i in 0 until numSamples) {
                val angle = i * (2.0 * Math.PI * frequency / SAMPLE_RATE)
                val value = (sin(angle) * 32767.0).toInt().toShort()
                sample[idx++] = (value.toInt() and 0x00ff).toByte()
                sample[idx++] = ((value.toInt() and 0xff00) ushr 8).toByte()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(sample.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(sample, 0, sample.size)
            audioTrack.play()
            
            Thread.sleep(durationMs.toLong() + 10)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error reproduciendo clic de metrónomo", e)
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
