package com.musicdownloader.converter.beatstudio.presentation.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicdownloader.converter.util.AudioTrimmer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isMonitorLiveOn: Boolean = false,
    val isArmRecordOn: Boolean = true,
    val isMetronomeOn: Boolean = false,
    val currentBpm: Int = 120,
    val currentBeat: Int = 1,
    val currentMeterLevel: Float = 0.0f,
    val recordedTakesCount: Int = 0,
    val pendingTrimFile: File? = null
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState = _uiState.asStateFlow()

    private var rawAudioFile: File? = null
    private var wavOutputFile: File? = null

    fun toggleMonitorLive(enabled: Boolean) {
        _uiState.update { it.copy(isMonitorLiveOn = enabled) }
    }

    fun toggleArmRecord(enabled: Boolean) {
        _uiState.update { it.copy(isArmRecordOn = enabled) }
    }

    // --- Control de BPM y Metrónomo ---
    fun updateBpm(newBpm: Int) {
        val clampedBpm = newBpm.coerceIn(40, 240)
        RecordingEngineCore.currentBpm = clampedBpm
        _uiState.update { it.copy(currentBpm = clampedBpm) }
    }

    fun toggleMetronome(enabled: Boolean) {
        _uiState.update { it.copy(isMetronomeOn = enabled) }
        if (enabled) {
            RecordingEngineCore.startMetronome { beat ->
                viewModelScope.launch {
                    _uiState.update { it.copy(currentBeat = beat) }
                }
            }
        } else {
            RecordingEngineCore.stopMetronome()
        }
    }

    fun startRecordingSession(title: String) {
        _uiState.update { it.copy(isArmRecordOn = true, pendingTrimFile = null) }

        val cacheDir = context.cacheDir
        rawAudioFile = File(cacheDir, "raw_recording_${System.currentTimeMillis()}.pcm")

        rawAudioFile?.let { file ->
            _uiState.update { it.copy(isRecording = true) }
            
            if (!_uiState.value.isMetronomeOn) {
                toggleMetronome(true)
            }

            RecordingEngineCore.startRecording(file) { rms ->
                viewModelScope.launch {
                    _uiState.update { it.copy(currentMeterLevel = rms) }
                }
            }
        }
    }

    fun stopRecordingSession(customTitle: String = "Toma_Voz") {
        RecordingEngineCore.stopRecording()
        RecordingEngineCore.stopMetronome()

        viewModelScope.launch(Dispatchers.IO) {
            rawAudioFile?.let { raw ->
                if (raw.exists()) {
                    val exportDir = File(context.filesDir, "exports")
                    if (!exportDir.exists()) {
                        exportDir.mkdirs()
                    }

                    val fileName = "${customTitle}_${System.currentTimeMillis()}.wav"
                    wavOutputFile = File(exportDir, fileName)

                    wavOutputFile?.let { wav ->
                        rawToWav(raw, wav, 44100, 2, 16)
                    }

                    raw.delete()

                    withContext(Dispatchers.Main) {
                        _uiState.update { 
                            it.copy(
                                isRecording = false,
                                isMetronomeOn = false,
                                currentMeterLevel = 0.0f,
                                recordedTakesCount = it.recordedTakesCount + 1,
                                pendingTrimFile = wavOutputFile
                            ) 
                        }
                    }
                }
            }
        }
    }

    // --- FUNCIÓN DE RECORTAR TOMA (TRIMMING) ---
    fun applyTrimToPending(startSec: Int, endSec: Int, onFinished: (Boolean) -> Unit) {
        val fileToTrim = _uiState.value.pendingTrimFile ?: return
        if (startSec >= endSec) {
            onFinished(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parentDir = fileToTrim.parentFile ?: File(context.filesDir, "exports")
                val trimmedFile = File(parentDir, "Trimmed_${fileToTrim.name}")

                AudioTrimmer.trim(
                    srcPath = fileToTrim.absolutePath,
                    dstPath = trimmedFile.absolutePath,
                    startMs = startSec * 1000L,
                    endMs = endSec * 1000L
                )

                fileToTrim.delete()
                trimmedFile.renameTo(fileToTrim)

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(pendingTrimFile = null) }
                    onFinished(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onFinished(false)
                }
            }
        }
    }

    fun dismissTrim() {
        _uiState.update { it.copy(pendingTrimFile = null) }
    }

    private fun rawToWav(rawFile: File, wavFile: File, sampleRate: Int, channels: Int, bitRate: Int) {
        val rawData = ByteArray(rawFile.length().toInt())
        FileInputStream(rawFile).use { it.read(rawData) }

        FileOutputStream(wavFile).use { fos ->
            val totalDataLen = rawData.size + 36
            val byteRate = sampleRate * channels * (bitRate / 8)

            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(totalDataLen), 0, 4)
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16), 0, 4)
            fos.write(shortToByteArray(1), 0, 2)
            fos.write(shortToByteArray(channels.toShort()), 0, 2)
            fos.write(intToByteArray(sampleRate), 0, 4)
            fos.write(intToByteArray(byteRate), 0, 4)
            fos.write(shortToByteArray((channels * (bitRate / 8)).toShort()), 0, 2)
            fos.write(shortToByteArray(bitRate.toShort()), 0, 2)
            fos.write("data".toByteArray())
            fos.write(intToByteArray(rawData.size), 0, 4)
            fos.write(rawData)
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteArray(4) { (value shr (it * 8)).toByte() }
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteArray(2) { (value.toInt() shr (it * 8)).toByte() }
    }
}
