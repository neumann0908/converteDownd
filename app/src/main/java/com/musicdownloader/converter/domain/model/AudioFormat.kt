package com.musicdownloader.converter.domain.model

enum class AudioFormat(val extension: String, val mimeType: String) {
    MP3("mp3", "audio/mpeg"),
    WAV("wav", "audio/wav"),
    AAC("aac", "audio/aac"),
    M4A("m4a", "audio/mp4"),
    FLAC("flac", "audio/flac"),
    OGG("ogg", "audio/ogg")
}
