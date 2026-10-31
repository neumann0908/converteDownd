package com.musicdownloader.converter.beatstudio.presentation.sequencer

import com.musicdownloader.converter.beatstudio.presentation.midi.MusicalScaleType

// 🟢 Estructura de Patrón independiente para el DAW (A, B, C)
data class SequencerPattern(
    val id: String,
    val name: String,
    val tracks: List<SequencerTrack>
)

// Representa un cuadrito (paso) individual con soporte de polifonía para acordes, duración, pitch bend y modulación
data class SequencerStep(
    val id: Int,
    val isActive: Boolean = false,
    val velocity: Float = 1.0f,     // Qué tan fuerte suena el golpe (Edición de Velocity)
    val pitch: Int = 0,           // Altura de la nota principal / Nota raíz para el Piano Roll
    val chordPitches: List<Int> = emptyList(), // 🎹 Notas armónicas adicionales del acorde (polifonía real por paso)
    val durationMs: Long = 125L,  // Duración exacta en milisegundos que suena la nota
    val offsetMs: Long = 0L,      // Desplazamiento temporal (Shift/Offset) ligero de la posición
    val probability: Float = 1.0f, // Probabilidad de disparo (0.0 a 1.0 / 0% a 100% de que suene el paso)
    val pitchBend: Float = 0.0f,    // 🎚️ Pitch Bend (-1.0 a 1.0) para modificar la afinación de la nota
    val modulation: Float = 0.0f    // 🎛️ Modulation (0.0 a 1.0) para efectos de vibrato y modulación
)

// Representa una pista de sonido con soporte de Volumen, Paneo Estéreo, Mute, Solo y Swing
data class SequencerTrack(
    val trackId: Int,
    val trackName: String,
    val steps: List<SequencerStep> = List(16) { SequencerStep(it) }, // 16 pasos por compás por defecto
    val volume: Float = 1.0f,      // 🟢 Volumen general de la pista
    val pan: Float = 0.0f,         // 🟢 Paneo estéreo (-1.0 Izquierda a 1.0 Derecha)
    val isMuted: Boolean = false,  // 🟢 Silenciar pista
    val isSolo: Boolean = false,   // 🟢 Aislar pista (Solo)
    val customPitchSamples: Map<Int, String> = emptyMap(), // Mapea Pitch con la ruta del archivo .wav
    val trackSwing: Float = 0.0f,          // Parámetro de Swing individual por pista (0.0 a 1.0)
    val humanizeEnabled: Boolean = false   // Control de humanización orgánica de velocidad por pista
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
    val swing: Float = 0.0f,             // Parámetro de Swing Global del Groove
    val isPlaying: Boolean = false,      // Si está sonando o detenido
    val currentStepIndex: Int = -1,      // El paso que va parpadeando mientras suena
    val isMetronomeEnabled: Boolean = false, // Estado de activación del metrónomo
    val arrangement: List<SongSection> = listOf(
        SongSection("1", SongSectionType.INTRO, "Intro", repeatCount = 1),
        SongSection("2", SongSectionType.VERSO, "Verso 1", repeatCount = 2),
        SongSection("3", SongSectionType.ESTRIBILLO, "Estribillo", repeatCount = 2)
    ),
    val currentSectionIndex: Int = 0,     // Índice de la sección del arreglo actual en reproducción
    val currentLoopInverses: Int = 0,     // Contador de bucles completados dentro de la sección actual
    val isPianoRollMode: Boolean = false, // Indica si la vista activa es el Piano Roll
    val selectedPianoTrackIndex: Int = 3,  // Pista seleccionada para editar en el Piano Roll
    
    // --- 🎛️ CAMPOS DE PATRONES INDEPENDIENTES ---
    val currentPatternIndex: Int = 0,
    val patterns: List<SequencerPattern> = listOf(
        SequencerPattern("A", "Patrón A", createDefaultTracks()),
        SequencerPattern("B", "Patrón B", createDefaultTracks()),
        SequencerPattern("C", "Patrón C", createDefaultTracks())
    ),

    // --- 🎹 CAMPOS DEL MOTOR MIDI (MOTOR 4) ---
    val selectedScale: MusicalScaleType = MusicalScaleType.MAJOR, // Escala musical activa por defecto
    val rootKey: Int = 60 // Nota raíz base por defecto (C4 / Do central)
) {
    // 🟢 Propiedad calculada para mantener compatibilidad absoluta con la UI y el reproductor
    val tracks: List<SequencerTrack>
        get() = patterns.getOrNull(currentPatternIndex)?.tracks ?: patterns.first().tracks
}

// 🟢 Función auxiliar para inicializar las pistas por defecto en cada patrón
fun createDefaultTracks(): List<SequencerTrack> {
    return listOf(
        SequencerTrack(0, "Kick"),
        SequencerTrack(1, "Snare"),
        SequencerTrack(2, "HiHat"),
        SequencerTrack(3, "SynthBass")
    )
}
