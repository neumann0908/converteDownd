package com.musicdownloader.converter.beatstudio.presentation.synth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * SynthEngineCore v4.2 (Con Polifonía Real, Voice Stealing, Unison/Detune, LFO Matrix, Portamento y FX Rack)
 *
 * Motor de síntesis DSP profesional con matriz de modulación expandida y efectos nativos.
 */
object SynthEngineCore {

    private const val TAG = "SynthEngineCore"

    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2
    private const val BYTES_PER_SAMPLE = 2

    private const val MAX_VOICES = 8
    private const val MAX_QUEUED_NOTES = 16

    private const val MAX_DURATION_MS = 10_000L

    private const val MIN_FREQUENCY = 20.0f
    private const val MAX_FREQUENCY = SAMPLE_RATE * 0.45f

    private const val MASTER_HEADROOM = 0.90f

    private const val DEFAULT_FREQUENCY = 440.0f
    private const val DEFAULT_CUTOFF = 12_000.0f

    private const val MAX_DURATION_SAMPLES =
        SAMPLE_RATE * MAX_DURATION_MS / 1000L

    // Control de ID de voces para Voice Stealing
    private val voiceIdCounter = AtomicInteger(0)

    // Estructura para rastrear voces activas en el Pool
    private data class ActiveVoice(
        val voiceId: Int,
        val audioTrack: AudioTrack,
        val timestamp: Long
    )

    private val activeVoices = mutableListOf<ActiveVoice>()

    /**
     * ThreadPool optimizado con DiscardOldestPolicy para gestión de voces.
     */
    private val audioExecutor = ThreadPoolExecutor(
        MAX_VOICES,
        MAX_VOICES,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_NOTES),
        ThreadFactory { runnable ->
            Thread(runnable, "SynthVoice-${voiceIdCounter.incrementAndGet()}").apply {
                isDaemon = true
            }
        },
        ThreadPoolExecutor.DiscardOldestPolicy()
    )

    /**
     * Sobrecarga de compatibilidad para llamadas clásicas (ej. Secuenciador).
     */
    fun playTone(
        frequency: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ) {
        playTone(
            frequency = frequency,
            startFrequency = frequency,
            portamentoMs = patch.portamentoMs,
            durationMs = durationMs,
            patch = patch,
            volume = volume,
            pan = pan
        )
    }

    /**
     * Método principal de reproducción con gestión de voces, Voice Stealing, Unison/Detune, Matriz LFO y FX Rack.
     */
    fun playTone(
        frequency: Float,
        startFrequency: Float,
        portamentoMs: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ) {
        val safeFrequency = if (frequency.isFinite()) {
            frequency.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)
        } else {
            DEFAULT_FREQUENCY
        }

        val safeStartFrequency = if (startFrequency.isFinite()) {
            startFrequency.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)
        } else {
            safeFrequency
        }

        val safePortamentoMs = if (portamentoMs.isFinite()) {
            portamentoMs.coerceIn(0f, 1000f)
        } else {
            0f
        }

        val safeDuration = durationMs.coerceAtLeast(1L)

        val safeVolume = if (volume.isFinite()) {
            volume.coerceIn(0.0f, 1.0f)
        } else {
            0.0f
        }

        val safePan = if (pan.isFinite()) {
            pan.coerceIn(-1.0f, 1.0f)
        } else {
            0.0f
        }

        if (safeVolume <= 0.0f) return

        try {
            audioExecutor.execute {
                var audioTrack: AudioTrack? = null
                val currentVoiceId = voiceIdCounter.incrementAndGet()

                try {
                    val audioData = generateAudio(
                        frequency = safeFrequency,
                        startFrequency = safeStartFrequency,
                        portamentoMs = safePortamentoMs,
                        durationMs = safeDuration,
                        patch = patch,
                        volume = safeVolume,
                        pan = safePan
                    )

                    audioTrack = createAudioTrack(audioData.size * BYTES_PER_SAMPLE)
                    if (audioTrack == null || audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                        Log.e(TAG, "AudioTrack no pudo inicializarse para la voz $currentVoiceId")
                        return@execute
                    }

                    // --- GESTIÓN DE POOL Y VOICE STEALING ---
                    synchronized(activeVoices) {
                        if (activeVoices.size >= MAX_VOICES) {
                            val oldestVoice = activeVoices.removeAt(0)
                            try {
                                if (oldestVoice.audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                    oldestVoice.audioTrack.stop()
                                }
                                oldestVoice.audioTrack.release()
                            } catch (e: Exception) {
                                Log.w(TAG, "Error al liberar voz en Voice Stealing", e)
                            }
                        }

                        activeVoices.add(ActiveVoice(currentVoiceId, audioTrack, System.currentTimeMillis()))
                    }

                    audioTrack.play()
                    audioTrack.write(audioData, 0, audioData.size)

                    Thread.sleep(safeDuration + 60L)

                } catch (exception: Exception) {
                    Log.e(TAG, "Error en ejecución de voz $currentVoiceId", exception)
                } finally {
                    audioTrack?.let { track ->
                        synchronized(activeVoices) {
                            activeVoices.removeAll { it.voiceId == currentVoiceId }
                        }
                        try {
                            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                track.stop()
                            }
                        } catch (_: Exception) {}
                        try {
                            track.release()
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Nota descartada por sobrecarga del ejecutor", exception)
        }
    }

    private fun createAudioTrack(bufferSizeBytes: Int): AudioTrack? {
        return try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val finalBufferSize = max(bufferSizeBytes, minBufferSize * 2)

            AudioTrack.Builder()
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
                .setBufferSizeInBytes(finalBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error creando AudioTrack", e)
            null
        }
    }

    private fun generateAudio(
        frequency: Float,
        startFrequency: Float,
        portamentoMs: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ): ShortArray {
        val safeDuration = durationMs.coerceIn(1L, MAX_DURATION_MS)
        val totalSamples = (safeDuration * SAMPLE_RATE / 1000L)
            .coerceIn(1L, MAX_DURATION_SAMPLES).toInt()

        val stereoBuffer = ShortArray(totalSamples * CHANNELS)

        val attackSamples = millisecondsToSamples(patch.adsr.attackMs)
        val decaySamples = millisecondsToSamples(patch.adsr.decayMs)
        val configuredReleaseSamples = millisecondsToSamples(patch.adsr.releaseMs)

        val sustainValue = patch.adsr.sustainLevel
        val sustainLevel = if (sustainValue.isFinite()) {
            sustainValue.coerceIn(0.0f, 1.0f)
        } else {
            0.5f
        }

        val releaseSamples = min(configuredReleaseSamples, totalSamples / 3)
        val releaseStart = max(0, totalSamples - releaseSamples)

        val releaseInitialLevel = calculateAttackDecaySustain(
            sampleIndex = releaseStart,
            attackSamples = attackSamples,
            decaySamples = decaySamples,
            sustainLevel = sustainLevel
        )

        val portamentoSamples = millisecondsToSamples(portamentoMs)
        val safeCutoff = if (patch.cutoffFreq.isFinite()) patch.cutoffFreq else DEFAULT_CUTOFF

        // --- CONFIGURACIÓN DE UNISON Y DETUNE (SUPERSAW) ---
        val unisonCount = patch.unisonVoices.coerceIn(1, 5)
        val detuneIntensity = patch.detuneAmount.coerceIn(0.0f, 1.0f)

        val voiceFrequencies = FloatArray(unisonCount)
        val voicePans = FloatArray(unisonCount)
        for (v in 0 until unisonCount) {
            val offsetMultiplier = if (unisonCount > 1) {
                (v.toFloat() / (unisonCount - 1)) - 0.5f
            } else {
                0.0f
            }
            val detuneOffset = offsetMultiplier * detuneIntensity * 15.0f * (frequency / 440.0f)
            voiceFrequencies[v] = frequency + detuneOffset

            val voicePanSpread = pan + (offsetMultiplier * 1.2f * detuneIntensity)
            voicePans[v] = voicePanSpread.coerceIn(-1.0f, 1.0f)
        }

        // --- CONFIGURACIÓN DEL FX RACK ---
        val reverbMix = patch.fx.reverbMix.coerceIn(0.0f, 1.0f)
        val delayMix = patch.fx.delayMix.coerceIn(0.0f, 1.0f)
        val distortionDrive = patch.fx.distortionDrive.coerceIn(0.0f, 1.0f)

        // Línea de retardo para Delay (~220 ms)
        val delayTimeSamples = (SAMPLE_RATE * 0.22f).toInt()
        val delayBufferLeft = FloatArray(delayTimeSamples)
        val delayBufferRight = FloatArray(delayTimeSamples)
        var delayIndex = 0

        // Filtros de peine para Reverb espacial
        val revComb1 = FloatArray((SAMPLE_RATE * 0.037f).toInt())
        val revComb2 = FloatArray((SAMPLE_RATE * 0.043f).toInt())
        var rIdx1 = 0
        var rIdx2 = 0

        // --- CONFIGURACIÓN DE LA MATRIZ LFO EXPANDIDA ---
        val lfoRate = patch.lfo.rateHz.coerceIn(0.1f, 20.0f)
        val lfoDepth = patch.lfo.depth.coerceIn(0.0f, 1.0f)
        val lfoDestination = patch.lfo.destination
        var lfoPhase = 0.0
        val lfoPhaseIncrement = lfoRate / SAMPLE_RATE

        val phases = DoubleArray(unisonCount) { 0.0 }
        val subPhases = DoubleArray(unisonCount) { 0.0 }
        val filteredSamples = FloatArray(unisonCount) { 0.0f }
        val dt = 1.0f / SAMPLE_RATE

        for (i in 0 until totalSamples) {
            val currentFreqBase = if (portamentoSamples > 0 && i < portamentoSamples) {
                val progress = i.toFloat() / portamentoSamples
                startFrequency + (frequency - startFrequency) * progress
            } else {
                frequency
            }.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)

            // Cálculo del ciclo de modulación LFO
            lfoPhase += lfoPhaseIncrement
            if (lfoPhase >= 1.0) lfoPhase -= floor(lfoPhase)
            val lfoRaw = sin(2.0 * PI * lfoPhase).toFloat()
            val lfoModulation = lfoRaw * lfoDepth

            // Enrutamiento de la Matriz de Modulación Expandida
            val dynamicCutoff = if (lfoDestination == ModulationDestination.CUTOFF) {
                (safeCutoff * (1.0f + lfoModulation * 0.5f)).coerceIn(20.0f, SAMPLE_RATE * 0.45f)
            } else {
                safeCutoff
            }

            val dynamicPan = if (lfoDestination == ModulationDestination.PAN) {
                (pan + lfoModulation * 0.5f).coerceIn(-1.0f, 1.0f)
            } else {
                pan
            }

            val rc = 1.0f / (2.0f * PI.toFloat() * dynamicCutoff)
            val alpha = dt / (rc + dt)

            var mixedLeft = 0.0f
            var mixedRight = 0.0f

            for (v in 0 until unisonCount) {
                // Modulación de tono (Vibrato) si el destino es PITCH
                val pitchModulationFactor = if (lfoDestination == ModulationDestination.PITCH) {
                    1.0f + (lfoModulation * 0.03f)
                } else {
                    1.0f
                }

                val voiceTargetFreq = (voiceFrequencies[v] + (currentFreqBase - frequency)) * pitchModulationFactor
                val pitchCorrectedFreq = calculatePitch(
                    baseFrequency = voiceTargetFreq,
                    instrumentType = patch.instrumentType,
                    sampleIndex = i
                ).coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)

                val phaseIncrement = (pitchCorrectedFreq / SAMPLE_RATE)
                    .coerceIn(0.000001f, 0.49f).toDouble()
                val subPhaseIncrement = phaseIncrement * 0.5

                val rawSample = generateWaveform(
                    waveform = patch.waveform,
                    phase = phases[v],
                    subPhase = subPhases[v],
                    phaseIncrement = phaseIncrement,
                    frequency = pitchCorrectedFreq,
                    instrumentType = patch.instrumentType
                )

                phases[v] += phaseIncrement
                if (phases[v] >= 1.0) phases[v] -= floor(phases[v])
                subPhases[v] += subPhaseIncrement
                if (subPhases[v] >= 1.0) subPhases[v] -= floor(subPhases[v])

                filteredSamples[v] += alpha * (rawSample - filteredSamples[v])

                val voicePanAngle = ((voicePans[v] + 1.0f) * 0.25f) * PI.toFloat()
                mixedLeft += filteredSamples[v] * cos(voicePanAngle)
                mixedRight += filteredSamples[v] * sin(voicePanAngle)
            }

            // Normalización de ganancia de Unison
            val unisonGainCorrection = 1.0f / kotlin.math.sqrt(unisonCount.toFloat())
            mixedLeft *= unisonGainCorrection
            mixedRight *= unisonGainCorrection

            val envelope = calculateEnvelope(
                sampleIndex = i,
                attackSamples = attackSamples,
                decaySamples = decaySamples,
                sustainLevel = sustainLevel,
                releaseStart = releaseStart,
                releaseSamples = releaseSamples,
                releaseInitialLevel = releaseInitialLevel
            )

            val finalLeft = softClip(mixedLeft * envelope * volume * MASTER_HEADROOM)
            val finalRight = softClip(mixedRight * envelope * volume * MASTER_HEADROOM)

            var processedLeft = finalLeft
            var processedRight = finalRight

            // --- PROCESAMIENTO FX RACK ---

            // 1. Distorsión cálida (Saturation)
            if (distortionDrive > 0.0f) {
                val driveFactor = 1.0f + (distortionDrive * 5.0f)
                processedLeft = tanh(processedLeft * driveFactor) / tanh(driveFactor)
                processedRight = tanh(processedRight * driveFactor) / tanh(driveFactor)
            }

            // 2. Delay (Eco rítmico)
            if (delayMix > 0.0f) {
                val delayedL = delayBufferLeft[delayIndex]
                val delayedR = delayBufferRight[delayIndex]

                delayBufferLeft[delayIndex] = processedLeft + (delayedL * 0.4f)
                delayBufferRight[delayIndex] = processedRight + (delayedR * 0.4f)

                processedLeft += delayedL * delayMix
                processedRight += delayedR * delayMix

                delayIndex = (delayIndex + 1) % delayTimeSamples
            }

            // 3. Reverb (Profundidad y espacio)
            if (reverbMix > 0.0f) {
                val c1 = revComb1[rIdx1]
                val c2 = revComb2[rIdx2]

                revComb1[rIdx1] = processedLeft + (c1 * 0.5f)
                revComb2[rIdx2] = processedRight + (c2 * 0.5f)

                rIdx1 = (rIdx1 + 1) % revComb1.size
                rIdx2 = (rIdx2 + 1) % revComb2.size

                processedLeft += (c1 - processedLeft) * reverbMix * 0.5f
                processedRight += (c2 - processedRight) * reverbMix * 0.5f
            }

            // Paneo maestro (afectado por LFO si el destino es PAN)
            val masterPanAngle = ((dynamicPan + 1.0f) * 0.25f) * PI.toFloat()
            val masterLeftPan = cos(masterPanAngle)
            val masterRightPan = sin(masterPanAngle)

            val bufferIndex = i * CHANNELS
            stereoBuffer[bufferIndex] = floatToPcm(processedLeft * masterLeftPan)
            stereoBuffer[bufferIndex + 1] = floatToPcm(processedRight * masterRightPan)
        }

        return stereoBuffer
    }

    private fun generateWaveform(
        waveform: WaveformType,
        phase: Double,
        subPhase: Double,
        phaseIncrement: Double,
        frequency: Float,
        instrumentType: SynthInstrumentType
    ): Float {
        return when (waveform) {
            WaveformType.SINE -> {
                val fundamental = sin(2.0 * PI * phase).toFloat()
                if (instrumentType == SynthInstrumentType.SUB_BASS) {
                    val sub = sin(2.0 * PI * subPhase).toFloat()
                    fundamental * 0.75f + sub * 0.25f
                } else {
                    fundamental
                }
            }
            WaveformType.SAW -> {
                val dt = phaseIncrement.coerceIn(0.000001, 0.49)
                var sample = (2.0 * phase - 1.0).toFloat()
                sample -= polyBlep(phase, dt).toFloat()
                sample
            }
            WaveformType.SQUARE -> {
                val dt = phaseIncrement.coerceIn(0.000001, 0.49)
                var sample = if (phase < 0.5) 1.0f else -1.0f
                sample += polyBlep(phase, dt).toFloat()
                var secondPhase = phase + 0.5
                if (secondPhase >= 1.0) secondPhase -= 1.0
                sample -= polyBlep(secondPhase, dt).toFloat()
                sample
            }
            WaveformType.TRIANGLE -> {
                generateTriangle(phase = phase, frequency = frequency)
            }
        }
    }

    private fun polyBlep(phase: Double, phaseIncrement: Double): Double {
        if (phase < phaseIncrement) {
            val t = phase / phaseIncrement
            return 2.0 * t - t * t - 1.0
        }
        if (phase > 1.0 - phaseIncrement) {
            val t = (phase - 1.0) / phaseIncrement
            return t * t + 2.0 * t + 1.0
        }
        return 0.0
    }

    private fun generateTriangle(phase: Double, frequency: Float): Float {
        var output = 0.0
        val maxHarmonic = min(31, (MAX_FREQUENCY / frequency).toInt())
        var harmonic = 1

        while (harmonic <= maxHarmonic) {
            if (harmonic % 2 == 1) {
                val sign = if (((harmonic - 1) / 2) % 2 == 0) 1.0 else -1.0
                val amplitude = sign / (harmonic.toDouble() * harmonic)
                output += amplitude * sin(2.0 * PI * harmonic * phase)
            }
            harmonic += 2
        }
        return (output * 8.0 / (PI * PI)).toFloat()
    }

    private fun calculatePitch(
        baseFrequency: Float,
        instrumentType: SynthInstrumentType,
        sampleIndex: Int
    ): Float {
        if (instrumentType != SynthInstrumentType.SYNTH_808) {
            return baseFrequency
        }
        val pitchDropSamples = (SAMPLE_RATE * 0.12f).toInt()
        if (sampleIndex >= pitchDropSamples) {
            return baseFrequency
        }
        val progress = (sampleIndex.toFloat() / pitchDropSamples).coerceIn(0.0f, 1.0f)
        val decay = 1.0f - progress
        return baseFrequency + 80.0f * decay * decay
    }

    private fun calculateAttackDecaySustain(
        sampleIndex: Int,
        attackSamples: Int,
        decaySamples: Int,
        sustainLevel: Float
    ): Float {
        return when {
            attackSamples > 0 && sampleIndex < attackSamples -> {
                sampleIndex.toFloat() / attackSamples
            }
            decaySamples > 0 && sampleIndex < attackSamples + decaySamples -> {
                val progress = (sampleIndex - attackSamples).toFloat() / decaySamples
                1.0f - ((1.0f - sustainLevel) * progress)
            }
            else -> sustainLevel
        }
    }

    private fun calculateEnvelope(
        sampleIndex: Int,
        attackSamples: Int,
        decaySamples: Int,
        sustainLevel: Float,
        releaseStart: Int,
        releaseSamples: Int,
        releaseInitialLevel: Float
    ): Float {
        if (releaseSamples > 0 && sampleIndex >= releaseStart) {
            val progress = (sampleIndex - releaseStart).toFloat() / releaseSamples
            return (releaseInitialLevel * (1.0f - progress)).coerceAtLeast(0.0f)
        }
        return calculateAttackDecaySustain(
            sampleIndex = sampleIndex,
            attackSamples = attackSamples,
            decaySamples = decaySamples,
            sustainLevel = sustainLevel
        )
    }

    private fun softClip(input: Float): Float {
        val safeInput = input.coerceIn(-4.0f, 4.0f)
        return tanh(safeInput).toFloat()
    }

    private fun floatToPcm(input: Float): Short {
        val safe = if (input.isFinite()) input.coerceIn(-1.0f, 1.0f) else 0.0f
        return (safe * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
    }

    private fun millisecondsToSamples(milliseconds: Number): Int {
        val value = milliseconds.toDouble()
        if (!value.isFinite() || value <= 0.0) return 0
        val samples = value * SAMPLE_RATE / 1000.0
        return samples.coerceIn(0.0, MAX_DURATION_SAMPLES.toDouble()).toInt()
    }
}
