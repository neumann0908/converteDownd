
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
 * SynthEngineCore v2.2
 *
 * Motor de síntesis DSP mejorado.
 *
 * Características:
 * - Osciladores Sine, Saw, Square y Triangle.
 * - PolyBLEP para reducir aliasing en Saw y Square.
 * - Oscilador subgrave con fase independiente.
 * - Envolvente ADSR.
 * - Filtro Low-Pass Biquad.
 * - Paneo estéreo constant-power.
 * - Protección contra valores y duraciones excesivas.
 * - Ejecutor con cola limitada.
 * - Reproducción mediante AudioTrack MODE_STATIC.
 *
 * IMPORTANTE:
 * Esta implementación es una etapa intermedia.
 * No sustituye todavía al futuro Audio Runtime centralizado
 * basado en Oboe/C++.
 */
object SynthEngineCore {

    // ---------------------------------------------------------
    // 1. CONFIGURACIÓN GENERAL
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // 2. EJECUTOR LIMITADO
    // ---------------------------------------------------------

    /**
     * Limita las tareas simultáneas y las tareas pendientes.
     *
     * Evita que una sucesión excesiva de notas cree una cola
     * ilimitada de trabajos y consuma memoria progresivamente.
     *
     * Si la cola está llena, se descarta la nueva tarea y
     * se registra una advertencia.
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
        ThreadPoolExecutor.AbortPolicy()
    )

    // ---------------------------------------------------------
    // 3. API PÚBLICA
    // ---------------------------------------------------------

    /**
     * Reproduce una nota sintetizada.
     *
     * Se conserva la firma pública para no romper
     * las llamadas existentes desde otros componentes.
     */
    fun playTone(
        frequency: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ) {

        val safeFrequency = if (frequency.isFinite()) {
            frequency.coerceIn(
                MIN_FREQUENCY,
                MAX_FREQUENCY
            )
        } else {
            DEFAULT_FREQUENCY
        }

        val safeDuration = durationMs.coerceIn(
            1L,
            MAX_DURATION_MS
        )

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

                    playAudioData(audioData)

                } catch (exception: Exception) {

                    Log.e(
                        TAG,
                        "Error generando o reproduciendo nota",
                        exception
                    )
                }
            }

        } catch (exception: java.util.concurrent.RejectedExecutionException) {

            Log.w(
                TAG,
                "Nota descartada: límite de tareas alcanzado",
                exception
            )
        }
    }

    // ---------------------------------------------------------
    // 4. GENERACIÓN PRINCIPAL
    // ---------------------------------------------------------

    private fun generateAudio(
        frequency: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ): ShortArray {

        // Duración limitada antes de reservar memoria.
        val safeDuration = durationMs.coerceIn(
            1L,
            MAX_DURATION_MS
        )

        val totalSamples = (
            safeDuration * SAMPLE_RATE / 1000L
        ).coerceIn(
            1L,
            MAX_DURATION_SAMPLES
        ).toInt()

        // Evita desbordamientos en el tamaño del array.
        val totalValues = totalSamples * CHANNELS

        val stereoBuffer = ShortArray(totalValues)

        // -----------------------------------------------------
        // ADSR
        // -----------------------------------------------------

        val attackSamples = millisecondsToSamples(
            patch.adsr.attackMs
        )

        val decaySamples = millisecondsToSamples(
            patch.adsr.decayMs
        )

        val configuredReleaseSamples = millisecondsToSamples(
            patch.adsr.releaseMs
        )

        val sustainValue = patch.adsr.sustainLevel

        val sustainLevel = if (sustainValue.isFinite()) {
            sustainValue.coerceIn(0.0f, 1.0f)
        } else {
            0.5f
        }

        /*
         * El Release no puede ocupar más de un tercio
         * de la duración disponible.
         *
         * Así evitamos que una nota corta pase todo su tiempo
         * intentando ejecutar una fase de Release demasiado larga.
         */
        val releaseSamples = min(
            configuredReleaseSamples,
            totalSamples / 3
        )

        val releaseStart = max(
            0,
            totalSamples - releaseSamples
        )

        val releaseInitialLevel = calculateAttackDecaySustain(
            sampleIndex = releaseStart,
            attackSamples = attackSamples,
            decaySamples = decaySamples,
            sustainLevel = sustainLevel
        )

        // -----------------------------------------------------
        // FILTRO
        // -----------------------------------------------------

        val cutoffValue = patch.cutoffFreq

        val safeCutoff = if (cutoffValue.isFinite()) {
            cutoffValue
        } else {
            DEFAULT_CUTOFF
        }

        val filter = LowPassFilter(
            sampleRate = SAMPLE_RATE.toFloat(),
            cutoffFrequency = safeCutoff
        )

        // -----------------------------------------------------
        // OSCILADORES
        // -----------------------------------------------------

        var phase = 0.0

        /*
         * El suboscilador tiene su propia fase.
         * Esto permite generar una frecuencia real de media
         * fundamental sin reiniciar la onda en cada ciclo.
         */
        var subPhase = 0.0

        val baseFrequency = frequency.coerceIn(
            MIN_FREQUENCY,
            MAX_FREQUENCY
        )

        // -----------------------------------------------------
        // PANEO CONSTANT POWER
        // -----------------------------------------------------

        val panAngle = (
            (pan.coerceIn(-1.0f, 1.0f) + 1.0f) *
                0.25f
            ) * PI.toFloat()

        val leftPan = cos(panAngle)
        val rightPan = sin(panAngle)

        // -----------------------------------------------------
        // BUCLE DE SÍNTESIS
        // -----------------------------------------------------

        for (i in 0 until totalSamples) {

            val currentFrequency = calculatePitch(
                baseFrequency = baseFrequency,
                instrumentType = patch.instrumentType,
                sampleIndex = i
            ).coerceIn(
                MIN_FREQUENCY,
                MAX_FREQUENCY
            )

            val phaseIncrement = (
                currentFrequency / SAMPLE_RATE
            ).coerceIn(
                0.000001f,
                0.49f
            ).toDouble()

            val subPhaseIncrement = phaseIncrement * 0.5

            // Generación de la forma de onda.
            val rawSample = generateWaveform(
                waveform = patch.waveform,
                phase = phase,
                subPhase = subPhase,
                phaseIncrement = phaseIncrement,
                frequency = currentFrequency,
                instrumentType = patch.instrumentType
            )

            // Avance del oscilador principal.
            phase += phaseIncrement

            if (phase >= 1.0) {
                phase -= floor(phase)
            }

            // Avance del suboscilador.
            subPhase += subPhaseIncrement

            if (subPhase >= 1.0) {
                subPhase -= floor(subPhase)
            }

            // Filtro.
            val filteredSample = filter.process(rawSample)

            // ADSR.
            val envelope = calculateEnvelope(
                sampleIndex = i,
                attackSamples = attackSamples,
                decaySamples = decaySamples,
                sustainLevel = sustainLevel,
                releaseStart = releaseStart,
                releaseSamples = releaseSamples,
                releaseInitialLevel = releaseInitialLevel
            )

            // Ganancia.
            val amplitude = (
                filteredSample *
                    envelope *
                    volume *
                    MASTER_HEADROOM
                )

            // Protección de amplitud.
            val protectedSample = softClip(amplitude)

            // Paneo.
            val leftSample = protectedSample * leftPan
            val rightSample = protectedSample * rightPan

            // Conversión PCM estéreo.
            val bufferIndex = i * CHANNELS

            stereoBuffer[bufferIndex] = floatToPcm(leftSample)
            stereoBuffer[bufferIndex + 1] = floatToPcm(rightSample)
        }

        return stereoBuffer
    }

    // ---------------------------------------------------------
    // 5. OSCILADORES
    // ---------------------------------------------------------

    private fun generateWaveform(
        waveform: WaveformType,
        phase: Double,
        subPhase: Double,
        phaseIncrement: Double,
        frequency: Float,
        instrumentType: SynthInstrumentType
    ): Float {

        return when (waveform) {

            // -------------------------------------------------
            // SINE
            // -------------------------------------------------

            WaveformType.SINE -> {

                val fundamental = sin(
                    2.0 * PI * phase
                ).toFloat()

                if (instrumentType == SynthInstrumentType.SUB_BASS) {

                    val sub = sin(
                        2.0 * PI * subPhase
                    ).toFloat()

                    fundamental * 0.75f + sub * 0.25f

                } else {

                    fundamental
                }
            }

            // -------------------------------------------------
            // SAW + POLYBLEP
            // -------------------------------------------------

            WaveformType.SAW -> {

                val dt = phaseIncrement.coerceIn(
                    0.000001,
                    0.49
                )

                var sample = (
                    2.0 * phase - 1.0
                    ).toFloat()

                sample -= polyBlep(
                    phase,
                    dt
                ).toFloat()

                sample
            }

            // -------------------------------------------------
            // SQUARE + POLYBLEP
            // -------------------------------------------------

            WaveformType.SQUARE -> {

                val dt = phaseIncrement.coerceIn(
                    0.000001,
                    0.49
                )

                var sample = if (phase < 0.5) {
                    1.0f
                } else {
                    -1.0f
                }

                sample += polyBlep(
                    phase,
                    dt
                ).toFloat()

                var secondPhase = phase + 0.5

                if (secondPhase >= 1.0) {
                    secondPhase -= 1.0
                }

                sample -= polyBlep(
                    secondPhase,
                    dt
                ).toFloat()

                sample
            }

            // -------------------------------------------------
            // TRIANGLE
            // -------------------------------------------------

            WaveformType.TRIANGLE -> {

                generateTriangle(
                    phase = phase,
                    frequency = frequency
                )
            }
        }
    }

    // ---------------------------------------------------------
    // 6. POLYBLEP
    // ---------------------------------------------------------

    private fun polyBlep(
        phase: Double,
        phaseIncrement: Double
    ): Double {

        if (phase < phaseIncrement) {

            val t = phase / phaseIncrement

            return 2.0 * t - t * t - 1.0
        }

        if (phase > 1.0 - phaseIncrement) {

            val t = (
                phase - 1.0
                ) / phaseIncrement

            return t * t + 2.0 * t + 1.0
        }

        return 0.0
    }

    // ---------------------------------------------------------
    // 7. TRIANGLE BAND-LIMITED
    // ---------------------------------------------------------

    private fun generateTriangle(
        phase: Double,
        frequency: Float
    ): Float {

        var output = 0.0

        val maxHarmonic = min(
            31,
            (MAX_FREQUENCY / frequency).toInt()
        )

        var harmonic = 1

        while (harmonic <= maxHarmonic) {

            if (harmonic % 2 == 1) {

                val sign = if (
                    ((harmonic - 1) / 2) % 2 == 0
                ) {
                    1.0
                } else {
                    -1.0
                }

                val amplitude = sign / (
                    harmonic.toDouble() * harmonic
                )

                output += amplitude * sin(
                    2.0 * PI * harmonic * phase
                )
            }

            harmonic += 2
        }

        return (
            output * 8.0 / (PI * PI)
            ).toFloat()
    }

    // ---------------------------------------------------------
    // 8. PITCH ENVELOPE 808
    // ---------------------------------------------------------

    private fun calculatePitch(
        baseFrequency: Float,
        instrumentType: SynthInstrumentType,
        sampleIndex: Int
    ): Float {

        if (
            instrumentType != SynthInstrumentType.SYNTH_808
        ) {
            return baseFrequency
        }

        val pitchDropSamples = (
            SAMPLE_RATE * 0.12f
            ).toInt()

        if (sampleIndex >= pitchDropSamples) {
            return baseFrequency
        }

        val progress = (
            sampleIndex.toFloat() / pitchDropSamples
            ).coerceIn(0.0f, 1.0f)

        val decay = 1.0f - progress

        return baseFrequency + 80.0f * decay * decay
    }

    // ---------------------------------------------------------
    // 9. ADSR: ATTACK, DECAY Y SUSTAIN
    // ---------------------------------------------------------

    private fun calculateAttackDecaySustain(
        sampleIndex: Int,
        attackSamples: Int,
        decaySamples: Int,
        sustainLevel: Float
    ): Float {

        return when {

            attackSamples > 0 &&
                sampleIndex < attackSamples -> {

                sampleIndex.toFloat() / attackSamples
            }

            decaySamples > 0 &&
                sampleIndex < attackSamples + decaySamples -> {

                val progress = (
                    sampleIndex - attackSamples
                    ).toFloat() / decaySamples

                1.0f - (
                    (1.0f - sustainLevel) * progress
                    )
            }

            else -> sustainLevel
        }
    }

    // ---------------------------------------------------------
    // 10. ADSR COMPLETA
    // ---------------------------------------------------------

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

            val progress = (
                sampleIndex - releaseStart
                ).toFloat() / releaseSamples

            return (
                releaseInitialLevel * (1.0f - progress)
                ).coerceAtLeast(0.0f)
        }

        return calculateAttackDecaySustain(
            sampleIndex = sampleIndex,
            attackSamples = attackSamples,
            decaySamples = decaySamples,
            sustainLevel = sustainLevel
        )
    }

    // ---------------------------------------------------------
    // 11. FILTRO LOW-PASS BIQUAD
    // ---------------------------------------------------------

    private class LowPassFilter(
        sampleRate: Float,
        cutoffFrequency: Float
    ) {

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

            val safeCutoff = (
                if (cutoffFrequency.isFinite()) {
                    cutoffFrequency
                } else {
                    DEFAULT_CUTOFF
                }
                ).coerceIn(
                    20.0f,
                    sampleRate * 0.45f
                )

            val omega = (
                2.0 * PI * safeCutoff / sampleRate
                )

            val cosOmega = cos(omega)
            val sinOmega = sin(omega)

            // Q de Butterworth.
            val q = 0.70710678

            val alpha = sinOmega / (2.0 * q)

            val a0 = 1.0 + alpha

            b0 = (
                (1.0 - cosOmega) * 0.5
                ) / a0

            b1 = (
                1.0 - cosOmega
                ) / a0

            b2 = (
                (1.0 - cosOmega) * 0.5
                ) / a0

            a1 = (
                -2.0 * cosOmega
                ) / a0

            a2 = (
                1.0 - alpha
                ) / a0
        }

        fun process(input: Float): Float {

            val output =
                b0 * input +
                    b1 * x1 +
                    b2 * x2 -
                    a1 * y1 -
                    a2 * y2

            x2 = x1
            x1 = input.toDouble()

            y2 = y1
            y1 = output

            return output.toFloat()
        }
    }

    // ---------------------------------------------------------
    // 12. SOFT CLIPPING
    // ---------------------------------------------------------

    private fun softClip(input: Float): Float {

        val safeInput = input.coerceIn(
            -4.0f,
            4.0f
        )

        return tanh(safeInput).toFloat()
    }

    // ---------------------------------------------------------
    // 13. CONVERSIÓN FLOAT A PCM
    // ---------------------------------------------------------

    private fun floatToPcm(input: Float): Short {

        val safe = if (input.isFinite()) {
            input.coerceIn(-1.0f, 1.0f)
        } else {
            0.0f
        }

        return (
            safe * 32767.0f
            ).toInt().coerceIn(
                -32768,
                32767
            ).toShort()
    }

    // ---------------------------------------------------------
    // 14. MILISEGUNDOS A MUESTRAS
    // ---------------------------------------------------------

    private fun millisecondsToSamples(
        milliseconds: Number
    ): Int {

        val value = milliseconds.toDouble()

        if (!value.isFinite() || value <= 0.0) {
            return 0
        }

        val samples = (
            value * SAMPLE_RATE / 1000.0
            )

        return samples.coerceIn(
            0.0,
            MAX_DURATION_SAMPLES.toDouble()
        ).toInt()
    }

    // ---------------------------------------------------------
    // 15. REPRODUCCIÓN AUDIO TRACK
    // ---------------------------------------------------------

    /**
     * Reproduce un buffer PCM ya generado.
     *
     * MODE_STATIC es apropiado aquí porque todo el contenido
     * de la nota ya está generado antes de iniciar la reproducción.
     */
    private fun playAudioData(
        stereoBuffer: ShortArray
    ) {

        if (stereoBuffer.isEmpty()) return

        var audioTrack: AudioTrack? = null

        try {

            val bufferSizeBytes =
                stereoBuffer.size * BYTES_PER_SAMPLE

            val track = AudioTrack.Builder()

                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            AudioAttributes.USAGE_MEDIA
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_MUSIC
                        )
                        .build()
                )

                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                        .setSampleRate(
                            SAMPLE_RATE
                        )
                        .setChannelMask(
                            AudioFormat.CHANNEL_OUT_STEREO
                        )
                        .build()
                )

                .setBufferSizeInBytes(
                    bufferSizeBytes
                )

                .setTransferMode(
                    AudioTrack.MODE_STATIC
                )

                .build()

            audioTrack = track

            if (track.state != AudioTrack.STATE_INITIALIZED) {

                Log.e(
                    TAG,
                    "AudioTrack no pudo inicializarse."
                )

                return
            }

            // -------------------------------------------------
            // ESCRITURA COMPLETA DEL BUFFER
            // -------------------------------------------------

            var writtenSamples = 0

            while (writtenSamples < stereoBuffer.size) {

                val remaining = (
                    stereoBuffer.size - writtenSamples
                    )

                val written = track.write(
                    stereoBuffer,
                    writtenSamples,
                    remaining
                )

                if (written <= 0) {

                    throw IllegalStateException(
                        "AudioTrack.write devolvió $written"
                    )
                }

                writtenSamples += written
            }

            // -------------------------------------------------
            // INICIO DE REPRODUCCIÓN
            // -------------------------------------------------

            track.play()

            val totalFrames = (
                stereoBuffer.size / CHANNELS
                ).toLong()

            // Esperar a que el cabezal consuma todos los frames.
            while (
                track.playState == AudioTrack.PLAYSTATE_PLAYING &&
                track.playbackHeadPosition.toLong() < totalFrames
            ) {

                Thread.sleep(5L)
            }

        } catch (exception: InterruptedException) {

            Thread.currentThread().interrupt()

            Log.w(
                TAG,
                "Reproducción interrumpida",
                exception
            )

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Error reproduciendo audio DSP",
                exception
            )

        } finally {

            audioTrack?.let { track ->

                try {

                    if (
                        track.playState == AudioTrack.PLAYSTATE_PLAYING
                    ) {
                        track.stop()
                    }

                } catch (_: Exception) {
                    // Evita que un error al detener impida liberar.
                }

                try {

                    track.release()

                } catch (_: Exception) {
                    // Liberación defensiva.
                }
            }
        }
    }
}
