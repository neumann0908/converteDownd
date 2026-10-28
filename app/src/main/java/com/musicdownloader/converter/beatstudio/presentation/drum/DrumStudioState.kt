package com.musicdownloader.converter.beatstudio.presentation.drum

data class DrumTrack(
    val id: String,
    val name: String,
    val sampleResName: String,
    val colorHex: Long = 0xFFFFD700,
    val steps: List<Boolean> = List(16) { false }, // 16 casillas para tocar el ritmo
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val pitch: Float = 1.0f,      // Función diferencial: Tono independiente por pad[span_3](start_span)[span_3](end_span)
    val pan: Float = 0.0f         // Función diferencial: Panorama Izq/Der por pad[span_4](start_span)[span_4](end_span)
)

data class DrumStudioState(
    val isPlaying: Boolean = false,
    val bpm: Int = 120,
    val currentStep: Int = 0,
    val selectedTrackIndex: Int = 0,
    val swing: Float = 0.0f,      // Porcentaje de Swing global para el groove[span_5](start_span)[span_5](end_span)
    val tracks: List<DrumTrack> = listOf(
        DrumTrack(id = "kick", name = "KICK 808", sampleResName = "kick_808", colorHex = 0xFFFF4500),
        DrumTrack(id = "snare", name = "SNARE PRO", sampleResName = "snare_pro", colorHex = 0xFFFFD700),
        DrumTrack(id = "hihat_cl", name = "CLOSED HAT", sampleResName = "hihat_closed", colorHex = 0xFF00E676),
        DrumTrack(id = "hihat_op", name = "OPEN HAT", sampleResName = "hihat_open", colorHex = 0xFF00E5FF),
        DrumTrack(id = "clap", name = "CLAP TRAP", sampleResName = "clap_trap", colorHex = 0xFFE040FB),
        DrumTrack(id = "perc", name = "PERC/PERCUSSION", sampleResName = "perc_1", colorHex = 0xFFFFAB00),
        // 🎛️ Nuevos instrumentos incorporados de las referencias profesionales:
        DrumTrack(id = "toms", name = "TOMS", sampleResName = "toms_drum", colorHex = 0xFFE91E63),
        DrumTrack(id = "rimshot", name = "RIMSHOT", sampleResName = "rimshot", colorHex = 0xFF009688),
        DrumTrack(id = "shaker", name = "SHAKER", sampleResName = "shaker", colorHex = 0xFF8BC34A)
    )
)
