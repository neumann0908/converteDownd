package com.musicdownloader.converter.converter.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

object AssetDownloader {
    private const val TAG = "AssetDownloader"
    
    // URL exacta al archivo .zip en las Releases de tu repositorio de GitHub
    private const val GITHUB_ZIP_URL = "https://github.com/neumann0908/converteDownd/releases/download/v1.0.0/kits.zip"

    // Estados reactivos para gobernar la barra de progreso en la UI
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f) // Varía de 0.0 a 1.0
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    suspend fun downloadAndUnzipIfNeeded(context: Context) {
        withContext(Dispatchers.IO) {
            val kitsDir = File(context.filesDir, "kits")
            
            // Verificamos si los kits ya existen y no están vacíos
            if (kitsDir.exists() && kitsDir.listFiles()?.isNotEmpty() == true) {
                Log.i(TAG, "✅ Los kits de audio ya se encuentran en la memoria interna. Omitiendo descarga.")
                return@withContext
            }

            Log.i(TAG, "🚀 Iniciando descarga de los 671MB desde GitHub Releases...")
            _isDownloading.value = true
            _downloadProgress.value = 0f

            val zipFile = File(context.cacheDir, "kits.zip")

            try {
                val url = URL(GITHUB_ZIP_URL)
                val connection = url.openConnection()
                connection.connect()
                
                val contentLength = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(zipFile)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        // Calculamos el progreso exacto en porcentaje
                        _downloadProgress.value = totalBytesRead.toFloat() / contentLength.toFloat()
                    }
                }
                outputStream.close()
                inputStream.close()
                
                Log.i(TAG, "📦 Descarga finalizada con éxito. Procediendo a descomprimir...")
                unzip(zipFile, kitsDir)
                
                if (zipFile.exists()) {
                    zipFile.delete()
                }
                
                Log.i(TAG, "🎉 ¡Kits de audio instalados y listos en ${kitsDir.absolutePath}!")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error crítico durante la descarga o descompresión: ${e.message}", e)
            } finally {
                _isDownloading.value = false
            }
        }
    }

    private fun unzip(zipFile: File, targetDirectory: File) {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val newFile = File(targetDirectory, zipEntry.name)
                if (zipEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
        }
    }
}
