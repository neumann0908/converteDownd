
package com.musicdownloader.converter.beatstudio.presentation.synth

/**
 * Formas de onda disponibles para el oscilador.
 *
 * SINE: onda sinusoidal, sonido suave.
 * SAW: onda diente de sierra, sonido brillante.
 * SQUARE: onda cuadrada, sonido rico en armónicos.
 * TRIANGLE: onda triangular, sonido más suave que SAW.
 */
enum class WaveformType {
    SINE,
    SAW,
    SQUARE,
    TRIANGLE
}

/**
 * Catálogo de instrumentos del Synth Studio.
 *
 * displayName:
 * Nombre que puede mostrar la interfaz.
 *
 * defaultWaveform:
 * Forma de onda recomendada al crear un parche nuevo
 * con este instrumento.
 *
 * Nota:
 * El usuario puede cambiar la forma de onda posteriormente.
 */
enum class SynthInstrumentType(
    val displayName: String,
    val defaultWaveform: WaveformType
) {

    SYNTH_808(
        "808 Bass",
        WaveformType.SINE
    ),

    SUB_BASS(
        "Sub Bass",
        WaveformType.SINE
    ),

    LEAD_SAW(
        "Lead Saw",
        WaveformType.SAW
    ),

    SQUARE_PLUCK(
        "Square Pluck",
        WaveformType.SQUARE
    ),

    PAD_WARM(
        "Warm Pad",
        WaveformType.TRIANGLE
    ),

    BRASS_SYNTH(
        "Brass Synth",
        WaveformType.SAW
    ),

    CHIP_TUNE(
        "ChipTune",
        WaveformType.SQUARE
    ),

    PLUCK_BELL(
        "Bell Pluck",
        WaveformType.SINE
    ),

    ORGAN_CLASSIC(
        "Classic Organ",
        WaveformType.SQUARE
    ),

    ACID_BASS(
        "Acid Bass",
        WaveformType.SAW
    )
}

/**
 * Estado de la envolvente ADSR.
 *
 * attackMs:
 * Tiempo que tarda el sonido en alcanzar su nivel máximo.
 *
 * decayMs:
 * Tiempo que tarda en bajar desde el máximo hasta Sustain.
 *
 * sustainLevel:
 * Nivel de volumen sostenido entre 0.0 y 1.0.
 *
 * releaseMs:
 * Tiempo que tarda el sonido en apagarse después
 * de recibir Note Off.
 *
 * Importante:
 * El SynthEngineCore actual utiliza una duración fija
 * por nota. El Note Off real se implementará en una
 * etapa posterior.
 */
data class ADSRState(

    val attackMs: Float = 15f,

    val decayMs: Float = 200f,

    val sustainLevel: Float = 0.7f,

    val releaseMs: Float = 300f
)

/**
 * Parche de síntesis.
 *
 * Contiene la configuración de un instrumento:
 * - Tipo de instrumento.
 * - Forma de onda.
 * - Envolvente ADSR.
 * - Frecuencia de corte.
 * - Resonancia del filtro.
 *
 * Los nombres y tipos de propiedades se conservan
 * para mantener compatibilidad con SynthEngineCore
 * y con las llamadas existentes desde la interfaz.
 */
data class SynthPatch(

    val instrumentType: SynthInstrumentType =
        SynthInstrumentType.SYNTH_808,

    val waveform: WaveformType =
        SynthInstrumentType.SYNTH_808.defaultWaveform,

    val adsr: ADSRState =
        ADSRState(),

    /**
     * Frecuencia de corte del filtro Low-Pass.
     * Unidad: Hz.
     */
    val cutoffFreq: Float = 5000f,

    /**
     * Factor Q del filtro.
     *
     * 0.707 aproximadamente:
     * Respuesta Butterworth sin un pico pronunciado
     * cerca de la frecuencia de corte.
     *
     * Valores superiores:
     * Mayor resonancia alrededor de la frecuencia
     * de corte.
     *
     * El motor limita el valor efectivo a un rango
     * seguro antes de calcular los coeficientes.
     */
    val resonance: Float = 0.707f
)
