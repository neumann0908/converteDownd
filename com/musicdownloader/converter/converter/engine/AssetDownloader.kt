package com.musicdownloader.converter.converter.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

object AssetDownloader {
    private const val TAG = "AssetDownloader"
    
    // 🌐 URL directa de tu archivo .zip en GitHub Releases (Asegúrate de que coincida con tu release v1.0.0)
    private const val GITHUB_ZIP_URL = "https://github.com/neumann09/converteDownd/releases/download/v1.0.0/kits.zip"

    suspend fun downloadAndUnzipIfNeeded(context: Context) {
        withContext(Dispatchers.IO) {
            val kitsDir = File(context.filesDir, "kits")
            
            // 1. Verificamos si los kits ya existen y no están vacíos
            if (kitsDir.exists() && kitsDir.listFiles()?.isNotEmpty() == true) {
                Log.i(TAG, "✅ Los kits de audio ya se encuentran en la memoria interna. Omitiendo descarga.")
                return@withContext
            }

            Log.i(TAG, "🚀 Iniciando descarga de los 671MB de kits desde GitHub Releases...")
            val zipFile = File(context.cacheDir, "kits.zip")

            try {
                // 2. Descarga del archivo comprimido mediante HTTP
                val url = URL(GITHUB_ZIP_URL)
                val connection = url.openConnection()
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val outputStream = FileOutputStream(zipFile)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.close()
                inputStream.close()
                
                Log.i(TAG, "📦 Descarga finalizada con éxito. Procediendo a descomprimir...")

                // 3. Descompresión quirúrgica dentro de filesDir/kits
                unzip(zipFile, kitsDir)
                
                // 4. Limpieza del archivo ZIP temporal de la caché
                if (zipFile.exists()) {
                    zipFile.delete()
                }
                
                Log.i(TAG, "🎉 ¡Kits de audio instalados y listos para Oboe en: ${kitsDir.absolutePath}!")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error crítico durante la descarga o descompresión de los kits: ${e.message}", e)
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
