package com.musicdownloader.converter.data.worker

import android.content.Context
import android.os.Environment
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
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString("KEY_URL") ?: return@withContext Result.failure()
        val isAudioOnly = inputData.getBoolean("KEY_AUDIO_ONLY", true)

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val request = YoutubeDLRequest(url)
            
            if (isAudioOnly) {
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
            } else {
                request.addOption("-f", "bestvideo+bestaudio/best")
            }
            
            request.addOption("-o", "${downloadsDir.absolutePath}/%(title)s.%(ext)s")

            YoutubeDL.getInstance().execute(request) { _, _, _ -> }

            Result.success(workDataOf("FILE_URI" to downloadsDir.absolutePath))
        } catch (e: Exception) {
            Result.failure(workDataOf("ERROR_MSG" to (e.message ?: "Error al procesar el enlace")))
        }
    }
}
