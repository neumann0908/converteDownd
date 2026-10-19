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

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val url = inputData.getString("URL") ?: return@withContext Result.failure(
                workDataOf("ERROR_MSG" to "URL no proporcionada")
            )

            try {
                val downloadDir = File(applicationContext.getExternalFilesDir(null), "MusicDownloads")
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val request = YoutubeDLRequest(url).apply {
                    addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")
                    addOption("-f", "ba/b")
                    addOption("--extract-audio")
                    addOption("--audio-format", "mp3")
                    
                    // Forzar el uso del cliente web para evitar errores de precondición en Android
                    addOption("--extractor-args", "youtube:player_client=web")
                    addOption("--no-check-certificate")
                }

                YoutubeDL.getInstance().execute(request)
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                val errorDetails = e.localizedMessage ?: e.message ?: "Error desconocido en YoutubeDL"
                Result.failure(workDataOf("ERROR_MSG" to errorDetails))
            }
        }
    }
}
