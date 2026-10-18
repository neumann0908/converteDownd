package com.musicdownloader.converter.domain.model

data class AudioMetadata(
    val title: String,
    val artist: String = "Desconocido",
    val album: String = "Desconocido",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val format: AudioFormat,
    val mimeType: String,
    val downloadUrl: String,
    val thumbnailUrl: String? = null
)
