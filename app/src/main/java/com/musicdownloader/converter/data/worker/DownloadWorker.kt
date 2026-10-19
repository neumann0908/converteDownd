package com.musicdownloader.converter.data.worker

import android.content.Context
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

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override async suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString("URL") ?: return@withContext Result.failure(
            workDataOf("ERROR_MSG" to "URL no proporcionada")
        )

        return@withContext try {
            val downloadDir = File(applicationContext.getExternalFilesDir(null), "MusicDownloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val request = YoutubeDLRequest(url).apply {
                addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")
                addOption("-f", "bestaudio/best")
                addOption("--extract-audio")
                addOption("--audio-format", "mp3")
            }

            // Ejecución nativa en hilo de I/O
            YoutubeDL.getInstance().execute(request)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Retorna el mensaje exacto de la excepción en lugar de fallo genérico
            val errorDetails = e.localizedMessage ?: e.message ?: "Error desconocido en YoutubeDL"
            Result.failure(workDataOf("ERROR_MSG" to errorDetails))
        }
    }
}
