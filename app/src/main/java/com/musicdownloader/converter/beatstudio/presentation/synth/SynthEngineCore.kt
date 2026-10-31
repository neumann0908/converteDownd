package com.musicdownloader.converter.beatstudio.presentation.synth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

object SynthEngineCore {
    private const val SAMPLE_RATE = 44100 // Calidad de audio estándar de CD

    // 🎵 Función principal para disparar una nota musical desde cero con matemáticas
    fun playTone(frequency: Float, durationMs: Long, patch: SynthPatch, volume: Float, pan: Float) {
        Thread {
            try {
                val numSamples = ((durationMs * SAMPLE_RATE) / 1000).toInt()
                val buffer = ShortArray(numSamples)
                
                val attackSamples = ((patch.adsr.attackMs * SAMPLE_RATE) / 1000).toInt()
                val decaySamples = ((patch.adsr.decayMs * SAMPLE_RATE) / 1000).toInt()
                val releaseSamples = ((patch.adsr.releaseMs * SAMPLE_RATE) / 1000).toInt()
                val sustainLevel = patch.adsr.sustainLevel

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    
                    // 1. Dibujar la onda matemática según el tipo elegido (Senoidal, Sierra, Cuadrada, Triangular)
                    val rawSample = when (patch.waveform) {
                        WaveformType.SINE -> sin(2.0 * PI * frequency * t).toFloat()
                        WaveformType.SAW -> (2.0 * (t * frequency - kotlin.math.floor(0.5 + t * frequency))).toFloat()
                        WaveformType.SQUARE -> if (sin(2.0 * PI * frequency * t) >= 0) 1.0f else -1.0f
                        WaveformType.TRIANGLE -> (2.0 * kotlin.math.abs(2.0 * (t * frequency - kotlin.math.floor(t * frequency + 0.5))) - 1.0f).toFloat()
                    }

                    // 2. Aplicar la envolvente ADSR (El ciclo de vida del volumen de la nota)
                    val envelope = when {
                        i < attackSamples -> if (attackSamples > 0) i.toFloat() / attackSamples else 1f
                        i < attackSamples + decaySamples -> {
                            val decayProgress = (i - attackSamples).toFloat() / decaySamples
                            1.0f - (1.0f - sustainLevel) * decayProgress
                        }
                        i > numSamples - releaseSamples -> {
                            val releaseProgress = (numSamples - i).toFloat() / releaseSamples
                            sustainLevel * releaseProgress
                        }
                        else -> sustainLevel
                    }

                    val sampleVal = (rawSample * envelope * 15000f * volume).toInt().coerceIn(-32768, 32767)
                    buffer[i] = sampleVal.toShort()
                }

                // 3. Configurar el canal de audio físico de Android para reproducirlo al instante
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
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                // 4. Calcular el Paneo Estéreo (Izquierda / Derecha)
                val stereoBuffer = ShortArray(numSamples * 2)
                val leftFactor = (1.0f - pan.coerceAtLeast(0.0f))
                val rightFactor = (1.0f + pan.coerceAtMost(0.0f))

                for (i in 0 until numSamples) {
                    stereoBuffer[i * 2] = (buffer[i] * leftFactor).toInt().toShort()
                    stereoBuffer[i * 2 + 1] = (buffer[i] * rightFactor).toInt().toShort()
                }

                audioTrack.write(stereoBuffer, 0, stereoBuffer.size)
                audioTrack.setStereoVolume(leftFactor, rightFactor)
                audioTrack.play()
                
                Thread.sleep(durationMs)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
