package com.musicdownloader.converter.beatstudio.presentation.synth

// Formas de onda disponibles para el oscilador
enum class WaveformType {
    SINE,
    SAW,
    SQUARE,
    TRIANGLE
}

// Catálogo oficial de los 10 instrumentos recomendados para el DAW
enum class SynthInstrumentType(val displayName: String, val defaultWaveform: WaveformType) {
    SYNTH_808("808 Bass", WaveformType.SINE),
    SUB_BASS("Sub Bass", WaveformType.SINE),
    LEAD_SAW("Lead Saw", WaveformType.SAW),
    SQUARE_PLUCK("Square Pluck", WaveformType.SQUARE),
    PAD_WARM("Warm Pad", WaveformType.TRIANGLE),
    BRASS_SYNTH("Brass Synth", WaveformType.SAW),
    CHIP_TUNE("ChipTune", WaveformType.SQUARE),
    PLUCK_BELL("Bell Pluck", WaveformType.SINE),
    ORGAN_CLASSIC("Classic Organ", WaveformType.SQUARE),
    ACID_BASS("Acid Bass", WaveformType.SAW)
}

// Configuración de la Envolvente ADSR (Attack, Decay, Sustain, Release)
data class ADSRState(
    val attackMs: Float = 15f,
    val decayMs: Float = 200f,
    val sustainLevel: Float = 0.7f,
    val releaseMs: Float = 300f
)

// Parche de síntesis activa (Metadata y parámetros editables desde la UI)
data class SynthPatch(
    val instrumentType: SynthInstrumentType = SynthInstrumentType.SYNTH_808,
    val waveform: WaveformType = SynthInstrumentType.SYNTH_808.defaultWaveform,
    val adsr: ADSRState = ADSRState(),
    val cutoffFreq: Float = 5000f,
    val resonance: Float = 0.707f
)
