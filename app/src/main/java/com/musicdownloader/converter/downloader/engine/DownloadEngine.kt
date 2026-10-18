package com.musicdownloader.converter.downloader.engine

import kotlinx.coroutines.flow.Flow
import java.io.File

sealed interface DownloadStatus {
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long, val speedBytesPerSec: Long) : DownloadStatus
    data class Completed(val outputFile: File) : DownloadStatus
    data class Failed(val error: Throwable) : DownloadStatus
    object Paused : DownloadStatus
}

interface DownloadEngine {
    fun download(url: String, destinationFile: File): Flow<DownloadStatus>
    suspend fun pauseDownload(downloadId: String)
    suspend fun resumeDownload(downloadId: String)
    suspend fun cancelDownload(downloadId: String)
}
