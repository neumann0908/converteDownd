package com.musicdownloader.converter.data.worker

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString("KEY_URL") ?: return@withContext Result.failure()
        val isAudioOnly = inputData.getBoolean("KEY_AUDIO_ONLY", true)

        try {
            // Directorio interno seguro para evitar bloqueos de Android 10+
            val cacheDir = File(appContext.cacheDir, "ytdl_downloads")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val request = YoutubeDLRequest(url)
            
            if (isAudioOnly) {
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
            } else {
                request.addOption("-f", "bestvideo+bestaudio/best")
            }
            
            // Guardar temporalmente en caché interno
            val outputTemplate = File(cacheDir, "%(title)s.%(ext)s").absolutePath
            request.addOption("-o", outputTemplate)

            // Ejecutar extracción con yt-dlp
            YoutubeDL.getInstance().execute(request) { _, _, _ -> }

            // Buscar el archivo descargado en el caché interno
            val downloadedFile = cacheDir.listFiles()?.maxByOrNull { it.lastModified() }
                ?: return@withContext Result.failure(workDataOf("ERROR_MSG" to "No se encontró el archivo descargado"))

            // Mover el archivo al almacenamiento público mediante MediaStore
            val savedUri = saveFileToPublicStorage(appContext, downloadedFile)

            // Limpiar caché temporal
            downloadedFile.delete()

            if (savedUri != null) {
                Result.success(workDataOf("FILE_URI" to savedUri.toString()))
            } else {
                Result.failure(workDataOf("ERROR_MSG" to "No se pudo guardar el archivo en el dispositivo"))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf("ERROR_MSG" to (e.message ?: "Error al procesar el enlace")))
        }
    }

    private fun saveFileToPublicStorage(context: Context, file: File): Uri? {
        val fileName = file.name
        val mimeType = if (fileName.endsWith(".mp3", true)) "audio/mpeg" else "video/mp4"
        val directory = if (mimeType.startsWith("audio")) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
            }
            val resolver = context.contentResolver
            val contentUri = if (mimeType.startsWith("audio")) {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            
            val uri = resolver.insert(contentUri, contentValues)
            uri?.also {
                resolver.openOutputStream(it)?.use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } else {
            val targetDir = Environment.getExternalStoragePublicDirectory(directory)
            if (!targetDir.exists()) targetDir.mkdirs()
            val destFile = File(targetDir, fileName)
            file.copyTo(destFile, overwrite = true)
            Uri.fromFile(destFile)
        }
    }
}
