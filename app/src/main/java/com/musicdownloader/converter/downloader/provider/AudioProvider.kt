package com.musicdownloader.converter.downloader.provider

import com.musicdownloader.converter.core.common.Result
import com.musicdownloader.converter.domain.model.AudioMetadata

interface AudioProvider {
    val name: String
    fun canHandle(url: String): Boolean
    suspend fun resolveMetadata(url: String): Result<AudioMetadata>
}
