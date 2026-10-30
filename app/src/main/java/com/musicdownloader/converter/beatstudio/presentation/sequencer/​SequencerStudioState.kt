package com.musicdownloader.converter.beatstudio.presentation.sequencer

// Representa un cuadrito (paso) individual en el secuenciador o piano roll
data class SequencerStep(
    val id: Int,
    val isActive: Boolean = false,
    val velocity: Float = 1.0f, // Qué tan fuerte suena el golpe (Edición de Velocity)
    val pitch: Int = 0,       // Altura de la nota musical para el Piano Roll
    val durationMs: Long = 125L // Cuánto dura sonando la nota
)

// Representa una pista de sonido (por ejemplo: el bombo, la caja, un sintetizador)
data class SequencerTrack(
    val trackId: Int,
    val trackName: String,
    val steps: List<SequencerStep> = List(16) { SequencerStep(it) }, // 16 pasos por compás por defecto
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val customPitchSamples: Map<Int, String> = emptyMap() // 🟢 Mapea Pitch (ej. 0 para C4) con la ruta del archivo .wav asignado
)

// Las partes de nuestra canción para construir un arreglo profesional
enum class SongSectionType {
    INTRO, VERSO, ESTRIBILLO, PUENTE, OUTRO
}

data class SongSection(
    val id: String,
    val type: SongSectionType,
    val name: String,
    val repeatCount: Int = 2
)

// El estado gigante que guarda todo lo que ocurre en el Secuenciador Pro
data class SequencerStudioState(
    val bpm: Float = 120.0f,             // Velocidad del ritmo (Tempo)
    val timeSignature: String = "4/4",   // Compás (4/4, 3/4, 6/8, etc.)
    val resolution: String = "1/16",     // Resolución de las notas (1/4, 1/8, 1/16, 1/32)
    val swing: Float = 0.0f,             // Ritmo con "groove" o toque humano
    val isPlaying: Boolean = false,      // Si está sonando o detenido
    val currentStepIndex: Int = -1,      // El paso que va parpadeando mientras suena
    val isMetronomeEnabled: Boolean = false, // Estado de activación del metrónomo
    val tracks: List<SequencerTrack> = listOf(
        SequencerTrack(0, "Kick"),
        SequencerTrack(1, "Snare"),
        SequencerTrack(2, "HiHat"),
        SequencerTrack(3, "SynthBass") // Pista adaptada para melodías en el Piano Roll
    ),
    val arrangement: List<SongSection> = listOf(
        SongSection("1", SongSectionType.INTRO, "Intro", repeatCount = 1),
        SongSection("2", SongSectionType.VERSO, "Verso 1", repeatCount = 2),
        SongSection("3", SongSectionType.ESTRIBILLO, "Estribillo", repeatCount = 2)
    ),
    val currentSectionIndex: Int = 0,     // Índice de la sección del arreglo actual en reproducción
    val currentLoopInverses: Int = 0,     // Contador de bucles completados dentro de la sección actual
    val isPianoRollMode: Boolean = false, // Indica si la vista activa es el Piano Roll
    val selectedPianoTrackIndex: Int = 3  // Pista seleccionada para editar en el Piano Roll
)
