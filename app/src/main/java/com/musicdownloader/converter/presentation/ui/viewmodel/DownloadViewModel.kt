package com.musicdownloader.converter.presentation.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.work.*
import com.musicdownloader.converter.data.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    data class Success(val message: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    fun startDownload(url: String) {
        if (url.isBlank()) {
            _downloadState.value = DownloadState.Error("Ingresa una URL válida")
            return
        }

        _downloadState.value = DownloadState.Downloading

        val workData = workDataOf(
            "KEY_URL" to url,
            "KEY_FILE_NAME" to "downloaded_audio_${System.currentTimeMillis()}.mp3"
        )

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(downloadRequest)

        WorkManager.getInstance(context)
            .getWorkInfoByIdLiveData(downloadRequest.id)
            .observeForever { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _downloadState.value = DownloadState.Success("¡Audio descargado en la carpeta Downloads!")
                        }
                        WorkInfo.State.FAILED -> {
                            val errorMsg = workInfo.outputData.getString("ERROR_MSG") ?: "Fallo la descarga"
                            _downloadState.value = DownloadState.Error(errorMsg)
                        }
                        else -> {}
                    }
                }
            }
    }
}
