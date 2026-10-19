package com.musicdownloader.converter.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString("KEY_URL") ?: return@withContext Result.failure()
        
        try {
            val downloadsDir = File(applicationContext.filesDir, "downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val request = YoutubeDLRequest(url).apply {
                addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
                addOption("-o", "${downloadsDir.absolutePath}/%(title)s.%(ext)s")
            }

            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, line ->
                // Progreso de descarga opcional
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
