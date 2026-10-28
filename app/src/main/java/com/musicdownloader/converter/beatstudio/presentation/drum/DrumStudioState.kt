package com.musicdownloader.converter.beatstudio.presentation.drum

data class DrumTrack(
    val id: String,
    val name: String,
    val sampleResName: String,
    val colorHex: Long = 0xFFFFD700,
    val steps: List<Boolean> = List(16) { false }, // 16 casillas para tocar el ritmo
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false
)

data class DrumStudioState(
    val isPlaying: Boolean = false,
    val bpm: Int = 120,
    val currentStep: Int = 0,
    val selectedTrackIndex: Int = 0,
    val tracks: List<DrumTrack> = listOf(
        DrumTrack(id = "kick", name = "KICK 808", sampleResName = "kick_808", colorHex = 0xFFFF4500),
        DrumTrack(id = "snare", name = "SNARE PRO", sampleResName = "snare_pro", colorHex = 0xFFFFD700),
        DrumTrack(id = "hihat_cl", name = "CLOSED HAT", sampleResName = "hihat_closed", colorHex = 0xFF00E676),
        DrumTrack(id = "hihat_op", name = "OPEN HAT", sampleResName = "hihat_open", colorHex = 0xFF00E5FF),
        DrumTrack(id = "clap", name = "CLAP TRAP", sampleResName = "clap_trap", colorHex = 0xFFE040FB),
        DrumTrack(id = "perc", name = "PERC/PERCUSSION", sampleResName = "perc_1", colorHex = 0xFFFFAB00)
    )
)
