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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString("KEY_URL") ?: return@withContext Result.failure()
        val fileName = inputData.getString("KEY_FILE_NAME") ?: "audio_${System.currentTimeMillis()}.mp3"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(workDataOf("ERROR_MSG" to "Error HTTP: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure()
            val inputStream: InputStream = body.byteStream()
            
            val savedUri = saveFileToDownloads(appContext, inputStream, fileName)
            if (savedUri != null) {
                Result.success(workDataOf("FILE_URI" to savedUri.toString()))
            } else {
                Result.failure(workDataOf("ERROR_MSG" to "No se pudo guardar el archivo"))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf("ERROR_MSG" to (e.message ?: "Error desconocido")))
        }
    }

    private fun saveFileToDownloads(context: Context, inputStream: InputStream, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.also {
                resolver.openOutputStream(it)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Uri.fromFile(file)
        }
    }
}
