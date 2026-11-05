package com.musicdownloader.converter.beatstudio.presentation.master

enum class ExportFormat(val extension: String, val displayName: String) {
    WAV("wav", "WAV (Sin pérdida)"),
    MP3("mp3", "MP3 (Comprimido)"),
    FLAC("flac", "FLAC (Alta fidelidad)")
}

enum class SampleRate(val hz: Int, val displayName: String) {
    SR_44100(44100, "44.1 kHz (CD Standard)"),
    SR_48000(48000, "48.0 kHz (Professional / Video)"),
    SR_96000(96000, "96.0 kHz (High-Res Audio)")
}

enum class BitDepth(val bits: Int, val displayName: String) {
    BIT_16(16, "16-bit"),
    BIT_24(24, "24-bit"),
    BIT_32(32, "32-bit Float")
}

data class MasteringDiagnostics(
    val lufs: Float = -14.2f,
    val truePeakDb: Float = -0.3f,
    val hasClipping: Boolean = false,
    val hasExcessBass: Boolean = false,
    val hasStereoImbalance: Boolean = false,
    val isLevelTooHigh: Boolean = false,
    val analysisMessage: String = "Análisis óptimo para streaming profesional."
)

data class MasterUiState(
    val selectedFormat: ExportFormat = ExportFormat.WAV,
    val selectedSampleRate: SampleRate = SampleRate.SR_44100,
    val selectedBitDepth: BitDepth = BitDepth.BIT_24,
    val isNormalizationEnabled: Boolean = true,
    val targetLufs: Float = -14.0f,
    val isLimiterEnabled: Boolean = true,
    val isStemExportEnabled: Boolean = false,
    val diagnostics: MasteringDiagnostics = MasteringDiagnostics(),
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f
)
