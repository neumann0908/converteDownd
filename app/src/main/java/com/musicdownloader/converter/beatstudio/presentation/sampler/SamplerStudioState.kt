package com.musicdownloader.converter.beatstudio.presentation.sampler

data class AudioSlice(
    val id: Int,
    val startSample: Long,
    val endSample: Long,
    val triggerProbability: Float = 1.0f,
    val isReversed: Boolean = false,
    val pitchShift: Float = 1.0f,     // Tono independiente[span_14](start_span)[span_14](end_span)
    val volume: Float = 1.0f,         // Ajuste de volumen[span_15](start_span)[span_15](end_span)
    val pan: Float = 0.0f
)

data class SamplerTrack(
    val sampleName: String = "Ninguna muestra cargada",
    val filePath: String? = null,
    val totalDurationMs: Long = 0L,
    val detectedBpm: Float = 120.0f,  // Detección de BPM[span_16](start_span)[span_16](end_span)
    val isLooping: Boolean = false,   // Loop[span_17](start_span)[span_17](end_span)
    val isNormalized: Boolean = false, // Normalización[span_18](start_span)[span_18](end_span)
    val fadeInMs: Long = 0L,          // Fade in[span_19](start_span)[span_19](end_span)
    val fadeOutMs: Long = 0L,         // Fade out[span_20](start_span)[span_20](end_span)
    val startPoint: Long = 0L,        // Ajuste de punto de inicio[span_21](start_span)[span_21](end_span)
    val endPoint: Long = 0L,          // Ajuste de punto final[span_22](start_span)[span_22](end_span)
    // Envolvente ADSR[span_23](start_span)[span_23](end_span)
    val attack: Float = 0.01f,
    val decay: Float = 0.2f,
    val sustain: Float = 0.8f,
    val release: Float = 0.1f,
    // Auto Chop Inteligente y Chops manuales[span_24](start_span)[span_24](end_span)[span_25](start_span)[span_25](end_span)[span_26](start_span)[span_26](end_span)
    val slices: List<AudioSlice> = emptyList(),
    val activeSliceIndex: Int = 0,
    val autoChopMode: AutoChopType = AutoChopType.TRANSIENTS
)

enum class AutoChopType {
    TRANSIENTS, // Basado en transitorios[span_27](start_span)[span_27](end_span)
    SILENCES,   // Basado en silencios[span_28](start_span)[span_28](end_span)
    ENERGY,     // Cambios de energía[span_29](start_span)[span_29](end_span)
    MEASURES    // Compases[span_30](start_span)[span_30](end_span)
}

data class SamplerStudioState(
    val isRecording: Boolean = false,    // Grabación desde el micrófono[span_31](start_span)[span_31](end_span)
    val isPlayingSample: Boolean = false,
    val currentTrack: SamplerTrack = SamplerTrack(),
    val supportedFormats: List<String> = listOf("WAV", "MP3", "FLAC") // Formatos compatibles[span_32](start_span)[span_32](end_span)
)
