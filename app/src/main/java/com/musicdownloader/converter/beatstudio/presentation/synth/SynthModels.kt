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
 * attackMs: Tiempo que tarda el sonido en alcanzar su nivel máximo.
 * decayMs: Tiempo que tarda en bajar desde el máximo hasta Sustain.
 * sustainLevel: Nivel de volumen sostenido entre 0.0 y 1.0.
 * releaseMs: Tiempo que tarda el sonido en apagarse después de Note Off.
 */
data class ADSRState(
    val attackMs: Float = 15f,
    val decayMs: Float = 200f,
    val sustainLevel: Float = 0.7f,
    val releaseMs: Float = 300f
)

/**
 * Destinos posibles de modulación para el LFO en la Matriz Expandida.
 */
enum class ModulationDestination {
    CUTOFF,
    PITCH,
    PAN
}

/**
 * Estado del LFO (Oscilador de Baja Frecuencia).
 *
 * rateHz: Velocidad de oscilación en Hertz (ej. modulación rítmica).
 * depth: Profundidad o intensidad con la que afecta al parámetro destino.
 * destination: Destino de modulación activo (Cutoff, Pitch o Pan).
 */
data class LFOState(
    val rateHz: Float = 2.0f,
    val depth: Float = 0.3f,
    val destination: ModulationDestination = ModulationDestination.CUTOFF
)

/**
 * Estado del Rack de Efectos (FX Rack).
 *
 * reverbMix: Mezcla de reverberación / espacio (0.0 a 1.0).
 * delayMix: Mezcla de retardo / eco rítmico (0.0 a 1.0).
 * distortionDrive: Saturación o distorsión cálida (0.0 a 1.0).
 */
data class FXState(
    val reverbMix: Float = 0.0f,
    val delayMix: Float = 0.0f,
    val distortionDrive: Float = 0.0f
)

/**
 * Parche de síntesis.
 *
 * Contiene la configuración completa del instrumento:
 * - Tipo de instrumento
 * - Forma de onda
 * - Envolvente ADSR
 * - Parámetros de LFO (con matriz de modulación expandida)
 * - Frecuencia de corte y resonancia del filtro
 * - Tiempo de Portamento (Glide) en milisegundos
 * - Cantidad de voces Unison (apilamiento SuperSaw)
 * - Intensidad del microdesfase de afinación (Detune)
 * - Rack de efectos nativos (FX Rack: Reverb, Delay, Distorsión)
 */
data class SynthPatch(

    val instrumentType: SynthInstrumentType =
        SynthInstrumentType.SYNTH_808,

    val waveform: WaveformType =
        SynthInstrumentType.SYNTH_808.defaultWaveform,

    val adsr: ADSRState =
        ADSRState(),

    val lfo: LFOState =
        LFOState(),

    /**
     * Frecuencia de corte del filtro Low-Pass en Hz.
     */
    val cutoffFreq: Float = 5000f,

    /**
     * Factor Q del filtro (Resonancia).
     */
    val resonance: Float = 0.707f,

    /**
     * Tiempo de Portamento (Glide) en milisegundos.
     * Define la velocidad de deslizamiento de tono entre notas consecutivas.
     */
    val portamentoMs: Float = 0f,

    /**
     * Cantidad de voces simultáneas para el efecto Unison / SuperSaw (1 a 5).
     */
    val unisonVoices: Int = 1,

    /**
     * Intensidad del microdesfase de afinación (Detune) entre 0.0 y 1.0.
     */
    val detuneAmount: Float = 0.0f,

    /**
     * Configuración del Rack de Efectos Nativos (Reverb, Delay, Distorsión).
     */
    val fx: FXState = FXState()
)
