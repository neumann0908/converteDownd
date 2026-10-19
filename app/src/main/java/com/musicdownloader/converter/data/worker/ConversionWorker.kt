package com.musicdownloader.converter.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class ConversionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val inputPath = inputData.getString("KEY_INPUT") ?: ""
        val outputPath = inputData.getString("KEY_OUTPUT") ?: ""
        
        if (inputPath.isEmpty() || outputPath.isEmpty()) {
            return@withContext Result.failure()
        }
        
        Result.success()
    }
}
