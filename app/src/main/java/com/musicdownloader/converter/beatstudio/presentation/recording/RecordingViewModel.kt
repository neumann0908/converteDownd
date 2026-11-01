package com.musicdownloader.converter.beatstudio.presentation.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isMonitorLiveOn: Boolean = false,
    val isArmRecordOn: Boolean = true,
    val currentMeterLevel: Float = 0.0f,
    val recordedTakesCount: Int = 0
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState = _uiState.asStateFlow()

    private var activeFile: File? = null

    fun toggleMonitorLive(enabled: Boolean) {
        _uiState.update { it.copy(isMonitorLiveOn = enabled) }
    }

    fun toggleArmRecord(enabled: Boolean) {
        _uiState.update { it.copy(isArmRecordOn = enabled) }
    }

    fun startRecordingSession(title: String) {
        if (!_uiState.value.isArmRecordOn) return

        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) recordingsDir.mkdirs()

        activeFile = File(recordingsDir, "${title}_take_${System.currentTimeMillis()}.pcm")

        activeFile?.let { file ->
            _uiState.update { it.copy(isRecording = true) }
            RecordingEngineCore.startRecording(file) { rms ->
                viewModelScope.launch {
                    _uiState.update { it.copy(currentMeterLevel = rms) }
                }
            }
        }
    }

    fun stopRecordingSession() {
        RecordingEngineCore.stopRecording()
        _uiState.update { 
            it.copy(
                isRecording = false,
                currentMeterLevel = 0.0f,
                recordedTakesCount = it.recordedTakesCount + 1
            ) 
        }
    }
}
