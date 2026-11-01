package com.musicdownloader.converter.beatstudio.presentation.synth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * SynthEngineCore v2.7 (Con LFO Dinámico y AudioTrack MODE_STREAM robusto)
 *
 * Motor de síntesis DSP profesional con modulación LFO integrada.
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

    /**
     * ThreadPool optimizado con DiscardOldestPolicy para evitar saturación de hilos.
     */
    private val audioExecutor = ThreadPoolExecutor(
        MAX_VOICES,
        MAX_VOICES,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_NOTES),
        ThreadFactory { runnable ->
            Thread(runnable, "SynthVoice").apply {
                isDaemon = true
            }
        },
        ThreadPoolExecutor.DiscardOldestPolicy()
    )

    fun playTone(
        frequency: Float,
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
                try {
                    val audioData = generateAudio(
                        frequency = safeFrequency,
                        durationMs = safeDuration,
                        patch = patch,
                        volume = safeVolume,
                        pan = safePan
                    )

                    playAudioData(audioData, safeDuration)

                } catch (exception: Exception) {
                    Log.e(TAG, "Error generando o reproduciendo nota", exception)
                }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Nota descartada por sobrecarga del ejecutor", exception)
        }
    }

    private fun generateAudio(
        frequency: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ): ShortArray {
        val safeDuration = durationMs.coerceIn(1L, MAX_DURATION_MS)
        val totalSamples = (safeDuration * SAMPLE_RATE / 1000L)
            .coerceIn(1L, MAX_DURATION_SAMPLES).toInt()

        val totalValues = totalSamples * CHANNELS
        val stereoBuffer = ShortArray(totalValues)

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

        val cutoffValue = patch.cutoffFreq
        val safeCutoff = if (cutoffValue.isFinite()) cutoffValue else DEFAULT_CUTOFF

        // Configuración del LFO
        val lfoRate = patch.lfo.rateHz.coerceIn(0.1f, 20.0f)
        val lfoDepth = patch.lfo.depth.coerceIn(0.0f, 1.0f)
        var lfoPhase = 0.0
        val lfoPhaseIncrement = lfoRate / SAMPLE_RATE

        var phase = 0.0
        var subPhase = 0.0
        val baseFrequency = frequency.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)

        var filteredSample = 0.0f
        val dt = 1.0f / SAMPLE_RATE

        val panAngle = ((pan.coerceIn(-1.0f, 1.0f) + 1.0f) * 0.25f) * PI.toFloat()
        val leftPan = cos(panAngle)
        val rightPan = sin(panAngle)

        for (i in 0 until totalSamples) {
            // Modulación dinámica de filtro mediante LFO
            lfoPhase += lfoPhaseIncrement
            if (lfoPhase >= 1.0) lfoPhase -= floor(lfoPhase)
            val lfoModulation = sin(2.0 * PI * lfoPhase).toFloat() * lfoDepth
            val dynamicCutoff = (safeCutoff * (1.0f + lfoModulation * 0.5f))
                .coerceIn(20.0f, SAMPLE_RATE * 0.45f)

            val rc = 1.0f / (2.0f * PI.toFloat() * dynamicCutoff)
            val alpha = dt / (rc + dt)

            val currentFrequency = calculatePitch(
                baseFrequency = baseFrequency,
                instrumentType = patch.instrumentType,
                sampleIndex = i
            ).coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)

            val phaseIncrement = (currentFrequency / SAMPLE_RATE)
                .coerceIn(0.000001f, 0.49f).toDouble()
            val subPhaseIncrement = phaseIncrement * 0.5

            val rawSample = generateWaveform(
                waveform = patch.waveform,
                phase = phase,
                subPhase = subPhase,
                phaseIncrement = phaseIncrement,
                frequency = currentFrequency,
                instrumentType = patch.instrumentType
            )

            phase += phaseIncrement
            if (phase >= 1.0) phase -= floor(phase)

            subPhase += subPhaseIncrement
            if (subPhase >= 1.0) subPhase -= floor(subPhase)

            // Procesamiento del filtro con cutoff modulado por LFO
            filteredSample += alpha * (rawSample - filteredSample)

            val envelope = calculateEnvelope(
                sampleIndex = i,
                attackSamples = attackSamples,
                decaySamples = decaySamples,
                sustainLevel = sustainLevel,
                releaseStart = releaseStart,
                releaseSamples = releaseSamples,
                releaseInitialLevel = releaseInitialLevel
            )

            val amplitude = filteredSample * envelope * volume * MASTER_HEADROOM
            val protectedSample = softClip(amplitude)

            val leftSample = protectedSample * leftPan
            val rightSample = protectedSample * rightPan

            val bufferIndex = i * CHANNELS
            stereoBuffer[bufferIndex] = floatToPcm(leftSample)
            stereoBuffer[bufferIndex + 1] = floatToPcm(rightSample)
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

    private fun playAudioData(stereoBuffer: ShortArray, durationMs: Long) {
        if (stereoBuffer.isEmpty()) return

        var audioTrack: AudioTrack? = null
        try {
            val bufferSizeBytes = stereoBuffer.size * BYTES_PER_SAMPLE
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val finalBufferSize = max(bufferSizeBytes, minBufferSize * 2)

            audioTrack = AudioTrack.Builder()
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

            if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack no pudo inicializarse.")
                return
            }

            audioTrack.play()
            audioTrack.write(stereoBuffer, 0, stereoBuffer.size)

            Thread.sleep(durationMs + 60L)

        } catch (exception: Exception) {
            Log.e(TAG, "Error reproduciendo audio DSP", exception)
        } finally {
            audioTrack?.let { track ->
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
}
