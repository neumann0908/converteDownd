package com.musicdownloader.converter.beatstudio.presentation.synth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * SynthEngineCore v2.1 (DSP Avanzado + AudioTrack Mode Stream Robusto)
 * Generador DSP de audio profesional para Synth Studio.
 */
object SynthEngineCore {

    // ---------------------------------------------------------
    // 1. CONFIGURACIÓN GENERAL
    // ---------------------------------------------------------

    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2
    private const val MAX_VOICES = 8

    private const val MAX_FREQUENCY = SAMPLE_RATE * 0.45f
    private const val MIN_FREQUENCY = 20.0f
    private const val MASTER_HEADROOM = 0.90f

    private val audioExecutor = Executors.newFixedThreadPool(
        MAX_VOICES,
        ThreadFactory { runnable ->
            Thread(runnable, "SynthVoice").apply {
                isDaemon = true
            }
        }
    )

    // ---------------------------------------------------------
    // 2. API PÚBLICA (COMPATIBLE CON EL MOTOR)
    // ---------------------------------------------------------

    fun playTone(
        frequency: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ) {
        val safeFrequency = frequency.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)
        val safeDuration = durationMs.coerceAtLeast(1L)
        val safeVolume = volume.coerceIn(0.0f, 1.0f)
        val safePan = pan.coerceIn(-1.0f, 1.0f)

        if (safeVolume <= 0.0f) return

        audioExecutor.execute {
            try {
                val audioData = generateAudio(
                    frequency = safeFrequency,
                    durationMs = safeDuration,
                    patch = patch,
                    volume = safeVolume,
                    pan = safePan
                )

                playAudioData(audioData)

            } catch (exception: Exception) {
                android.util.Log.e(
                    "SynthEngineCore",
                    "Error reproduciendo nota DSP",
                    exception
                )
            }
        }
    }

    // ---------------------------------------------------------
    // 3. GENERACIÓN PRINCIPAL (DSP)
    // ---------------------------------------------------------

    private fun generateAudio(
        frequency: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ): ShortArray {

        val totalSamples = (
            durationMs * SAMPLE_RATE / 1000L
        ).coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        val stereoBuffer = ShortArray(totalSamples * CHANNELS)

        val attackSamples = millisecondsToSamples(patch.adsr.attackMs)
        val decaySamples = millisecondsToSamples(patch.adsr.decayMs)
        val releaseSamples = millisecondsToSamples(patch.adsr.releaseMs)
        val sustainLevel = patch.adsr.sustainLevel.coerceIn(0.0f, 1.0f)

        val releaseStart = max(0, totalSamples - releaseSamples)

        val releaseInitialLevel = calculateAttackDecaySustain(
            sampleIndex = releaseStart,
            attackSamples = attackSamples,
            decaySamples = decaySamples,
            sustainLevel = sustainLevel
        )

        val filter = LowPassFilter(
            sampleRate = SAMPLE_RATE.toFloat(),
            cutoffFrequency = patch.cutoffFreq
        )

        var phase = 0.0
        val baseFrequency = frequency.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)

        val panAngle = ((pan + 1.0f) * 0.25f) * PI.toFloat()
        val leftPan = cos(panAngle)
        val rightPan = sin(panAngle)

        for (i in 0 until totalSamples) {
            val time = i.toFloat() / SAMPLE_RATE

            val currentFrequency = calculatePitch(
                baseFrequency = baseFrequency,
                instrumentType = patch.instrumentType,
                sampleIndex = i
            )

            val safeCurrentFrequency = currentFrequency.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)
            val phaseIncrement = (safeCurrentFrequency / SAMPLE_RATE).coerceIn(0.0f, 0.49f).toDouble()

            val rawSample = generateWaveform(
                waveform = patch.waveform,
                phase = phase,
                phaseIncrement = phaseIncrement,
                frequency = safeCurrentFrequency,
                instrumentType = patch.instrumentType
            )

            phase += phaseIncrement
            if (phase >= 1.0) {
                phase -= floor(phase)
            }

            val filteredSample = filter.process(rawSample)

            val envelope = calculateEnvelope(
                sampleIndex = i,
                attackSamples = attackSamples,
                decaySamples = decaySamples,
                sustainLevel = sustainLevel,
                releaseStart = releaseStart,
                releaseSamples = releaseSamples,
                releaseInitialLevel = releaseInitialLevel
            )

            val amplitude = (
                filteredSample *
                envelope *
                volume *
                MASTER_HEADROOM
            )

            val protectedSample = softClip(amplitude)

            val leftSample = protectedSample * leftPan
            val rightSample = protectedSample * rightPan

            stereoBuffer[i * 2] = floatToPcm(leftSample)
            stereoBuffer[i * 2 + 1] = floatToPcm(rightSample)
        }

        return stereoBuffer
    }

    // ---------------------------------------------------------
    // 4. OSCILADORES Y POLYBLEP
    // ---------------------------------------------------------

    private fun generateWaveform(
        waveform: WaveformType,
        phase: Double,
        phaseIncrement: Double,
        frequency: Float,
        instrumentType: SynthInstrumentType
    ): Float {
        return when (waveform) {
            WaveformType.SINE -> {
                val fundamental = sin(2.0 * PI * phase).toFloat()
                if (instrumentType == SynthInstrumentType.SUB_BASS) {
                    val sub = sin(PI * phase).toFloat()
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

    // ---------------------------------------------------------
    // 5. MODULACIÓN Y ENVOLVENTES
    // ---------------------------------------------------------

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
        val decay = (1.0f - progress)
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
        if (sampleIndex >= releaseStart) {
            if (releaseSamples <= 0) return 0.0f
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

    // ---------------------------------------------------------
    // 6. FILTRO LOW-PASS BIQUAD
    // ---------------------------------------------------------

    private class LowPassFilter(sampleRate: Float, cutoffFrequency: Float) {
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        private val b0: Double
        private val b1: Double
        private val b2: Double
        private val a1: Double
        private val a2: Double

        init {
            val safeCutoff = cutoffFrequency.coerceIn(20.0f, sampleRate * 0.45f)
            val omega = 2.0 * PI * safeCutoff / sampleRate
            val cosOmega = cos(omega)
            val sinOmega = sin(omega)
            val q = 0.70710678
            val alpha = sinOmega / (2.0 * q)
            val a0 = 1.0 + alpha

            b0 = ((1.0 - cosOmega) * 0.5) / a0
            b1 = (1.0 - cosOmega) / a0
            b2 = ((1.0 - cosOmega) * 0.5) / a0
            a1 = (-2.0 * cosOmega) / a0
            a2 = (1.0 - alpha) / a0
        }

        fun process(input: Float): Float {
            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = input.toDouble()
            y2 = y1
            y1 = output
            return output.toFloat()
        }
    }

    private fun softClip(input: Float): Float {
        val safeInput = input.coerceIn(-4.0f, 4.0f)
        return tanh(safeInput).toFloat()
    }

    private fun floatToPcm(input: Float): Short {
        val safe = input.coerceIn(-1.0f, 1.0f)
        return (safe * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
    }

    private fun millisecondsToSamples(milliseconds: Number): Int {
        val safeMs = milliseconds.toDouble().coerceAtLeast(0.0)
        return (safeMs * SAMPLE_RATE / 1000.0).toInt().coerceAtMost(Int.MAX_VALUE)
    }

    // ---------------------------------------------------------
    // 7. REPRODUCCIÓN ROBUSTA CON AUDIOTRACK (MODE_STREAM)
    // ---------------------------------------------------------

    private fun playAudioData(stereoBuffer: ShortArray) {
        if (stereoBuffer.isEmpty()) return

        var audioTrack: AudioTrack? = null
        try {
            val bufferSizeBytes = stereoBuffer.size * Short.SIZE_BYTES
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val finalBufferSize = max(bufferSizeBytes, minBufferSize * 2)

            val track = AudioTrack.Builder()
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

            audioTrack = track

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                android.util.Log.e("SynthEngineCore", "AudioTrack no pudo inicializarse correctamente.")
                return
            }

            track.play()
            track.write(stereoBuffer, 0, stereoBuffer.size)

            val durationMs = (stereoBuffer.size.toLong() / CHANNELS * 1000L / SAMPLE_RATE)
            Thread.sleep(durationMs + 60L)

        } catch (exception: Exception) {
            android.util.Log.e("SynthEngineCore", "Excepción al reproducir flujo de audio DSP", exception)
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
