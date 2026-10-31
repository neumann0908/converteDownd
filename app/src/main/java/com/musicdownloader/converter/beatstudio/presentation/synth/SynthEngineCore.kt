
package com.musicdownloader.converter.beatstudio.presentation.synth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * SynthEngineCore v2
 *
 * Generador DSP de audio.
 *
 * Conserva la interfaz playTone() de la versión anterior.
 *
 * IMPORTANTE:
 * Esta versión utiliza AudioTrack como salida temporal.
 * No sustituye todavía al Audio Runtime central basado en Oboe.
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

    /**
     * Executor limitado.
     *
     * Evita crear un Thread ilimitado por cada nota.
     * Permite hasta MAX_VOICES reproducciones concurrentes.
     *
     * Esta solución es transitoria hasta integrar el Audio Runtime.
     */
    private val audioExecutor = Executors.newFixedThreadPool(
        MAX_VOICES,
        ThreadFactory { runnable ->
            Thread(runnable, "SynthVoice").apply {
                isDaemon = true
            }
        }
    )

    // ---------------------------------------------------------
    // 2. API COMPATIBLE CON LA VERSIÓN ANTERIOR
    // ---------------------------------------------------------

    /**
     * Reproduce una nota generada mediante DSP.
     *
     * frequency: frecuencia fundamental en Hz.
     * durationMs: duración total de la nota en milisegundos.
     * patch: configuración del instrumento.
     * volume: volumen entre 0 y 1.
     * pan: posición estéreo entre -1 y 1.
     */
    fun playTone(
        frequency: Float,
        durationMs: Long,
        patch: SynthPatch,
        volume: Float,
        pan: Float
    ) {
        val safeFrequency = frequency.coerceIn(
            MIN_FREQUENCY,
            MAX_FREQUENCY
        )

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
                    "Error reproduciendo nota",
                    exception
                )
            }
        }
    }

    // ---------------------------------------------------------
    // 3. GENERACIÓN PRINCIPAL
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

        val attackSamples = millisecondsToSamples(
            patch.adsr.attackMs
        )

        val decaySamples = millisecondsToSamples(
            patch.adsr.decayMs
        )

        val releaseSamples = millisecondsToSamples(
            patch.adsr.releaseMs
        )

        val sustainLevel = patch.adsr.sustainLevel.coerceIn(
            0.0f,
            1.0f
        )

        // El Release ocurre al final de la duración solicitada.
        val releaseStart = max(
            0,
            totalSamples - releaseSamples
        )

        // Calculamos el nivel de la envolvente al comenzar Release.
        val releaseInitialLevel = calculateAttackDecaySustain(
            sampleIndex = releaseStart,
            attackSamples = attackSamples,
            decaySamples = decaySamples,
            sustainLevel = sustainLevel
        )

        // Filtro independiente para esta voz.
        val filter = LowPassFilter(
            sampleRate = SAMPLE_RATE.toFloat(),
            cutoffFrequency = patch.cutoffFreq
        )

        // Estado de oscilador: la fase se acumula muestra a muestra.
        var phase = 0.0

        val baseFrequency = frequency.coerceIn(
            MIN_FREQUENCY,
            MAX_FREQUENCY
        )

        // Paneo de potencia constante.
        val panAngle = ((pan + 1.0f) * 0.25f) * PI.toFloat()

        val leftPan = cos(panAngle)
        val rightPan = sin(panAngle)

        for (i in 0 until totalSamples) {

            val time = i.toFloat() / SAMPLE_RATE

            // ---------------------------------------------
            // A. ENVOLVENTE DE TONO PARA 808
            // ---------------------------------------------

            val currentFrequency = calculatePitch(
                baseFrequency = baseFrequency,
                instrumentType = patch.instrumentType,
                sampleIndex = i
            )

            val safeCurrentFrequency = currentFrequency.coerceIn(
                MIN_FREQUENCY,
                MAX_FREQUENCY
            )

            val phaseIncrement = (
                safeCurrentFrequency / SAMPLE_RATE
            ).coerceIn(0.0f, 0.49f).toDouble()

            // ---------------------------------------------
            // B. GENERADOR DE ONDAS
            // ---------------------------------------------

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

            // ---------------------------------------------
            // C. FILTRO LOW-PASS
            // ---------------------------------------------

            val filteredSample = filter.process(rawSample)

            // ---------------------------------------------
            // D. ENVOLVENTE ADSR
            // ---------------------------------------------

            val envelope = calculateEnvelope(
                sampleIndex = i,
                attackSamples = attackSamples,
                decaySamples = decaySamples,
                sustainLevel = sustainLevel,
                releaseStart = releaseStart,
                releaseSamples = releaseSamples,
                releaseInitialLevel = releaseInitialLevel
            )

            // ---------------------------------------------
            // E. AMPLITUD Y PROTECCIÓN
            // ---------------------------------------------

            val amplitude = (
                filteredSample *
                envelope *
                volume *
                MASTER_HEADROOM
            )

            // Evitamos clipping digital mediante soft clipping.
            val protectedSample = softClip(amplitude)

            // ---------------------------------------------
            // F. PANEAMIENTO ESTÉREO
            // ---------------------------------------------

            val leftSample = protectedSample * leftPan
            val rightSample = protectedSample * rightPan

            stereoBuffer[i * 2] = floatToPcm(leftSample)
            stereoBuffer[i * 2 + 1] = floatToPcm(rightSample)
        }

        return stereoBuffer
    }

    // ---------------------------------------------------------
    // 4. GENERADOR DE OSCILADORES
    // ---------------------------------------------------------

    private fun generateWaveform(
        waveform: WaveformType,
        phase: Double,
        phaseIncrement: Double,
        frequency: Float,
        instrumentType: SynthInstrumentType
    ): Float {

        return when (waveform) {

            // -----------------------------------------
            // SINE
            // -----------------------------------------

            WaveformType.SINE -> {

                val fundamental = sin(
                    2.0 * PI * phase
                ).toFloat()

                if (instrumentType == SynthInstrumentType.SUB_BASS) {

                    val sub = sin(
                        PI * phase
                    ).toFloat()

                    fundamental * 0.75f + sub * 0.25f

                } else {
                    fundamental
                }
            }

            // -----------------------------------------
            // SAW CON POLYBLEP
            // -----------------------------------------

            WaveformType.SAW -> {

                val dt = phaseIncrement.coerceIn(0.000001, 0.49)

                var sample = (2.0 * phase - 1.0).toFloat()

                sample -= polyBlep(phase, dt).toFloat()

                sample
            }

            // -----------------------------------------
            // SQUARE CON POLYBLEP
            // -----------------------------------------

            WaveformType.SQUARE -> {

                val dt = phaseIncrement.coerceIn(0.000001, 0.49)

                var sample = if (phase < 0.5) {
                    1.0f
                } else {
                    -1.0f
                }

                sample += polyBlep(phase, dt).toFloat()

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

            // -----------------------------------------
            // TRIANGLE BAND-LIMITED POR ARMÓNICOS
            // -----------------------------------------

            WaveformType.TRIANGLE -> {

                generateTriangle(
                    phase = phase,
                    frequency = frequency
                )
            }
        }
    }

    /**
     * Corrección PolyBLEP.
     *
     * Reduce el aliasing de las discontinuidades
     * de Saw y Square.
     */
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

    /**
     * Triángulo mediante serie de Fourier truncada.
     *
     * Solo se calculan armónicos inferiores a Nyquist.
     */
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
                ) 1.0 else -1.0

                val amplitude = sign / (
                    harmonic.toDouble() * harmonic
                )

                output += amplitude * sin(
                    2.0 * PI * harmonic * phase
                )
            }

            harmonic += 2
        }

        // Normalización aproximada de la serie.
        return (output * 8.0 / (PI * PI)).toFloat()
    }

    // ---------------------------------------------------------
    // 5. ENVOLVENTE DE TONO 808
    // ---------------------------------------------------------

    private fun calculatePitch(
        baseFrequency: Float,
        instrumentType: SynthInstrumentType,
        sampleIndex: Int
    ): Float {

        if (instrumentType != SynthInstrumentType.SYNTH_808) {
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

        // Caída suave y exponencial.
        val decay = (1.0f - progress)

        return baseFrequency + 80.0f * decay * decay
    }

    // ---------------------------------------------------------
    // 6. ENVOLVENTE ADSR
    // ---------------------------------------------------------

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

            if (releaseSamples <= 0) {
                return 0.0f
            }

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
    // 7. FILTRO LOW-PASS RESONANTE
    // ---------------------------------------------------------

    /**
     * Filtro biquad Low-Pass.
     *
     * Utiliza una respuesta Butterworth aproximada.
     * Q = 0.707.
     *
     * Resonancia configurable será añadida en la
     * siguiente etapa sin romper SynthPatch.
     */
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

            val safeCutoff = cutoffFrequency.coerceIn(
                20.0f,
                sampleRate * 0.45f
            )

            val omega = (
                2.0 * PI * safeCutoff / sampleRate
            )

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

            val output = (
                b0 * input +
                b1 * x1 +
                b2 * x2 -
                a1 * y1 -
                a2 * y2
            )

            x2 = x1
            x1 = input.toDouble()

            y2 = y1
            y1 = output

            return output.toFloat()
        }
    }

    // ---------------------------------------------------------
    // 8. SOFT CLIPPING
    // ---------------------------------------------------------

    /**
     * Protección suave contra amplitudes excesivas.
     *
     * No sustituye a un limiter profesional de mastering.
     */
    private fun softClip(input: Float): Float {

        val safeInput = input.coerceIn(-4.0f, 4.0f)

        return tanh(safeInput).toFloat()
    }

    // ---------------------------------------------------------
    // 9. CONVERSIÓN PCM
    // ---------------------------------------------------------

    private fun floatToPcm(input: Float): Short {

        val safe = input.coerceIn(-1.0f, 1.0f)

        return (
            safe * 32767.0f
        ).toInt().coerceIn(-32768, 32767).toShort()
    }

    private fun millisecondsToSamples(
        milliseconds: Number
    ): Int {

        val safeMs = milliseconds.toDouble().coerceAtLeast(0.0)

        return (
            safeMs * SAMPLE_RATE / 1000.0
        ).toInt().coerceAtMost(Int.MAX_VALUE)
    }

    // ---------------------------------------------------------
    // 10. REPRODUCCIÓN MEDIANTE AUDIOTRACK
    // ---------------------------------------------------------

    private fun playAudioData(
        stereoBuffer: ShortArray
    ) {

        if (stereoBuffer.isEmpty()) return

        var audioTrack: AudioTrack? = null

        try {

            val bufferSizeBytes = (
                stereoBuffer.size * Short.SIZE_BYTES
            )

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
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(
                            AudioFormat.CHANNEL_OUT_STEREO
                        )
                        .build()
                )

                .setBufferSizeInBytes(bufferSizeBytes)

                .setTransferMode(
                    AudioTrack.MODE_STATIC
                )

                .build()

            audioTrack = track

            if (
                track.state != AudioTrack.STATE_INITIALIZED
            ) {
                throw IllegalStateException(
                    "AudioTrack no pudo inicializarse"
                )
            }

            val written = track.write(
                stereoBuffer,
                0,
                stereoBuffer.size
            )

            if (written < 0) {
                throw IllegalStateException(
                    "Error escribiendo PCM: $written"
                )
            }

            if (written != stereoBuffer.size) {
                throw IllegalStateException(
                    "Escritura PCM incompleta: $written"
                )
            }

            // El paneo ya está incorporado al buffer.
            // No aplicar setStereoVolume nuevamente.
            track.play()

            val durationMs = (
                stereoBuffer.size.toLong() /
                CHANNELS * 1000L / SAMPLE_RATE
            )

            Thread.sleep(durationMs + 30L)

        } finally {

            audioTrack?.let { track ->

                try {
                    if (
                        track.playState == AudioTrack.PLAYSTATE_PLAYING
                    ) {
                        track.stop()
                    }
                } catch (_: Exception) {
                    // El recurso se libera igualmente.
                }

                try {
                    track.release()
                } catch (_: Exception) {
                    // Evita bloquear la liberación.
                }
            }
        }
    }
}
