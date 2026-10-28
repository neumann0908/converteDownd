package com.musicdownloader.converter.beatstudio.presentation.drum

data class DrumTrack(
    val id: String,
    val name: String,
    val sampleResName: String,
    val colorHex: Long = 0xFFFFD700,
    val steps: List<Boolean> = List(16) { false }, // 16, 32 o 64 casillas de ritmo[span_11](start_span)[span_11](end_span)
    val velocities: List<Float> = List(16) { 0.8f }, // Velocity (fuerza) por cada golpe[span_12](start_span)[span_12](end_span)[span_13](start_span)[span_13](end_span)
    val probabilities: List<Float> = List(16) { 1.0f }, // Probabilidad de disparo (0.0 a 1.0)[span_14](start_span)[span_14](end_span)
    val accents: List<Boolean> = List(16) { false }, // Acentos por paso[span_15](start_span)[span_15](end_span)
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    // 🎛️ Función diferencial profunda por pad[span_16](start_span)[span_16](end_span)
    val pitch: Float = 1.0f,
    val pan: Float = 0.0f,
    val attack: Float = 0.01f,  // Envolvente ADSR: Ataque[span_17](start_span)[span_17](end_span)
    val decay: Float = 0.3f,   // Envolvente ADSR: Caída[span_18](start_span)[span_18](end_span)
    val sustain: Float = 0.5f, // Envolvente ADSR: Sostenimiento[span_19](start_span)[span_19](end_span)
    val release: Float = 0.2f, // Envolvente ADSR: Liberación[span_20](start_span)[span_20](end_span)
    val reverb: Float = 0.0f,  // Efecto independiente: Reverb[span_21](start_span)[span_21](end_span)
    val delay: Float = 0.0f    // Efecto independiente: Delay[span_22](start_span)[span_22](end_span)
)

data class DrumStudioState(
    val isPlaying: Boolean = false,
    val bpm: Int = 120,
    val currentStep: Int = 0,
    val selectedTrackIndex: Int = 0,
    val stepCount: Int = 16,        // Soporte dinámico para 16, 32 o 64 pads[span_23](start_span)[span_23](end_span)
    val swing: Float = 0.0f,        // Groove: Swing global[span_24](start_span)[span_24](end_span)[span_25](start_span)[span_25](end_span)
    val humanization: Float = 0.0f, // Groove: Humanización de tiempo y volumen[span_26](start_span)[span_26](end_span)
    val currentKit: String = "Kit Standard 808", // Kits de batería intercambiables[span_27](start_span)[span_27](end_span)[span_28](start_span)[span_28](end_span)
    val tracks: List<DrumTrack> = listOf(
        DrumTrack(id = "kick", name = "KICK 808", sampleResName = "kick_808", colorHex = 0xFFFF4500),
        DrumTrack(id = "snare", name = "SNARE PRO", sampleResName = "snare_pro", colorHex = 0xFFFFD700),
        DrumTrack(id = "hihat_cl", name = "CLOSED HAT", sampleResName = "hihat_closed", colorHex = 0xFF00E676),
        DrumTrack(id = "hihat_op", name = "OPEN HAT", sampleResName = "hihat_open", colorHex = 0xFF00E5FF),
        DrumTrack(id = "clap", name = "CLAP TRAP", sampleResName = "clap_trap", colorHex = 0xFFE040FB),
        DrumTrack(id = "perc", name = "PERC/PERCUSSION", sampleResName = "perc_1", colorHex = 0xFFFFAB00),
        DrumTrack(id = "toms", name = "TOMS", sampleResName = "toms_drum", colorHex = 0xFFE91E63),
        DrumTrack(id = "rimshot", name = "RIMSHOT", sampleResName = "rimshot", colorHex = 0xFF009688),
        DrumTrack(id = "shaker", name = "SHAKER", sampleResName = "shaker", colorHex = 0xFF8BC34A)
    )
)
