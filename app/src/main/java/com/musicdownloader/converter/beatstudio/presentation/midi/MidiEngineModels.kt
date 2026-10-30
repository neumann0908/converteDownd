package com.musicdownloader.converter.beatstudio.presentation.midi

// 🎹 La Ficha de Nota Musical Inteligente
data class MidiNote(
    val id: String,
    val pitch: Int,             // El número de la tecla del piano (ej. 60 es el Do central / C4)
    val startTick: Long,        // En qué momento exacto del tiempo arranca
    val durationTicks: Long,    // Cuánto tiempo dura sonando
    val velocity: Float = 1.0f, // Qué tan fuerte suena (de 0.1 a 1.0)
    val probability: Float = 1.0f // Probabilidad de que suene (para efectos modernos)
)

// 🎶 Las Escalas Musicales (Grupos de notas que siempre se llevan bien)
enum class MusicalScaleType(val displayName: String, val intervals: List<Int>) {
    MAJOR("Escala Mayor (Feliz)", listOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR_NATURAL("Menor Natural (Triste/Épica)", listOf(0, 2, 3, 5, 7, 8, 10)),
    PENTATONIC_MINOR("Pentatónica Menor (Blues/Rock)", listOf(0, 3, 5, 7, 10)),
    BLUES("Escala de Blues", listOf(0, 3, 5, 6, 7, 10))
}

// 🎸 Los Acordes Mágicos (Tocar varias notas al mismo tiempo con un toque)
enum class ChordType(val displayName: String, val intervals: List<Int>) {
    TRIAD_MAJOR("Tríada Mayor", listOf(0, 4, 7)),
    TRIAD_MINOR("Tríada Menor", listOf(0, 3, 7)),
    SEVENTH_MAJOR("Séptima Mayor (Maj7)", listOf(0, 4, 7, 11)),
    SEVENTH_MINOR("Séptima Menor (m7)", listOf(0, 3, 7, 10))
}

// ⏱️ Las medidas del tiempo para el Imán de Ritmo (Quantize)
enum class QuantizeResolution(val label: String, val ticksPerStep: Long) {
    Q_1_4("1/4", 16L),
    Q_1_8("1/8", 8L),
    Q_1_16("1/16", 4L),
    Q_1_32("1/32", 2L)
}
