package com.musicdownloader.converter.converter.engine

import com.musicdownloader.converter.domain.model.AudioFormat
import kotlinx.coroutines.flow.Flow
import java.io.File

data class ConversionConfig(
    val targetFormat: AudioFormat,
    val bitrateKbps: Int = 192,
    val sampleRateHz: Int = 44100,
    val channels: Int = 2
)

sealed interface ConversionStatus {
    data class Progress(val percentage: Float) : ConversionStatus
    data class Completed(val outputFile: File) : ConversionStatus
    data class Failed(val error: Throwable) : ConversionStatus
}

interface AudioConverterEngine {
    fun convert(
        inputFile: File,
        outputFile: File,
        config: ConversionConfig
    ): Flow<ConversionStatus>
}
