package com.musicdownloader.converter.beatstudio.presentation.synth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

object SynthEngineCore {
    private const val SAMPLE_RATE = 44100 // Calidad de audio estándar de CD

    // 🎵 Función principal para disparar notas con DSP, Filtro y ADSR reactivo
    fun playTone(frequency: Float, durationMs: Long, patch: SynthPatch, volume: Float, pan: Float) {
        Thread {
            try {
                // Duración mínima garantizada de 600ms para apreciar perfectamente el ADSR y Release
                val effectiveDurationMs = durationMs.coerceAtLeast(600L)
                val numSamples = ((effectiveDurationMs * SAMPLE_RATE) / 1000).toInt()
                val buffer = ShortArray(numSamples)
                
                val attackSamples = ((patch.adsr.attackMs * SAMPLE_RATE) / 1000).toInt().coerceAtLeast(1)
                val decaySamples = ((patch.adsr.decayMs * SAMPLE_RATE) / 1000).toInt().coerceAtLeast(1)
                val releaseSamples = ((patch.adsr.releaseMs * SAMPLE_RATE) / 1000).toInt().coerceAtLeast(1)
                val sustainLevel = patch.adsr.sustainLevel.coerceIn(0.0f, 1.0f)

                // 🎛️ Configuración del Filtro Pasa-Bajos (Low-Pass Filter) con el Cutoff del Patch
                val cutoff = patch.cutoffFreq.coerceIn(100f, 20000f)
                val dt = 1.0f / SAMPLE_RATE
                val rc = 1.0f / (2.0f * PI.toFloat() * cutoff)
                val alpha = dt / (rc + dt)
                var filteredSample = 0.0f

                val releaseStartSample = (numSamples - releaseSamples).coerceAtLeast(0)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    
                    // 🥁 Efecto dinámico de caída de frecuencia (Pitch Drop) exclusivo para el 808 Synth
                    val currentFreq = if (patch.instrumentType == SynthInstrumentType.SYNTH_808 && i < SAMPLE_RATE * 0.12f) {
                        frequency + (80f * (1.0f - (i / (SAMPLE_RATE * 0.12f))))
                    } else {
                        frequency
                    }

                    // 1. Generación de onda base según el instrumento y la waveform elegida
                    val rawSample = when (patch.waveform) {
                        WaveformType.SINE -> {
                            val base = sin(2.0 * PI * currentFreq * t).toFloat()
                            if (patch.instrumentType == SynthInstrumentType.SUB_BASS) {
                                base * 0.7f + sin(2.0 * PI * (currentFreq / 2.0) * t).toFloat() * 0.3f
                            } else {
                                base
                            }
                        }
                        WaveformType.SAW -> (2.0 * (t * currentFreq - floor(0.5 + t * currentFreq))).toFloat()
                        WaveformType.SQUARE -> if (sin(2.0 * PI * currentFreq * t) >= 0) 1.0f else -1.0f
                        WaveformType.TRIANGLE -> (2.0 * abs(2.0 * (t * currentFreq - floor(t * currentFreq + 0.5))) - 1.0f).toFloat()
                    }

                    // 2. Aplicar el Filtro Pasa-Bajos (Modifica el brillo del sonido en tiempo real)
                    filteredSample += alpha * (rawSample - filteredSample)

                    // 3. Envolvente ADSR Precisa y Suave
                    val envelope = when {
                        i < attackSamples -> i.toFloat() / attackSamples
                        i < attackSamples + decaySamples -> {
                            val decayProgress = (i - attackSamples).toFloat() / decaySamples.coerceAtLeast(1)
                            1.0f - (1.0f - sustainLevel) * decayProgress
                        }
                        i >= releaseStartSample -> {
                            val releaseProgress = (i - releaseStartSample).toFloat() / releaseSamples.coerceAtLeast(1)
                            (sustainLevel * (1.0f - releaseProgress)).coerceAtLeast(0.0f)
                        }
                        else -> sustainLevel
                    }

                    val sampleVal = (filteredSample * envelope * 16000f * volume).toInt().coerceIn(-32768, 32767)
                    buffer[i] = sampleVal.toShort()
                }

                // 4. Configuración del canal de audio físico de Android
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

                // 5. Calcular Paneo Estéreo (Izquierda / Derecha)
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
                
                Thread.sleep(effectiveDurationMs)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
