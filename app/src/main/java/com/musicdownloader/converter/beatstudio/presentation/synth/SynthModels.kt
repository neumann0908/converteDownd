package com.musicdownloader.converter.beatstudio.presentation.synth

// 🎹 Catálogo de los 10 instrumentos recomendados basados en nuestras referencias
enum class SynthInstrumentType(val displayName: String, val defaultWaveform: WaveformType) {
    SYNTH_808("808 Synth", WaveformType.SINE),
    SUB_BASS("Sub Bass", WaveformType.SINE),
    ANALOG_BASS("Analog Bass", WaveformType.SAW),
    ELECTRIC_PIANO("Electric Piano", WaveformType.TRIANGLE),
    KEYS("Keys", WaveformType.SQUARE),
    PAD_SYNTH("Pad Synth", WaveformType.SAW),
    LEAD_SYNTH("Lead Synth", WaveformType.SAW),
    BELL_SYNTH("Bell Synth", WaveformType.SINE),
    STRINGS("Strings", WaveformType.SAW),
    PLUCKS("Plucks", WaveformType.SQUARE)
}

// 🌊 Las formas de onda matemática que crean el sonido básico
enum class WaveformType {
    SINE,      // Sonido suave y puro (como un silbido)
    SAW,       // Sonido brillante y con fuerza (ideal para bajos y leads)
    SQUARE,    // Sonido hueco y retro (tipo videojuego antiguo)
    TRIANGLE   // Sonido cálido y dulce
}

// 🎚️ El sistema ADSR (Controla cómo sube, se mantiene y se apaga el sonido con el tiempo)
data class ADSRState(
    val attackMs: Float = 10f,    // Tiempo que tarda en abrirse el sonido al tocar
    val decayMs: Float = 100f,    // Tiempo que tarda en bajar al nivel de sustain
    val sustainLevel: Float = 0.7f, // Nivel en el que se queda mientras mantienes presionado
    val releaseMs: Float = 200f   // Tiempo que tarda en apagarse por completo al soltar
)

// 🎛️ El "Parche" o configuración actual del sintetizador
data class SynthPatch(
    val instrumentType: SynthInstrumentType,
    val waveform: WaveformType = instrumentType.defaultWaveform,
    val cutoffFreq: Float = 2000f,
    val resonance: Float = 1.0f,
    val adsr: ADSRState = ADSRState(),
    val isPolyphonic: Boolean = true // Permite tocar varios dedos a la vez (acordes)
)
