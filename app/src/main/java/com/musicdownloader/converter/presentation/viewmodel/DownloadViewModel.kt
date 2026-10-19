package com.musicdownloader.converter.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.musicdownloader.converter.data.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DownloadUiState {
    object Idle : DownloadUiState
    object Loading : DownloadUiState
    object Success : DownloadUiState
    data class Error(val message: String) : DownloadUiState
}

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    fun startDownload(url: String) {
        _uiState.value = DownloadUiState.Loading

        val workManager = WorkManager.getInstance(context)
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("URL" to url))
            .build()

        workManager.enqueue(workRequest)

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _uiState.value = DownloadUiState.Success
                        }
                        WorkInfo.State.FAILED -> {
                            val errorMsg = workInfo.outputData.getString("ERROR_MSG") ?: "Fallo la descarga"
                            _uiState.value = DownloadUiState.Error(errorMsg)
                        }
                        WorkInfo.State.CANCELLED -> {
                            _uiState.value = DownloadUiState.Error("Descarga cancelada")
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
