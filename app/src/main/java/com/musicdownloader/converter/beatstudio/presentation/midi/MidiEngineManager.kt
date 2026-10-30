package com.musicdownloader.converter.beatstudio.presentation.midi

object MidiEngineManager {

    // 🎸 Constructor automático de acordes: le das una nota raíz y te devuelve el acorde completo
    fun buildChordNotes(rootPitch: Int, chordType: ChordType, startTick: Long, durationTicks: Long): List<MidiNote> {
        return chordType.intervals.mapIndexed { index, interval ->
            MidiNote(
                id = "chord_${System.currentTimeMillis()}_$index",
                pitch = rootPitch + interval,
                startTick = startTick,
                durationTicks = durationTicks
            )
        }
    }

    // 🔍 Validador de escalas: te avisa si una nota encaja en la escala musical elegida
    fun isNoteInScale(pitch: Int, rootKey: Int, scaleType: MusicalScaleType): Boolean {
        val normalizedPitch = (pitch - rootKey) % 12
        val absoluteNormalized = if (normalizedPitch < 0) normalizedPitch + 12 else normalizedPitch
        return scaleType.intervals.contains(absoluteNormalized)
    }

    // 🧲 El Imán de Ritmo (Cuantizador): endereza las notas a la raya más cercana
    fun quantizeTick(currentTick: Long, resolution: QuantizeResolution): Long {
        val grid = resolution.ticksPerStep
        val remainder = currentTick % grid
        return if (remainder < grid / 2) {
            currentTick - remainder
        } else {
            currentTick + (grid - remainder)
        }
    }
}
