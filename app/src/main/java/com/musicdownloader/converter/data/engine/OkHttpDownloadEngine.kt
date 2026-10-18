package com.musicdownloader.converter.data.engine

import com.musicdownloader.converter.core.common.AppError
import com.musicdownloader.converter.core.network.SsrfValidator
import com.musicdownloader.converter.downloader.engine.DownloadEngine
import com.musicdownloader.converter.downloader.engine.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class OkHttpDownloadEngine(
    private val client: OkHttpClient = OkHttpClient()
) : DownloadEngine {

    override fun download(url: String, destinationFile: File): Flow<DownloadStatus> = flow {
        if (!SsrfValidator.isSafeUrl(url)) {
            emit(DownloadStatus.Failed(IllegalArgumentException("URL bloqueada por políticas de seguridad SSRF")))
            return@flow
        }

        val request = Request.Builder().url(url).build()
        
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadStatus.Failed(Exception("Error HTTP: ${response.code}")))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadStatus.Failed(Exception("Cuerpo de respuesta vacío")))
                return@flow
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            val buffer = ByteArray(8192)
            
            destinationFile.parentFile?.mkdirs()

            body.byteStream().use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    var bytes = inputStream.read(buffer)
                    val startTime = System.currentTimeMillis()

                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        downloadedBytes += bytes
                        
                        val elapsedTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                        val speed = (downloadedBytes * 1000) / elapsedTime

                        emit(DownloadStatus.Progress(downloadedBytes, totalBytes, speed))
                        bytes = inputStream.read(buffer)
                    }
                    outputStream.flush()
                }
            }
            emit(DownloadStatus.Completed(destinationFile))
        } catch (e: Exception) {
            emit(DownloadStatus.Failed(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun pauseDownload(downloadId: String) {
        // Implementación de control de pausa en capas superiores de WorkManager
    }

    override suspend fun resumeDownload(downloadId: String) {
        // Implementación de reanudación
    }

    override suspend fun cancelDownload(downloadId: String) {
        // Implementación de cancelación
    }
}
